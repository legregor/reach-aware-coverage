package com.github.legregor.microservice.coverage.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Hierarchical coverage along an element tree, shared by the element-level metrics: the coverage of an element is
 * the mean of its own coverage (1 if covered, 0 otherwise) and the coverage of each judged child; the value of a
 * tree is the mean over its judged top-level elements. Every element weighs as much as its siblings, independent
 * of the size of its sub-tree.
 */
public final class HierarchicalCoverage {

    private HierarchicalCoverage() {
    }

    /**
     * @param topLevel the top-level elements of the tree
     * @param children the children of an element
     * @param judged   whether an element is a coverage item (not judged elements and their sub-trees are skipped)
     * @param covered  whether a judged element is covered
     * @return the coverage of the tree (0-1), or null if no element is judged
     */
    public static <T> Double of(List<T> topLevel, Function<T, List<T>> children, Predicate<T> judged,
                                Predicate<T> covered) {
        List<Double> values = new ArrayList<>();
        for (T element : topLevel) {
            Double value = element(element, children, judged, covered);
            if (value != null) {
                values.add(value);
            }
        }
        return values.isEmpty() ? null : mean(values);
    }

    /** The mean of the defined values (0-1), or null if there is none. */
    public static Double meanOfDefined(List<Double> values) {
        List<Double> defined = new ArrayList<>();
        for (Double value : values) {
            if (value != null) {
                defined.add(value);
            }
        }
        return defined.isEmpty() ? null : mean(defined);
    }

    private static <T> Double element(T element, Function<T, List<T>> children, Predicate<T> judged,
                                      Predicate<T> covered) {
        if (!judged.test(element)) {
            return null;
        }
        List<Double> values = new ArrayList<>();
        values.add(covered.test(element) ? 1.0 : 0.0);
        for (T child : children.apply(element)) {
            Double value = element(child, children, judged, covered);
            if (value != null) {
                values.add(value);
            }
        }
        return mean(values);
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).sum() / values.size();
    }
}
