package com.github.legregor.microservice.coverage.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Tests for {@link RequestBodySchemaValidator}, mainly the handling of {@code null} values. */
class RequestBodySchemaValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPONENTS = """
            {"Reservation": {"type": "object", "required": ["orderId"], "properties": {
               "orderId": {"type": "string"},
               "note": {"type": "string"},
               "holdMinutes": {"type": "integer"},
               "deliveryAddress": {"$ref": "#/components/schemas/Address"},
               "comment": {"type": "string", "nullable": true},
               "items": {"type": "array", "items": {"$ref": "#/components/schemas/Item"}}}},
             "Address": {"type": "object", "required": ["street"], "properties": {"street": {"type": "string"}}},
             "Item": {"type": "object", "properties": {"sku": {"type": "string"}}}}
            """;

    private final RequestBodySchemaValidator validator = new RequestBodySchemaValidator();

    @Test
    void nullInOptionalFieldsCountsAsAbsent() throws Exception {
        RequestBodySchemaValidator.ValidationResult result = validate("""
                {"orderId": "o-1", "note": null, "holdMinutes": null, "deliveryAddress": null,
                 "items": [{"sku": null}]}
                """);

        assertTrue(result.matches(), result.getMessage());
        assertFalse(result.getPresentFields().contains("note"));
        assertTrue(result.getMissingFields().contains("note"));
    }

    @Test
    void nullInARequiredFieldIsAMismatch() throws Exception {
        RequestBodySchemaValidator.ValidationResult result = validate("""
                {"orderId": null}
                """);

        assertFalse(result.matches());
        assertEquals("Required field 'orderId' is null at $.", result.getMessage());
    }

    @Test
    void nullableRequiredFieldMayBeNull() throws Exception {
        OpenApiSchema schema = new OpenApiSchema(MAPPER.readTree("""
                {"type": "object", "required": ["comment"], "properties": {"comment": {"type": "string", "nullable": true}}}
                """), null, null);

        assertTrue(validator.validate("{\"comment\": null}", schema).matches());
    }

    @Test
    void wrongTypesAreStillMismatches() throws Exception {
        assertEquals("Expected integer at $.holdMinutes.",
                validate("{\"orderId\": \"o-1\", \"holdMinutes\": \"soon\"}").getMessage());
        assertEquals("Expected object at $.deliveryAddress.",
                validate("{\"orderId\": \"o-1\", \"deliveryAddress\": \"Main Street 1\"}").getMessage());
    }

    private RequestBodySchemaValidator.ValidationResult validate(String body) throws Exception {
        OpenApiSchema schema = new OpenApiSchema(MAPPER.readTree("{\"$ref\": \"#/components/schemas/Reservation\"}"),
                null, MAPPER.readTree(COMPONENTS));
        return validator.validate(body, schema);
    }
}
