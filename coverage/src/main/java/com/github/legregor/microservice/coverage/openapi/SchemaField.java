package com.github.legregor.microservice.coverage.openapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A field of a schema (request body, response body or parameter), as described by the OpenAPI spec (no coverage
 * concepts).
 * <p>
 * For an object, {@link #getChildren()} are its properties. For a list ({@link #isList()}), they are the properties
 * of its items. The root returned by {@link SchemaFieldReader#read(OpenApiSchema)} has no name: it is the body
 * itself. {@link #getConstraints()} holds the value constraints (format, enum, range, length, pattern, ...); for a
 * list they are the constraints of the array itself (minItems, maxItems).
 */
public class SchemaField {

    private final String name;
    private final String type;
    private final boolean required;
    private final boolean requiredDeclared;
    private final boolean list;
    private final SchemaConstraints constraints;
    private final List<SchemaField> children = new ArrayList<>();

    public SchemaField(String name, String type, boolean required, boolean list) {
        this(name, type, required, required, list, SchemaConstraints.NONE);
    }

    /**
     * @param requiredDeclared true if the enclosing object declares a {@code required} list (so "optional" is a
     *                         statement of the spec and not just a missing list)
     */
    public SchemaField(String name, String type, boolean required, boolean requiredDeclared, boolean list,
                       SchemaConstraints constraints) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.requiredDeclared = requiredDeclared;
        this.list = list;
        this.constraints = constraints == null ? SchemaConstraints.NONE : constraints;
    }

    /** Property name, null for the body itself. */
    public String getName() {
        return name;
    }

    /** Schema type (object, array, string, integer, number, boolean), null if unknown. */
    public String getType() {
        return type;
    }

    /** Listed in the {@code required} array of the enclosing object. */
    public boolean isRequired() {
        return required;
    }

    /** The enclosing object declares a {@code required} list (true for the body itself and for parameters). */
    public boolean isRequiredDeclared() {
        return requiredDeclared;
    }

    /** True for an array: the children are the properties of its items. */
    public boolean isList() {
        return list;
    }

    /** True if the field is an object with properties (not a list, not a single value). */
    public boolean isObject() {
        return !list && "object".equals(type);
    }

    public SchemaConstraints getConstraints() {
        return constraints;
    }

    /** Shortcut for {@code getConstraints().isReadOnly()}: the field is never sent in a request. */
    public boolean isReadOnly() {
        return constraints.isReadOnly();
    }

    /** Shortcut for {@code getConstraints().isWriteOnly()}: the field is never returned in a response. */
    public boolean isWriteOnly() {
        return constraints.isWriteOnly();
    }

    public List<SchemaField> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void addChild(SchemaField child) {
        children.add(child);
    }

    @Override
    public String toString() {
        return (name == null ? "<body>" : name) + (list ? "[]" : "") + (children.isEmpty() ? "" : " " + children);
    }
}
