package com.github.legregor.microservice.coverage.evaluation.responseValuePartition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.ElementPartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.OutOfSpecValue;
import com.github.legregor.microservice.coverage.evaluation.responseField.ObservedResponse;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldTreeBuilder;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;
import com.github.legregor.microservice.coverage.openapi.OpenApiResponseResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ResponseValuePartitionCoverage}: scope (covered response fields per status code), partitions from
 * the response schema, values per status code, no value, list items, values outside the spec, pooled /
 * constraint-only / hierarchical values and the n/a reasons. Expected values are calculated by hand (see comments).
 */
class ResponseValuePartitionCoverageTest {

    private static final List<String> DEFAULT_EXPECTED = List.of("2xx", "4xx");
    private static final String QUOTES = "/quotes";
    private static final double DELTA = 1e-9;
    private static final String UUID_1 = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String UUID_2 = "3fa85f64-5717-4562-b3fc-2c963f66afa7";

    @TempDir
    Path specDir;

    private ResponseFieldTreeBuilder builder;

    @BeforeEach
    void writeSpec() throws IOException {
        Files.writeString(specDir.resolve("pricing.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/quotes": {"post": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {
                       "schema": {"$ref": "#/components/schemas/Quote"}}}},
                     "400": {"description": "invalid", "content": {"application/json": {
                       "schema": {"$ref": "#/components/schemas/ErrorResponse"}}}},
                     "404": {"description": "unknown", "content": {"application/json": {
                       "schema": {"$ref": "#/components/schemas/ErrorResponse"}}}}}}},
                   "/prices": {"get": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "array", "items": {"type": "object", "required": ["sku", "price"], "properties": {
                         "sku": {"type": "string"}, "price": {"type": "number"}}}}}}}}}},
                   "/nested": {"get": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["data"], "properties": {
                         "data": {"type": "object", "required": ["meta"], "properties": {
                           "meta": {"type": "object"}}}}}}}}}}}},
                 "components": {"schemas": {
                   "Quote": {"type": "object", "required": ["quoteId", "currency", "total", "items"], "properties": {
                     "quoteId": {"type": "string", "format": "uuid"},
                     "currency": {"type": "string", "enum": ["EUR", "CHF", "USD"]},
                     "total": {"type": "number", "minimum": 0},
                     "discountApplied": {"type": "number", "minimum": 0, "maximum": 50},
                     "items": {"type": "array", "items": {"$ref": "#/components/schemas/Item"}}}},
                   "Item": {"type": "object", "required": ["sku", "quantity"], "properties": {
                     "sku": {"type": "string"},
                     "quantity": {"type": "integer", "minimum": 1, "maximum": 99}}},
                   "ErrorResponse": {"type": "object", "required": ["message"], "properties": {
                     "message": {"type": "string"}}}}}}
                """);
        builder = new ResponseFieldTreeBuilder(new OpenApiResponseResolver(new OpenApiSpecRegistry(specDir)));
    }

    /**
     * Two 200 responses. Partitions (covered / all):
     * quoteId #format 1/1; currency #enum=EUR, CHF, USD 1/3; total #min, #above 2/2;
     * discountApplied #none, #min, #interior, #max 2/4 (absent once, 10 once); items #size-0, -1, -many 2/3 (type
     * default); items[].sku #empty, #non-empty 2/2 (type default); items[].quantity #min, #interior, #max 2/3.
     * Pooled 12/18; constraint-only 8/13 (without items and items[].sku);
     * hierarchical: items = (2/3 + 1 + 2/3) / 3 = 7/9; status 200 = (1 + 1/3 + 1 + 1/2 + 7/9) / 5 = 13/18.
     */
    @Test
    void pooledConstraintOnlyAndHierarchicalValues() {
        ResponseValuePartitionCoverage coverage = coverage(QUOTES, "POST", List.of(
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_1 + "\", \"currency\": \"EUR\", \"total\": 0, "
                        + "\"items\": []}"),
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_2 + "\", \"currency\": \"EUR\", \"total\": 12.5, "
                        + "\"discountApplied\": 10, \"items\": [{\"sku\": \"ABC\", \"quantity\": 1}, "
                        + "{\"sku\": \"\", \"quantity\": 5}]}")));

        assertFalse(coverage.isNotApplicable());
        assertEquals(18, coverage.getPartitionCount());
        assertEquals(12, coverage.getCoveredCount());
        assertEquals(12.0 / 18 * 100, coverage.getPooledCoveragePercentage(), DELTA);
        assertEquals(13, coverage.getConstraintPartitionCount());
        assertEquals(8, coverage.getConstraintCoveredCount());
        assertEquals(8.0 / 13 * 100, coverage.getConstraintPooledCoveragePercentage(), DELTA);
        assertEquals(13.0 / 18 * 100, coverage.getHierarchicalCoveragePercentage(), DELTA);

        Map<String, ElementPartitionCoverage> fields = byName(coverage);
        assertEquals(List.of("200:quoteId", "200:currency", "200:total", "200:discountApplied", "200:items",
                "200:items[].sku", "200:items[].quantity"), List.copyOf(fields.keySet()));
        assertEquals(List.of("#enum=CHF", "#enum=USD"), fields.get("200:currency").getUncoveredPartitions());
        assertEquals(List.of("#none", "#interior"), fields.get("200:discountApplied").getCoveredPartitions());
        assertEquals(List.of("#size-1"), fields.get("200:items").getUncoveredPartitions());
        assertEquals(List.of("#max"), fields.get("200:items[].quantity").getUncoveredPartitions());
        assertEquals(0, coverage.getOutOfSpecValueCount());
    }

    /** The same field under two status codes has separate partitions; a value outside the spec is not counted. */
    @Test
    void valuesPerStatusCodeAndValuesOutsideTheSpec() {
        ResponseValuePartitionCoverage coverage = coverage(QUOTES, "POST", List.of(
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_1 + "\", \"currency\": \"CHF\", \"total\": -1, "
                        + "\"items\": [{\"sku\": \"A\", \"quantity\": 1}]}"),
                new ObservedResponse(400, "{\"message\": \"bad request\"}"),
                new ObservedResponse(404, "{\"message\": \"\"}"),
                new ObservedResponse(500, "{\"message\": \"ignored\"}")));

        assertEquals(List.of(200, 400, 404), coverage.getStatusCodes().stream()
                .map(ResponseValuePartitionCoverage.StatusPartitionCoverage::statusCode).toList());
        Map<String, ElementPartitionCoverage> fields = byName(coverage);
        assertEquals(List.of("#non-empty"), fields.get("400:message").getCoveredPartitions());
        assertEquals(List.of("#empty"), fields.get("404:message").getCoveredPartitions());

        ElementPartitionCoverage total = fields.get("200:total");
        assertEquals(0, total.getCoveredCount()); // -1 covers no partition
        assertEquals(2, total.getReachableCount());
        assertEquals(1, total.getOutOfSpecValues().size());
        OutOfSpecValue outOfSpec = total.getOutOfSpecValues().get(0);
        assertEquals("-1", outOfSpec.value());
        assertEquals(1, outOfSpec.count());
        assertEquals(1, coverage.getOutOfSpecValueCount());
    }

    /** A top-level list: the list itself is {@code []}, its item fields {@code [].x}. */
    @Test
    void topLevelList() {
        ResponseValuePartitionCoverage coverage = coverage("/prices", "GET", List.of(
                new ObservedResponse(200, "[]"),
                new ObservedResponse(200, "[{\"sku\": \"ABC\", \"price\": 1.5}]")));

        Map<String, ElementPartitionCoverage> fields = byName(coverage);
        assertEquals(List.of("200:[]", "200:[].sku", "200:[].price"), List.copyOf(fields.keySet()));
        assertEquals(List.of("#size-0", "#size-1"), fields.get("200:[]").getCoveredPartitions());
        assertEquals(List.of("#non-empty"), fields.get("200:[].sku").getCoveredPartitions());
        assertEquals(List.of("#positive"), fields.get("200:[].price").getCoveredPartitions());
        assertEquals(4.0 / 8 * 100, coverage.getPooledCoveragePercentage(), DELTA);
    }

    /** An explicit null of an optional field covers #none, like an absent field. */
    @Test
    void nullCountsAsNoValue() {
        ResponseValuePartitionCoverage coverage = coverage(QUOTES, "POST", List.of(
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_1 + "\", \"currency\": \"EUR\", \"total\": 1, "
                        + "\"discountApplied\": 50, \"items\": []}"),
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_2 + "\", \"currency\": \"EUR\", \"total\": 1, "
                        + "\"discountApplied\": null, \"items\": []}")));

        assertEquals(List.of("#none", "#max"), byName(coverage).get("200:discountApplied").getCoveredPartitions());
    }

    @Test
    void notApplicableReasons() {
        assertEquals(ResponseValuePartitionNotApplicableReason.CALLEE_NOT_TRACED,
                reason(QUOTES, "POST", List.of()));
        assertEquals(ResponseValuePartitionNotApplicableReason.NO_EXPECTED_STATUS_RETURNED,
                reason(QUOTES, "POST", List.of(new ObservedResponse(503, "{}"))));
        assertEquals(ResponseValuePartitionNotApplicableReason.BODY_NOT_CAPTURED,
                reason(QUOTES, "POST", List.of(new ObservedResponse(200, null))));
        // responses were judged, but no field carried a value
        assertEquals(ResponseValuePartitionNotApplicableReason.NO_COVERED_FIELDS,
                reason(QUOTES, "POST", List.of(new ObservedResponse(200, "{}"))));
        // covered fields, but only required objects without own partitions
        assertEquals(ResponseValuePartitionNotApplicableReason.NO_PARTITIONS,
                reason("/nested", "GET", List.of(new ObservedResponse(200, "{\"data\": {\"meta\": {}}}"))));
    }

    @Test
    void notApplicableWithoutFieldCoverage() {
        ResponseValuePartitionCoverage coverage = ResponseValuePartitionCoverage.of(null,
                new PartitionGenerator(), new ValueClassifier());
        assertTrue(coverage.isNotApplicable());
        assertNull(coverage.getPooledCoveragePercentage());
        assertNull(coverage.getHierarchicalCoveragePercentage());
    }

    private ResponseValuePartitionCoverage coverage(String path, String method, List<ObservedResponse> observed) {
        return coverage(path, method, observed, DEFAULT_EXPECTED);
    }

    private ResponseValuePartitionCoverage coverage(String path, String method, List<ObservedResponse> observed,
                                                    List<String> expected) {
        ResponseFieldCoverage fieldCoverage = ResponseFieldCoverage.of(observed, expected, builder,
                "pricing", method, path);
        return ResponseValuePartitionCoverage.of(fieldCoverage, new PartitionGenerator(), new ValueClassifier());
    }

    /**
     * Body class only ("currency=EUR"): the CHF response at the same HTTP status code is not in a relevant class, so
     * its values are not classified (currency CHF and total 5 stay uncovered).
     */
    @Test
    void valuesOfResponsesOutsideTheRelevantClassesAreNotClassified() {
        ResponseValuePartitionCoverage coverage = coverage(QUOTES, "POST", List.of(
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_1 + "\", \"currency\": \"EUR\", \"total\": 0, "
                        + "\"items\": []}"),
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_2 + "\", \"currency\": \"CHF\", \"total\": 5, "
                        + "\"items\": []}")), List.of("currency=EUR"));

        Map<String, ElementPartitionCoverage> fields = byName(coverage);
        assertEquals(List.of("#enum=EUR"), fields.get("200:currency").getCoveredPartitions());
        assertEquals(List.of("#min"), fields.get("200:total").getCoveredPartitions());
    }

    private ResponseValuePartitionNotApplicableReason reason(String path, String method,
                                                             List<ObservedResponse> observed) {
        ResponseValuePartitionCoverage coverage = coverage(path, method, observed);
        assertTrue(coverage.isNotApplicable());
        assertNull(coverage.getPooledCoveragePercentage());
        return coverage.getNotApplicableReason();
    }

    private static Map<String, ElementPartitionCoverage> byName(ResponseValuePartitionCoverage coverage) {
        return coverage.getFields().stream().collect(Collectors.toMap(ElementPartitionCoverage::getElement,
                Function.identity(), (a, b) -> a, java.util.LinkedHashMap::new));
    }
}
