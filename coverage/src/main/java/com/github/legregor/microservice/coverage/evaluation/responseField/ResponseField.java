package com.github.legregor.microservice.coverage.evaluation.responseField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.legregor.microservice.coverage.openapi.SchemaField;

/**
 * A response field of one response schema: its path ({@code message}, {@code address.zip},
 * {@code fieldErrors[].field}, {@code []} for a top-level list), whether the schema requires it, its schema (for
 * the value partitions) and its children.
 */
public class ResponseField {
    private final String name;
    private final boolean required;
    private final SchemaField schemaField;
    private final List<ResponseField> children = new ArrayList<>();

    public ResponseField(String name, boolean required) {
        this(name, required, null);
    }

    /** @param schemaField the field's schema in the response schema (null if unknown) */
    public ResponseField(String name, boolean required, SchemaField schemaField) {
        this.name = name;
        this.required = required;
        this.schemaField = schemaField;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    /** The field's schema (constraints for the value partitions), null if unknown. */
    @JsonIgnore
    public SchemaField getSchemaField() {
        return schemaField;
    }

    public List<ResponseField> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void addChild(ResponseField child) {
        children.add(child);
    }

    /** This field and all fields below it, depth first. */
    public List<ResponseField> flatten() {
        List<ResponseField> result = new ArrayList<>();
        result.add(this);
        for (ResponseField child : children) {
            result.addAll(child.flatten());
        }
        return result;
    }

    @Override
    public String toString() {
        return name + (required ? " (required)" : "");
    }
}
