package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.legregor.microservice.coverage.analysis.cache.ServiceAnalysisCache;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.model.StaticServiceTraceResult;
import com.github.legregor.microservice.coverage.analysis.util.ServiceLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursiveStaticServiceTraceAnalyzerCacheTest {
    @TempDir
    Path tempDir;

    @Test
    void cachesOriginAndIntermediateServices() throws IOException {
        Path serviceA = createMicroservice("ts-cancel-service", Map.of(
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

                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
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

        Path serviceB = createMicroservice("ts-inside-payment-service", Map.of(
                "src/main/java/inside_payment/controller/InsidePaymentController.java", """
                        package inside_payment.controller;

                        import org.springframework.http.HttpHeaders;
                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.PathVariable;
                        import org.springframework.web.bind.annotation.RequestHeader;
                        import org.springframework.web.bind.annotation.RestController;

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

                        import org.springframework.http.HttpEntity;
                        import org.springframework.http.HttpHeaders;
                        import org.springframework.http.HttpMethod;
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

        AtomicInteger analyzeCalls = new AtomicInteger();
        StaticServiceTraceAnalyzer countingAnalyzer = new StaticServiceTraceAnalyzer() {
            @Override
            public StaticServiceTraceResult analyze(List<Path> microserviceRoots) throws IOException {
                analyzeCalls.incrementAndGet();
                return super.analyze(microserviceRoots);
            }
        };

        ServiceAnalysisCache cache = new ServiceAnalysisCache(tempDir.resolve("cache"));
        RecursiveStaticServiceTraceAnalyzer analyzer =
                new RecursiveStaticServiceTraceAnalyzer(countingAnalyzer, new ServiceLocator(tempDir), cache);

        CommunicationPathResult firstResult = analyzer.analyzeRecursively(serviceA);
        assertEquals(3, analyzeCalls.get());
        assertTrue(cache.resolveCacheFile(serviceA).toFile().exists());
        assertTrue(cache.resolveCacheFile(serviceB).toFile().exists());
        assertTrue(cache.resolveCacheFile(tempDir.resolve("ts-payment-service")).toFile().exists());

        CommunicationPathResult secondResult = analyzer.analyzeRecursively(serviceA);
        assertEquals(3, analyzeCalls.get());
        assertEquals(firstResult.getPaths().size(), secondResult.getPaths().size());
        assertEquals(firstResult.getAnalyzedServices(), secondResult.getAnalyzedServices());
    }

    private Path createMicroservice(String name, Map<String, String> files) throws IOException {
        Path serviceRoot = tempDir.resolve(name);
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path file = serviceRoot.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
        }
        return serviceRoot;
    }
}
