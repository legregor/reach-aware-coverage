package com.github.legregor.microservice.coverage.openapi;

import java.util.Iterator;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resolves the documented response of an endpoint for a returned status code from the service's OpenAPI document
 * (Swagger 2.0 and OpenAPI 3.x).
 * <p>
 * The response entry is looked up as OpenAPI defines the precedence: the exact status code ({@code "409"}), then
 * the range of its class ({@code "4XX"}, OpenAPI 3, case-insensitive), then {@code default}. Entries may be
 * references to {@code #/components/responses/} (OpenAPI 3) or {@code #/responses/} (Swagger 2.0).
 */
public class OpenApiResponseResolver {
    private final OpenApiOperationLocator locator;

    public OpenApiResponseResolver(OpenApiSpecRegistry registry) {
        this.locator = new OpenApiOperationLocator(registry);
    }

    /**
     * A documented response: the key it was found under ({@code "409"}, {@code "4XX"} or {@code "default"}) and its
     * body schema, which is null if the response is documented without a (JSON) body, e.g. a 204.
     */
    public record DocumentedResponse(String key, OpenApiSchema schema) {
        public boolean hasBody() {
            return schema != null;
        }
    }

    /**
     * The documented response of the first matching operation that documents the status code; empty if there is no
     * spec, no matching operation, or no entry for the code (exact, range or default).
     */
    public Optional<DocumentedResponse> resolve(String serviceName, String method, String path, int statusCode) {
        for (OpenApiOperationLocator.Operation operation : locator.findOperations(serviceName, method, path)) {
            JsonNode responses = operation.node().get("responses");
            if (responses == null || !responses.isObject()) {
                continue;
            }
            String key = findKey(responses, statusCode);
            if (key == null) {
                continue;
            }
            JsonNode response = resolveResponse(operation.spec(), responses.get(key));
            if (response == null) {
                continue;
            }
            return Optional.of(new DocumentedResponse(key, bodySchema(operation, response)));
        }
        return Optional.empty();
    }

    /** The key for a status code: exact code, then range (e.g. "4XX", any case), then "default"; null if none. */
    static String findKey(JsonNode responses, int statusCode) {
        String exact = String.valueOf(statusCode);
        if (responses.has(exact)) {
            return exact;
        }
        String range = (statusCode / 100) + "XX";
        for (Iterator<String> keys = responses.fieldNames(); keys.hasNext(); ) {
            String key = keys.next();
            if (key.equalsIgnoreCase(range)) {
                return key;
            }
        }
        return responses.has("default") ? "default" : null;
    }

    private static JsonNode resolveResponse(JsonNode spec, JsonNode response) {
        if (response == null || !response.has("$ref")) {
            return response;
        }
        JsonNode resolved = OpenApiOperationLocator.resolveComponent(spec, response, "#/components/responses/");
        return resolved != null ? resolved
                : OpenApiOperationLocator.resolveComponent(spec, response, "#/responses/");
    }

    /** OpenAPI 3: schema of the JSON content; Swagger 2.0: the response's {@code schema}. Null if there is none. */
    private static OpenApiSchema bodySchema(OpenApiOperationLocator.Operation operation, JsonNode response) {
        JsonNode schema = OpenApiOperationLocator.jsonSchema(response.get("content"));
        if (schema == null) {
            JsonNode swaggerSchema = response.get("schema");
            schema = swaggerSchema == null || swaggerSchema.isNull() ? null : swaggerSchema;
        }
        return schema == null ? null : operation.schemas().withSchema(schema);
    }
}
