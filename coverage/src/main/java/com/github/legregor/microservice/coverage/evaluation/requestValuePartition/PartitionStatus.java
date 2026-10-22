package com.github.legregor.microservice.coverage.evaluation.requestValuePartition;

import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGroup;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionSource;

/**
 * Status of one value partition of an element in the report.
 *
 * @param id        e.g. {@code #min}
 * @param reachable the caller can send a value of this partition (coverage item)
 * @param covered   a value of this partition was sent during the tests
 */
public record PartitionStatus(String id, PartitionGroup group, PartitionSource source, boolean reachable,
                              boolean covered) {
}
