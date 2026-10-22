package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.legregor.microservice.coverage.openapi.SchemaConstraints;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaVariant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Classifies a value of a schema field into its value partitions (direction-neutral: request elements, response
 * fields, static constants).
 * <ol>
 * <li>Absent or null: the no-value partition; out of spec if the field is required and not nullable.</li>
 * <li>Wrong type (e.g. a string for an integer): out of spec.</li>
 * <li>Any violated constraint (enum, range, length, pattern, format, size, variants): out of spec. An out-of-spec
 * value covers no partition, not even in groups it satisfies.</li>
 * <li>Otherwise the value covers one partition in every group of the field (oneOf / anyOf: every matching
 * alternative).</li>
 * </ol>
 * Patterns are applied with Java regular expressions and, as in JSON Schema, not anchored ({@code find});
 * a pattern Java cannot compile is ignored.
 */
public class ValueClassifier {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final PartitionGenerator generator;

    public ValueClassifier() {
        this(new PartitionGenerator());
    }

    public ValueClassifier(PartitionGenerator generator) {
        this.generator = generator;
    }

    /**
     * @param value the JSON value; null, {@code NullNode} or {@code MissingNode} mean "no value"
     */
    public ValueClassification classify(SchemaField field, JsonNode value) {
        List<ValuePartition> partitions = generator.partitions(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return partitions.stream().filter(partition -> partition.group() == PartitionGroup.NO_VALUE).findFirst()
                    .map(partition -> ValueClassification.of(Set.of(partition)))
                    .orElseGet(() -> ValueClassification.outOfSpec(
                            "no value (absent or null) for a required, non-nullable element"));
        }
        String typeError = typeError(field, value);
        if (typeError != null) {
            return ValueClassification.outOfSpec(typeError);
        }
        String violation = violation(field.getConstraints(), value);
        if (violation != null) {
            return ValueClassification.outOfSpec(violation);
        }
        List<String> variants = matchingVariants(field.getConstraints(), value);
        if (variants == null) {
            return ValueClassification.outOfSpec(variantError(field.getConstraints(), value));
        }

        Set<ValuePartition> covered = new LinkedHashSet<>();
        for (ValuePartition partition : partitions) {
            if (partition.group() == PartitionGroup.VARIANT) {
                if (variants.contains(partition.name().substring("variant=".length()))) {
                    covered.add(partition);
                }
            } else if (partition.group() != PartitionGroup.NO_VALUE
                    && partition.name().equals(coveredName(partition.group(), field, value))) {
                covered.add(partition);
            }
        }
        return ValueClassification.of(covered);
    }

