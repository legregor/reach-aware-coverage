package com.github.legregor.microservice.coverage.evaluation.requestElement;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link RequestElementCoverage}: pooled, hierarchical and optional-only values of one connection. */
class RequestElementCoverageTest {

    private static final double DELTA = 1e-9;

    @Test
    void pooledCoverageCountsReachableElementsOnly() {
        RequestElementTree tree = tree(
                path("path:orderId"),
                body("body:id", false, false),        // not reachable: not an item
                body("body:from", true, false),
                body("body:to", true, false),
                body("body:weight", true, false));
        ExecutedRequestElements executed = executed(1, 1, "path:orderId", "body:from");

        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed);

        assertEquals(50.0, coverage.getPooledCoveragePercentage(), DELTA);   // 2 of 4
        assertEquals(List.of("path:orderId", "body:from"), coverage.getCoveredElements());
        assertEquals(List.of("body:to", "body:weight"), coverage.getUncoveredElements());
        assertEquals(List.of("body:id"), coverage.getNotReachableElements());
        assertEquals(4, coverage.getReachableCount());
        assertEquals(2, coverage.getCoveredCount());
    }

    @Test
    void hierarchicalExampleFromThePaper() {
        // the structure of the aggregation example in the paper, applied to element coverage:
        // top level: a (covered), b (not covered), address (covered) with zip (not covered) and city (covered)
        RequestElement address = body("body:address", true, false);
        address.addChild(body("body:address.zip", true, false));
        address.addChild(body("body:address.city", true, false));
        RequestElementTree tree = tree(body("body:a", true, false), body("body:b", true, false), address);
        ExecutedRequestElements executed = executed(1, 1, "body:a", "body:address", "body:address.city");

        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed);

        // pooled: a, address, city of a, b, address, zip, city = 3 of 5
        assertEquals(60.0, coverage.getPooledCoveragePercentage(), DELTA);
        // hierarchical: address = (1 + 0 + 1) / 3 = 2/3; request = (1 + 0 + 2/3) / 3 = 5/9
        assertEquals(5.0 / 9.0 * 100.0, coverage.getHierarchicalCoveragePercentage(), DELTA);
    }

    @Test
    void optionalOnlyLeavesRequiredElementsOut() {
        RequestElementTree tree = tree(
                path("path:id"),                       // required
                body("body:name", true, true),         // required
                body("body:comment", true, false),
                body("body:voucher", true, false));
        ExecutedRequestElements executed = executed(1, 1, "path:id", "body:name", "body:comment");

        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed);

        assertEquals(75.0, coverage.getPooledCoveragePercentage(), DELTA);
        assertEquals(50.0, coverage.getOptionalPooledCoveragePercentage(), DELTA);
    }

    @Test
    void connectionWithoutElementsIsNotApplicable() {
        RequestElementCoverage coverage = new RequestElementCoverage(tree(), executed(3, 0));

        assertTrue(coverage.isNotApplicable());
        assertNull(coverage.getPooledCoveragePercentage());
        assertNull(coverage.getHierarchicalCoveragePercentage());
        assertNull(coverage.getOptionalPooledCoveragePercentage());
    }

    @Test
    void bodyElementsAreNotApplicableWhenNoBodyWasRecorded() {
        RequestElementTree tree = tree(path("path:orderId"), body("body:from", true, false), body("body:to", true, false));

        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed(2, 0, "path:orderId"));

        assertTrue(coverage.isBodyNotRecorded());
        assertEquals(100.0, coverage.getPooledCoveragePercentage(), DELTA);   // only the path parameter counts
        assertEquals(List.of("body:from", "body:to"), coverage.getNotRecordedElements());
        assertTrue(coverage.getUncoveredElements().isEmpty());
    }

    @Test
    void onlyBodyAndNothingRecordedIsNotApplicable() {
        RequestElementTree tree = tree(body("body:from", true, false));

        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed(1, 0));

        assertTrue(coverage.isNotApplicable());
        assertNull(coverage.getPooledCoveragePercentage());
    }

    @Test
    void notApplicableReasons() {
        assertEquals(NotApplicableReason.NO_ELEMENTS,
                new RequestElementCoverage(tree(), executed(1, 0)).getNotApplicableReason());
        assertEquals(NotApplicableReason.NONE_REACHABLE,
                new RequestElementCoverage(tree(body("body:id", false, false)), executed(1, 1)).getNotApplicableReason());

        RequestElementTree bodyOnly = tree(body("body:from", true, false));
        // no server span for the call: the called service is not traced (e.g. a mock)
        RequestElementCoverage notTraced = new RequestElementCoverage(bodyOnly,
                new ExecutedRequestElements(Set.of(), 1, 0, 0));
        assertEquals(NotApplicableReason.CALLEE_NOT_TRACED, notTraced.getNotApplicableReason());
        // server span exists, but it has no body
        RequestElementCoverage notCaptured = new RequestElementCoverage(bodyOnly,
                new ExecutedRequestElements(Set.of(), 1, 1, 0));
        assertEquals(NotApplicableReason.BODY_NOT_CAPTURED, notCaptured.getNotApplicableReason());

        // applicable coverage has no reason
        assertNull(new RequestElementCoverage(bodyOnly, executed(1, 1, "body:from")).getNotApplicableReason());
    }

    @Test
    void bodyNotRecordedReasonWhenPathParametersStillCount() {
        RequestElementTree tree = tree(path("path:orderId"), body("body:from", true, false));

        RequestElementCoverage coverage = new RequestElementCoverage(tree,
                new ExecutedRequestElements(Set.of("path:orderId"), 2, 0, 0));

        assertFalse(coverage.isNotApplicable());
        assertNull(coverage.getNotApplicableReason());
        assertEquals(NotApplicableReason.CALLEE_NOT_TRACED, coverage.getBodyNotRecordedReason());
    }

    @Test
    void recordedBodyWithNullValuesIsJudged() {
        RequestElementTree tree = tree(body("body:from", true, false), body("body:to", true, false));

        // a body was recorded, but every field was null
        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed(1, 1));

        assertFalse(coverage.isBodyNotRecorded());
        assertEquals(0.0, coverage.getPooledCoveragePercentage(), DELTA);
    }

    @Test
    void sentFieldsUnknownToTheSpecAreListed() {
        RequestElementTree tree = tree(body("body:email", true, false));

        RequestElementCoverage coverage = new RequestElementCoverage(tree, executed(1, 1, "body:email", "body:debug"));

        assertEquals(List.of("body:debug"), coverage.getUnexpectedElements());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static RequestElement path(String name) {
        return new RequestElement(name, RequestElement.Kind.PATH_PARAMETER, "string", true, true);
    }

    private static RequestElement body(String name, boolean reachable, boolean required) {
        return new RequestElement(name, RequestElement.Kind.BODY_PROPERTY, "string", required, reachable);
    }

    private static RequestElementTree tree(RequestElement... elements) {
        return new RequestElementTree(List.of(elements), false, List.of(), List.of());
    }

    private static ExecutedRequestElements executed(int calls, int callsWithBody, String... names) {
        return new ExecutedRequestElements(Set.of(names), calls, callsWithBody);
    }
}
