package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.github.legregor.microservice.coverage.evaluation.HierarchicalCoverage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Request element coverage of one (called) connection.
 * <p>
 * Coverage items are the reachable request elements of the connection ({@link RequestElementTree}); an item is
 * covered if it was assigned a value in at least one recorded call ({@link ExecutedRequestElements}). If the
 * connection sends a body but no body was recorded in the traces, the body elements are not judged
 * ({@link #isBodyNotRecorded()}) and only the path parameters count.
 * <p>
 * All percentages are 0-100, or {@code null} (n/a) if there is no item to judge.
 */
public class RequestElementCoverage {

    private final RequestElementTree tree;
    private final ExecutedRequestElements executed;
    private final boolean bodyNotRecorded;

    public RequestElementCoverage(RequestElementTree tree, ExecutedRequestElements executed) {
        this.tree = tree;
        this.executed = executed;
        this.bodyNotRecorded = executed.getCallCount() > 0 && executed.getCallsWithBody() == 0
                && tree.reachableElements().stream().anyMatch(RequestElementCoverage::isBodyElement);
    }

    // ---------------------------------------------------------------------------------------------
    // Coverage values
    // ---------------------------------------------------------------------------------------------

    /** Pooled: covered items / reachable items. Every element has the same weight. */
    public Double getPooledCoveragePercentage() {
        return pooled(judgedElements());
    }

    /** Pooled over the optional elements only (required elements are always sent in a valid request). */
    public Double getOptionalPooledCoveragePercentage() {
        return pooled(judgedElements().stream().filter(element -> !element.isRequired()).collect(Collectors.toList()));
    }

    /**
     * Hierarchical: coverage of an element = mean of its own coverage (1 or 0) and the coverage of each of its
     * reachable children; the connection value is the mean over the reachable top-level elements. Every element
     * weighs as much as its siblings, independent of the size of its sub-tree.
     */
    public Double getHierarchicalCoveragePercentage() {
        Double value = HierarchicalCoverage.of(
                tree.getTopLevelElements(), RequestElement::getChildren, this::isJudged, this::isCovered);
        return value == null ? null : value * 100.0;
    }

    /** True if no request element can be judged for this connection (coverage n/a). */
    public boolean isNotApplicable() {
        return judgedElements().isEmpty();
    }

    /** Why the coverage of this connection is n/a, or null if it is applicable. */
    public NotApplicableReason getNotApplicableReason() {
        if (!isNotApplicable()) {
            return null;
        }
        if (tree.allElements().isEmpty()) {
            return NotApplicableReason.NO_ELEMENTS;
        }
        if (tree.reachableElements().isEmpty()) {
            return NotApplicableReason.NONE_REACHABLE;
        }
        // Reachable elements exist but none is judged: only body elements, and the body was not recorded
        NotApplicableReason bodyReason = getBodyNotRecordedReason();
        return bodyReason != null ? bodyReason : NotApplicableReason.NONE_REACHABLE;
    }

    /** Why the body elements could not be judged, or null if the body was recorded (or nothing is sent). */
    public NotApplicableReason getBodyNotRecordedReason() {
        if (!bodyNotRecorded) {
            return null;
        }
        return executed.getCallsWithServerSpan() == 0 ? NotApplicableReason.CALLEE_NOT_TRACED
                : NotApplicableReason.BODY_NOT_CAPTURED;
    }

    // ---------------------------------------------------------------------------------------------
    // Element lists for developers
    // ---------------------------------------------------------------------------------------------

    /** Reachable elements that were assigned a value during the tests. */
    public List<String> getCoveredElements() {
        return names(judgedElements().stream().filter(this::isCovered).collect(Collectors.toList()));
    }

    /** Reachable elements that the tests never assigned a value (test gaps). */
    public List<String> getUncoveredElements() {
        return names(judgedElements().stream().filter(element -> !isCovered(element)).collect(Collectors.toList()));
    }

    /** Elements the caller can never send with a value (static analysis); not part of the coverage. */
    public List<String> getNotReachableElements() {
        return names(tree.notReachableElements());
    }

    /** Body elements that could not be judged because no request body was recorded in the traces. */
    public List<String> getNotRecordedElements() {
        if (!bodyNotRecorded) {
            return List.of();
        }
        return names(tree.reachableElements().stream().filter(RequestElementCoverage::isBodyElement)
                .collect(Collectors.toList()));
    }

    /**
     * Elements sent during the tests that are not in the element tree (e.g. fields the spec does not know).
     * Developer information only.
     */
    public List<String> getUnexpectedElements() {
        Set<String> known = new LinkedHashSet<>(names(tree.allElements()));
        return executed.getNames().stream().filter(name -> !known.contains(name)).collect(Collectors.toList());
    }

    /** The connection sends a body, but no body was recorded in the traces: body elements are n/a. */
    public boolean isBodyNotRecorded() {
        return bodyNotRecorded;
    }

    public boolean isBodySpecMissing() {
        return tree.isBodySpecMissing();
    }

    public List<String> getOnlyInSpec() {
        return tree.getOnlyInSpec();
    }

    public List<String> getOnlyInCaller() {
        return tree.getOnlyInCaller();
    }

    public int getReachableCount() {
        return judgedElements().size();
    }

    public int getCoveredCount() {
        return getCoveredElements().size();
    }

    /** Number of optional reachable elements (the items of the optional-only value). */
    public int getOptionalReachableCount() {
        return (int) judgedElements().stream().filter(element -> !element.isRequired()).count();
    }

    public int getOptionalCoveredCount() {
        return (int) judgedElements().stream().filter(element -> !element.isRequired()).filter(this::isCovered)
                .count();
    }

    /** True if the caller's DTO and the spec of the called endpoint do not have the same top-level properties. */
    public boolean isInterfaceMismatch() {
        return !tree.getOnlyInSpec().isEmpty() || !tree.getOnlyInCaller().isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** The coverage items: reachable elements, without the body elements if no body was recorded. */
    private List<RequestElement> judgedElements() {
        return tree.reachableElements().stream().filter(this::isJudged).collect(Collectors.toList());
    }

    private boolean isJudged(RequestElement element) {
        return element.isReachable() && !(bodyNotRecorded && isBodyElement(element));
    }

    private boolean isCovered(RequestElement element) {
        return executed.contains(element.getName());
    }

    private Double pooled(List<RequestElement> items) {
        if (items.isEmpty()) {
            return null;
        }
        long covered = items.stream().filter(this::isCovered).count();
        return (double) covered / items.size() * 100.0;
    }

    private static boolean isBodyElement(RequestElement element) {
        return element.getKind() != RequestElement.Kind.PATH_PARAMETER;
    }

    private static List<String> names(List<RequestElement> elements) {
        return elements.stream().map(RequestElement::getName).collect(Collectors.toList());
    }
}
