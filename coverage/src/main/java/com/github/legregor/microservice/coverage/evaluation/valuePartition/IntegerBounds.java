package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Inclusive integer bounds (either may be missing) and the boundary partitions they yield: {@code min},
 * {@code interior} / {@code above} / {@code below}, {@code max}, each with a prefix ({@code len-}, {@code size-}).
 * Used for integer ranges, string lengths and list sizes.
 */
final class IntegerBounds {

    private final BigInteger lower;
    private final BigInteger upper;

    IntegerBounds(BigInteger lower, BigInteger upper) {
        this.lower = lower;
        this.upper = upper;
    }

    /** Integer range from schema bounds; exclusive bounds become the first / last integer inside. */
    static IntegerBounds ofRange(BigDecimal minimum, boolean exclusiveMinimum, BigDecimal maximum,
                                 boolean exclusiveMaximum) {
        BigInteger lower = minimum == null ? null : exclusiveMinimum
                ? minimum.setScale(0, RoundingMode.FLOOR).toBigIntegerExact().add(BigInteger.ONE)
                : minimum.setScale(0, RoundingMode.CEILING).toBigIntegerExact();
        BigInteger upper = maximum == null ? null : exclusiveMaximum
                ? maximum.setScale(0, RoundingMode.CEILING).toBigIntegerExact().subtract(BigInteger.ONE)
                : maximum.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
        return new IntegerBounds(lower, upper);
    }

    /** Counts (length, size): the lower bound is at least 0. */
    static IntegerBounds ofCount(Integer minimum, Integer maximum) {
        return new IntegerBounds(BigInteger.valueOf(minimum == null ? 0 : Math.max(0, minimum)),
                maximum == null ? null : BigInteger.valueOf(maximum));
    }

    /** No value satisfies the bounds. */
    boolean isEmpty() {
        return lower != null && upper != null && lower.compareTo(upper) > 0;
    }

    List<String> names(String prefix) {
        List<String> names = new ArrayList<>();
        if (isEmpty()) {
            return names;
        }
        if (lower != null) {
            names.add(prefix + "min");
        }
        if (lower != null && upper != null) {
            if (upper.subtract(lower).compareTo(BigInteger.TWO) >= 0) {
                names.add(prefix + "interior");
            }
        } else if (lower != null) {
            names.add(prefix + "above");
        } else if (upper != null) {
            names.add(prefix + "below");
        }
        if (upper != null && (lower == null || upper.compareTo(lower) > 0)) {
            names.add(prefix + "max");
        }
        return names;
    }

    /** Partition name of a value, or null if it is outside the bounds. */
    String classify(BigInteger value, String prefix) {
        if (isBelow(value) || isAbove(value)) {
            return null;
        }
        if (lower != null && value.equals(lower)) {
            return prefix + "min";
        }
        if (upper != null && value.equals(upper)) {
            return prefix + "max";
        }
        if (lower != null && upper != null) {
            return prefix + "interior";
        }
        return prefix + (lower != null ? "above" : "below");
    }

    boolean isBelow(BigInteger value) {
        return lower != null && value.compareTo(lower) < 0;
    }

    boolean isAbove(BigInteger value) {
        return upper != null && value.compareTo(upper) > 0;
    }
}