    /**
     * A value given as text (e.g. a path parameter from the URL), converted to the field's type first; a text that
     * cannot be converted is out of spec.
     */
    public ValueClassification classifyText(SchemaField field, String text) {
        if (text == null) {
            return classify(field, null);
        }
        String type = field.getType();
        try {
            JsonNode value;
            if ("integer".equals(type)) {
                value = NODES.numberNode(new BigInteger(text.trim()));
            } else if ("number".equals(type)) {
                value = NODES.numberNode(new BigDecimal(text.trim()));
            } else if ("boolean".equals(type)) {
                if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                    return ValueClassification.outOfSpec("not a boolean: '" + text + "'");
                }
                value = NODES.booleanNode(Boolean.parseBoolean(text.toLowerCase()));
            } else {
                value = NODES.textNode(text);
            }
            return classify(field, value);
        } catch (NumberFormatException e) {
            return ValueClassification.outOfSpec("not a valid " + type + ": '" + text + "'");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Type and constraint checks
    // ---------------------------------------------------------------------------------------------

    private static String typeError(SchemaField field, JsonNode value) {
        String type = field.isList() ? "array" : field.getType();
        if (type == null) {
            return null;
        }
        boolean ok = switch (type) {
            case "integer" -> isInteger(value);
            case "number" -> value.isNumber();
            case "string" -> value.isTextual();
            case "boolean" -> value.isBoolean();
            case "array" -> value.isArray();
            case "object" -> value.isObject();
            default -> true;
        };
        return ok ? null : "wrong type: expected " + type + ", got " + value.getNodeType().name().toLowerCase();
    }

    private static boolean isInteger(JsonNode value) {
        if (value.isIntegralNumber()) {
            return true;
        }
        return value.isNumber() && value.decimalValue().stripTrailingZeros().scale() <= 0;
    }

    private static String violation(SchemaConstraints constraints, JsonNode value) {
        if (constraints.hasEnum() && constraints.getEnumValues().stream().noneMatch(allowed -> same(allowed, value))) {
            return "not one of the enum values " + constraints.getEnumValues();
        }
        if (value.isNumber()) {
            return numberViolation(constraints, value.decimalValue(), isInteger(value));
        }
        if (value.isTextual()) {
            return stringViolation(constraints, value.asText());
        }
        if (value.isArray()) {
            IntegerBounds size = PartitionGenerator.sizeBounds(constraints);
            BigInteger count = BigInteger.valueOf(value.size());
            if (size.isBelow(count)) {
                return "fewer than minItems " + constraints.getMinItems() + " items";
            }
            if (size.isAbove(count)) {
                return "more than maxItems " + constraints.getMaxItems() + " items";
            }
        }
        return null;
    }

    private static String numberViolation(SchemaConstraints constraints, BigDecimal number, boolean integer) {
        DecimalBounds range = PartitionGenerator.decimalRange(constraints);
        if (range.isBelow(number)) {
            return "below " + (constraints.isExclusiveMinimum() ? "exclusive " : "") + "minimum "
                    + constraints.getMinimum().toPlainString();
        }
        if (range.isAbove(number)) {
            return "above " + (constraints.isExclusiveMaximum() ? "exclusive " : "") + "maximum "
                    + constraints.getMaximum().toPlainString();
        }
        if (integer && !FormatChecker.isValidInteger(constraints.getFormat(), number.toBigInteger())) {
            return "outside the range of " + constraints.getFormat();
        }
        return null;
    }

    private static String stringViolation(SchemaConstraints constraints, String text) {
        IntegerBounds length = PartitionGenerator.lengthBounds(constraints);
        BigInteger codePoints = BigInteger.valueOf(text.codePointCount(0, text.length()));
        if (length.isBelow(codePoints)) {
            return "shorter than minLength " + constraints.getMinLength();
        }
        if (length.isAbove(codePoints)) {
            return "longer than maxLength " + constraints.getMaxLength();
        }
        if (constraints.getPattern() != null && !matches(constraints.getPattern(), text)) {
            return "does not match the pattern " + constraints.getPattern();
        }
        if (!FormatChecker.isValidString(constraints.getFormat(), text)) {
            return "not a valid " + constraints.getFormat();
        }
        return null;
    }

    private static boolean matches(String pattern, String text) {
        try {
            return Pattern.compile(pattern).matcher(text).find();
        } catch (PatternSyntaxException e) {
            return true;
        }
    }

    private static boolean same(JsonNode allowed, JsonNode value) {
        if (allowed.isNumber() && value.isNumber()) {
            return allowed.decimalValue().compareTo(value.decimalValue()) == 0;
        }
        return allowed.equals(value);
    }

    // ---------------------------------------------------------------------------------------------
    // Covered partitions
    // ---------------------------------------------------------------------------------------------

    private static String coveredName(PartitionGroup group, SchemaField field, JsonNode value) {
        SchemaConstraints constraints = field.getConstraints();
        return switch (group) {
            case ENUM -> constraints.getEnumValues().stream().filter(allowed -> same(allowed, value)).findFirst()
                    .map(PartitionGenerator::enumName).orElse(null);
            case BOOLEAN -> value.asBoolean() ? "true" : "false";
            case RANGE -> "integer".equals(field.getType())
                    ? PartitionGenerator.integerRange(constraints).classify(value.decimalValue().toBigInteger(), "")
                    : PartitionGenerator.decimalRange(constraints).classify(value.decimalValue());
            case SIGN -> switch (value.decimalValue().signum()) {
                case -1 -> "negative";
                case 0 -> "zero";
                default -> "positive";
            };
            case LENGTH -> PartitionGenerator.lengthBounds(constraints).classify(
                    BigInteger.valueOf(value.asText().codePointCount(0, value.asText().length())), "len-");
            case EMPTY -> value.asText().isEmpty() ? "empty" : "non-empty";
            case FORMAT -> "format";
            case PATTERN -> "pattern";
            case SIZE -> constraints.hasSizeBounds()
                    ? PartitionGenerator.sizeBounds(constraints).classify(BigInteger.valueOf(value.size()), "size-")
                    : value.size() == 0 ? "size-0" : value.size() == 1 ? "size-1" : "size-many";
            case NO_VALUE, VARIANT -> null;
        };
    }

    /**
     * Names of the oneOf / anyOf alternatives the value matches; empty list if the field has no alternatives; null
     * if it has alternatives but the value matches none (out of spec).
     */
    private static List<String> matchingVariants(SchemaConstraints constraints, JsonNode value) {
        List<SchemaVariant> variants = constraints.getVariants();
        if (variants.isEmpty()) {
            return List.of();
        }
        if (!value.isObject()) {
            return null;
        }
        String property = constraints.getDiscriminatorProperty();
        if (property != null && value.hasNonNull(property)) {
            String discriminator = value.get(property).asText();
            Map<String, String> mapping = constraints.getDiscriminatorMapping();
            String name = mapping.getOrDefault(discriminator, discriminator);
            return variants.stream().anyMatch(variant -> variant.name().equals(name)) ? List.of(name) : null;
        }
        List<String> matching = new ArrayList<>();
        for (SchemaVariant variant : variants) {
            if (variant.requiredProperties().stream().allMatch(value::hasNonNull)) {
                matching.add(variant.name());
            }
        }
        return matching.isEmpty() ? null : matching;
    }

    private static String variantError(SchemaConstraints constraints, JsonNode value) {
        String property = constraints.getDiscriminatorProperty();
        if (property != null && value.isObject() && value.hasNonNull(property)) {
            return "unknown discriminator value '" + value.get(property).asText() + "'";
        }
        return "matches none of the " + (constraints.isExclusiveVariants() ? "oneOf" : "anyOf") + " alternatives";
    }
}
