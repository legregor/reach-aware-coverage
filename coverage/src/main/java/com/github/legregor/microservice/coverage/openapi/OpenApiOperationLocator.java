package com.github.legregor.microservice.coverage.openapi;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Finds the operations of a service's OpenAPI document (Swagger 2.0 and OpenAPI 3.x) that match a called
 * method and path, shared by the request and the response side so both match endpoints the same way.
 */
final class OpenApiOperationLocator {
    private final OpenApiSpecRegistry registry;

    OpenApiOperationLocator(OpenApiSpecRegistry registry) {
        this.registry = registry;
    }

    /** An operation of a spec that matches a call, with the spec's schema definitions for $ref resolution. */
    record Operation(JsonNode spec, JsonNode pathItem, JsonNode node, OpenApiSchema schemas) {
    }

    /** Matching operations: exact path matches first, then template matches. */
    List<Operation> findOperations(String serviceName, String method, String path) {
        List<Operation> exact = new ArrayList<>();
        List<Operation> templated = new ArrayList<>();
        Optional<JsonNode> specOptional = registry.getSpec(serviceName);
        String normalizedPath = normalizePath(path);
        if (specOptional.isEmpty() || normalizedPath.isEmpty()) {
            return exact;
        }
        JsonNode spec = specOptional.get();
        JsonNode paths = spec.get("paths");
        if (paths == null || !paths.isObject()) {
            return exact;
        }
        String methodLower = method == null ? "" : method.toLowerCase();
        OpenApiSchema schemas = new OpenApiSchema(null, spec.get("definitions"),
                spec.path("components").get("schemas"));
        for (Iterator<String> it = paths.fieldNames(); it.hasNext(); ) {
            String specPath = it.next();
            if (!isPathMatch(specPath, normalizedPath)) {
                continue;
            }
            JsonNode pathItem = paths.get(specPath);
            JsonNode operation = pathItem.get(methodLower);
            if (operation == null || operation.isMissingNode()) {
                continue;
            }
            Operation match = new Operation(spec, pathItem, operation, schemas);
            if (trimTrailingSlash(specPath).equals(normalizedPath)) {
                exact.add(match);
            } else {
                templated.add(match);
            }
        }
        exact.addAll(templated);
        return exact;
    }

    /**
     * Follows a {@code $ref} to a component of the given kind (e.g. {@code #/components/requestBodies/}); the node
     * itself if it is no reference, null if it references another kind or cannot be resolved.
     */
    static JsonNode resolveComponent(JsonNode spec, JsonNode node, String prefix) {
        if (node == null || !node.has("$ref")) {
            return node;
        }
        String ref = node.get("$ref").asText();
        if (!ref.startsWith(prefix)) {
            return null;
        }
        JsonNode current = spec;
        for (String segment : ref.substring(2).split("/")) {
            current = current == null ? null : current.get(segment.replace("~1", "/").replace("~0", "~"));
        }
        return current;
    }

    /** The schema of the JSON media type of a content map (application/json, then any +json, then the first). */
    static JsonNode jsonSchema(JsonNode content) {
        if (content == null || !content.isObject() || content.size() == 0) {
            return null;
        }
        JsonNode json = content.get("application/json");
        if (json == null) {
            for (Iterator<String> types = content.fieldNames(); types.hasNext(); ) {
                String type = types.next();
                if (type.contains("json")) {
                    json = content.get(type);
                    break;
                }
            }
        }
        if (json == null) {
            json = content.elements().next();
        }
        JsonNode schema = json.get("schema");
        return schema == null || schema.isNull() ? null : schema;
    }

    /** Normalizes URLs and raw paths. */
    private static String normalizePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String trimmed = rawPath.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String pathOnly = extractPath(trimmed);
        if (pathOnly.isEmpty()) {
            return "";
        }
        if (!pathOnly.startsWith("/")) {
            pathOnly = "/" + pathOnly;
        }
        return trimTrailingSlash(pathOnly);
    }

    private static String extractPath(String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            try {
                URI uri = new URI(raw);
                String uriPath = uri.getPath();
                return uriPath == null ? "" : uriPath;
            } catch (Exception e) {
                // not a valid URI (e.g. a template with {variables}): cut off scheme and host by hand
                int pathStart = raw.indexOf('/', raw.indexOf("://") + 3);
                raw = pathStart < 0 ? "/" : raw.substring(pathStart);
            }
        }
        String withoutQuery = raw.split("\\?")[0];
        return withoutQuery.split("#")[0];
    }

    private static String trimTrailingSlash(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /** Supports exact matches and path-template matches like /orders/{id}. */
    private static boolean isPathMatch(String specPath, String actualPath) {
        String normalizedSpec = trimTrailingSlash(specPath);
        String normalizedActual = trimTrailingSlash(actualPath);
        if (normalizedSpec.equals(normalizedActual)) {
            return true;
        }
        if (!normalizedSpec.contains("{")) {
            return false;
        }
        String pattern = normalizedSpec.replaceAll("\\{[^/]+\\}", "[^/]+?");
        return normalizedActual.matches("^" + pattern + "$");
    }
}
