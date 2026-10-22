package com.github.legregor.microservice.coverage.openapi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates JSON request bodies against a subset of OpenAPI schema constructs.
 * <p>
 * {@code null} in an optional property counts as absent: Jackson writes unset fields as {@code null} by default, so
 * the traces cannot tell the two apart (the same rule as the "no value" partition of the value partition coverage).
 * {@code null} in a required property is only valid if the property is nullable ({@code nullable: true},
 * {@code x-nullable: true} or {@code type: [..., "null"]}).
 */
public class RequestBodySchemaValidator {
    private final ObjectMapper mapper;

    public RequestBodySchemaValidator() {
        this.mapper = new ObjectMapper();
    }

    public ValidationResult validate(String body, OpenApiSchema schema) {
        return validateWithFieldPresence(body, schema);
    }

    /**
     * Validates structure and also reports which schema fields are present/missing
     * in the payload.
     */
    public ValidationResult validateWithFieldPresence(String body, OpenApiSchema schema) {
        if (schema == null || schema.getSchema() == null) {
            return ValidationResult.fail("No schema available");
        }
        if (body == null || body.trim().isEmpty()) {
            return ValidationResult.fail("Body is empty");
        }

        try {
            JsonNode instance = mapper.readTree(body);
            JsonNode resolvedSchema = schema.getResolvedSchema();
            List<String> expectedFields = collectExpectedFields(resolvedSchema, schema, "");
            List<String> presentFields = expectedFields.stream()
                    .filter(fieldPath -> isFieldPresent(instance, fieldPath))
                    .toList();
            List<String> missingFields = expectedFields.stream()
                    .filter(fieldPath -> !isFieldPresent(instance, fieldPath))
                    .toList();

            ValidationResult baseResult = validateNode(instance, schema.getSchema(), schema, "$.");
            if (baseResult.matches()) {
                return ValidationResult.ok(presentFields, missingFields);
            }
            return ValidationResult.fail(baseResult.getMessage(), presentFields, missingFields);
        } catch (Exception e) {
            return ValidationResult.fail("Body is not valid JSON");
        }
    }

    /**
     * Recursive validator for objects, arrays, scalar types and composition
     * keywords.
     */
    private ValidationResult validateNode(JsonNode instance, JsonNode schemaNode, OpenApiSchema schema, String path) {
        if (schemaNode == null || schemaNode.isMissingNode() || schemaNode.isNull()) {
            return ValidationResult.ok();
        }
        if (instance != null && instance.isNull() && isNullable(schemaNode, schema, 0)) {
            return ValidationResult.ok();
        }

        JsonNode refNode = schemaNode.get("$ref");
        if (refNode != null && refNode.isTextual()) {
            String ref = refNode.asText();
            JsonNode resolved = schema.resolveRef(ref);
            if (resolved == null) {
                return ValidationResult.fail("Unresolved schema reference: " + ref);
            }
            return validateNode(instance, resolved, schema, path);
        }

        JsonNode anyOf = schemaNode.get("anyOf");
        if (anyOf != null && anyOf.isArray()) {
            for (JsonNode option : anyOf) {
                if (validateNode(instance, option, schema, path).matches()) {
                    return ValidationResult.ok();
                }
            }
            return ValidationResult.fail("No anyOf schema matched at " + path);
        }

        JsonNode oneOf = schemaNode.get("oneOf");
        if (oneOf != null && oneOf.isArray()) {
            for (JsonNode option : oneOf) {
                if (validateNode(instance, option, schema, path).matches()) {
                    return ValidationResult.ok();
                }
            }
            return ValidationResult.fail("No oneOf schema matched at " + path);
        }

        JsonNode allOf = schemaNode.get("allOf");
        if (allOf != null && allOf.isArray()) {
            for (JsonNode option : allOf) {
                ValidationResult result = validateNode(instance, option, schema, path);
                if (!result.matches()) {
                    return result;
                }
            }
            return ValidationResult.ok();
        }

        String type = schemaNode.has("type") ? schemaNode.get("type").asText() : null;
        if (type == null) {
            if (schemaNode.has("properties")) {
                type = "object";
            } else if (schemaNode.has("items")) {
                type = "array";
            }
        }

        if (type == null) {
            return ValidationResult.ok();
        }

        switch (type) {
            case "object":
                if (!instance.isObject()) {
                    return ValidationResult.fail("Expected object at " + path);
                }
                JsonNode required = schemaNode.get("required");
                if (required != null && required.isArray()) {
                    for (JsonNode requiredField : required) {
                        if (requiredField.isTextual() && !instance.has(requiredField.asText())) {
                            return ValidationResult
                                    .fail(" Missing required field '" + requiredField.asText() + "' at " + path);
                        }
                    }
                }
                JsonNode properties = schemaNode.get("properties");
                if (properties != null && properties.isObject()) {
                    for (Iterator<String> it = properties.fieldNames(); it.hasNext();) {
                        String fieldName = it.next();
                        if (instance.has(fieldName)) {
                            JsonNode value = instance.get(fieldName);
                            JsonNode propertySchema = properties.get(fieldName);
                            if (value.isNull() && !isNullable(propertySchema, schema, 0)) {
                                if (isRequired(required, fieldName)) {
                                    return ValidationResult.fail("Required field '" + fieldName + "' is null at "
                                            + path);
                                }
                                // optional and null = absent (Jackson writes unset fields as null)
                                continue;
                            }
                            ValidationResult result = validateNode(value, propertySchema,
                                    schema,
                                    path + fieldName + ".");
                            if (!result.matches()) {
                                return result;
                            }
                        }
                    }
                }
                return ValidationResult.ok();
            case "array":
                if (!instance.isArray()) {
                    return ValidationResult.fail("Expected array at " + path);
                }
                JsonNode items = schemaNode.get("items");
                if (items != null && !items.isNull()) {
                    int index = 0;
                    for (JsonNode element : instance) {
                        ValidationResult result = validateNode(element, items, schema, path + "[" + index + "]");
                        if (!result.matches()) {
                            return result;
                        }
                        index++;
                    }
                }
                return ValidationResult.ok();
            case "string":
                return instance.isTextual() ? ValidationResult.ok()
                        : ValidationResult.fail("Expected string at " + path);
            case "integer":
                return instance.isIntegralNumber() ? ValidationResult.ok()
                        : ValidationResult.fail("Expected integer at " + path);
            case "number":
                return instance.isNumber() ? ValidationResult.ok()
                        : ValidationResult.fail("Expected number at " + path);
            case "boolean":
                return instance.isBoolean() ? ValidationResult.ok()
                        : ValidationResult.fail("Expected boolean at " + path);
            default:
                return ValidationResult.ok();
        }
    }

