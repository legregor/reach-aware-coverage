package com.github.legregor.microservice.coverage.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the constraints read by {@link SchemaFieldReader}: Swagger 2.0, OpenAPI 3.0 and OpenAPI 3.1 notations
 * end up in the same {@link SchemaConstraints}.
 */
class SchemaConstraintsReaderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void swagger2Constraints() throws IOException {
        SchemaField body = read2("""
                {"$ref": "#/definitions/Order"}
                """, """
                {"Order": {"type": "object", "required": ["quantity"], "properties": {
                  "id": {"type": "string", "readOnly": true},
                  "quantity": {"type": "integer", "format": "int32", "minimum": 0, "exclusiveMinimum": true,
                               "maximum": 99},
                  "comment": {"type": "string", "maxLength": 200, "x-nullable": true},
                  "code": {"type": "string", "minLength": 2, "pattern": "^[A-Z]+$"},
                  "seatClass": {"type": "string", "enum": ["ECONOMY", "FIRST"]}}}}
                """);

        SchemaConstraints quantity = child(body, "quantity").getConstraints();
        assertTrue(child(body, "quantity").isRequired());
        assertTrue(child(body, "quantity").isRequiredDeclared());
        assertEquals("int32", quantity.getFormat());
        assertEquals(new BigDecimal(0), quantity.getMinimum());
        assertTrue(quantity.isExclusiveMinimum());
        assertEquals(new BigDecimal(99), quantity.getMaximum());
        assertFalse(quantity.isExclusiveMaximum());

