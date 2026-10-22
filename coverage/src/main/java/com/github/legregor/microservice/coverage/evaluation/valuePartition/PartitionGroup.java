package com.github.legregor.microservice.coverage.evaluation.valuePartition;

/**
 * Groups of value partitions. Each group comes from one constraint (or type) of a schema field; a value covers one
 * partition in every group of its field ("each choice"), so the partitions of an element add up instead of
 * multiplying.
 */
public enum PartitionGroup {
    /** Absent or null: optional or nullable elements. */
    NO_VALUE,
    /** One partition per enum / const value; replaces the type-based groups. */
    ENUM,
    /** true, false. */
    BOOLEAN,
    /** Lower boundary, interior (or above / below for one-sided ranges), upper boundary of a numeric range. */
    RANGE,
    /** Negative, zero, positive: numbers without a range. */
    SIGN,
    /** Minimum length, interior, maximum length of a string with length bounds. */
    LENGTH,
    /** Empty, non-empty: strings without length bounds, format or pattern. */
    EMPTY,
    /** One valid-format partition (date-time, uuid, email, ...). */
    FORMAT,
    /** One partition for a string pattern. */
    PATTERN,
    /** Number of list items: 0, 1, several, or the minItems / maxItems boundaries. */
    SIZE,
    /** One partition per oneOf / anyOf alternative. */
    VARIANT
}
