package com.github.legregor.microservice.coverage.evaluation.valuePartition;

/** Where a value partition comes from. */
public enum PartitionSource {
    /** Derived from a constraint of the spec (enum, range, length, format, pattern, required list, nullable, ...). */
    CONSTRAINT,
    /** Derived from the type alone, because the spec has no constraint (fallback, e.g. sign of a number). */
    TYPE_DEFAULT
}
