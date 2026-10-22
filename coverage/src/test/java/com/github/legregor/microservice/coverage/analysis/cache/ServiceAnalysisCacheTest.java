package com.github.legregor.microservice.coverage.analysis.cache;

import com.github.legregor.microservice.coverage.analysis.model.StaticServiceTraceResult;
import com.github.legregor.microservice.coverage.analysis.trace.StaticServiceTraceAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAnalysisCacheTest {
    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsStaticAnalysisInSmileFormat() throws IOException {
        Path serviceRoot = createMicroservice("orders", Map.of(
                "src/main/java/com/example/orders/OrderController.java", """
                        package com.example.orders;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.client.RestTemplate;

                        @RestController
                        public class OrderController {
                            private final RestTemplate restTemplate = new RestTemplate();

                            @GetMapping("/orders")
                            public String orders() {
                                return restTemplate.getForObject("http://inventory-service/api/items", String.class);
                            }
                        }
                        """));

        StaticServiceTraceResult result = new StaticServiceTraceAnalyzer().analyze(List.of(serviceRoot));

        ServiceAnalysisCache cache = new ServiceAnalysisCache(tempDir.resolve("cache"));
        cache.store(serviceRoot, result);

        Path cacheFile = cache.resolveCacheFile(serviceRoot);
        assertTrue(Files.exists(cacheFile));
        assertTrue(cacheFile.toString().endsWith(".smile"));

        StaticServiceTraceResult reloaded = cache.load(serviceRoot).orElseThrow();
        assertEquals(result.getEndpointTraces().size(), reloaded.getEndpointTraces().size());
        assertEquals(result.getMethodTraceCache().size(), reloaded.getMethodTraceCache().size());
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
