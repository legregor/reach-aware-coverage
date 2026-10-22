package com.github.legregor.microservice.coverage.config;

import com.github.legregor.microservice.coverage.config.model.ConnectionOverride;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CoverageConfigReaderTests {

    @Test
    void testReadConfig() throws Exception{
        String fileName = "CancelConfig.json";
        String filePath = "src/test/resources/" + fileName;

        Path path = Paths.get(filePath);

        CoverageConfig config = new CoverageConfigReader().read(path);

        assertFalse(config.getGlobalExpectedStatusCodes().isEmpty());
        assertEquals(config.getConfigName(), "CancelConfig");
        List<String> expStatusCodes = Arrays.asList("2xx", "4xx", "500");

        for (String code : config.getGlobalExpectedStatusCodes()){
            assertEquals(expStatusCodes.contains(code), true);
        }

        ConnectionOverride override = config.getConnectionOverrides().get(0);
        assertEquals(override.getSourceService(), "ts-cancel-service");
        assertEquals(override.getDestinationService(), "ts-order-service");
        assertEquals(override.getMethod(), "POST");
        assertEquals(override.getPath(), "/api/v1/orders");
        List<String> expStatusCodeOverride = Arrays.asList("200", "404");

        for (String code : override.getExpectedStatusCodes()){
            assertEquals(expStatusCodeOverride.contains(code), true);
        }
    }
}
