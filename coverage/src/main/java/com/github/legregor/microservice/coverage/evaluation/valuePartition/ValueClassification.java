package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Result of classifying one value of an element: the partitions it covers (one per group), or, if the value is not
 * allowed by the spec, the reason. An out-of-spec value covers no partition.
 */
public record ValueClassification(Set<ValuePartition> covered, String outOfSpecReason) {

    public ValueClassification {
        covered = covered == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(covered));
    }

    static ValueClassification of(Set<ValuePartition> covered) {
        return new ValueClassification(covered, null);
    }

    static ValueClassification outOfSpec(String reason) {
        return new ValueClassification(Set.of(), reason);
    }

    public boolean isOutOfSpec() {
        return outOfSpecReason != null;
    }
}
