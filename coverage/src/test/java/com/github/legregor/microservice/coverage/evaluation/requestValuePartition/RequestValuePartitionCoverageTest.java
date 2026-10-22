package com.github.legregor.microservice.coverage.evaluation.requestValuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.evaluation.requestElement.ExecutedRequestElementsCollector;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTree;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTestTrees;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RequestValuePartitionCoverage}: scope (executed elements), pooled and hierarchical values,
 * out-of-spec values and n/a reasons. Trees, executed elements and values are built from real bodies.
 */
class RequestValuePartitionCoverageTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double DELTA = 1e-9;

    /** The structure of the aggregation example in the paper. */
    private static final String PAPER_EXAMPLE = """
            {"type": "object", "required": ["flag", "address"], "properties": {
              "customerId": {"type": "integer", "minimum": 1, "maximum": 99},
              "flag": {"type": "boolean"},
              "address": {"type": "object", "required": ["city"],
                          "oneOf": [{"title": "Home", "required": ["zip"]}, {"title": "Office", "required": ["street"]}],
                          "properties": {"zip": {"type": "integer", "minimum": 1, "maximum": 99},
                                         "city": {"type": "boolean"}}}}}
            """;

    @Test
    void paperExampleForPooledAndHierarchicalAggregation() throws IOException {
        // customerId 2 of 4 (#min, #interior), flag 2 of 2, address 1 of 2 own (#variant=Home),
        // address.zip 1 of 4 (#interior), address.city 2 of 2
        RequestValuePartitionCoverage coverage = coverage(PAPER_EXAMPLE, "/customers", List.of(), List.of(
                "{\"customerId\": 1, \"flag\": true, \"address\": {\"zip\": 5, \"city\": true}}",
                "{\"customerId\": 50, \"flag\": false, \"address\": {\"zip\": 7, \"city\": false}}"));

        assertFalse(coverage.isNotApplicable());
        assertEquals(14, coverage.getReachableCount());
        assertEquals(8, coverage.getCoveredCount());
        assertEquals(8.0 / 14.0 * 100.0, coverage.getPooledCoveragePercentage(), DELTA);
        // address = (1/2 + 1/4 + 1) / 3; request = (1/2 + 1 + address) / 3
        double address = (0.5 + 0.25 + 1.0) / 3.0;
        assertEquals((0.5 + 1.0 + address) / 3.0 * 100.0, coverage.getHierarchicalCoveragePercentage(), DELTA);

        ElementPartitionCoverage customerId = element(coverage, "body:customerId");
        assertEquals(List.of("#min", "#interior"), customerId.getCoveredPartitions());
        assertEquals(List.of("#none", "#max"), customerId.getUncoveredPartitions());
        assertEquals(List.of("#variant=Home"), element(coverage, "body:address").getCoveredPartitions());
    }

    @Test
    void constraintOnlyValueLeavesTypeDefaultsOut() throws IOException {
        RequestValuePartitionCoverage coverage = coverage(PAPER_EXAMPLE, "/customers", List.of(), List.of(
                "{\"customerId\": 1, \"flag\": true, \"address\": {\"zip\": 5, \"city\": true}}",
                "{\"customerId\": 50, \"flag\": false, \"address\": {\"zip\": 7, \"city\": false}}"));

        // flag and city (booleans) are type defaults: customerId 2/4, address 1/2, zip 1/4
        assertEquals(10, coverage.getConstraintReachableCount());
        assertEquals(4, coverage.getConstraintCoveredCount());
        assertEquals(40.0, coverage.getConstraintPooledCoveragePercentage(), DELTA);
    }

    @Test
    void onlyExecutedElementsAreInScope() throws IOException {
        RequestValuePartitionCoverage coverage = coverage("""
                {"type": "object", "properties": {
                  "quantity": {"type": "integer", "minimum": 1, "maximum": 99},
                  "comment": {"type": "string", "maxLength": 100}}}
                """, "/orders", List.of(), List.of("{\"quantity\": 5, \"comment\": null}", "{\"quantity\": 99}"));

        // comment never had a value: a gap of request element coverage, not judged here
        assertEquals(List.of("body:quantity"), names(coverage));
        // quantity: #none, #min, #interior, #max -> #interior, #max covered
        assertEquals(50.0, coverage.getPooledCoveragePercentage(), DELTA);
    }

    @Test
    void absentValuesCoverTheNoValuePartition() throws IOException {
        RequestValuePartitionCoverage coverage = coverage("""
                {"type": "object", "properties": {"quantity": {"type": "integer", "minimum": 1, "maximum": 99}}}
                """, "/orders", List.of(), List.of("{\"quantity\": 5}", "{}"));

        assertEquals(List.of("#none", "#interior"), element(coverage, "body:quantity").getCoveredPartitions());
    }

    @Test
    void valuesOutsideTheSpecAreListedAndNotCounted() throws IOException {
        RequestValuePartitionCoverage coverage = coverage("""
                {"type": "object", "required": ["id"], "properties": {
                  "id": {"type": "string"},
                  "quantity": {"type": "integer", "minimum": 1, "maximum": 99}}}
                """, "/orders", List.of(), List.of("{\"id\": \"a\", \"quantity\": 0}", "{\"id\": null, \"quantity\": 0}",
                "{\"id\": \"b\", \"quantity\": 5}"));

        ElementPartitionCoverage quantity = element(coverage, "body:quantity");
        assertEquals(List.of("#interior"), quantity.getCoveredPartitions());
        assertEquals(List.of(new OutOfSpecValue("0", "below minimum 1", 2)), quantity.getOutOfSpecValues());
        ElementPartitionCoverage id = element(coverage, "body:id");
        assertEquals(List.of(new OutOfSpecValue("(no value)",
                "no value (absent or null) for a required, non-nullable element", 1)), id.getOutOfSpecValues());
        assertEquals(2, coverage.getOutOfSpecValueCount());
    }

    @Test
    void pathParametersAreClassifiedFromTheUrl() throws IOException {
        SchemaField page = SchemaFieldReader.readParameter(new OpenApiSchema(null, null, null), MAPPER.readTree("""
                {"name": "page", "in": "path", "type": "integer", "minimum": 0}
                """));
        SchemaField orderId = SchemaFieldReader.readParameter(new OpenApiSchema(null, null, null), MAPPER.readTree("""
                {"name": "orderId", "in": "path", "type": "string", "minLength": 1}
                """));

        RequestValuePartitionCoverage coverage = coverage(null, "/orders/{orderId}/pages/{page}",
                List.of(orderId, page), List.of(),
                List.of("http://shop:8080/orders/a%20b/pages/0", "http://shop:8080/orders/x/pages/3?size=10",
                        "http://shop:8080/orders/x/pages/abc"));

        assertEquals(List.of("#min", "#above"), element(coverage, "path:page").getCoveredPartitions());
        assertEquals(List.of(new OutOfSpecValue("abc", "not a valid integer: 'abc'", 1)),
                element(coverage, "path:page").getOutOfSpecValues());
        // "a b" (decoded) has length 3, "x" has the minimum length 1
        assertEquals(List.of("#len-min", "#len-above"), element(coverage, "path:orderId").getCoveredPartitions());
    }

    @Test
    void anEmptyPathParameterIsNotReachable() throws IOException {
        SchemaField orderId = SchemaFieldReader.readParameter(new OpenApiSchema(null, null, null), MAPPER.readTree("""
                {"name": "orderId", "in": "path", "type": "string"}
                """));

        ElementPartitionCoverage element = element(coverage(null, "/orders/{orderId}", List.of(orderId), List.of(),
                List.of("http://shop/orders/abc")), "path:orderId");

        assertEquals(List.of("#non-empty"), element.getCoveredPartitions());
        assertEquals(List.of("#empty"), element.getNotReachablePartitions());
        assertEquals(100.0, element.getCoveragePercentage(), DELTA);
    }

    @Test
    void staticConstantsAndPrimitivesRestrictTheReachablePartitions() throws IOException {
        SchemaField body = SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree("""
                {"type": "object", "properties": {
                  "seatClass": {"type": "string", "enum": ["ECONOMY", "BUSINESS", "FIRST"]},
                  "count": {"type": "integer", "minimum": 1, "maximum": 9},
                  "comment": {"type": "string"}}}
                """), null, MAPPER.createObjectNode()));
        RequestElementTree tree = RequestElementTestTrees.tree(List.of(), body);
        // the caller always sends FIRST; count is a primitive int that starts at 0 (outside the spec) or is set to 2
        RequestElementTestTrees.staticValues(tree, "body:seatClass", List.of("\"FIRST\""), false);
        RequestElementTestTrees.staticValues(tree, "body:count", List.of("0", "2"), true);
        List<String> bodies = List.of("{\"seatClass\": \"FIRST\", \"count\": 2, \"comment\": \"x\"}");
        RequestElementCoverage elementCoverage = new RequestElementCoverage(tree,
                new ExecutedRequestElementsCollector().collect("/seats", bodies.size(), bodies));
        PartitionGenerator generator = new PartitionGenerator();
        RequestValuePartitionCoverage coverage = new RequestValuePartitionCoverage(tree, elementCoverage,
                new ExecutedRequestValuesCollector().collect(tree, "/seats", List.of(), bodies), generator,
                new ValueClassifier(generator));

        ElementPartitionCoverage seatClass = element(coverage, "body:seatClass");
        // #none stays reachable (the field may be unset), ECONOMY and BUSINESS are never sent
        assertEquals(List.of("#enum=ECONOMY", "#enum=BUSINESS"), seatClass.getNotReachablePartitions());
        assertEquals(List.of("#enum=FIRST"), seatClass.getCoveredPartitions());
        assertEquals(List.of("#none"), seatClass.getUncoveredPartitions());
        assertEquals(List.of("\"FIRST\""), seatClass.getStaticConstants());

        ElementPartitionCoverage count = element(coverage, "body:count");
        assertTrue(count.isNeverNull());
        assertEquals(List.of("#none", "#min", "#max"), count.getNotReachablePartitions());
        assertEquals(List.of("#interior"), count.getCoveredPartitions());
        assertEquals(List.of(new OutOfSpecValue("0", "below minimum 1", 1)), count.getOutOfSpecConstants());

        // without static information, every partition of an executed element is reachable
        assertTrue(element(coverage, "body:comment").getNotReachablePartitions().isEmpty());
    }

    @Test
    void notApplicableReasons() throws IOException {
        // no elements at all
        assertEquals(NotApplicableReason.NO_ELEMENTS, coverage("""
                {"type": "object", "properties": {}}
                """, "/health", List.of(), List.of("{}")).getNotApplicableReason());

        // elements, but none was ever sent with a value
        RequestValuePartitionCoverage nothingSent = coverage("""
                {"type": "object", "properties": {"comment": {"type": "string"}}}
                """, "/orders", List.of(), List.of("{\"comment\": null}"));
        assertEquals(NotApplicableReason.NO_EXECUTED_ELEMENTS, nothingSent.getNotApplicableReason());
        assertNull(nothingSent.getPooledCoveragePercentage());
        assertNull(nothingSent.getHierarchicalCoveragePercentage());

        // executed element without own partitions and without children (required plain object)
        assertEquals(NotApplicableReason.NO_PARTITIONS, coverage("""
                {"type": "object", "required": ["meta"], "properties": {"meta": {"type": "object"}}}
                """, "/orders", List.of(), List.of("{\"meta\": {}}")).getNotApplicableReason());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static RequestValuePartitionCoverage coverage(String bodySchema, String endpointPath,
                                                          List<String> bodies) throws IOException {
        return coverage(bodySchema, endpointPath, List.of(), bodies, List.of());
    }

    private static RequestValuePartitionCoverage coverage(String bodySchema, String endpointPath,
                                                          List<SchemaField> pathParameters, List<String> bodies)
            throws IOException {
        return coverage(bodySchema, endpointPath, pathParameters, bodies, List.of());
    }

    private static RequestValuePartitionCoverage coverage(String bodySchema, String endpointPath,
                                                          List<SchemaField> pathParameters, List<String> bodies,
                                                          List<String> urls) throws IOException {
        SchemaField body = bodySchema == null ? null : SchemaFieldReader.read(new OpenApiSchema(
                MAPPER.readTree(bodySchema), null, MAPPER.createObjectNode()));
        RequestElementTree tree = RequestElementTestTrees.tree(pathParameters, body);
        int calls = Math.max(bodies.size(), urls.size());
        RequestElementCoverage elementCoverage = new RequestElementCoverage(tree,
                new ExecutedRequestElementsCollector().collect(endpointPath, calls, bodies));
        Map<String, List<JsonNode>> values = new ExecutedRequestValuesCollector().collect(tree, endpointPath, urls,
                bodies);
        PartitionGenerator generator = new PartitionGenerator();
        return new RequestValuePartitionCoverage(tree, elementCoverage, values, generator,
                new ValueClassifier(generator));
    }

    private static ElementPartitionCoverage element(RequestValuePartitionCoverage coverage, String name) {
        return coverage.getElements().stream().filter(element -> element.getElement().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("no element " + name + " in " + names(coverage)));
    }

    private static List<String> names(RequestValuePartitionCoverage coverage) {
        return coverage.getElements().stream().map(ElementPartitionCoverage::getElement).collect(Collectors.toList());
    }
}