        assertTrue(child(body, "id").isReadOnly());
        assertTrue(child(body, "comment").getConstraints().isNullable());
        assertEquals(Integer.valueOf(200), child(body, "comment").getConstraints().getMaxLength());
        assertFalse(child(body, "comment").isRequired());
        assertEquals(Integer.valueOf(2), child(body, "code").getConstraints().getMinLength());
        assertEquals("^[A-Z]+$", child(body, "code").getConstraints().getPattern());
        assertEquals(List.of("ECONOMY", "FIRST"), texts(child(body, "seatClass").getConstraints().getEnumValues()));
    }

    @Test
    void openApi30ComponentsNullableAndKeywordsNextToReferences() throws IOException {
        SchemaField body = read3("""
                {"$ref": "#/components/schemas/Order"}
                """, """
                {"Order": {"type": "object", "properties": {
                   "address": {"allOf": [{"$ref": "#/components/schemas/Address"}], "nullable": true},
                   "createdAt": {"$ref": "#/components/schemas/Timestamp", "readOnly": true}}},
                 "Address": {"type": "object", "required": ["zip"], "properties": {
                   "zip": {"type": "string", "pattern": "^[0-9]{5}$"}}},
                 "Timestamp": {"type": "string", "format": "date-time"}}
                """);

        SchemaField address = child(body, "address");
        assertEquals("object", address.getType());
        assertTrue(address.getConstraints().isNullable());
        assertEquals(List.of("zip"), names(address.getChildren()));
        assertTrue(child(address, "zip").isRequired());

        SchemaField createdAt = child(body, "createdAt");
        assertEquals("string", createdAt.getType());
        assertEquals("date-time", createdAt.getConstraints().getFormat());
        assertTrue(createdAt.isReadOnly());
        // the Order schema declares no required list: optional only by omission
        assertFalse(createdAt.isRequiredDeclared());
    }

    @Test
    void openApi31TypeListsNumericExclusiveBoundsAndConst() throws IOException {
        SchemaField body = read3("""
                {"type": "object", "properties": {
                  "price": {"type": ["number", "null"], "exclusiveMinimum": 0, "maximum": 1000},
                  "version": {"const": 2},
                  "level": {"type": "integer", "minimum": 1, "exclusiveMaximum": 10}}}
                """, "{}");

        SchemaField price = child(body, "price");
        assertEquals("number", price.getType());
        assertTrue(price.getConstraints().isNullable());
        assertEquals(new BigDecimal(0), price.getConstraints().getMinimum());
        assertTrue(price.getConstraints().isExclusiveMinimum());
        assertEquals(new BigDecimal(1000), price.getConstraints().getMaximum());

        assertEquals(List.of("2"), texts(child(body, "version").getConstraints().getEnumValues()));

        SchemaConstraints level = child(body, "level").getConstraints();
        assertEquals(new BigDecimal(1), level.getMinimum());
        assertFalse(level.isExclusiveMinimum());
        assertEquals(new BigDecimal(10), level.getMaximum());
        assertTrue(level.isExclusiveMaximum());
    }

    @Test
    void enumWithNullMakesTheFieldNullable() throws IOException {
        SchemaField body = read3("""
                {"type": "object", "properties": {"color": {"type": "string", "enum": ["RED", null]}}}
                """, "{}");

        SchemaConstraints color = child(body, "color").getConstraints();
        assertTrue(color.isNullable());
        assertEquals(List.of("RED"), texts(color.getEnumValues()));
    }

    @Test
    void arrayBoundsBelongToTheListField() throws IOException {
        SchemaField body = read3("""
                {"type": "object", "properties": {
                  "items": {"type": "array", "minItems": 1, "maxItems": 10,
                            "items": {"type": "object", "properties": {"sku": {"type": "string"}}}}}}
                """, "{}");

        SchemaField items = child(body, "items");
        assertTrue(items.isList());
        assertEquals(Integer.valueOf(1), items.getConstraints().getMinItems());
        assertEquals(Integer.valueOf(10), items.getConstraints().getMaxItems());
        assertEquals(List.of("sku"), names(items.getChildren()));
    }

    @Test
    void oneOfWithDiscriminatorMapping() throws IOException {
        SchemaField body = read3("""
                {"type": "object", "properties": {"payment": {
                  "oneOf": [{"$ref": "#/components/schemas/Card"}, {"$ref": "#/components/schemas/Iban"}],
                  "discriminator": {"propertyName": "kind",
                                    "mapping": {"card": "#/components/schemas/Card",
                                                "iban": "#/components/schemas/Iban"}}}}}
                """, """
                {"Card": {"type": "object", "required": ["kind", "number"],
                          "properties": {"kind": {"type": "string"}, "number": {"type": "string"}}},
                 "Iban": {"type": "object", "required": ["kind", "iban"],
                          "properties": {"kind": {"type": "string"}, "iban": {"type": "string"}}}}
                """);

        SchemaField payment = child(body, "payment");
        assertEquals("object", payment.getType());
        SchemaConstraints constraints = payment.getConstraints();
        assertTrue(constraints.isExclusiveVariants());
        assertEquals(List.of("Card", "Iban"),
                constraints.getVariants().stream().map(SchemaVariant::name).collect(Collectors.toList()));
        assertEquals(Set.of("kind", "number"), constraints.getVariants().get(0).requiredProperties());
        assertEquals("kind", constraints.getDiscriminatorProperty());
        assertEquals(Map.of("card", "Card", "iban", "Iban"), constraints.getDiscriminatorMapping());
        // the properties of all alternatives are (optional) children
        assertEquals(List.of("kind", "number", "iban"), names(payment.getChildren()));
        assertTrue(payment.getChildren().stream().noneMatch(SchemaField::isRequired));
    }

    @Test
    void parametersOfBothSpecVersions() throws IOException {
        OpenApiSchema spec = new OpenApiSchema(null, null, MAPPER.readTree("""
                {"OrderId": {"type": "string", "format": "uuid"}}
                """));

        SchemaField swagger2 = SchemaFieldReader.readParameter(spec, MAPPER.readTree("""
                {"name": "page", "in": "path", "type": "integer", "minimum": 0}
                """));
        assertEquals("page", swagger2.getName());
        assertEquals("integer", swagger2.getType());
        assertTrue(swagger2.isRequired());
        assertEquals(new BigDecimal(0), swagger2.getConstraints().getMinimum());

        SchemaField openApi3 = SchemaFieldReader.readParameter(spec, MAPPER.readTree("""
                {"name": "orderId", "in": "path", "required": true,
                 "schema": {"$ref": "#/components/schemas/OrderId"}}
                """));
        assertEquals("string", openApi3.getType());
        assertEquals("uuid", openApi3.getConstraints().getFormat());
    }

    @Test
    void fieldsWithoutConstraintsHaveNone() throws IOException {
        SchemaField body = read2("""
                {"type": "object", "properties": {"name": {"type": "string"}}}
                """, "{}");

        SchemaConstraints name = child(body, "name").getConstraints();
        assertNull(name.getFormat());
        assertFalse(name.hasEnum());
        assertFalse(name.hasRange());
        assertFalse(name.hasLengthBounds());
        assertFalse(name.isNullable());
        assertFalse(name.isReadOnly());
        assertTrue(name.getVariants().isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static SchemaField read2(String bodySchema, String definitions) throws IOException {
        return SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree(bodySchema), MAPPER.readTree(definitions)));
    }

    private static SchemaField read3(String bodySchema, String componentSchemas) throws IOException {
        return SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree(bodySchema), null,
                MAPPER.readTree(componentSchemas)));
    }

    private static SchemaField child(SchemaField parent, String name) {
        return parent.getChildren().stream().filter(field -> name.equals(field.getName())).findFirst()
                .orElseThrow(() -> new AssertionError("no field " + name + " in " + parent));
    }

    private static List<String> names(List<SchemaField> fields) {
        return fields.stream().map(SchemaField::getName).collect(Collectors.toList());
    }

    private static List<String> texts(List<JsonNode> values) {
        return values.stream().map(JsonNode::asText).collect(Collectors.toList());
    }
}
