package com.github.legregor.microservice.coverage.analysis;

import com.github.legregor.microservice.coverage.analysis.model.EndpointTrace;
import com.github.legregor.microservice.coverage.analysis.model.StaticServiceTraceResult;
import com.github.legregor.microservice.coverage.analysis.model.TraceTarget;
import com.github.legregor.microservice.coverage.analysis.trace.StaticServiceTraceAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StaticServiceTraceAnalyzerTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesRecursiveHelperCallsToDownstreamRestTemplateExchange() throws IOException {
        Path serviceRoot = createMicroservice("orders", Map.of(
                "src/main/java/com/example/orders/OrderController.java", """
                        package com.example.orders;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/orders")
                        public class OrderController {
                            private final OrderService orderService = new OrderService();

                            @GetMapping("/summary")
                            public String summary() {
                                return orderService.loadSummary();
                            }
                        }
                        """,
                "src/main/java/com/example/orders/OrderService.java",
                """
                        package com.example.orders;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class OrderService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String loadSummary() {
                                return fetchSummary();
                            }

                            public String fetchSummary() {
                                return restTemplate.exchange("http://inventory-service/api/items", HttpMethod.GET, null, String.class).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        assertEquals(1, result.getEndpointTraces().size());
        EndpointTrace endpointTrace = result.getEndpointTraces().get(0);
        assertEquals("orders", endpointTrace.getSourceService());
        assertEquals("GET", endpointTrace.getHttpMethod());
        assertEquals("/orders/summary", endpointTrace.getPath());
        assertEquals("String", endpointTrace.getReturnType());
        assertEquals(1, endpointTrace.getDownstreamTargets().size());

        TraceTarget traceTarget = endpointTrace.getDownstreamTargets().iterator().next();
        assertEquals("GET", traceTarget.getHttpMethod());
        assertEquals("http://inventory-service/api/items", traceTarget.getUrl());
        assertEquals("inventory-service", traceTarget.getDestinationService());
        assertEquals("String", traceTarget.getReturnType());

        assertTrue(result.getMethodTraceCache().values().stream()
                .anyMatch(methodTraceResult -> methodTraceResult.getCalledMethodKeys().stream()
                        .anyMatch(called -> called.contains("fetchSummary"))));
    }

    @Test
    void resolvesRestTemplateUrlsBuiltFromMethodParameters() throws IOException {
        Path serviceRoot = createMicroservice("cancel", Map.of(
                "src/main/java/com/example/cancel/CancelController.java", """
                        package com.example.cancel;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/cancel")
                        public class CancelController {
                            private final CancelService cancelService = new CancelService();

                            @GetMapping("/drawback/{userId}/{money}")
                            public String drawback(@PathVariable String userId, @PathVariable String money) {
                                return cancelService.drawbackMoney(userId, money);
                            }
                        }
                        """,
                "src/main/java/com/example/cancel/CancelService.java",
                """
                        package com.example.cancel;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class CancelService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String drawbackMoney(String userId, String money) {
                                return restTemplate.exchange(
                                        "http://payments-service/api/drawback/" + userId + "/" + money,
                                        HttpMethod.GET,
                                        null,
                                        String.class
                                ).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        assertEquals(1, result.getEndpointTraces().size());
        EndpointTrace endpointTrace = result.getEndpointTraces().get(0);
        assertEquals(1, endpointTrace.getDownstreamTargets().size());

        TraceTarget traceTarget = endpointTrace.getDownstreamTargets().iterator().next();
        assertEquals("GET", traceTarget.getHttpMethod());
        assertEquals("http://payments-service/api/drawback/{userId}/{money}", traceTarget.getUrl());
        assertEquals("payments-service", traceTarget.getDestinationService());
    }

    /**
     * TrainTicket's seat service builds URLs from getters and from local variables assigned from getters
     * ({@code "/routes/" + trainNumber}, {@code "/train_types/" + seatRequest.getTrainNumber()}); such calls were
     * dropped before, which hid their connections.
     */
    @Test
    void resolvesRuntimeUrlPartsAsPathPlaceholders() throws IOException {
        Path serviceRoot = createMicroservice("seat", Map.of(
                "src/main/java/com/example/seat/SeatController.java", """
                        package com.example.seat;

                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestBody;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/seats")
                        public class SeatController {
                            private final SeatService seatService = new SeatService();

                            @PostMapping("/distribute")
                            public String distribute(@RequestBody Seat seat) {
                                return seatService.distribute(seat);
                            }
                        }
                        """,
                "src/main/java/com/example/seat/Seat.java", """
                        package com.example.seat;

                        public class Seat {
                            private String trainNumber;

                            public String getTrainNumber() {
                                return trainNumber;
                            }
                        }
                        """,
                "src/main/java/com/example/seat/SeatService.java", """
                        package com.example.seat;

                        import org.springframework.beans.factory.annotation.Value;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class SeatService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @Value("${ts.travel.service.url:travel-service}")
                            private String travelHost;

                            public String distribute(Seat seat) {
                                String trainNumber = seat.getTrainNumber();
                                restTemplate.exchange("http://" + travelHost + "/api/routes/" + trainNumber,
                                        HttpMethod.GET, null, String.class);
                                return restTemplate.exchange(
                                        "http://" + travelHost + "/api/train_types/" + seat.getTrainNumber(),
                                        HttpMethod.GET, null, String.class).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        Set<String> urls = result.getEndpointTraces().stream()
                .flatMap(trace -> trace.getDownstreamTargets().stream())
                .map(TraceTarget::getUrl)
                .collect(Collectors.toSet());
        assertEquals(Set.of("http://travel-service/api/routes/{trainNumber}",
                "http://travel-service/api/train_types/{trainNumber}"), urls);
        result.getEndpointTraces().stream().flatMap(trace -> trace.getDownstreamTargets().stream())
                .forEach(target -> assertEquals("travel-service", target.getDestinationService()));
    }

    /**
     * TrainTicket's rebook and route-plan services choose the URL in branches ({@code String requestUrl = "";}
     * then {@code requestUrl = travel...} or {@code travel2...}); both alternatives are calls of the endpoint.
     */
    @Test
    void urlAssignedInBranchesGivesOneTargetPerAlternative() throws IOException {
        Path serviceRoot = createMicroservice("rebook", Map.of(
                "src/main/java/com/example/rebook/RebookController.java", """
                        package com.example.rebook;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/rebook")
                        public class RebookController {
                            private final RebookService rebookService = new RebookService();

                            @GetMapping("/{tripId}/{orderId}")
                            public String rebook(@PathVariable String tripId, @PathVariable String orderId) {
                                return rebookService.deleteOrder(tripId, orderId);
                            }
                        }
                        """,
                "src/main/java/com/example/rebook/RebookService.java", """
                        package com.example.rebook;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class RebookService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String deleteOrder(String tripId, String orderId) {
                                String requestUrl = "";
                                if (tripId.startsWith("G")) {
                                    requestUrl = "http://order-service/api/orders/" + orderId;
                                } else {
                                    requestUrl = "http://order-other-service/api/orders/" + orderId;
                                }
                                return restTemplate.exchange(requestUrl, HttpMethod.DELETE, null, String.class)
                                        .getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        Map<String, String> destinationByUrl = result.getEndpointTraces().stream()
                .flatMap(trace -> trace.getDownstreamTargets().stream())
                .collect(Collectors.toMap(TraceTarget::getUrl, TraceTarget::getDestinationService));
        assertEquals(Map.of("http://order-service/api/orders/{orderId}", "order-service",
                "http://order-other-service/api/orders/{orderId}", "order-other-service"), destinationByUrl);
    }

    @Test
    void placeholderNamesForRuntimeUrlParts() {
        assertEquals("trainNumber", StaticServiceTraceAnalyzer.placeholderName(
                com.github.javaparser.StaticJavaParser.parseExpression("seat.getTrainNumber()")));
        assertEquals("orderId", StaticServiceTraceAnalyzer.placeholderName(
                com.github.javaparser.StaticJavaParser.parseExpression("orderId")));
        assertEquals("paid", StaticServiceTraceAnalyzer.placeholderName(
                com.github.javaparser.StaticJavaParser.parseExpression("order.isPaid()")));
        assertEquals("value", StaticServiceTraceAnalyzer.placeholderName(
                com.github.javaparser.StaticJavaParser.parseExpression("a + 1")));
        assertEquals("value", StaticServiceTraceAnalyzer.placeholderName(
                com.github.javaparser.StaticJavaParser.parseExpression("var")));
    }

    @Test
    void reusesMethodCacheAcrossMultipleEndpoints() throws IOException {
        Path serviceRoot = createMicroservice("catalog", Map.of(
                "src/main/java/com/example/catalog/CatalogController.java", """
                        package com.example.catalog;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/catalog")
                        public class CatalogController {
                            private final CatalogService catalogService = new CatalogService();

                            @GetMapping("/one")
                            public String one() {
                                return catalogService.load();
                            }

                            @GetMapping("/two")
                            public String two() {
                                return catalogService.load();
                            }
                        }
                        """,
                "src/main/java/com/example/catalog/CatalogService.java",
                """
                        package com.example.catalog;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class CatalogService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String load() {
                                return restTemplate.exchange("http://pricing-service/api/prices", HttpMethod.POST, null, String.class).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        assertEquals(2, result.getEndpointTraces().size());
        assertEquals(2, result.getEndpointTraces().stream()
                .filter(endpointTrace -> !endpointTrace.getDownstreamTargets().isEmpty())
                .count());
        assertTrue(result.getMethodTraceCache().keySet().stream()
                .anyMatch(methodKey -> methodKey.contains("CatalogService#load")));

        Set<String> destinationServices = result.getAllTraceTargets().stream()
                .map(TraceTarget::getDestinationService)
                .collect(Collectors.toSet());
        assertTrue(destinationServices.contains("pricing-service"));
    }

    @Test
    void resolvesConcreteImplementationBehindInterfaceTypedField() throws IOException {
        Path serviceRoot = createMicroservice("shipping", Map.of(
                "src/main/java/com/example/shipping/ShippingController.java", """
                        package com.example.shipping;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/shipping")
                        public class ShippingController {
                            private final ShippingService shippingService = new ShippingServiceImpl();

                            @GetMapping("/status")
                            public String status() {
                                return shippingService.loadStatus();
                            }
                        }
                        """,
                "src/main/java/com/example/shipping/ShippingService.java", """
                        package com.example.shipping;

                        public interface ShippingService {
                            String loadStatus();
                        }
                        """,
                "src/main/java/com/example/shipping/ShippingServiceImpl.java",
                """
                        package com.example.shipping;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class ShippingServiceImpl implements ShippingService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @Override
                            public String loadStatus() {
                                return restTemplate.exchange("http://tracking-service/api/status", HttpMethod.GET, null, String.class).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        assertEquals(1, result.getEndpointTraces().size());
        EndpointTrace endpointTrace = result.getEndpointTraces().get(0);
        assertEquals(1, endpointTrace.getDownstreamTargets().size());

        TraceTarget traceTarget = endpointTrace.getDownstreamTargets().iterator().next();
        assertEquals("http://tracking-service/api/status", traceTarget.getUrl());
        assertEquals("tracking-service", traceTarget.getDestinationService());
        assertTrue(result.getMethodTraceCache().keySet().stream()
                .anyMatch(methodKey -> methodKey.contains("ShippingServiceImpl#loadStatus")));
    }

    @Test
    void doesNotReportUnreachableRestTemplateExchangeForEndpoint() throws IOException {
        Path serviceRoot = createMicroservice("billing", Map.of(
                "src/main/java/com/example/billing/BillingController.java", """
                        package com.example.billing;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        @RequestMapping("/billing")
                        public class BillingController {
                            private final BillingService billingService = new BillingService();

                            @GetMapping("/public")
                            public String publicStatus() {
                                return billingService.publicStatus();
                            }

                            @GetMapping("/internal")
                            public String internalStatus() {
                                return billingService.internalStatus();
                            }
                        }
                        """,
                "src/main/java/com/example/billing/BillingService.java",
                """
                        package com.example.billing;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.client.RestTemplate;

                        public class BillingService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String publicStatus() {
                                return "ok";
                            }

                            public String internalStatus() {
                                return fetchInternalStatus();
                            }

                            private String fetchInternalStatus() {
                                return restTemplate.exchange("http://payments-service/api/internal", HttpMethod.GET, null, String.class).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        assertEquals(2, result.getEndpointTraces().size());
        Map<String, EndpointTrace> endpointByPath = result.getEndpointTraces().stream()
                .collect(Collectors.toMap(EndpointTrace::getPath, endpointTrace -> endpointTrace));

        assertTrue(endpointByPath.containsKey("/billing/public"));
        assertTrue(endpointByPath.containsKey("/billing/internal"));
        assertTrue(endpointByPath.get("/billing/public").getDownstreamTargets().isEmpty());
        assertEquals(1, endpointByPath.get("/billing/internal").getDownstreamTargets().size());

        TraceTarget traceTarget = endpointByPath.get("/billing/internal").getDownstreamTargets().iterator().next();
        assertEquals("http://payments-service/api/internal", traceTarget.getUrl());
        assertEquals("payments-service", traceTarget.getDestinationService());
    }

    @Test
    void capturesSentPathQueryBodyAndHeadersForRestTemplateExchange() throws IOException {
        Path serviceRoot = createMicroservice("checkout", Map.of(
                "src/main/java/com/example/checkout/CheckoutController.java", """
                        package com.example.checkout;

                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.http.ResponseEntity;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        @RequestMapping("/checkout")
                        public class CheckoutController {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/place")
                            public String place() {
                                HttpHeaders headers = new HttpHeaders();
                                Order order = new Order();
                                HttpEntity<Order> requestEntity = new HttpEntity<>(order, headers);
                                ResponseEntity<String> response = restTemplate.exchange(
                                        "http://orders-service/api/orders/{orderId}?mode=fast&currency=EUR",
                                        HttpMethod.PUT,
                                        requestEntity,
                                        String.class
                                );
                                return response.getBody();
                            }
                        }
                        """,
                "src/main/java/com/example/checkout/Order.java", """
                        package com.example.checkout;

                        public class Order {
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        assertEquals(1, result.getEndpointTraces().size());
        TraceTarget traceTarget = result.getEndpointTraces().get(0).getDownstreamTargets().iterator().next();

        assertEquals("orders-service", traceTarget.getDestinationService());
        assertTrue(traceTarget.isRequestBodySent());
        assertTrue(traceTarget.isHeadersSent());
        assertTrue(traceTarget.getSentPathVariables().contains("orderId"));
        assertTrue(traceTarget.getSentQueryParameters().contains("mode"));
        assertTrue(traceTarget.getSentQueryParameters().contains("currency"));
    }

    @Test
    void detectsMissingBodyAndHeadersWhenRequestEntityIsNull() throws IOException {
        Path serviceRoot = createMicroservice("pricing", Map.of(
                "src/main/java/com/example/pricing/PricingController.java", """
                        package com.example.pricing;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        @RequestMapping("/pricing")
                        public class PricingController {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/refresh")
                            public String refresh() {
                                return restTemplate.exchange(
                                        "http://catalog-service/api/catalog/refresh",
                                        HttpMethod.POST,
                                        null,
                                        String.class
                                ).getBody();
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        TraceTarget traceTarget = result.getEndpointTraces().get(0).getDownstreamTargets().iterator().next();
        assertFalse(traceTarget.isRequestBodySent());
        assertFalse(traceTarget.isHeadersSent());
    }

    private Path createMicroservice(String serviceName, Map<String, String> files) throws IOException {
        Path serviceRoot = tempDir.resolve(serviceName);
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path file = serviceRoot.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
        }
        return serviceRoot;
    }
}