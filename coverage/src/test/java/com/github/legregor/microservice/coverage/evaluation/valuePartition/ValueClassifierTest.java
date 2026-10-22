package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.openapi.OpenApiSchema;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaFieldReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link ValueClassifier}: which partitions a value covers, and when it is out of spec. */
class ValueClassifierTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ValueClassifier classifier = new ValueClassifier();

    @Test
    void integerRange() throws IOException {
        SchemaField quantity = PartitionGeneratorTest.field("""
                {"type": "integer", "minimum": 1, "maximum": 99}
                """, false, true);

        assertEquals(List.of("#min"), covered(quantity, "1"));
        assertEquals(List.of("#interior"), covered(quantity, "5"));
        assertEquals(List.of("#interior"), covered(quantity, "5.0"));
        assertEquals(List.of("#max"), covered(quantity, "99"));
        assertEquals(List.of("#none"), covered(quantity, "null"));
        assertEquals(List.of("#none"), ids(classifier.classify(quantity, null)));
        assertEquals("below minimum 1", reason(quantity, "0"));
        assertEquals("above maximum 99", reason(quantity, "100"));
        assertEquals("wrong type: expected integer, got string", reason(quantity, "\"5\""));
        assertEquals("wrong type: expected integer, got number", reason(quantity, "5.5"));
    }

    @Test
    void requiredElementWithoutValueIsOutOfSpec() throws IOException {
        SchemaField id = PartitionGeneratorTest.field("""
                {"type": "string"}
                """, true, true);

        ValueClassification classification = classifier.classify(id, null);

        assertTrue(classification.isOutOfSpec());
        assertTrue(classification.covered().isEmpty());
    }

    @Test
    void numberWithExclusiveMinimum() throws IOException {
        SchemaField price = PartitionGeneratorTest.field("""
                {"type": "number", "exclusiveMinimum": 0, "maximum": 1}
                """, true, true);

        assertEquals("below exclusive minimum 0", reason(price, "0"));
        assertEquals(List.of("#interior"), covered(price, "0.5"));
        assertEquals(List.of("#max"), covered(price, "1.00"));
    }

    @Test
    void signsWithoutRange() throws IOException {
        SchemaField delta = PartitionGeneratorTest.field("""
                {"type": "integer"}
                """, true, true);

        assertEquals(List.of("#negative"), covered(delta, "-3"));
        assertEquals(List.of("#zero"), covered(delta, "0"));
        assertEquals(List.of("#positive"), covered(delta, "7"));
    }

    @Test
    void lengthAndPatternEachChoice() throws IOException {
        SchemaField code = PartitionGeneratorTest.field("""
                {"type": "string", "minLength": 2, "maxLength": 10, "pattern": "^(A|B)"}
                """, true, true);

        assertEquals(List.of("#len-min", "#pattern"), covered(code, "\"A1\""));
        assertEquals(List.of("#len-interior", "#pattern"), covered(code, "\"B12345\""));
        assertEquals(List.of("#len-max", "#pattern"), covered(code, "\"A123456789\""));
        assertEquals("shorter than minLength 2", reason(code, "\"A\""));
        assertEquals("longer than maxLength 10", reason(code, "\"A1234567890\""));
        // length is fine, but the pattern is violated: the value covers nothing at all
        ValueClassification wrongPattern = classifier.classify(code, json("\"C1\""));
        assertEquals("does not match the pattern ^(A|B)", wrongPattern.outOfSpecReason());
        assertTrue(wrongPattern.covered().isEmpty());
    }

    @Test
    void emptyAndNonEmptyStrings() throws IOException {
        SchemaField name = PartitionGeneratorTest.field("""
                {"type": "string"}
                """, true, true);

        assertEquals(List.of("#empty"), covered(name, "\"\""));
        assertEquals(List.of("#non-empty"), covered(name, "\"Lena\""));
    }

    @Test
    void formats() throws IOException {
        SchemaField boughtDate = PartitionGeneratorTest.field("""
                {"type": "string", "format": "date-time"}
                """, true, true);
        SchemaField orderId = PartitionGeneratorTest.field("""
                {"type": "string", "format": "uuid"}
                """, true, true);
        SchemaField count = PartitionGeneratorTest.field("""
                {"type": "integer", "format": "int32"}
                """, true, true);

        assertEquals(List.of("#format"), covered(boughtDate, "\"2026-10-06T09:51:46.135+00:00\""));
        assertEquals("not a valid date-time", reason(boughtDate, "\"yesterday\""));
        assertEquals(List.of("#format"), covered(orderId, "\"e9cb042e-54d3-4b56-b5fa-67ff1c1c992b\""));
        assertEquals("not a valid uuid", reason(orderId, "\"42\""));
        assertEquals("outside the range of int32", reason(count, "3000000000"));
    }

    @Test
    void enumValues() throws IOException {
        SchemaField seatClass = PartitionGeneratorTest.field("""
                {"type": "string", "enum": ["ECONOMY", "FIRST"]}
                """, true, true);
        SchemaField level = PartitionGeneratorTest.field("""
                {"type": "integer", "enum": [1, 2]}
                """, true, true);

        assertEquals(List.of("#enum=FIRST"), covered(seatClass, "\"FIRST\""));
        assertEquals("not one of the enum values [\"ECONOMY\", \"FIRST\"]", reason(seatClass, "\"BUSINESS\""));
        assertEquals(List.of("#enum=2"), covered(level, "2.0"));
    }

    @Test
    void booleansAndListSizes() throws IOException {
        SchemaField within = PartitionGeneratorTest.field("""
                {"type": "boolean"}
                """, true, true);
        SchemaField tags = PartitionGeneratorTest.field("""
                {"type": "array", "items": {"type": "string"}}
                """, true, true);
        SchemaField items = PartitionGeneratorTest.field("""
                {"type": "array", "minItems": 1, "maxItems": 3, "items": {"type": "string"}}
                """, true, true);

        assertEquals(List.of("#false"), covered(within, "false"));
        assertEquals(List.of("#size-0"), covered(tags, "[]"));
        assertEquals(List.of("#size-1"), covered(tags, "[\"a\"]"));
        assertEquals(List.of("#size-many"), covered(tags, "[\"a\", \"b\"]"));
        assertEquals(List.of("#size-interior"), covered(items, "[\"a\", \"b\"]"));
        assertEquals("fewer than minItems 1 items", reason(items, "[]"));
        assertEquals("more than maxItems 3 items", reason(items, "[1, 2, 3, 4]"));
    }

    @Test
    void variantsByDiscriminatorOrRequiredProperties() throws IOException {
        SchemaField payment = SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree("""
                {"oneOf": [{"$ref": "#/components/schemas/Card"}, {"$ref": "#/components/schemas/Iban"}],
                 "discriminator": {"propertyName": "kind", "mapping": {"card": "#/components/schemas/Card"}}}
                """), null, MAPPER.readTree("""
                {"Card": {"type": "object", "required": ["number"], "properties": {"number": {"type": "string"}}},
                 "Iban": {"type": "object", "required": ["iban"], "properties": {"iban": {"type": "string"}}}}
                """)));
        SchemaField noDiscriminator = SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree("""
                {"anyOf": [{"$ref": "#/components/schemas/Card"}, {"$ref": "#/components/schemas/Iban"}]}
                """), null, MAPPER.readTree("""
                {"Card": {"type": "object", "required": ["number"], "properties": {"number": {"type": "string"}}},
                 "Iban": {"type": "object", "required": ["iban"], "properties": {"iban": {"type": "string"}}}}
                """)));

        assertEquals(List.of("#variant=Card"), covered(payment, "{\"kind\": \"card\", \"number\": \"4111\"}"));
        assertEquals(List.of("#variant=Iban"), covered(payment, "{\"kind\": \"Iban\", \"iban\": \"DE00\"}"));
        assertEquals("unknown discriminator value 'cash'", reason(payment, "{\"kind\": \"cash\"}"));
        assertEquals(List.of("#variant=Iban"), covered(noDiscriminator, "{\"iban\": \"DE00\"}"));
        assertEquals("matches none of the anyOf alternatives", reason(noDiscriminator, "{\"other\": 1}"));
    }

    @Test
    void pathParameterTextIsConvertedToTheDeclaredType() throws IOException {
        SchemaField page = PartitionGeneratorTest.field("""
                {"type": "integer", "minimum": 0}
                """, true, true);
        SchemaField orderId = PartitionGeneratorTest.field("""
                {"type": "string"}
                """, true, true);

        assertEquals(List.of("#min"), ids(classifier.classifyText(page, "0")));
        assertEquals(List.of("#above"), ids(classifier.classifyText(page, "42")));
        assertEquals("not a valid integer: 'abc'", classifier.classifyText(page, "abc").outOfSpecReason());
        assertEquals(List.of("#non-empty"), ids(classifier.classifyText(orderId, "e9cb")));
    }

    @Test
    void classificationOnlyNamesExistingPartitions() throws IOException {
        SchemaField quantity = PartitionGeneratorTest.field("""
                {"type": "integer", "minimum": 1, "maximum": 99}
                """, false, true);
        List<ValuePartition> partitions = new PartitionGenerator().partitions(quantity);

        for (String value : List.of("1", "2", "50", "98", "99", "null")) {
            assertTrue(partitions.containsAll(classifier.classify(quantity, json(value)).covered()), value);
        }
        assertFalse(classifier.classify(quantity, json("50")).isOutOfSpec());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private List<String> covered(SchemaField field, String value) throws IOException {
        ValueClassification classification = classifier.classify(field, json(value));
        assertFalse(classification.isOutOfSpec(), () -> value + ": " + classification.outOfSpecReason());
        return ids(classification);
    }

    private String reason(SchemaField field, String value) throws IOException {
        ValueClassification classification = classifier.classify(field, json(value));
        assertTrue(classification.covered().isEmpty());
        return classification.outOfSpecReason();
    }

    private static List<String> ids(ValueClassification classification) {
        return classification.covered().stream().map(ValuePartition::id).collect(Collectors.toList());
    }

    private static JsonNode json(String value) throws IOException {
        return MAPPER.readTree(value);
    }
}
