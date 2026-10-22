package com.github.legregor.microservice.coverage.evaluation.responseValuePartition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldTreeBuilder;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ElementValueCollector;
import com.github.legregor.microservice.coverage.openapi.SchemaField;

/**
 * Collects the values returned for each response field of one status code, from the response bodies of that status
 * code, along its documented body schema. Paths follow the response field names ({@code message},
 * {@code fieldErrors[].field}, {@code []} / {@code [].x} for a top-level list). An absent field and an explicit
 * {@code null} are both recorded as "no value"; fields below an absent object or list are not visited.
 */
public class ExecutedResponseValuesCollector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ElementValueCollector valueCollector = new ElementValueCollector();

    /**
     * @param bodySchema the documented body schema of the status code (root field)
     * @param bodies     the captured response bodies with this status code; null, empty and non-JSON bodies are
     *                   skipped
     * @return values per response field path, one entry per occurrence
     */
    public Map<String, List<JsonNode>> collect(SchemaField bodySchema, List<String> bodies) {
        Map<String, List<JsonNode>> values = new LinkedHashMap<>();
        if (bodySchema == null || bodies == null) {
            return values;
        }
        for (String body : bodies) {
            JsonNode json = parse(body);
            if (json == null) {
                continue;
            }
            Map<String, List<JsonNode>> bodyValues = new LinkedHashMap<>();
            valueCollector.collect(bodySchema, json).forEach((path, list) -> {
                String fieldPath = fieldPath(path, bodySchema);
                if (fieldPath != null) {
                    bodyValues.put(fieldPath, list);
                }
            });
            ElementValueCollector.addAll(values, bodyValues);
        }
        return values;
    }

    /** Value path of {@link ElementValueCollector} -> response field name, null for the body object itself. */
    static String fieldPath(String valuePath, SchemaField bodySchema) {
        if (valuePath.isEmpty()) {
            return bodySchema.isList() ? ResponseFieldTreeBuilder.TOP_LEVEL_LIST : null;
        }
        return valuePath;
    }

    private static JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode json = MAPPER.readTree(body);
            return json == null || json.isMissingNode() ? null : json;
        } catch (Exception e) {
            return null; // not JSON: no values
        }
    }
}
