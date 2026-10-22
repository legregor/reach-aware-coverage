package com.github.legregor.microservice.coverage.openapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Wrapper around a request-body schema plus OpenAPI definitions for $ref
 * resolution.
 */
public class OpenApiSchema {
    private static final String DEFINITIONS_PREFIX = "#/definitions/";
    private static final String COMPONENTS_PREFIX = "#/components/schemas/";

    private final JsonNode schema;
    private final JsonNode definitions;
    private final JsonNode componentSchemas;

    /** Swagger 2.0: references point to {@code #/definitions/<name>}. */
    public OpenApiSchema(JsonNode schema, JsonNode definitions) {
        this(schema, definitions, null);
    }

    /**
     * @param definitions      the {@code definitions} of a Swagger 2.0 spec, may be null
     * @param componentSchemas the {@code components.schemas} of an OpenAPI 3.x spec, may be null
     */
    public OpenApiSchema(JsonNode schema, JsonNode definitions, JsonNode componentSchemas) {
        this.schema = schema;
        this.definitions = definitions;
        this.componentSchemas = componentSchemas;
    }

    /** Same definitions, another schema (e.g. a parameter schema of the same spec). */
    public OpenApiSchema withSchema(JsonNode otherSchema) {
        return new OpenApiSchema(otherSchema, definitions, componentSchemas);
    }

    public JsonNode getSchema() {
        return schema;
    }

    /**
     * Resolves local references of the form {@code #/definitions/<name>} (Swagger 2.0) or
     * {@code #/components/schemas/<name>} (OpenAPI 3.x); null if the reference cannot be resolved.
     */
    public JsonNode resolveRef(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith(DEFINITIONS_PREFIX)) {
            return lookup(definitions, ref.substring(DEFINITIONS_PREFIX.length()));
        }
        if (ref.startsWith(COMPONENTS_PREFIX)) {
            return lookup(componentSchemas, ref.substring(COMPONENTS_PREFIX.length()));
        }
        return null;
    }

    /** Name of the referenced schema (last segment of the reference), e.g. {@code Order}. */
    public static String refName(String ref) {
        if (ref == null) {
            return null;
        }
        int slash = ref.lastIndexOf('/');
        return slash >= 0 ? ref.substring(slash + 1) : ref;
    }

    private static JsonNode lookup(JsonNode container, String name) {
        if (container == null || !container.isObject()) {
            return null;
        }
        return container.get(name.replace("~1", "/").replace("~0", "~"));
    }

    /**
     * Resolves the schema, following any top-level $ref to its definition.
     * If the schema contains a $ref, returns the referenced definition instead.
     * Otherwise returns the schema as-is.
     *
     * @return The resolved schema (with $ref followed to its definition)
     */
    public JsonNode getResolvedSchema() {
        if (schema == null) {
            return schema;
        }

        JsonNode ref = schema.get("$ref");
        if (ref != null && ref.isTextual()) {
            JsonNode resolved = resolveRef(ref.asText());
            if (resolved != null) {
                return resolved;
            }
        }

        return schema;
    }
}
