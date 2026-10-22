package com.github.legregor.microservice.coverage.analysis.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Names of request elements (the coverage items of request element coverage).
 * <p>
 * Path parameters and top-level body properties are siblings under the virtual root of a request:
 * {@code path:orderId}, {@code body:email}. A body that is not an object (List, String, number, ...) is the single
 * element {@code body}. Nested body fields will later use dotted names such as {@code body:address.zip}.
 */
public final class RequestElements {

    public static final String PATH_PREFIX = "path:";
    public static final String BODY_PREFIX = "body:";
    /** A request body that is one value (not an object with properties). */
    public static final String WHOLE_BODY = "body";

    /** {name} or {name:regex} in a Spring path template. */
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^/{}:]+)(?::[^}]*)?}");

    private RequestElements() {
    }

    public static String path(String variableName) {
        return PATH_PREFIX + variableName;
    }

    public static String body(String propertyName) {
        return BODY_PREFIX + propertyName;
    }

    /** Path variable names of an endpoint path template, e.g. /orders/{orderId} -> orderId. */
    public static Set<String> pathVariables(String pathTemplate) {
        Set<String> names = new LinkedHashSet<>();
        if (pathTemplate == null) {
            return names;
        }
        Matcher matcher = PATH_VARIABLE.matcher(pathTemplate);
        while (matcher.find()) {
            names.add(matcher.group(1).trim());
        }
        return names;
    }
}
