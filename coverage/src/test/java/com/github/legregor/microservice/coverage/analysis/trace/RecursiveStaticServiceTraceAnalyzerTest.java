package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.legregor.microservice.coverage.analysis.model.CommunicationPath;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.util.ServiceLocator;
import com.github.legregor.microservice.coverage.config.model.EntryEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursiveStaticServiceTraceAnalyzerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void analyzesRootServiceAndReturnsValidResult() throws IOException {
        // Create root service with endpoint that calls downstream service
        Path serviceA = createMicroservice("serviceA", Map.of(
                "src/main/java/com/example/a/ControllerA.java", """
                        package com.example.a;
                        
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;
                        
                        @RestController
                        public class ControllerA {
                            private final RestTemplate restTemplate = new RestTemplate();
                            
                            @GetMapping("/api/data")
                            public String getData() {
                                return restTemplate.getForObject("http://serviceB/api/items", String.class);
                            }
                        }
                        """));
        
        // Create downstream service
        Path serviceB = createMicroservice("serviceB", Map.of(
                "src/main/java/com/example/b/ControllerB.java", """
                        package com.example.b;
                        
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        
                        @RestController
                        public class ControllerB {
                            @GetMapping("/api/items")
                            public String getItems() {
                                return "items";
                            }
                        }
                        """));
        
        ServiceLocator serviceLocator = new ServiceLocator(tempDir);
        RecursiveStaticServiceTraceAnalyzer analyzer = 
                new RecursiveStaticServiceTraceAnalyzer(new StaticServiceTraceAnalyzer(), serviceLocator);
        
        CommunicationPathResult result = analyzer.analyzeRecursively(serviceA);
        
        assertEquals("serviceA", result.getRootService());
        assertTrue(result.getAnalyzedServices().contains("serviceA"));
        // Result should be valid
        assertFalse(result.getAnalyzedServices().isEmpty());
    }
    
    @Test
    void analyzesRootServiceWithoutErrors() throws IOException {
        Path serviceA = createMicroservice("serviceA", Map.of(
                "src/main/java/com/example/a/ControllerA.java", """
                        package com.example.a;
                        
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        
                        @RestController
                        public class ControllerA {
                            @GetMapping("/api/data")
                            public String getData() {
                                return "data";
                            }
                        }
                        """));
        
        ServiceLocator serviceLocator = new ServiceLocator(tempDir);
        RecursiveStaticServiceTraceAnalyzer analyzer = 
                new RecursiveStaticServiceTraceAnalyzer(new StaticServiceTraceAnalyzer(), serviceLocator);
        
        // Should not throw and should return valid result
        CommunicationPathResult result = analyzer.analyzeRecursively(serviceA);
        
        assertEquals("serviceA", result.getRootService());
        assertTrue(result.getAnalyzedServices().contains("serviceA"));
    }
    
    /**
     * The tests of a run call only some endpoints of the entry service: the reachable connections are those of these
     * endpoints (TrainTicket: SearchCheapestResultTest only calls /routePlan/cheapestRoute of ts-route-plan-service).
     */
    @Test
    void restrictsReachabilityToTheConfiguredEntryEndpoints() throws IOException {
        Path serviceA = createMicroservice("serviceA", Map.of(
                "src/main/java/com/example/a/ControllerA.java", """
                        package com.example.a;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        @RequestMapping("/api/v1/a")
                        public class ControllerA {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @PostMapping("/cheapest")
                            public String cheapest() {
                                return restTemplate.exchange("http://serviceB/api/v1/b/items", HttpMethod.GET, null,
                                        String.class).getBody();
                            }

                            @GetMapping("/quickest/{id}")
                            public String quickest(@PathVariable String id) {
                                return restTemplate.exchange("http://serviceC/api/v1/c/items", HttpMethod.GET, null,
                                        String.class).getBody();
                            }
                        }
                        """));
        createMicroservice("serviceB", Map.of(
                "src/main/java/com/example/b/ControllerB.java", controller("b", "/api/v1/b/items")));
        createMicroservice("serviceC", Map.of(
                "src/main/java/com/example/c/ControllerC.java", controller("c", "/api/v1/c/items")));

        RecursiveStaticServiceTraceAnalyzer analyzer = new RecursiveStaticServiceTraceAnalyzer(
                new StaticServiceTraceAnalyzer(), new ServiceLocator(tempDir));

        CommunicationPathResult all = analyzer.analyzeRecursively(serviceA);
        assertEquals(java.util.Set.of("serviceB", "serviceC"), destinations(all));
        assertTrue(all.getEntryEndpoints().isEmpty());

        CommunicationPathResult cheapest = analyzer.analyzeRecursively(serviceA, List.of(
                new EntryEndpoint("POST", "/api/v1/a/cheapest")));
        assertEquals(java.util.Set.of("serviceB"), destinations(cheapest));
        assertEquals(List.of("POST /api/v1/a/cheapest"), cheapest.getEntryEndpoints());
        assertTrue(cheapest.getUnmatchedEntryEndpoints().isEmpty());

        // a concrete path matches the template; without a method, any method
        CommunicationPathResult quickest = analyzer.analyzeRecursively(serviceA, List.of(
                new EntryEndpoint(null, "/api/v1/a/quickest/42")));
        assertEquals(java.util.Set.of("serviceC"), destinations(quickest));

        // a typo is reported, not silently ignored
        CommunicationPathResult typo = analyzer.analyzeRecursively(serviceA, List.of(
                new EntryEndpoint("GET", "/api/v1/a/cheapest")));
        assertEquals(List.of("GET /api/v1/a/cheapest"), typo.getUnmatchedEntryEndpoints());
        assertTrue(destinations(typo).isEmpty());
    }

    /**
     * Overlapping templates (TrainTicket ts-cancel-service): /cancel/{orderId}/{loginId} also matches the path
     * /cancel/refound/{orderId}. An entry endpoint or a call addresses only the endpoint Spring routes it to.
     */
    @Test
    void entryEndpointsAndCallsReachOnlyTheMostSpecificOfOverlappingTemplates() throws IOException {
        Path serviceA = createMicroservice("serviceA", Map.of(
                "src/main/java/com/example/a/ControllerA.java", """
                        package com.example.a;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RequestMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        @RequestMapping("/api/v1/a")
                        public class ControllerA {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/cancel/refound/{orderId}")
                            public String refund(@PathVariable String orderId) {
                                return restTemplate.exchange("http://serviceB/api/v1/b/items", HttpMethod.GET, null,
                                        String.class).getBody();
                            }

                            @GetMapping("/cancel/{orderId}/{loginId}")
                            public String cancel(@PathVariable String orderId, @PathVariable String loginId) {
                                return restTemplate.exchange("http://serviceC/api/v1/c/items", HttpMethod.GET, null,
                                        String.class).getBody();
                            }
                        }
                        """));
        createMicroservice("serviceB", Map.of(
                "src/main/java/com/example/b/ControllerB.java", controller("b", "/api/v1/b/items")));
        createMicroservice("serviceC", Map.of(
                "src/main/java/com/example/c/ControllerC.java", controller("c", "/api/v1/c/items")));
        Path serviceD = createMicroservice("serviceD", Map.of(
                "src/main/java/com/example/d/ControllerD.java", """
                        package com.example.d;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        public class ControllerD {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/api/v1/d/refund/{orderId}")
                            public String refund(@PathVariable String orderId) {
                                return restTemplate.exchange("http://serviceA/api/v1/a/cancel/refound/" + orderId,
                                        HttpMethod.GET, null, String.class).getBody();
                            }
                        }
                        """));

        RecursiveStaticServiceTraceAnalyzer analyzer = new RecursiveStaticServiceTraceAnalyzer(
                new StaticServiceTraceAnalyzer(), new ServiceLocator(tempDir));

        CommunicationPathResult template = analyzer.analyzeRecursively(serviceA, List.of(
                new EntryEndpoint("GET", "/api/v1/a/cancel/refound/{orderId}")));
        assertEquals(List.of("GET /api/v1/a/cancel/refound/{orderId}"), template.getEntryEndpoints());
        assertEquals(java.util.Set.of("serviceB"), destinations(template));

        CommunicationPathResult concreteRefund = analyzer.analyzeRecursively(serviceA, List.of(
                new EntryEndpoint("GET", "/api/v1/a/cancel/refound/42")));
        assertEquals(java.util.Set.of("serviceB"), destinations(concreteRefund));

        CommunicationPathResult concreteCancel = analyzer.analyzeRecursively(serviceA, List.of(
                new EntryEndpoint(null, "/api/v1/a/cancel/7/9")));
        assertEquals(java.util.Set.of("serviceC"), destinations(concreteCancel));

        // a call to /cancel/refound/{id} reaches serviceB through serviceA, never serviceC
        CommunicationPathResult downstream = analyzer.analyzeRecursively(serviceD);
        assertEquals(java.util.Set.of("serviceA", "serviceB"), destinations(downstream));
    }

    private static java.util.Set<String> destinations(CommunicationPathResult result) {
        java.util.Set<String> destinations = new java.util.HashSet<>();
        result.getPaths().forEach(path -> path.getSegments()
                .forEach(segment -> destinations.add(segment.getDestinationService())));
        return destinations;
    }

    private static String controller(String name, String path) {
        return """
                package com.example.%s;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class Controller%s {
                    @GetMapping("%s")
                    public String items() {
                        return "items";
                    }
                }
                """.formatted(name, name.toUpperCase(), path);
    }

    @Test
    void filtersPathByService() throws IOException {
        Path serviceA = createMicroservice("serviceA", Map.of(
                "src/main/java/com/example/a/ControllerA.java", """
                        package com.example.a;
                        
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;
                        
                        @RestController
                        public class ControllerA {
                            private final RestTemplate restTemplate = new RestTemplate();
                            
                            @GetMapping("/api/data")
                            public String getData() {
                                return restTemplate.getForObject("http://serviceB/api/items", String.class);
                            }
                        }
                        """));
        
        Path serviceB = createMicroservice("serviceB", Map.of(
                "src/main/java/com/example/b/ControllerB.java", """
                        package com.example.b;
                        
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        
                        @RestController
                        public class ControllerB {
                            @GetMapping("/api/items")
                            public String getItems() {
                                return "items";
                            }
                        }
                        """));
        
        ServiceLocator serviceLocator = new ServiceLocator(tempDir);
        RecursiveStaticServiceTraceAnalyzer analyzer = 
                new RecursiveStaticServiceTraceAnalyzer(new StaticServiceTraceAnalyzer(), serviceLocator);
        
        CommunicationPathResult result = analyzer.analyzeRecursively(serviceA);
        
        // Test filtering by destination service - should not throw
        List<CommunicationPath> pathsToB = result.getPathsTo("serviceB");
        assertTrue(pathsToB != null);
    }

    @Test
    void matchesDownstreamEndpointByFullPathTemplate() throws IOException {
        Path cancelService = createMicroservice("ts-cancel-service", Map.of(
                "src/main/java/cancel/controller/CancelController.java", """
                        package cancel.controller;

                        import org.springframework.http.HttpHeaders;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RequestHeader;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        public class CancelController {
                            private final CancelServiceImpl cancelService = new CancelServiceImpl();

                            @GetMapping("/api/v1/cancelservice/cancel/{orderId}/{loginId}")
                            public String cancelTicket(@PathVariable String orderId,
                                                       @PathVariable String loginId,
                                                       @RequestHeader HttpHeaders headers) {
                                return cancelService.cancelFromOtherOrder(orderId, loginId, headers);
                            }
                        }
                        """,
                "src/main/java/cancel/controller/CancelServiceImpl.java", """
                        package cancel.controller;

                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.ResponseEntity;
                        import org.springframework.web.client.RestTemplate;

                        public class CancelServiceImpl {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String cancelFromOtherOrder(String orderId, String loginId, HttpHeaders headers) {
                                HttpEntity requestEntity = new HttpEntity(headers);
                                ResponseEntity<String> re = restTemplate.exchange(
                                    "http://ts-inside-payment-service:18673/api/v1/inside_pay_service/inside_payment/drawback/"
                                        + loginId + "/" + orderId,
                                    HttpMethod.GET,
                                    requestEntity,
                                    String.class);
                                return re.getBody();
                            }
                        }
                        """));

        createMicroservice("ts-inside-payment-service", Map.of(
                "src/main/java/inside_payment/controller/InsidePaymentController.java", """
                        package inside_payment.controller;

                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.ResponseEntity;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RequestHeader;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        public class InsidePaymentController {
                            private final InsidePaymentService insidePaymentService = new InsidePaymentService();

                            @GetMapping("/api/v1/inside_pay_service/inside_payment")
                            public String pay(@RequestHeader HttpHeaders headers) {
                                return insidePaymentService.pay(headers);
                            }

                            @GetMapping("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}")
                            public String drawBack(@PathVariable String userId,
                                                   @PathVariable String money,
                                                   @RequestHeader HttpHeaders headers) {
                                return insidePaymentService.drawBack(userId, money, headers);
                            }
                        }
                        """,
                "src/main/java/inside_payment/controller/InsidePaymentService.java", """
                        package inside_payment.controller;

                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.ResponseEntity;
                        import org.springframework.web.client.RestTemplate;

                        public class InsidePaymentService {
                            private final RestTemplate restTemplate = new RestTemplate();

                            public String pay(HttpHeaders headers) {
                                return restTemplate.exchange(
                                    "http://ts-payment-service:19001/api/v1/paymentservice/payment",
                                    HttpMethod.POST,
                                    new HttpEntity(headers),
                                    String.class).getBody();
                            }

                            public String drawBack(String userId, String money, HttpHeaders headers) {
                                return restTemplate.exchange(
                                    "http://ts-payment-service:19001/api/v1/paymentservice/payment",
                                    HttpMethod.POST,
                                    new HttpEntity(headers),
                                    String.class).getBody();
                            }
                        }
                        """));

        createMicroservice("ts-payment-service", Map.of(
                "src/main/java/payment/controller/PaymentController.java", """
                        package payment.controller;

                        import org.springframework.web.bind.annotation.PostMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        public class PaymentController {
                            @PostMapping("/api/v1/paymentservice/payment")
                            public String payment() {
                                return "ok";
                            }
                        }
                        """));

        ServiceLocator serviceLocator = new ServiceLocator(tempDir);
        RecursiveStaticServiceTraceAnalyzer analyzer =
                new RecursiveStaticServiceTraceAnalyzer(new StaticServiceTraceAnalyzer(), serviceLocator);

        CommunicationPathResult result = analyzer.analyzeRecursively(cancelService);

        assertEquals(2, result.getPaths().size());
        CommunicationPath path = result.getPaths().stream()
                .filter(p -> p.getSegments().size() == 2)
                .findFirst()
                .orElseThrow();
        assertEquals(2, path.getSegments().size());
        assertEquals("/api/v1/cancelservice/cancel/{orderId}/{loginId}",
                path.getSegments().get(0).getSourcePath());
        assertEquals("/api/v1/inside_pay_service/inside_payment/drawback/{userId}/{money}",
                path.getSegments().get(1).getSourcePath());
        assertTrue(path.getSegments().get(1).getSourceEndpointMethodKey().contains("drawBack"));
    }

    @Test
    void templatedCallDoesNotMatchLiteralEndpointWithSamePrefix() throws IOException {
        // svc-a calls /api/items/{itemId}; svc-b has /api/items/welcome (declared first) and /api/items/{id}.
        // The call must be matched to /api/items/{id}, whose handler calls svc-c.
        Path serviceA = createMicroservice("svc-a", Map.of(
                "src/main/java/a/AController.java", """
                        package a;

                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        public class AController {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/api/a/{itemId}")
                            public String get(@PathVariable String itemId) {
                                return restTemplate.exchange("http://svc-b:8080/api/items/" + itemId,
                                        HttpMethod.GET, new HttpEntity(new HttpHeaders()), String.class).getBody();
                            }
                        }
                        """));
        createMicroservice("svc-b", Map.of(
                "src/main/java/b/BController.java", """
                        package b;

                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        public class BController {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/api/items/welcome")
                            public String welcome() {
                                return "welcome";
                            }

                            @GetMapping("/api/items/{id}")
                            public String item(@PathVariable String id) {
                                return restTemplate.exchange("http://svc-c:8080/api/stock",
                                        HttpMethod.GET, new HttpEntity(new HttpHeaders()), String.class).getBody();
                            }
                        }
                        """));
        createMicroservice("svc-c", Map.of(
                "src/main/java/c/CController.java", """
                        package c;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        public class CController {
                            @GetMapping("/api/stock")
                            public String stock() {
                                return "42";
                            }
                        }
                        """));

        RecursiveStaticServiceTraceAnalyzer analyzer = new RecursiveStaticServiceTraceAnalyzer(
                new StaticServiceTraceAnalyzer(), new ServiceLocator(tempDir));
        analyzer.setForceReanalysis(true);
        CommunicationPathResult result = analyzer.analyzeRecursively(serviceA);

        CommunicationPath.PathSegment firstHop = result.getPaths().stream()
                .map(path -> path.getSegments().get(0))
                .filter(segment -> "svc-b".equals(segment.getDestinationService()))
                .findFirst()
                .orElseThrow();
        assertEquals("/api/items/{id}", firstHop.getDestinationEndpointPath());
        // the caller names the variable itemId, the endpoint id: still sent (matched by position, not name)
        assertTrue(firstHop.getStaticallyMissingArguments().isEmpty(),
                () -> "unexpected missing arguments: " + firstHop.getStaticallyMissingArguments());
        assertTrue(result.getPaths().stream().anyMatch(path -> path.getSegments().size() == 2
                        && "svc-c".equals(path.getSegments().get(1).getDestinationService())),
                "the downstream call of /api/items/{id} must be found");
    }

    @Test
    void marksRequiredAndOptionalEndpointArgumentsThatAreNotStaticallySent() throws IOException {
        Path serviceA = createMicroservice("serviceA", Map.of(
                "src/main/java/com/example/a/ControllerA.java", """
                        package com.example.a;

                        import org.springframework.http.HttpMethod;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        public class ControllerA {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/api/data")
                            public String getData() {
                                return restTemplate.exchange("http://serviceB/api/items", HttpMethod.GET, null, String.class).getBody();
                            }
                        }
                        """));

        createMicroservice("serviceB", Map.of(
                "src/main/java/com/example/b/ControllerB.java", """
                        package com.example.b;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestHeader;
                        import org.springframework.web.bind.annotation.RequestParam;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        public class ControllerB {
                            @GetMapping("/api/items")
                            public String getItems(@RequestParam String filter,
                                                   @RequestParam(required = false) String debug,
                                                   @RequestHeader("Authorization") String authHeader) {
                                return "items";
                            }
                        }
                        """));

        ServiceLocator serviceLocator = new ServiceLocator(tempDir);
        RecursiveStaticServiceTraceAnalyzer analyzer =
                new RecursiveStaticServiceTraceAnalyzer(new StaticServiceTraceAnalyzer(), serviceLocator);

        CommunicationPathResult result = analyzer.analyzeRecursively(serviceA);

        assertEquals(1, result.getPaths().size());
        CommunicationPath.PathSegment segment = result.getPaths().get(0).getSegments().get(0);
        assertTrue(segment.getStaticallyMissingArguments().stream()
                .anyMatch(arg -> arg.contains("@RequestParam String filter")));
        assertTrue(segment.getStaticallyMissingArguments().stream()
                .anyMatch(arg -> arg.contains("@RequestHeader(\"Authorization\")")));
        assertTrue(segment.getStaticallyOptionalButUnsentArguments().stream()
                .anyMatch(arg -> arg.contains("@RequestParam(required = false) String debug")));
        assertEquals("String", segment.getSourceReturnType());
        assertEquals("String", segment.getDestinationReturnType());
    }
    
    private Path createMicroservice(String name, Map<String, String> files) throws IOException {
        Path serviceRoot = tempDir.resolve(name);
        Files.createDirectories(serviceRoot);
        
        for (Map.Entry<String, String> fileEntry : files.entrySet()) {
            Path filePath = serviceRoot.resolve(fileEntry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileEntry.getValue().getBytes());
        }
        
        return serviceRoot;
    }
}
