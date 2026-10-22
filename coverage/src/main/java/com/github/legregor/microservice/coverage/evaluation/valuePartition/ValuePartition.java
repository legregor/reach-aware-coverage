package com.github.legregor.microservice.coverage.evaluation.valuePartition;

/**
 * A value partition of an element, e.g. {@code #min}, {@code #enum=FIRST}, {@code #none}.
 *
 * @param group  the group (constraint) it belongs to
 * @param name   name within the element, without the leading {@code #}
 * @param source whether a spec constraint or only the type defines it
 */
public record ValuePartition(PartitionGroup group, String name, PartitionSource source) {

    /** Name as used in reports, e.g. {@code #min}; combined with an element: {@code body:quantity#min}. */
    public String id() {
        return "#" + name;
    }

    @Override
    public String toString() {
        return id();
    }
}
