package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.openapi.OpenApiSchema;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaFieldReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link ElementValueCollector}: the values of every schema field in a JSON body. */
class ElementValueCollectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ElementValueCollector collector = new ElementValueCollector();

    @Test
    void topLevelNestedAndListItemValues() throws IOException {
        SchemaField order = schema("""
                {"type": "object", "properties": {
                  "id": {"type": "string"},
                  "comment": {"type": "string"},
                  "address": {"type": "object", "properties": {"zip": {"type": "string"}}},
                  "items": {"type": "array", "items": {"type": "object", "properties": {"sku": {"type": "string"}}}}}}
                """);

        Map<String, List<JsonNode>> values = collector.collect(order, MAPPER.readTree("""
                {"id": "o1", "comment": null, "address": {"zip": "80333"},
                 "items": [{"sku": "A"}, {"sku": "B"}, {}]}
                """));

        assertEquals(List.of("", "id", "comment", "address", "address.zip", "items", "items[].sku"),
                List.copyOf(values.keySet()));
        assertEquals(List.of("\"o1\""), texts(values.get("id")));
        assertTrue(values.get("comment").get(0).isNull());
        assertEquals(List.of("\"80333\""), texts(values.get("address.zip")));
        // one entry per item; the third item has no sku
        assertEquals(3, values.get("items[].sku").size());
        assertTrue(values.get("items[].sku").get(2).isMissingNode());
    }

    @Test
    void childrenOfAbsentObjectsAreNotVisited() throws IOException {
        SchemaField order = schema("""
                {"type": "object", "properties": {
                  "address": {"type": "object", "properties": {"zip": {"type": "string"}}}}}
                """);

        Map<String, List<JsonNode>> values = collector.collect(order, MAPPER.readTree("{}"));

        assertTrue(values.get("address").get(0).isMissingNode());
        assertFalse(values.containsKey("address.zip"));
    }

    @Test
    void bodyThatIsAList() throws IOException {
        SchemaField tickets = schema("""
                {"type": "array", "items": {"type": "object", "properties": {"seat": {"type": "integer"}}}}
                """);

        Map<String, List<JsonNode>> values = collector.collect(tickets, MAPPER.readTree("""
                [{"seat": 1}, {"seat": 2}]
                """));

        assertEquals(List.of("[{\"seat\":1},{\"seat\":2}]"), texts(values.get("")));
        assertEquals(List.of("1", "2"), texts(values.get("[].seat")));
    }

    private static SchemaField schema(String json) throws IOException {
        return SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree(json), null, MAPPER.createObjectNode()));
    }

    private static List<String> texts(List<JsonNode> values) {
        return values.stream().map(JsonNode::toString).collect(Collectors.toList());
    }
}
