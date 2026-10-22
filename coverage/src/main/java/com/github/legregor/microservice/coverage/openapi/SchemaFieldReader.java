package com.github.legregor.microservice.coverage.openapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a schema into a tree of {@link SchemaField}s, with the value constraints of every field.
 * <p>
 * Handles {@code $ref}s (Swagger 2.0 definitions and OpenAPI 3.x components), {@code allOf} (properties and
 * constraints are merged), {@code oneOf} / {@code anyOf} (the alternatives become {@link SchemaVariant}s, their
 * properties optional children), arrays (the properties of their items become the children), {@code required}
 * lists and the differences between Swagger 2.0, OpenAPI 3.0 and 3.1 (see {@link SchemaConstraints}). Keywords next
 * to a {@code $ref} (e.g. {@code nullable}, {@code readOnly}) are kept. A reference that is already being expanded
 * (a cycle such as {@code Node.next -> Node}) is not expanded again, and nesting stops at {@link #MAX_DEPTH}.
 * readOnly and writeOnly fields are kept and flagged; the caller decides which direction skips them.
 */
public final class SchemaFieldReader {

    /** Maximum depth of nested fields (1 = top-level properties). */
    public static final int MAX_DEPTH = 5;

    private static final String REF = "$ref";

    private SchemaFieldReader() {
    }

    /** The body as a root field without a name: an object (properties as children), a list or a single value. */
    public static SchemaField read(OpenApiSchema spec) {
        Deque<String> refs = new ArrayDeque<>();
        JsonNode raw = spec.getSchema();
        JsonNode root = resolve(spec, raw, refs);
        String type = type(root, raw);
        SchemaField body = new SchemaField(null, type, true, true, "array".equals(type),
                constraints(spec, raw, root, refs));
        addChildren(spec, body, root, 0, refs);
        return body;
    }

    /**
     * A parameter (path, query, header) as a field: Swagger 2.0 keeps type and constraints on the parameter itself,
     * OpenAPI 3.x in its {@code schema}. Path parameters are always required.
     *
     * @param spec      the spec (for references); its own schema is not used
     * @param parameter the parameter object of the operation
     */
    public static SchemaField readParameter(OpenApiSchema spec, JsonNode parameter) {
        String name = parameter.path("name").asText(null);
        boolean required = parameter.path("required").asBoolean(false) || "path".equals(parameter.path("in").asText());
        JsonNode raw = parameter.has("schema") ? parameter.get("schema") : parameter;
        Deque<String> refs = new ArrayDeque<>();
        JsonNode resolved = resolve(spec, raw, refs);
        String type = type(resolved, raw);
        SchemaField field = new SchemaField(name, type, required, true, "array".equals(type),
                constraints(spec, raw, resolved, refs));
        addChildren(spec, field, resolved, 0, refs);
        return field;
    }

    private static void addChildren(OpenApiSchema spec, SchemaField parent, JsonNode schema, int depth,
                                    Deque<String> refs) {
        if (schema == null || depth >= MAX_DEPTH) {
            return;
        }
        JsonNode objectSchema = schema;
        if ("array".equals(type(schema, null))) {
            objectSchema = resolve(spec, schema.get("items"), refs);
        }
        if (objectSchema == null) {
            return;
        }
        JsonNode requiredList = requiredListOf(spec, objectSchema, refs);
        Set<String> required = required(spec, objectSchema, refs);
        for (Map.Entry<String, JsonNode> property : properties(spec, objectSchema, refs).entrySet()) {
            Deque<String> childRefs = new ArrayDeque<>(refs);
            JsonNode raw = property.getValue();
            JsonNode childSchema = resolve(spec, raw, childRefs);
            String childType = type(childSchema, raw);
            SchemaField child = new SchemaField(property.getKey(), childType, required.contains(property.getKey()),
                    requiredList != null, "array".equals(childType), constraints(spec, raw, childSchema, childRefs));
            addChildren(spec, child, childSchema, depth + 1, childRefs);
            parent.addChild(child);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // References, properties, required lists
    // ---------------------------------------------------------------------------------------------

    /** Follows {@code $ref}s; returns null for a reference cycle. */
    private static JsonNode resolve(OpenApiSchema spec, JsonNode schema, Deque<String> refs) {
        JsonNode current = schema;
        while (current != null && current.has(REF) && current.get(REF).isTextual()) {
            String ref = current.get(REF).asText();
            if (refs.contains(ref)) {
                return null;
            }
            refs.push(ref);
            current = spec.resolveRef(ref);
        }
        return current;
    }

    /** Properties of the schema, its allOf parts and (optional) its oneOf / anyOf alternatives. */
    private static Map<String, JsonNode> properties(OpenApiSchema spec, JsonNode schema, Deque<String> refs) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        if (schema == null) {
            return result;
        }
        JsonNode allOf = schema.get("allOf");
        if (allOf != null && allOf.isArray()) {
            for (JsonNode part : allOf) {
                result.putAll(properties(spec, resolve(spec, part, new ArrayDeque<>(refs)), refs));
            }
        }
        JsonNode properties = schema.get("properties");
        if (properties != null && properties.isObject()) {
            for (Iterator<String> names = properties.fieldNames(); names.hasNext(); ) {
                String name = names.next();
                result.put(name, properties.get(name));
            }
        }
        for (String keyword : List.of("oneOf", "anyOf")) {
            JsonNode alternatives = schema.get(keyword);
            if (alternatives != null && alternatives.isArray()) {
                for (JsonNode alternative : alternatives) {
                    properties(spec, resolve(spec, alternative, new ArrayDeque<>(refs)), refs)
                            .forEach(result::putIfAbsent);
                }
            }
        }
        return result;
    }

    private static Set<String> required(OpenApiSchema spec, JsonNode schema, Deque<String> refs) {
        Set<String> result = new LinkedHashSet<>();
        if (schema == null) {
            return result;
        }
        JsonNode allOf = schema.get("allOf");
        if (allOf != null && allOf.isArray()) {
            for (JsonNode part : allOf) {
                result.addAll(required(spec, resolve(spec, part, new ArrayDeque<>(refs)), refs));
            }
        }
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            required.forEach(name -> result.add(name.asText()));
        }
        return result;
    }

    /** The schema's own or an allOf part's required list, null if none is declared. */
    private static JsonNode requiredListOf(OpenApiSchema spec, JsonNode schema, Deque<String> refs) {
        if (schema == null) {
            return null;
        }
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            return required;
        }
        JsonNode allOf = schema.get("allOf");
        if (allOf != null && allOf.isArray()) {
            for (JsonNode part : allOf) {
                JsonNode partRequired = requiredListOf(spec, resolve(spec, part, new ArrayDeque<>(refs)), refs);
                if (partRequired != null) {
                    return partRequired;
                }
            }
        }
        return null;
    }

    /**
     * Schema type; OpenAPI 3.1 type lists ({@code ["string", "null"]}) give their non-null entry. Falls back to the
     * raw node (keywords next to a $ref) and to object / array if properties or items are present.
     */
    private static String type(JsonNode schema, JsonNode raw) {
        for (JsonNode node : new JsonNode[] {schema, raw}) {
            if (node == null) {
                continue;
            }
            JsonNode type = node.get("type");
            if (type != null && type.isTextual()) {
                return type.asText();
            }
            if (type != null && type.isArray()) {
                for (JsonNode entry : type) {
                    if (entry.isTextual() && !"null".equals(entry.asText())) {
                        return entry.asText();
                    }
                }
            }
        }
        if (schema == null) {
            return null;
        }
        if (schema.has("items")) {
            return "array";
        }
        return schema.has("properties") || schema.has("allOf") || schema.has("oneOf") || schema.has("anyOf")
                ? "object" : null;
    }

    // ---------------------------------------------------------------------------------------------
    // Constraints
    // ---------------------------------------------------------------------------------------------

    /**
     * Constraints of a field: from the allOf parts, the resolved schema and the raw node (keywords next to a $ref),
     * later ones overriding earlier ones.
     */
    private static SchemaConstraints constraints(OpenApiSchema spec, JsonNode raw, JsonNode resolved,
                                                 Deque<String> refs) {
        List<JsonNode> nodes = new ArrayList<>();
        if (resolved != null) {
            JsonNode allOf = resolved.get("allOf");
            if (allOf != null && allOf.isArray()) {
                for (JsonNode part : allOf) {
                    JsonNode resolvedPart = resolve(spec, part, new ArrayDeque<>(refs));
                    if (resolvedPart != null) {
                        nodes.add(resolvedPart);
                    }
                }
            }
            nodes.add(resolved);
        }
        if (raw != null && raw != resolved) {
            nodes.add(raw);
        }
        SchemaConstraints.Builder builder = SchemaConstraints.builder();
        for (JsonNode node : nodes) {
            apply(spec, node, builder, refs);
        }
        return builder.build();
    }

    private static void apply(OpenApiSchema spec, JsonNode node, SchemaConstraints.Builder builder,
                              Deque<String> refs) {
        if (node.hasNonNull("format") && node.get("format").isTextual()) {
            builder.format(node.get("format").asText());
        }
        if (node.has("enum") && node.get("enum").isArray()) {
            List<JsonNode> values = new ArrayList<>();
            for (JsonNode value : node.get("enum")) {
                if (value.isNull()) {
                    builder.nullable(true);
                } else {
                    values.add(value);
                }
            }
            builder.enumValues(values);
        }
        if (node.has("const")) {
            if (node.get("const").isNull()) {
                builder.nullable(true);
            } else {
                builder.enumValues(List.of(node.get("const")));
            }
        }
        if (node.path("nullable").asBoolean(false) || node.path("x-nullable").asBoolean(false)) {
            builder.nullable(true);
        }
        JsonNode type = node.get("type");
        if (type != null && type.isArray()) {
            for (JsonNode entry : type) {
                if ("null".equals(entry.asText())) {
                    builder.nullable(true);
                }
            }
        }
        if (node.has("readOnly")) {
            builder.readOnly(node.get("readOnly").asBoolean(false));
        }
        if (node.has("writeOnly")) {
            builder.writeOnly(node.get("writeOnly").asBoolean(false));
        }
        applyBound(node, "minimum", "exclusiveMinimum", true, builder);
        applyBound(node, "maximum", "exclusiveMaximum", false, builder);
        if (node.has("minLength")) {
            builder.minLength(node.get("minLength").asInt());
        }
        if (node.has("maxLength")) {
            builder.maxLength(node.get("maxLength").asInt());
        }
        if (node.hasNonNull("pattern")) {
            builder.pattern(node.get("pattern").asText());
        }
        if (node.has("minItems")) {
            builder.minItems(node.get("minItems").asInt());
        }
        if (node.has("maxItems")) {
            builder.maxItems(node.get("maxItems").asInt());
        }
        for (String keyword : List.of("oneOf", "anyOf")) {
            JsonNode alternatives = node.get(keyword);
            if (alternatives != null && alternatives.isArray() && alternatives.size() > 0) {
                builder.variants(variants(spec, alternatives, refs), "oneOf".equals(keyword));
            }
        }
        JsonNode discriminator = node.get("discriminator");
        if (discriminator != null && discriminator.isTextual()) {
            builder.discriminator(discriminator.asText(), Map.of());
        } else if (discriminator != null && discriminator.isObject()) {
            Map<String, String> mapping = new LinkedHashMap<>();
            JsonNode mappingNode = discriminator.get("mapping");
            if (mappingNode != null && mappingNode.isObject()) {
                mappingNode.fields().forEachRemaining(entry ->
                        mapping.put(entry.getKey(), OpenApiSchema.refName(entry.getValue().asText())));
            }
            builder.discriminator(discriminator.path("propertyName").asText(null), mapping);
        }
    }

    /**
     * Swagger 2.0 / OpenAPI 3.0: {@code exclusiveMinimum: true} next to {@code minimum}; OpenAPI 3.1:
     * {@code exclusiveMinimum: <number>} on its own (the stricter of both bounds wins).
     */
    private static void applyBound(JsonNode node, String boundKey, String exclusiveKey, boolean lower,
                                   SchemaConstraints.Builder builder) {
        BigDecimal bound = node.has(boundKey) && node.get(boundKey).isNumber() ? node.get(boundKey).decimalValue()
                : null;
        JsonNode exclusive = node.get(exclusiveKey);
        if (exclusive != null && exclusive.isNumber()) {
            BigDecimal exclusiveBound = exclusive.decimalValue();
            boolean exclusiveIsStricter = bound == null
                    || (lower ? exclusiveBound.compareTo(bound) >= 0 : exclusiveBound.compareTo(bound) <= 0);
            set(builder, lower, exclusiveIsStricter ? exclusiveBound : bound, exclusiveIsStricter);
        } else if (bound != null) {
            set(builder, lower, bound, exclusive != null && exclusive.asBoolean(false));
        }
    }

    private static void set(SchemaConstraints.Builder builder, boolean lower, BigDecimal bound, boolean exclusive) {
        if (lower) {
            builder.minimum(bound, exclusive);
        } else {
            builder.maximum(bound, exclusive);
        }
    }

    private static List<SchemaVariant> variants(OpenApiSchema spec, JsonNode alternatives, Deque<String> refs) {
        List<SchemaVariant> variants = new ArrayList<>();
        int index = 1;
        for (JsonNode alternative : alternatives) {
            String name;
            if (alternative.has(REF) && alternative.get(REF).isTextual()) {
                name = OpenApiSchema.refName(alternative.get(REF).asText());
            } else if (alternative.hasNonNull("title")) {
                name = alternative.get("title").asText();
            } else {
                name = "option" + index;
            }
            JsonNode resolved = resolve(spec, alternative, new ArrayDeque<>(refs));
            variants.add(new SchemaVariant(name, required(spec, resolved, refs)));
            index++;
        }
        return variants;
    }
}
