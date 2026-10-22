package com.github.legregor.microservice.coverage.evaluation.requestValuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElement;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTree;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Request value partition coverage of one called connection.
 * <p>
 * Scope: the executed request elements (E_exec: reachable elements sent with a non-null value at least once, i.e.
 * the covered elements of {@link RequestElementCoverage}); elements never sent with a value are gaps of request
 * element coverage and are skipped here. Coverage items are the reachable value partitions of these elements; a
 * partition is covered if at least one value sent falls into it. Values the spec does not allow are listed per
 * element and not counted.
 * <ul>
 * <li>Pooled: covered / reachable partitions.</li>
 * <li>Hierarchical: an element's own partitions count as one value (covered / reachable), averaged with the values
 * of its executed children; the connection value is the mean over the top-level elements. Elements with neither own
 * partitions nor executed children are skipped.</li>
 * </ul>
 * All percentages are 0-100, or null (n/a).
 */
public class RequestValuePartitionCoverage {

    private final List<ElementPartitionCoverage> elements;
    private final Double hierarchical;
    private final NotApplicableReason notApplicableReason;

    /**
     * @param tree             the request element tree of the connection (with the spec fields)
     * @param elementCoverage  request element coverage of the connection (defines E_exec)
     * @param values           values sent per element name (absent / null included)
     */
    public RequestValuePartitionCoverage(RequestElementTree tree, RequestElementCoverage elementCoverage,
                                         Map<String, List<JsonNode>> values, PartitionGenerator generator,
                                         ValueClassifier classifier) {
        Set<String> executed = Set.copyOf(elementCoverage.getCoveredElements());
        Map<String, ElementPartitionCoverage> byName = new LinkedHashMap<>();
        for (RequestElement element : tree.allElements()) {
            if (executed.contains(element.getName())) {
                byName.put(element.getName(), ElementPartitionCoverage.of(element,
                        values.getOrDefault(element.getName(), List.of()), generator, classifier));
            }
        }
        this.elements = List.copyOf(byName.values());

        List<Double> topLevel = new ArrayList<>();
        for (RequestElement element : tree.getTopLevelElements()) {
            Double value = hierarchical(element, byName);
            if (value != null) {
                topLevel.add(value);
            }
        }
        this.hierarchical = topLevel.isEmpty() ? null : mean(topLevel) * 100.0;

        if (elementCoverage.isNotApplicable()) {
            this.notApplicableReason = elementCoverage.getNotApplicableReason();
        } else if (elements.isEmpty()) {
            this.notApplicableReason = NotApplicableReason.NO_EXECUTED_ELEMENTS;
        } else if (sum(ElementPartitionCoverage::getReachableCount) == 0) {
            this.notApplicableReason = NotApplicableReason.NO_PARTITIONS;
        } else {
            this.notApplicableReason = null;
        }
    }

    /** Per executed element: partitions with status and out-of-spec values, depth-first. */
    public List<ElementPartitionCoverage> getElements() {
        return elements;
    }

    /** Covered / reachable partitions over all executed elements, null if n/a. */
    public Double getPooledCoveragePercentage() {
        return percentage(sum(ElementPartitionCoverage::getCoveredCount), sum(ElementPartitionCoverage::getReachableCount));
    }

    /** Pooled over the partitions that come from spec constraints only (no type defaults), null if there are none. */
    public Double getConstraintPooledCoveragePercentage() {
        return percentage(sum(ElementPartitionCoverage::getConstraintCoveredCount),
                sum(ElementPartitionCoverage::getConstraintReachableCount));
    }

    public Double getHierarchicalCoveragePercentage() {
        return isNotApplicable() ? null : hierarchical;
    }

    public boolean isNotApplicable() {
        return notApplicableReason != null;
    }

    /** Why the coverage is n/a, null if it is applicable. */
    public NotApplicableReason getNotApplicableReason() {
        return notApplicableReason;
    }

    public int getReachableCount() {
        return sum(ElementPartitionCoverage::getReachableCount);
    }

    public int getCoveredCount() {
        return sum(ElementPartitionCoverage::getCoveredCount);
    }

    public int getConstraintReachableCount() {
        return sum(ElementPartitionCoverage::getConstraintReachableCount);
    }

    public int getConstraintCoveredCount() {
        return sum(ElementPartitionCoverage::getConstraintCoveredCount);
    }

    /** Number of distinct out-of-spec values (per element, value and reason). */
    public int getOutOfSpecValueCount() {
        return elements.stream().mapToInt(element -> element.getOutOfSpecValues().size()).sum();
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Hierarchical value (0-1) of an element, null if it is not executed or has nothing to judge. */
    private static Double hierarchical(RequestElement element, Map<String, ElementPartitionCoverage> byName) {
        ElementPartitionCoverage coverage = byName.get(element.getName());
        if (coverage == null) {
            return null;
        }
        List<Double> parts = new ArrayList<>();
        if (coverage.getReachableCount() > 0) {
            parts.add((double) coverage.getCoveredCount() / coverage.getReachableCount());
        }
        for (RequestElement child : element.getChildren()) {
            Double childValue = hierarchical(child, byName);
            if (childValue != null) {
                parts.add(childValue);
            }
        }
        return parts.isEmpty() ? null : mean(parts);
    }

    private int sum(ToIntFunction<ElementPartitionCoverage> count) {
        return elements.stream().mapToInt(count).sum();
    }

    private Double percentage(int covered, int reachable) {
        return isNotApplicable() || reachable == 0 ? null : covered * 100.0 / reachable;
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).sum() / values.size();
    }
}
