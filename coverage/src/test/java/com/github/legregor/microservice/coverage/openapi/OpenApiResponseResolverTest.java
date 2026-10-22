package com.github.legregor.microservice.coverage.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link OpenApiResponseResolver}: lookup by status code (exact, range, default) and both spec versions. */
class OpenApiResponseResolverTest {

    @TempDir
    Path specDir;

    private OpenApiResponseResolver resolver;

    @BeforeEach
    void writeSpecs() throws IOException {
        Files.writeString(specDir.resolve("inventory.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/api/v1/reservations": {
                     "post": {"responses": {
                       "201": {"description": "created", "content": {"application/json": {
                         "schema": {"$ref": "#/components/schemas/Reservation"}}}},
                       "409": {"$ref": "#/components/responses/Conflict"},
                       "4XX": {"description": "client error", "content": {"application/problem+json": {
                         "schema": {"$ref": "#/components/schemas/Error"}}}},
                       "204": {"description": "nothing to report"},
                       "default": {"description": "other", "content": {"application/json": {
                         "schema": {"type": "object", "properties": {"trace": {"type": "string"}}}}}}}}},
                   "/api/v1/items/{sku}": {
                     "get": {"responses": {
                       "200": {"description": "ok", "content": {"application/json": {
                         "schema": {"type": "object", "properties": {"sku": {"type": "string"}}}}}}}}}},
                 "components": {
                   "responses": {"Conflict": {"description": "conflict", "content": {"application/json": {
                     "schema": {"$ref": "#/components/schemas/Error"}}}}},
                   "schemas": {
                     "Reservation": {"type": "object", "required": ["reservationId"], "properties": {
                       "reservationId": {"type": "string"}, "estimatedDelivery": {"type": "string", "format": "date"},
                       "secret": {"type": "string", "writeOnly": true}}},
                     "Error": {"type": "object", "required": ["message"], "properties": {
                       "message": {"type": "string"}, "availableQuantity": {"type": "integer"}}}}}}
                """);
        Files.writeString(specDir.resolve("legacy.json"), """
                {"swagger": "2.0",
                 "paths": {
                   "/orders/{id}": {
                     "get": {"responses": {
                       "200": {"description": "ok", "schema": {"$ref": "#/definitions/Order"}},
                       "404": {"$ref": "#/responses/NotFound"}}}}},
                 "responses": {"NotFound": {"description": "not found", "schema": {"$ref": "#/definitions/Error"}}},
                 "definitions": {
                   "Order": {"type": "object", "properties": {"id": {"type": "string"}, "total": {"type": "number"}}},
                   "Error": {"type": "object", "properties": {"message": {"type": "string"}}}}}
                """);
        resolver = new OpenApiResponseResolver(new OpenApiSpecRegistry(specDir));
    }

    @Test
    void exactCodeIsPreferred() {
        OpenApiResponseResolver.DocumentedResponse response =
                resolver.resolve("inventory", "POST", "/api/v1/reservations", 201).orElseThrow();

        assertEquals("201", response.key());
        assertEquals(List.of("reservationId", "estimatedDelivery", "secret"), fieldNames(response));
        assertTrue(SchemaFieldReader.read(response.schema()).getChildren().get(0).isRequired());
        assertTrue(SchemaFieldReader.read(response.schema()).getChildren().get(2).isWriteOnly());
    }

    @Test
    void exactCodeBehindAComponentReference() {
        OpenApiResponseResolver.DocumentedResponse response =
                resolver.resolve("inventory", "POST", "/api/v1/reservations", 409).orElseThrow();

        assertEquals("409", response.key());
        assertEquals(List.of("message", "availableQuantity"), fieldNames(response));
    }

    @Test
    void rangeThenDefault() {
        OpenApiResponseResolver.DocumentedResponse clientError =
                resolver.resolve("inventory", "POST", "/api/v1/reservations", 400).orElseThrow();
        OpenApiResponseResolver.DocumentedResponse serverError =
                resolver.resolve("inventory", "POST", "/api/v1/reservations", 500).orElseThrow();

        assertEquals("4XX", clientError.key());
        assertEquals(List.of("message", "availableQuantity"), fieldNames(clientError)); // +json media type
        assertEquals("default", serverError.key());
        assertEquals(List.of("trace"), fieldNames(serverError));
    }

    @Test
    void documentedWithoutBody() {
        OpenApiResponseResolver.DocumentedResponse response =
                resolver.resolve("inventory", "POST", "/api/v1/reservations", 204).orElseThrow();

        assertEquals("204", response.key());
        assertFalse(response.hasBody());
    }

    @Test
    void undocumentedCodeOrUnknownEndpointIsEmpty() {
        Optional<OpenApiResponseResolver.DocumentedResponse> undocumented =
                resolver.resolve("inventory", "GET", "/api/v1/items/ABC-0001", 404);

        assertTrue(undocumented.isEmpty());
        assertTrue(resolver.resolve("inventory", "DELETE", "/api/v1/reservations", 200).isEmpty());
        assertTrue(resolver.resolve("unknown-service", "GET", "/api/v1/items/ABC-0001", 200).isEmpty());
    }

    @Test
    void swagger2ResponsesWithDefinitionsAndReferences() {
        OpenApiResponseResolver.DocumentedResponse ok =
                resolver.resolve("legacy", "GET", "http://legacy:8080/orders/42", 200).orElseThrow();
        OpenApiResponseResolver.DocumentedResponse notFound =
                resolver.resolve("legacy", "GET", "/orders/42", 404).orElseThrow();

        assertEquals(List.of("id", "total"), fieldNames(ok));
        assertEquals("404", notFound.key());
        assertEquals(List.of("message"), fieldNames(notFound));
    }

    private static List<String> fieldNames(OpenApiResponseResolver.DocumentedResponse response) {
        return SchemaFieldReader.read(response.schema()).getChildren().stream()
                .map(SchemaField::getName)
                .collect(Collectors.toList());
    }
}
