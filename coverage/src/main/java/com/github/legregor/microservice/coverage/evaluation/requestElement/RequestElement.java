package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.analysis.model.RequestElements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One request element of a connection (a coverage item of request element coverage).
 * <p>
 * Names follow {@link RequestElements}:
 * {@code path:orderId}, {@code body:email}, nested {@code body:trip.tripId}, items of a list
 * {@code body:items[].name}, a body that is one value {@code body}.
 */
public class RequestElement {

    /** Where the element is transmitted. */
    public enum Kind {
        PATH_PARAMETER,
        /** A property of the request body (top-level or nested). */
        BODY_PROPERTY,
        /** The request body as a whole, when it is not an object (List, String, number, ...). */
        BODY_VALUE
    }

    private final String name;
    private final Kind kind;
    private final String type;
    private final boolean required;
    private final boolean reachable;
    private final SchemaField schemaField;
    private final List<RequestElement> children = new ArrayList<>();
    private List<String> constants;
    private boolean neverNull;

    public RequestElement(String name, Kind kind, String type, boolean required, boolean reachable) {
        this(name, kind, type, required, reachable, null);
    }

    /**
     * @param schemaField the field of the spec this element comes from (type and constraints, used for value
     *                    partitions); null if the element does not come from a spec
     */
    public RequestElement(String name, Kind kind, String type, boolean required, boolean reachable,
                          SchemaField schemaField) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.required = required;
        this.reachable = reachable;
        this.schemaField = schemaField;
    }

    /** Full element name, e.g. {@code body:trip.tripId}. */
    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    /** Schema type (string, integer, object, array, ...), null if unknown. */
    public String getType() {
        return type;
    }

    /** Required according to the spec (path parameters are always required). */
    public boolean isRequired() {
        return required;
    }

    /** The caller can send this element with a value (static analysis); children inherit it from their parent. */
    public boolean isReachable() {
        return reachable;
    }

    /** The spec field behind this element (type, constraints); null if the element does not come from a spec. */
    @JsonIgnore
    public SchemaField getSchemaField() {
        return schemaField;
    }

    /**
     * The only values the caller ever assigns to this element (JSON text, static analysis), or null if it can send
     * any value. Only known for top-level body properties.
     */
    public List<String> getConstants() {
        return constants;
    }

    /** The caller can never send this element without a value (a primitive field of its DTO). */
    public boolean isNeverNull() {
        return neverNull;
    }

    void setStaticValues(List<String> constants, boolean neverNull) {
        this.constants = constants == null ? null : List.copyOf(constants);
        this.neverNull = neverNull;
    }

    /** Direct children (properties of an object, properties of list items). */
    public List<RequestElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    void addChild(RequestElement child) {
        children.add(child);
    }

    @Override
    public String toString() {
        return name + (reachable ? "" : " (not reachable)") + (children.isEmpty() ? "" : " " + children);
    }
}
