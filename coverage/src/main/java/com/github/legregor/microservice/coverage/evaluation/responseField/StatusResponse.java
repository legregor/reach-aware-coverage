package com.github.legregor.microservice.coverage.evaluation.responseField;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.legregor.microservice.coverage.openapi.SchemaField;

/**
 * The documented response of one relevant status code of a connection: the key it was found under in the spec
 * ({@code "409"}, {@code "4XX"}, {@code "default"}; null if the code is not documented) and its top-level fields.
 */
public class StatusResponse {
    private final int statusCode;
    private final String specKey;
    private final List<ResponseField> topLevelFields;
    private final SchemaField bodySchema;

    public StatusResponse(int statusCode, String specKey, List<ResponseField> topLevelFields) {
        this(statusCode, specKey, topLevelFields, null);
    }

    /** @param bodySchema the documented body schema (root field), null if undocumented or without body */
    public StatusResponse(int statusCode, String specKey, List<ResponseField> topLevelFields, SchemaField bodySchema) {
        this.statusCode = statusCode;
        this.specKey = specKey;
        this.topLevelFields = topLevelFields == null ? List.of() : List.copyOf(topLevelFields);
        this.bodySchema = bodySchema;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getSpecKey() {
        return specKey;
    }

    /** True if the spec documents a response for this status code (exact, range or default). */
    public boolean isDocumented() {
        return specKey != null;
    }

    public List<ResponseField> getTopLevelFields() {
        return topLevelFields;
    }

    /** The documented body schema (root field), null if the status code is undocumented or has no body. */
    @JsonIgnore
    public SchemaField getBodySchema() {
        return bodySchema;
    }

    /** All fields of the response, depth first. */
    public List<ResponseField> allFields() {
        List<ResponseField> result = new ArrayList<>();
        topLevelFields.forEach(field -> result.addAll(field.flatten()));
        return result;
    }
}
