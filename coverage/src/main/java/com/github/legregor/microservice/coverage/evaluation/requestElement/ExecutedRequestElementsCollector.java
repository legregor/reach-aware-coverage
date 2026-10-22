package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.analysis.model.RequestElements;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects the executed request elements of a connection from its recorded calls.
 * <ul>
 * <li>Path parameters: executed as soon as the connection was called (a matched call always fills every path
 * variable).</li>
 * <li>Body: the captured request body (JSON) of each call. An element is executed if it is present with a non-null
 * value ({@code null} counts as "no value"). An empty list or object is a value itself, its children are not
 * executed. List items: {@code body:items[].name} is executed if at least one item has a non-null name.</li>
 * </ul>
 * Element names follow {@link RequestElements}, so they can be compared with the {@link RequestElementTree}.
 */
public class ExecutedRequestElementsCollector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @param endpointPath  path template of the called endpoint (for the path parameters)
     * @param callCount     number of recorded calls of the connection
     * @param requestBodies captured request bodies of these calls (missing bodies are simply not in the list)
     */
    public ExecutedRequestElements collect(String endpointPath, int callCount, List<String> requestBodies) {
        return collect(endpointPath, callCount, callCount, requestBodies);
    }

    /**
     * @param endpointPath        path template of the called endpoint (for the path parameters)
     * @param callCount           number of recorded calls of the connection (client spans)
     * @param callsWithServerSpan number of these calls with a matching server span of the called service
     * @param requestBodies       captured request bodies of these calls (missing bodies are simply not in the list)
     */
    public ExecutedRequestElements collect(String endpointPath, int callCount, int callsWithServerSpan,
                                           List<String> requestBodies) {
        Set<String> executed = new LinkedHashSet<>();
        if (callCount > 0) {
            RequestElements.pathVariables(endpointPath).forEach(variable -> executed.add(RequestElements.path(variable)));
        }
        int callsWithBody = 0;
        if (requestBodies != null) {
            for (String body : requestBodies) {
                if (body == null || body.isBlank()) {
                    continue;
                }
                callsWithBody++;
                executed.addAll(fromBody(body));
            }
        }
        return new ExecutedRequestElements(executed, callCount, callsWithServerSpan, callsWithBody);
    }

    /** Executed body elements of one request body. */
    Set<String> fromBody(String body) {
        Set<String> executed = new LinkedHashSet<>();
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            // not JSON (plain text, form data): the body as a whole has a value
            executed.add(RequestElements.WHOLE_BODY);
            return executed;
        }
        if (root == null || root.isNull() || root.isMissingNode()) {
            return executed;
        }
        if (root.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> fields = root.fields(); fields.hasNext(); ) {
                Map.Entry<String, JsonNode> field = fields.next();
                addValue(RequestElements.body(field.getKey()), field.getValue(), executed);
            }
        } else {
            addValue(RequestElements.WHOLE_BODY, root, executed);
        }
        return executed;
    }

    private void addValue(String name, JsonNode value, Set<String> executed) {
        if (value == null || value.isNull()) {
            return;
        }
        executed.add(name);
        if (value.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> fields = value.fields(); fields.hasNext(); ) {
                Map.Entry<String, JsonNode> field = fields.next();
                addValue(name + "." + field.getKey(), field.getValue(), executed);
            }
        } else if (value.isArray()) {
            for (JsonNode item : value) {
                if (item != null && item.isObject()) {
                    for (Iterator<Map.Entry<String, JsonNode>> fields = item.fields(); fields.hasNext(); ) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        addValue(name + "[]." + field.getKey(), field.getValue(), executed);
                    }
                }
            }
        }
    }
}
