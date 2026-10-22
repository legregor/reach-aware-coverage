package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.openapi.OpenApiSchema;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaFieldReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for {@link PartitionGenerator}: which partitions a schema field gets. */
class PartitionGeneratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final PartitionGenerator generator = new PartitionGenerator();

    @Test
    void integerRangeWithBoundariesAndNoValue() throws IOException {
        SchemaField quantity = field("""
                {"type": "integer", "minimum": 1, "maximum": 99}
                """, false, true);

        List<ValuePartition> partitions = generator.partitions(quantity);

        assertEquals(List.of("#none", "#min", "#interior", "#max"), ids(partitions));
        assertEquals(List.of(PartitionGroup.NO_VALUE, PartitionGroup.RANGE, PartitionGroup.RANGE, PartitionGroup.RANGE),
                partitions.stream().map(ValuePartition::group).collect(Collectors.toList()));
        assertEquals(List.of(PartitionSource.CONSTRAINT), sources(partitions));
    }

    @Test
    void noValueWithoutDeclaredRequiredListIsATypeDefault() throws IOException {
        List<ValuePartition> partitions = generator.partitions(field("""
                {"type": "integer", "minimum": 1}
                """, false, false));

        assertEquals(List.of("#none", "#min", "#above"), ids(partitions));
        assertEquals(PartitionSource.TYPE_DEFAULT, partitions.get(0).source());
        assertEquals(PartitionSource.CONSTRAINT, partitions.get(1).source());
    }

    @Test
    void requiredNullableFieldHasNoValuePartition() throws IOException {
        assertEquals(List.of("#none", "#true", "#false"), ids(generator.partitions(field("""
                {"type": "boolean", "nullable": true}
                """, true, true))));
        assertEquals(List.of("#true", "#false"), ids(generator.partitions(field("""
                {"type": "boolean"}
                """, true, true))));
    }

    @Test
    void integerBoundaryEdgeCases() throws IOException {
        // exclusive bounds: first and last integer inside are 1 and 2, nothing in between
        assertEquals(List.of("#min", "#max"), ids(required("""
                {"type": "integer", "minimum": 0, "exclusiveMinimum": true, "maximum": 3, "exclusiveMaximum": true}
                """)));
        assertEquals(List.of("#min"), ids(required("""
                {"type": "integer", "minimum": 5, "maximum": 5}
                """)));
        assertEquals(List.of("#below", "#max"), ids(required("""
                {"type": "integer", "maximum": 10}
                """)));
        // unsatisfiable range: no range partitions
        assertEquals(List.of(), ids(required("""
                {"type": "integer", "minimum": 5, "maximum": 3}
                """)));
    }

    @Test
    void numberWithExclusiveBoundHasNoBoundaryPartitionThere() throws IOException {
        assertEquals(List.of("#interior", "#max"), ids(required("""
                {"type": "number", "exclusiveMinimum": 0, "maximum": 1}
                """)));
        assertEquals(List.of("#above"), ids(required("""
                {"type": "number", "exclusiveMinimum": 0}
                """)));
    }

    @Test
    void numbersWithoutRangeGetSignPartitions() throws IOException {
        List<ValuePartition> partitions = required("""
                {"type": "number", "format": "double"}
                """);

        assertEquals(List.of("#negative", "#zero", "#positive"), ids(partitions));
        assertEquals(List.of(PartitionSource.TYPE_DEFAULT), sources(partitions));
    }

    @Test
    void stringGroups() throws IOException {
        assertEquals(List.of("#empty", "#non-empty"), ids(required("""
                {"type": "string"}
                """)));
        assertEquals(List.of("#len-min", "#len-interior", "#len-max"), ids(required("""
                {"type": "string", "maxLength": 10}
                """)));
        // length and pattern count separately ("each choice")
        assertEquals(List.of("#len-min", "#len-interior", "#len-max", "#pattern"), ids(required("""
                {"type": "string", "minLength": 2, "maxLength": 10, "pattern": "^(A|B)"}
                """)));
        assertEquals(List.of("#format"), ids(required("""
                {"type": "string", "format": "date-time"}
                """)));
    }

    @Test
    void enumReplacesTheTypeGroups() throws IOException {
        List<ValuePartition> partitions = required("""
                {"type": "string", "maxLength": 20, "enum": ["ECONOMY", "BUSINESS", "FIRST"]}
                """);

        assertEquals(List.of("#enum=ECONOMY", "#enum=BUSINESS", "#enum=FIRST"), ids(partitions));
        assertEquals(List.of(PartitionSource.CONSTRAINT), sources(partitions));
    }

    @Test
    void listSizes() throws IOException {
        assertEquals(List.of("#size-0", "#size-1", "#size-many"), ids(required("""
                {"type": "array", "items": {"type": "string"}}
                """)));
        assertEquals(List.of("#size-min", "#size-interior", "#size-max"), ids(required("""
                {"type": "array", "minItems": 1, "maxItems": 3, "items": {"type": "string"}}
                """)));
    }

    @Test
    void oneOfAlternativesAreVariantPartitions() throws IOException {
        SchemaField payment = SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree("""
                {"oneOf": [{"$ref": "#/components/schemas/Card"}, {"$ref": "#/components/schemas/Iban"}]}
                """), null, MAPPER.readTree("""
                {"Card": {"type": "object", "properties": {"number": {"type": "string"}}},
                 "Iban": {"type": "object", "properties": {"iban": {"type": "string"}}}}
                """)));

        assertEquals(List.of("#variant=Card", "#variant=Iban"), ids(generator.partitions(payment)));
    }

    @Test
    void plainObjectHasNoOwnPartitions() throws IOException {
        assertEquals(List.of(), ids(required("""
                {"type": "object", "properties": {"a": {"type": "string"}}}
                """)));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private List<ValuePartition> required(String schema) throws IOException {
        return generator.partitions(field(schema, true, true));
    }

    /** The field {@code f} of an object, with or without a required list. */
    static SchemaField field(String schema, boolean required, boolean requiredDeclared) throws IOException {
        String requiredList = required ? "\"required\": [\"f\"], " : requiredDeclared ? "\"required\": [], " : "";
        SchemaField object = SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree(
                "{\"type\": \"object\", " + requiredList + "\"properties\": {\"f\": " + schema + "}}"), null,
                MAPPER.createObjectNode()));
        return object.getChildren().get(0);
    }

    private static List<String> ids(List<ValuePartition> partitions) {
        return partitions.stream().map(ValuePartition::id).collect(Collectors.toList());
    }

    private static List<PartitionSource> sources(List<ValuePartition> partitions) {
        return partitions.stream().map(ValuePartition::source).distinct().collect(Collectors.toList());
    }
}
