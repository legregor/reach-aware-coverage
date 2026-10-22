package com.github.legregor.microservice.coverage.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resolves request-body and path parameter schemas from service-specific OpenAPI documents (Swagger 2.0 and
 * OpenAPI 3.x).
 */
public class OpenApiRequestBodyResolver {
    private final OpenApiOperationLocator locator;

    public OpenApiRequestBodyResolver(OpenApiSpecRegistry registry) {
        this.locator = new OpenApiOperationLocator(registry);
    }

    /**
     * Finds the request-body schema for a concrete service/method/path call: a Swagger 2.0 body parameter or an
     * OpenAPI 3.x {@code requestBody} (JSON content preferred).
     */
    public Optional<OpenApiSchema> resolveSchema(String serviceName, String method, String path) {
        for (OpenApiOperationLocator.Operation operation : locator.findOperations(serviceName, method, path)) {
            JsonNode parameters = operation.node().get("parameters");
            if (parameters != null && parameters.isArray()) {
                for (JsonNode parameter : parameters) {
                    JsonNode inNode = parameter.get("in");
                    if (inNode != null && "body".equalsIgnoreCase(inNode.asText())) {
                        JsonNode schema = parameter.get("schema");
                        if (schema != null && !schema.isNull()) {
                            return Optional.of(operation.schemas().withSchema(schema));
                        }
                    }
                }
            }
            JsonNode requestBody = OpenApiOperationLocator.resolveComponent(operation.spec(), operation.node().get("requestBody"),
                    "#/components/requestBodies/");
            JsonNode schema = OpenApiOperationLocator.jsonSchema(requestBody == null ? null : requestBody.get("content"));
            if (schema != null) {
                return Optional.of(operation.schemas().withSchema(schema));
            }
        }
        return Optional.empty();
    }

    /**
     * The path parameters of the matching operation (operation and path-level parameters) as schema fields, by
     * name. Empty if no spec or operation is found.
     */
    public Map<String, SchemaField> resolvePathParameters(String serviceName, String method, String path) {
        Map<String, SchemaField> result = new LinkedHashMap<>();
        for (OpenApiOperationLocator.Operation operation : locator.findOperations(serviceName, method, path)) {
            for (JsonNode parameters : new JsonNode[] {operation.pathItem().get("parameters"),
                    operation.node().get("parameters")}) {
                if (parameters == null || !parameters.isArray()) {
                    continue;
                }
                for (JsonNode parameterNode : parameters) {
                    JsonNode parameter = OpenApiOperationLocator.resolveComponent(operation.spec(), parameterNode,
                            "#/components/parameters/");
                    if (parameter == null) {
                        parameter = OpenApiOperationLocator.resolveComponent(operation.spec(), parameterNode, "#/parameters/");
                    }
                    if (parameter != null && "path".equalsIgnoreCase(parameter.path("in").asText())) {
                        SchemaField field = SchemaFieldReader.readParameter(operation.schemas(), parameter);
                        result.put(field.getName(), field); // operation-level parameters override path-level ones
                    }
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return result;
    }
}
