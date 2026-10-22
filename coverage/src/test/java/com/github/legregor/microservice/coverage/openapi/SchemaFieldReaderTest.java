package com.github.legregor.microservice.coverage.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link SchemaFieldReader}: the fields of a request body schema as a tree. */
class SchemaFieldReaderTest {

    @Test
    void objectPropertiesWithRequiredFlags() throws IOException {
        SchemaField body = read("""
                {"$ref": "#/definitions/Consign"}
                """, """
                {"Consign": {"type": "object", "required": ["from"],
                  "properties": {"id": {"type": "string"}, "from": {"type": "string"}, "weight": {"type": "number"}}}}
                """);

        assertTrue(body.isObject());
        assertNull(body.getName());
        assertEquals(List.of("id", "from", "weight"), names(body.getChildren()));
        assertTrue(body.getChildren().get(1).isRequired());
        assertFalse(body.getChildren().get(0).isRequired());
        assertEquals("number", body.getChildren().get(2).getType());
    }

    @Test
    void nestedObjectsAndListItems() throws IOException {
        SchemaField body = read("""
                {"$ref": "#/definitions/Order"}
                """, """
                {"Order": {"type": "object", "properties": {
                    "address": {"$ref": "#/definitions/Address"},
                    "items": {"type": "array", "items": {"$ref": "#/definitions/Item"}},
                    "tags": {"type": "array", "items": {"type": "string"}}}},
                 "Address": {"type": "object", "required": ["zip"], "properties": {"zip": {"type": "string"}}},
                 "Item": {"type": "object", "properties": {"name": {"type": "string"}}}}
                """);

        SchemaField address = body.getChildren().get(0);
        SchemaField items = body.getChildren().get(1);
        SchemaField tags = body.getChildren().get(2);
        assertTrue(address.isObject());
        assertEquals(List.of("zip"), names(address.getChildren()));
        assertTrue(address.getChildren().get(0).isRequired());
        assertTrue(items.isList());
        assertEquals(List.of("name"), names(items.getChildren()));
        assertTrue(tags.isList());
        assertTrue(tags.getChildren().isEmpty());
    }

    @Test
    void referenceCycleIsNotExpandedAgain() throws IOException {
        SchemaField body = read("""
                {"$ref": "#/definitions/Node"}
                """, """
                {"Node": {"type": "object", "properties": {"value": {"type": "string"},
                    "next": {"$ref": "#/definitions/Node"}}}}
                """);

        assertEquals(List.of("value", "next"), names(body.getChildren()));
        assertTrue(body.getChildren().get(1).getChildren().isEmpty());
    }

    @Test
    void nestingStopsAtTheDepthLimit() throws IOException {
        SchemaField body = read("""
                {"type": "object", "properties": {"a": {"type": "object", "properties": {"b": {"type": "object",
                  "properties": {"c": {"type": "object", "properties": {"d": {"type": "object", "properties": {
                  "e": {"type": "object", "properties": {"f": {"type": "string"}}}}}}}}}}}}}
                """, "{}");

        SchemaField deepest = body;
        int depth = 0;
        while (!deepest.getChildren().isEmpty()) {
            deepest = deepest.getChildren().get(0);
            depth++;
        }
        assertEquals(SchemaFieldReader.MAX_DEPTH, depth);
    }

    @Test
    void allOfPartsAreMerged() throws IOException {
        SchemaField body = read("""
                {"allOf": [{"$ref": "#/definitions/Base"},
                           {"type": "object", "required": ["name"], "properties": {"name": {"type": "string"}}}]}
                """, """
                {"Base": {"type": "object", "properties": {"id": {"type": "string"}}}}
                """);

        assertTrue(body.isObject());
        assertEquals(List.of("id", "name"), names(body.getChildren()));
        assertTrue(body.getChildren().get(1).isRequired());
    }

    @Test
    void nonObjectBodies() throws IOException {
        SchemaField strings = read("""
                {"type": "array", "items": {"type": "string"}}
                """, "{}");
        assertFalse(strings.isObject());
        assertTrue(strings.isList());
        assertTrue(strings.getChildren().isEmpty());

        SchemaField value = read("""
                {"type": "string"}
                """, "{}");
        assertFalse(value.isObject());
        assertFalse(value.isList());
        assertEquals("string", value.getType());
    }

    private static SchemaField read(String bodySchema, String definitions) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schema = mapper.readTree(bodySchema);
        JsonNode defs = mapper.readTree(definitions);
        return SchemaFieldReader.read(new OpenApiSchema(schema, defs));
    }

    private static List<String> names(List<SchemaField> fields) {
        return fields.stream().map(SchemaField::getName).collect(Collectors.toList());
    }
}
