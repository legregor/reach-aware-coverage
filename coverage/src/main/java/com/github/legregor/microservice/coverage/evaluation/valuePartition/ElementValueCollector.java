package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.legregor.microservice.coverage.openapi.SchemaField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the values of every field of a schema from a JSON body (direction-neutral: request and response
 * bodies). Field paths: {@code ""} for the body itself, {@code x}, nested {@code x.y}, items of a list
 * {@code x[].y}, items of a body that is a list {@code [].y}.
 * <p>
 * An absent field is recorded as {@link MissingNode}, an explicit null as {@code NullNode}; both mean "no value".
 * Children are only visited when their parent has a value (an object, or a list whose items are objects): a field
 * of an absent object is not "absent" itself, it simply was not exercised.
 */
public class ElementValueCollector {

    /**
     * @param root the body schema (root field without a name)
     * @param body the parsed body
     * @return values per field path, in schema order; each list has one entry per occurrence (several for list
     * items)
     */
    public Map<String, List<JsonNode>> collect(SchemaField root, JsonNode body) {
        Map<String, List<JsonNode>> values = new LinkedHashMap<>();
        JsonNode value = body == null ? MissingNode.getInstance() : body;
        add(values, "", value);
        visitChildren(root, "", value, values);
        return values;
    }

    /** Merges the values of several bodies (e.g. all calls of a connection). */
    public static void addAll(Map<String, List<JsonNode>> target, Map<String, List<JsonNode>> source) {
        source.forEach((path, list) -> target.computeIfAbsent(path, ignored -> new ArrayList<>()).addAll(list));
    }

    private void visitChildren(SchemaField field, String path, JsonNode value, Map<String, List<JsonNode>> values) {
        if (field.getChildren().isEmpty()) {
            return;
        }
        if (field.isList()) {
            if (!value.isArray()) {
                return;
            }
            String itemPath = path + "[]";
            for (JsonNode item : value) {
                if (item.isObject()) {
                    visitProperties(field, itemPath, item, values);
                }
            }
        } else if (value.isObject()) {
            visitProperties(field, path, value, values);
        }
    }

    private void visitProperties(SchemaField field, String path, JsonNode object, Map<String, List<JsonNode>> values) {
        for (SchemaField child : field.getChildren()) {
            String childPath = path.isEmpty() ? child.getName() : path + "." + child.getName();
            JsonNode childValue = object.has(child.getName()) ? object.get(child.getName()) : MissingNode.getInstance();
            add(values, childPath, childValue);
            visitChildren(child, childPath, childValue, values);
        }
    }

    private static void add(Map<String, List<JsonNode>> values, String path, JsonNode value) {
        values.computeIfAbsent(path, ignored -> new ArrayList<>()).add(value);
    }
}
