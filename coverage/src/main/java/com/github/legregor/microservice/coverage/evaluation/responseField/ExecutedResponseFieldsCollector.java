package com.github.legregor.microservice.coverage.evaluation.responseField;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Collects, per status code, the response fields that had a value in the recorded responses. Paths follow the
 * response field names: {@code message}, {@code address.zip}, {@code fieldErrors}, {@code fieldErrors[].field},
 * and {@code []} / {@code [].x} for a top-level list. Absent and {@code null} fields have no value.
 */
public class ExecutedResponseFieldsCollector {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ExecutedResponseFields collect(List<ObservedResponse> responses) {
        ExecutedResponseFields executed = new ExecutedResponseFields();
        if (responses == null) {
            return executed;
        }
        for (ObservedResponse response : responses) {
            executed.addResponse(response.statusCode(), response.hasBody(),
                    response.hasBody() ? fromBody(response.body()) : Set.of());
        }
        return executed;
    }

    /** Field paths with a value in one body; empty if the body is not JSON. */
    static Set<String> fromBody(String body) {
        Set<String> paths = new LinkedHashSet<>();
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            return paths; // not JSON: no field values
        }
        if (root == null || root.isNull() || root.isMissingNode()) {
            return paths;
        }
        if (root.isObject()) {
            addFields("", root, paths);
        } else if (root.isArray()) {
            addValue(ResponseFieldTreeBuilder.TOP_LEVEL_LIST, root, paths);
        }
        return paths;
    }

    private static void addFields(String prefix, JsonNode object, Set<String> paths) {
        for (Iterator<Map.Entry<String, JsonNode>> fields = object.fields(); fields.hasNext(); ) {
            Map.Entry<String, JsonNode> field = fields.next();
            addValue(prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey(), field.getValue(), paths);
        }
    }

    private static void addValue(String path, JsonNode value, Set<String> paths) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return;
        }
        paths.add(path);
        if (value.isObject()) {
            addFields(path, value, paths);
        } else if (value.isArray()) {
            String itemPrefix = path.equals(ResponseFieldTreeBuilder.TOP_LEVEL_LIST) ? path : path + "[]";
            for (JsonNode item : value) {
                if (item != null && item.isObject()) {
                    addFields(itemPrefix, item, paths);
                }
            }
        }
    }
}