    private static boolean isRequired(JsonNode required, String fieldName) {
        if (required == null || !required.isArray()) {
            return false;
        }
        for (JsonNode requiredField : required) {
            if (requiredField.isTextual() && requiredField.asText().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the schema allows null: {@code nullable: true} (OpenAPI 3.0), {@code x-nullable: true} (Swagger 2.0),
     * {@code type} containing {@code "null"} (OpenAPI 3.1), also behind a {@code $ref} or in an {@code allOf} wrapper.
     */
    private static boolean isNullable(JsonNode schemaNode, OpenApiSchema schema, int depth) {
        if (schemaNode == null || !schemaNode.isObject() || depth > 10) {
            return false;
        }
        if (schemaNode.path("nullable").asBoolean(false) || schemaNode.path("x-nullable").asBoolean(false)) {
            return true;
        }
        JsonNode type = schemaNode.get("type");
        if (type != null && type.isArray()) {
            for (JsonNode entry : type) {
                if ("null".equals(entry.asText())) {
                    return true;
                }
            }
        }
        JsonNode ref = schemaNode.get("$ref");
        if (ref != null && ref.isTextual() && isNullable(schema.resolveRef(ref.asText()), schema, depth + 1)) {
            return true;
        }
        for (String composition : new String[] { "anyOf", "oneOf" }) {
            JsonNode options = schemaNode.get(composition);
            if (options != null && options.isArray()) {
                for (JsonNode option : options) {
                    if (isNullable(option, schema, depth + 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Immutable validation result returned for one request body.
     */
    public static class ValidationResult {
        private final boolean matches;
        private final String message;
        private final List<String> presentFields;
        private final List<String> missingFields;

        private ValidationResult(boolean matches, String message, List<String> presentFields,
                List<String> missingFields) {
            this.matches = matches;
            this.message = message;
            this.presentFields = presentFields == null ? List.of() : presentFields;
            this.missingFields = missingFields == null ? List.of() : missingFields;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "OK", List.of(), List.of());
        }

        public static ValidationResult ok(List<String> presentFields, List<String> missingFields) {
            return new ValidationResult(true, "OK", presentFields, missingFields);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message, List.of(), List.of());
        }

        public static ValidationResult fail(String message, List<String> presentFields, List<String> missingFields) {
            return new ValidationResult(false, message, presentFields, missingFields);
        }

        public boolean matches() {
            return matches;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getPresentFields() {
            return presentFields;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }
    }

    /**
     * Collects flattened field paths from schema properties (including nested
     * objects).
     */
    private List<String> collectExpectedFields(JsonNode schemaNode, OpenApiSchema schema, String prefix) {
        if (schemaNode == null || schemaNode.isMissingNode() || schemaNode.isNull()) {
            return List.of();
        }

        JsonNode refNode = schemaNode.get("$ref");
        if (refNode != null && refNode.isTextual()) {
            JsonNode resolved = schema.resolveRef(refNode.asText());
            if (resolved == null) {
                return List.of();
            }
            return collectExpectedFields(resolved, schema, prefix);
        }

        JsonNode properties = schemaNode.get("properties");
        if (properties == null || !properties.isObject()) {
            return List.of();
        }

        Set<String> fields = new LinkedHashSet<>();
        for (Iterator<String> it = properties.fieldNames(); it.hasNext();) {
            String fieldName = it.next();
            String fullPath = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
            fields.add(fullPath);
            fields.addAll(collectExpectedFields(properties.get(fieldName), schema, fullPath));
        }
        return new ArrayList<>(fields);
    }

    private boolean isFieldPresent(JsonNode root, String fieldPath) {
        if (root == null || fieldPath == null || fieldPath.isBlank()) {
            return false;
        }
        JsonNode cursor = root;
        String[] parts = fieldPath.split("\\.");
        for (String part : parts) {
            if (cursor == null || !cursor.isObject() || !cursor.has(part)) {
                return false;
            }
            cursor = cursor.get(part);
        }
        return cursor != null && !cursor.isNull();
    }
}
