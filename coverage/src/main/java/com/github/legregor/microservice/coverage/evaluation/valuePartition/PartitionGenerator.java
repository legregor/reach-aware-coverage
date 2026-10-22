package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.legregor.microservice.coverage.openapi.SchemaConstraints;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaVariant;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives the value partitions of a schema field (direction-neutral: request elements and response fields).
 * <table>
 * <caption>Partition groups</caption>
 * <tr><th>Group</th><th>When</th><th>Partitions</th></tr>
 * <tr><td>no value</td><td>optional or nullable</td><td>none</td></tr>
 * <tr><td>enum</td><td>enum / const</td><td>enum=&lt;value&gt; each (replaces the type groups)</td></tr>
 * <tr><td>boolean</td><td>boolean</td><td>true, false</td></tr>
 * <tr><td>range</td><td>integer / number with minimum or maximum</td><td>min, interior / above / below, max</td></tr>
 * <tr><td>sign</td><td>integer / number without range</td><td>negative, zero, positive</td></tr>
 * <tr><td>length</td><td>string with minLength or maxLength</td><td>len-min, len-interior / len-above, len-max</td></tr>
 * <tr><td>empty</td><td>string without length bounds, format, pattern</td><td>empty, non-empty</td></tr>
 * <tr><td>format</td><td>string with format</td><td>format</td></tr>
 * <tr><td>pattern</td><td>string with pattern</td><td>pattern</td></tr>
 * <tr><td>size</td><td>array</td><td>size-0, size-1, size-many, or size-min, size-interior / size-above, size-max</td></tr>
 * <tr><td>variant</td><td>oneOf / anyOf</td><td>variant=&lt;name&gt; each</td></tr>
 * </table>
 * Partitions without a spec constraint behind them (sign, empty, boolean, size without bounds, no value without a
 * declared required list) have the source {@link PartitionSource#TYPE_DEFAULT}.
 */
public class PartitionGenerator {

    static final String NONE = "none";

    /** The value partitions of a field, in group order. */
    public List<ValuePartition> partitions(SchemaField field) {
        List<ValuePartition> partitions = new ArrayList<>();
        SchemaConstraints constraints = field.getConstraints();

        if (!field.isRequired() || constraints.isNullable()) {
            boolean declared = constraints.isNullable() || field.isRequiredDeclared();
            partitions.add(new ValuePartition(PartitionGroup.NO_VALUE, NONE,
                    declared ? PartitionSource.CONSTRAINT : PartitionSource.TYPE_DEFAULT));
        }

        if (constraints.hasEnum()) {
            for (JsonNode value : constraints.getEnumValues()) {
                partitions.add(new ValuePartition(PartitionGroup.ENUM, enumName(value), PartitionSource.CONSTRAINT));
            }
        } else {
            addTypePartitions(field, constraints, partitions);
        }

        for (SchemaVariant variant : constraints.getVariants()) {
            partitions.add(new ValuePartition(PartitionGroup.VARIANT, "variant=" + variant.name(),
                    PartitionSource.CONSTRAINT));
        }
        return partitions;
    }

    private void addTypePartitions(SchemaField field, SchemaConstraints constraints, List<ValuePartition> partitions) {
        String type = field.isList() ? "array" : field.getType();
        if (type == null) {
            return;
        }
        switch (type) {
            case "boolean" -> {
                add(partitions, PartitionGroup.BOOLEAN, PartitionSource.TYPE_DEFAULT, List.of("true", "false"));
            }
            case "integer" -> {
                if (constraints.hasRange()) {
                    add(partitions, PartitionGroup.RANGE, PartitionSource.CONSTRAINT, integerRange(constraints)
                            .names(""));
                } else {
                    add(partitions, PartitionGroup.SIGN, PartitionSource.TYPE_DEFAULT, signs());
                }
            }
            case "number" -> {
                if (constraints.hasRange()) {
                    add(partitions, PartitionGroup.RANGE, PartitionSource.CONSTRAINT, decimalRange(constraints)
                            .names());
                } else {
                    add(partitions, PartitionGroup.SIGN, PartitionSource.TYPE_DEFAULT, signs());
                }
            }
            case "string" -> {
                if (constraints.hasLengthBounds()) {
                    add(partitions, PartitionGroup.LENGTH, PartitionSource.CONSTRAINT, lengthBounds(constraints)
                            .names("len-"));
                } else if (constraints.getFormat() == null && constraints.getPattern() == null) {
                    add(partitions, PartitionGroup.EMPTY, PartitionSource.TYPE_DEFAULT, List.of("empty", "non-empty"));
                }
                if (constraints.getFormat() != null) {
                    add(partitions, PartitionGroup.FORMAT, PartitionSource.CONSTRAINT, List.of("format"));
                }
                if (constraints.getPattern() != null) {
                    add(partitions, PartitionGroup.PATTERN, PartitionSource.CONSTRAINT, List.of("pattern"));
                }
            }
            case "array" -> {
                if (constraints.hasSizeBounds()) {
                    add(partitions, PartitionGroup.SIZE, PartitionSource.CONSTRAINT, sizeBounds(constraints)
                            .names("size-"));
                } else {
                    add(partitions, PartitionGroup.SIZE, PartitionSource.TYPE_DEFAULT,
                            List.of("size-0", "size-1", "size-many"));
                }
            }
            default -> {
                // object: only no value and variants
            }
        }
    }

    // Shared with the classifier, so both always agree on the partition names.

    static String enumName(JsonNode value) {
        return "enum=" + (value.isValueNode() ? value.asText() : value.toString());
    }

    static List<String> signs() {
        return List.of("negative", "zero", "positive");
    }

    static IntegerBounds integerRange(SchemaConstraints constraints) {
        return IntegerBounds.ofRange(constraints.getMinimum(), constraints.isExclusiveMinimum(),
                constraints.getMaximum(), constraints.isExclusiveMaximum());
    }

    static DecimalBounds decimalRange(SchemaConstraints constraints) {
        return new DecimalBounds(constraints.getMinimum(), constraints.isExclusiveMinimum(),
                constraints.getMaximum(), constraints.isExclusiveMaximum());
    }

    static IntegerBounds lengthBounds(SchemaConstraints constraints) {
        return IntegerBounds.ofCount(constraints.getMinLength(), constraints.getMaxLength());
    }

    static IntegerBounds sizeBounds(SchemaConstraints constraints) {
        return IntegerBounds.ofCount(constraints.getMinItems(), constraints.getMaxItems());
    }

    private static void add(List<ValuePartition> partitions, PartitionGroup group, PartitionSource source,
                            List<String> names) {
        names.forEach(name -> partitions.add(new ValuePartition(group, name, source)));
    }
}
