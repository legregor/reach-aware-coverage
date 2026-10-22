package com.github.legregor.microservice.coverage.evaluation.responseField;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * What the recorded responses of a connection contained, per status code: the field paths that had a non-null value
 * in at least one response, and how many responses carried a captured body.
 */
public class ExecutedResponseFields {
    private final Map<Integer, Set<String>> paths = new LinkedHashMap<>();
    private final Map<Integer, Integer> responseCount = new LinkedHashMap<>();
    private final Map<Integer, Integer> bodyCount = new LinkedHashMap<>();

    void addResponse(int statusCode, boolean withBody, Set<String> fieldPaths) {
        responseCount.merge(statusCode, 1, Integer::sum);
        if (withBody) {
            bodyCount.merge(statusCode, 1, Integer::sum);
        }
        paths.computeIfAbsent(statusCode, code -> new LinkedHashSet<>()).addAll(fieldPaths);
    }

    /** Field paths with a value in at least one response with this status code. */
    public Set<String> getPaths(int statusCode) {
        return Collections.unmodifiableSet(paths.getOrDefault(statusCode, Set.of()));
    }

    /** True if the field had a value in at least one response with this status code. */
    public boolean contains(int statusCode, String path) {
        return paths.getOrDefault(statusCode, Set.of()).contains(path);
    }

    /** Number of recorded responses with this status code. */
    public int getResponseCount(int statusCode) {
        return responseCount.getOrDefault(statusCode, 0);
    }

    /** Number of recorded responses with this status code whose body was captured. */
    public int getBodyCount(int statusCode) {
        return bodyCount.getOrDefault(statusCode, 0);
    }

    /** All status codes that were returned, in order of appearance. */
    public Set<Integer> getStatusCodes() {
        return Collections.unmodifiableSet(responseCount.keySet());
    }
}
