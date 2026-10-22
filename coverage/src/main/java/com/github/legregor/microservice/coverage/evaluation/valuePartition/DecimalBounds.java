package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bounds of a decimal number. Only inclusive bounds get a boundary partition: an exclusive bound has no first value
 * inside, so the interior starts right at it.
 */
final class DecimalBounds {

    private final BigDecimal lower;
    private final boolean exclusiveLower;
    private final BigDecimal upper;
    private final boolean exclusiveUpper;

    DecimalBounds(BigDecimal lower, boolean exclusiveLower, BigDecimal upper, boolean exclusiveUpper) {
        this.lower = lower;
        this.exclusiveLower = exclusiveLower;
        this.upper = upper;
        this.exclusiveUpper = exclusiveUpper;
    }

    boolean isEmpty() {
        if (lower == null || upper == null) {
            return false;
        }
        int compare = lower.compareTo(upper);
        return compare > 0 || (compare == 0 && (exclusiveLower || exclusiveUpper));
    }

    List<String> names() {
        List<String> names = new ArrayList<>();
        if (isEmpty()) {
            return names;
        }
        boolean single = lower != null && upper != null && lower.compareTo(upper) == 0;
        if (lower != null && !exclusiveLower) {
            names.add("min");
        }
        if (lower != null && upper != null) {
            if (!single) {
                names.add("interior");
            }
        } else if (lower != null) {
            names.add("above");
        } else if (upper != null) {
            names.add("below");
        }
        if (upper != null && !exclusiveUpper && !single) {
            names.add("max");
        }
        return names;
    }

    /** Partition name of a value, or null if it is outside the bounds. */
    String classify(BigDecimal value) {
        if (isBelow(value) || isAbove(value)) {
            return null;
        }
        if (lower != null && value.compareTo(lower) == 0) {
            return "min";
        }
        if (upper != null && value.compareTo(upper) == 0) {
            return "max";
        }
        if (lower != null && upper != null) {
            return "interior";
        }
        return lower != null ? "above" : "below";
    }

    boolean isBelow(BigDecimal value) {
        if (lower == null) {
            return false;
        }
        int compare = value.compareTo(lower);
        return compare < 0 || (compare == 0 && exclusiveLower);
    }

    boolean isAbove(BigDecimal value) {
        if (upper == null) {
            return false;
        }
        int compare = value.compareTo(upper);
        return compare > 0 || (compare == 0 && exclusiveUpper);
    }
}
