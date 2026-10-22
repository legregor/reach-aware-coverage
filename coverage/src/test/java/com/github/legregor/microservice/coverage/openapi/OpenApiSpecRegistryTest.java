package com.github.legregor.microservice.coverage.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

class OpenApiSpecRegistryTest {

    @TempDir
    Path specDir;

    @Test
    void findsPlainJsonFileName() throws Exception {
        Files.writeString(specDir.resolve("orders.json"), "{\"openapi\": \"3.0.0\", \"paths\": {}}");

        Optional<JsonNode> spec = new OpenApiSpecRegistry(specDir).getSpec("orders");

        assertTrue(spec.isPresent());
        assertEquals("3.0.0", spec.get().path("openapi").asText());
    }

    @Test
    void stillFindsSlimFileName() throws Exception {
        Files.writeString(specDir.resolve("orders-slim.json"), "{\"swagger\": \"2.0\", \"paths\": {}}");

        Optional<JsonNode> spec = new OpenApiSpecRegistry(specDir).getSpec("orders");

        assertEquals("2.0", spec.orElseThrow().path("swagger").asText());
    }

    @Test
    void readsYamlAndFolderPerService() throws Exception {
        Files.createDirectories(specDir.resolve("orders"));
        Files.writeString(specDir.resolve("orders/openapi.yaml"), String.join("\n",
                "openapi: 3.1.0",
                "paths:",
                "  /orders/{id}:",
                "    get:",
                "      responses:",
                "        '200':",
                "          description: ok",
                ""));

        Optional<JsonNode> spec = new OpenApiSpecRegistry(specDir).getSpec("orders");

        assertEquals("3.1.0", spec.orElseThrow().path("openapi").asText());
        assertTrue(spec.get().path("paths").has("/orders/{id}"));
    }

    @Test
    void prefersPlainJsonOverOtherNames() throws Exception {
        Files.writeString(specDir.resolve("orders.json"), "{\"openapi\": \"3.0.0\"}");
        Files.writeString(specDir.resolve("orders-slim.json"), "{\"swagger\": \"2.0\"}");

        OpenApiSpecRegistry registry = new OpenApiSpecRegistry(specDir);

        assertEquals(specDir.resolve("orders.json"), registry.findSpecFile("orders").orElseThrow());
    }

    @Test
    void missingSpecIsEmpty() {
        OpenApiSpecRegistry registry = new OpenApiSpecRegistry(specDir);

        assertFalse(registry.getSpec("unknown").isPresent());
        assertFalse(new OpenApiSpecRegistry(null).getSpec("orders").isPresent());
    }
}
