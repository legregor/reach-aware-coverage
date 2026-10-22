package com.github.legregor.microservice.coverage.openapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link OpenApiRequestBodyResolver} with OpenAPI 3.x specs (request bodies and path parameters). */
class OpenApiRequestBodyResolverTest {

    @TempDir
    Path specDir;

    @Test
    void openApi3RequestBodyAndPathParameters() throws IOException {
        Files.writeString(specDir.resolve("shop-slim.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/orders/{orderId}/items": {
                     "parameters": [{"name": "orderId", "in": "path", "required": true,
                                     "schema": {"type": "string", "format": "uuid"}}],
                     "post": {
                       "parameters": [{"$ref": "#/components/parameters/Version"}],
                       "requestBody": {"$ref": "#/components/requestBodies/NewItem"}}}},
                 "components": {
                   "parameters": {"Version": {"name": "version", "in": "query",
                                              "schema": {"type": "integer"}}},
                   "requestBodies": {"NewItem": {"content": {"application/json": {
                     "schema": {"$ref": "#/components/schemas/Item"}}}}},
                   "schemas": {"Item": {"type": "object", "required": ["sku"], "properties": {
                     "sku": {"type": "string"}, "quantity": {"type": "integer", "minimum": 1}}}}}}
                """);
        OpenApiRequestBodyResolver resolver = new OpenApiRequestBodyResolver(new OpenApiSpecRegistry(specDir));

        Optional<OpenApiSchema> body = resolver.resolveSchema("shop", "POST", "/orders/{orderId}/items");
        assertTrue(body.isPresent());
        SchemaField item = SchemaFieldReader.read(body.get());
        assertEquals(List.of("sku", "quantity"),
                item.getChildren().stream().map(SchemaField::getName).collect(Collectors.toList()));
        assertEquals(new BigDecimal(1), item.getChildren().get(1).getConstraints().getMinimum());

        // path-level parameter; the query parameter is not a path parameter
        Map<String, SchemaField> parameters = resolver.resolvePathParameters("shop", "POST",
                "http://shop:8080/orders/{orderId}/items");
        assertEquals(List.of("orderId"), List.copyOf(parameters.keySet()));
        assertEquals("uuid", parameters.get("orderId").getConstraints().getFormat());
        assertTrue(parameters.get("orderId").isRequired());
    }

    @Test
    void exactPathIsPreferredOverTemplate() throws IOException {
        Files.writeString(specDir.resolve("shop-slim.json"), """
                {"openapi": "3.1.0",
                 "paths": {
                   "/orders/{orderId}": {"put": {"requestBody": {"content": {"application/json": {
                     "schema": {"type": "object", "properties": {"byId": {"type": "string"}}}}}}}},
                   "/orders/bulk": {"put": {"requestBody": {"content": {"application/merge-patch+json": {
                     "schema": {"type": "object", "properties": {"bulk": {"type": "string"}}}}}}}}}}
                """);
        OpenApiRequestBodyResolver resolver = new OpenApiRequestBodyResolver(new OpenApiSpecRegistry(specDir));

        SchemaField bulk = SchemaFieldReader.read(resolver.resolveSchema("shop", "PUT", "/orders/bulk").orElseThrow());

        assertEquals("bulk", bulk.getChildren().get(0).getName());
    }

    @Test
    void swagger2PathParameters() throws IOException {
        Files.writeString(specDir.resolve("shop-slim.json"), """
                {"swagger": "2.0",
                 "paths": {"/pages/{page}": {"get": {"parameters": [
                   {"name": "page", "in": "path", "required": true, "type": "integer", "minimum": 0}]}}}}
                """);
        OpenApiRequestBodyResolver resolver = new OpenApiRequestBodyResolver(new OpenApiSpecRegistry(specDir));

        Map<String, SchemaField> parameters = resolver.resolvePathParameters("shop", "GET", "/pages/{page}");

        assertEquals("integer", parameters.get("page").getType());
        assertEquals(new BigDecimal(0), parameters.get("page").getConstraints().getMinimum());
    }
}
