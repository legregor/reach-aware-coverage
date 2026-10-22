package com.github.legregor.microservice.coverage.config;

import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionExclusionConfigTest {

    @TempDir
    Path dir;

    private Path write(String json) throws Exception {
        Path file = dir.resolve("config.json");
        Files.writeString(file, json);
        return file;
    }

    @Test
    void readsAnExclusionWithReason() throws Exception {
        CoverageConfig config = new CoverageConfigReader().read(write("""
                {"connectionOverrides": [{"sourceService": "ts-cancel-service", "destinationService": "ts-order-service",
                  "method": "PUT", "path": "/api/v1/orderservice/order", "reachable": false,
                  "reason": "only for orders of the old booking flow"}]}
                """));
        assertEquals(1, config.getExclusions().size());
        assertEquals("only for orders of the old booking flow", config.getExclusions().get(0).getReason());
        assertFalse(config.getExclusionsFor("ts-cancel-service", "ts-order-service", "put",
                java.util.List.of("/api/v1/orderservice/order")).isEmpty());
    }

    @Test
    void anExclusionMustNameAllFourFields() throws Exception {
        Path file = write("""
                {"connectionOverrides": [{"destinationService": "ts-order-service", "reachable": false}]}
                """);
        CoverageConfigException e = assertThrows(CoverageConfigException.class,
                () -> new CoverageConfigReader().read(file));
        assertTrue(e.getMessage().contains("sourceService, method, path"));
    }

    @Test
    void anExclusionMustNotSetExpectedStatusCodes() throws Exception {
        Path file = write("""
                {"connectionOverrides": [{"sourceService": "a", "destinationService": "b", "method": "GET",
                  "path": "/x", "reachable": false, "expectedStatusCodes": ["200"]}]}
                """);
        assertThrows(CoverageConfigException.class, () -> new CoverageConfigReader().read(file));
    }

    @Test
    void reachableTrueIsNoExclusion() throws Exception {
        CoverageConfig config = new CoverageConfigReader().read(write("""
                {"connectionOverrides": [{"sourceService": "a", "reachable": true, "expectedStatusCodes": ["200"]}]}
                """));
        assertTrue(config.getExclusions().isEmpty());
    }

    @Test
    void pathWithoutLeadingSlashAndOtherVariableNamesStillMatches() throws Exception {
        CoverageConfig config = new CoverageConfigReader().read(write("""
                {"connectionOverrides": [{"sourceService": "ts-seat-service", "destinationService": "ts-travel2-service",
                  "method": "GET", "path": "api/v1/travel2service/routes/{trainNumber}", "reachable": false}]}
                """));
        assertFalse(config.getExclusionsFor("ts-seat-service", "ts-travel2-service", "GET",
                java.util.List.of("/api/v1/travel2service/routes/{tripId}")).isEmpty());
        assertTrue(config.getExclusionsFor("ts-seat-service", "ts-travel2-service", "GET",
                java.util.List.of("/api/v1/travel2service/train_types/{tripId}")).isEmpty());
    }
}
