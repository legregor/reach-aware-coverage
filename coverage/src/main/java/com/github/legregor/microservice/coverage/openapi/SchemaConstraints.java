package com.github.legregor.microservice.coverage.openapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Value constraints of a schema field, normalised from Swagger 2.0, OpenAPI 3.0 and OpenAPI 3.1:
 * <ul>
 * <li>nullable: {@code x-nullable} (2.0), {@code nullable} (3.0), {@code type: [..., "null"]} or {@code null} in the
 * enum (3.1)</li>
 * <li>exclusive bounds: boolean flags next to minimum / maximum (2.0, 3.0) or numeric
 * {@code exclusiveMinimum} / {@code exclusiveMaximum} (3.1)</li>
 * <li>{@code const} (3.1) is an enum with one value</li>
 * <li>discriminator: a property name (2.0) or an object with propertyName and mapping (3.x)</li>
 * </ul>
 * Fields that are not set are null (numbers) or empty (lists).
 */
public final class SchemaConstraints {

    /** No constraints at all. */
    public static final SchemaConstraints NONE = builder().build();

    private final String format;
    private final List<JsonNode> enumValues;
    private final boolean nullable;
    private final boolean readOnly;
    private final boolean writeOnly;
    private final BigDecimal minimum;
    private final boolean exclusiveMinimum;
    private final BigDecimal maximum;
    private final boolean exclusiveMaximum;
    private final Integer minLength;
    private final Integer maxLength;
    private final String pattern;
    private final Integer minItems;
    private final Integer maxItems;
    private final List<SchemaVariant> variants;
    private final boolean exclusiveVariants;
    private final String discriminatorProperty;
    private final Map<String, String> discriminatorMapping;

    private SchemaConstraints(Builder builder) {
        this.format = builder.format;
        this.enumValues = builder.enumValues == null ? null : List.copyOf(builder.enumValues);
        this.nullable = builder.nullable;
        this.readOnly = builder.readOnly;
        this.writeOnly = builder.writeOnly;
        this.minimum = builder.minimum;
        this.exclusiveMinimum = builder.exclusiveMinimum;
        this.maximum = builder.maximum;
        this.exclusiveMaximum = builder.exclusiveMaximum;
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.pattern = builder.pattern;
        this.minItems = builder.minItems;
        this.maxItems = builder.maxItems;
        this.variants = builder.variants == null ? List.of() : List.copyOf(builder.variants);
        this.exclusiveVariants = builder.exclusiveVariants;
        this.discriminatorProperty = builder.discriminatorProperty;
        this.discriminatorMapping = Collections.unmodifiableMap(new LinkedHashMap<>(builder.discriminatorMapping));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Format such as date-time, uuid, email, int32; null if none. */
    public String getFormat() {
        return format;
    }

    /** Allowed values (without null), or null if the field has no enum / const. */
    public List<JsonNode> getEnumValues() {
        return enumValues;
    }

    public boolean hasEnum() {
        return enumValues != null && !enumValues.isEmpty();
    }

    public boolean isNullable() {
        return nullable;
    }

    /** Only in responses (never sent in a request). */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** Only in requests (never returned in a response). */
    public boolean isWriteOnly() {
        return writeOnly;
    }

    public BigDecimal getMinimum() {
        return minimum;
    }

    public boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public BigDecimal getMaximum() {
        return maximum;
    }

    public boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public boolean hasRange() {
        return minimum != null || maximum != null;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public boolean hasLengthBounds() {
        return minLength != null || maxLength != null;
    }

    public String getPattern() {
        return pattern;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public boolean hasSizeBounds() {
        return minItems != null || maxItems != null;
    }

    /** Alternatives of a oneOf / anyOf; empty if none. */
    public List<SchemaVariant> getVariants() {
        return variants;
    }

    /** True for oneOf (exactly one alternative), false for anyOf. */
    public boolean isExclusiveVariants() {
        return exclusiveVariants;
    }

    public String getDiscriminatorProperty() {
        return discriminatorProperty;
    }

    /** Discriminator value -> variant name (from the mapping); empty if there is no mapping. */
    public Map<String, String> getDiscriminatorMapping() {
        return discriminatorMapping;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        append(sb, "format", format);
        append(sb, "enum", enumValues);
        append(sb, "nullable", nullable ? true : null);
        append(sb, "readOnly", readOnly ? true : null);
        append(sb, "writeOnly", writeOnly ? true : null);
        append(sb, exclusiveMinimum ? "exclusiveMinimum" : "minimum", minimum);
        append(sb, exclusiveMaximum ? "exclusiveMaximum" : "maximum", maximum);
        append(sb, "minLength", minLength);
        append(sb, "maxLength", maxLength);
        append(sb, "pattern", pattern);
        append(sb, "minItems", minItems);
        append(sb, "maxItems", maxItems);
        append(sb, exclusiveVariants ? "oneOf" : "anyOf", variants.isEmpty() ? null : variants);
        return sb.append("}").toString();
    }

    private static void append(StringBuilder sb, String key, Object value) {
        if (value != null) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(key).append('=').append(value);
        }
    }

    /** Builder; values set later override earlier ones, unset values (null) do not override. */
    public static final class Builder {
        private String format;
        private List<JsonNode> enumValues;
        private boolean nullable;
        private boolean readOnly;
        private boolean writeOnly;
        private BigDecimal minimum;
        private boolean exclusiveMinimum;
        private BigDecimal maximum;
        private boolean exclusiveMaximum;
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private Integer minItems;
        private Integer maxItems;
        private List<SchemaVariant> variants;
        private boolean exclusiveVariants;
        private String discriminatorProperty;
        private final Map<String, String> discriminatorMapping = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder enumValues(List<JsonNode> enumValues) {
            this.enumValues = enumValues;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder writeOnly(boolean writeOnly) {
            this.writeOnly = writeOnly;
            return this;
        }

        public Builder minimum(BigDecimal minimum, boolean exclusive) {
            this.minimum = minimum;
            this.exclusiveMinimum = minimum != null && exclusive;
            return this;
        }

        public Builder maximum(BigDecimal maximum, boolean exclusive) {
            this.maximum = maximum;
            this.exclusiveMaximum = maximum != null && exclusive;
            return this;
        }

        public Builder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder minItems(Integer minItems) {
            this.minItems = minItems;
            return this;
        }

        public Builder maxItems(Integer maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public Builder variants(List<SchemaVariant> variants, boolean exclusive) {
            this.variants = variants;
            this.exclusiveVariants = exclusive;
            return this;
        }

        public Builder discriminator(String property, Map<String, String> mapping) {
            this.discriminatorProperty = property;
            this.discriminatorMapping.clear();
            if (mapping != null) {
                this.discriminatorMapping.putAll(mapping);
            }
            return this;
        }

        public SchemaConstraints build() {
            return new SchemaConstraints(this);
        }
    }
}
