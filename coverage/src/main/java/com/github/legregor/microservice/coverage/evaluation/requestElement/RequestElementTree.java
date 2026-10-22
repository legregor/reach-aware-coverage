package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.github.legregor.microservice.coverage.openapi.SchemaField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The request elements of one connection as a tree. The top-level elements (path parameters and top-level body
 * properties) are the children of the virtual root r_p; nested body fields are their children.
 * <p>
 * Possible elements come from the OpenAPI spec of the called endpoint; reachability comes from the static analysis
 * of the caller. Without a body spec, the properties of the caller's DTO are used ({@link #isBodySpecMissing()}).
 */
public class RequestElementTree {

    private final List<RequestElement> topLevelElements;
    private final boolean bodySpecMissing;
    private final List<String> onlyInSpec;
    private final List<String> onlyInCaller;

    private final SchemaField bodySchema;

    public RequestElementTree(List<RequestElement> topLevelElements, boolean bodySpecMissing,
                              List<String> onlyInSpec, List<String> onlyInCaller) {
        this(topLevelElements, bodySpecMissing, onlyInSpec, onlyInCaller, null);
    }

    /** @param bodySchema the request body schema from the spec (root field), null if there is none */
    public RequestElementTree(List<RequestElement> topLevelElements, boolean bodySpecMissing,
                              List<String> onlyInSpec, List<String> onlyInCaller, SchemaField bodySchema) {
        this.topLevelElements = List.copyOf(topLevelElements);
        this.bodySpecMissing = bodySpecMissing;
        this.onlyInSpec = List.copyOf(onlyInSpec);
        this.onlyInCaller = List.copyOf(onlyInCaller);
        this.bodySchema = bodySchema;
    }

    /** The request body schema of the called endpoint (root field without a name), null if the spec has none. */
    public SchemaField getBodySchema() {
        return bodySchema;
    }

    /** Children of the virtual root r_p. */
    public List<RequestElement> getTopLevelElements() {
        return topLevelElements;
    }

    /**
     * True if the caller sends a request body but no body schema was found in the OpenAPI spec of the called
     * endpoint: the body elements are then the top-level properties of the caller's DTO.
     */
    public boolean isBodySpecMissing() {
        return bodySpecMissing;
    }

    /**
     * Interface mismatch: top-level body properties in the spec that the caller's DTO does not have (they can never
     * be sent over this connection and count as not reachable).
     */
    public List<String> getOnlyInSpec() {
        return onlyInSpec;
    }

    /** Interface mismatch: properties the caller sends that the spec of the called endpoint does not know. */
    public List<String> getOnlyInCaller() {
        return onlyInCaller;
    }

    /** True if the connection has no request elements at all (coverage is n/a). */
    public boolean isEmpty() {
        return topLevelElements.isEmpty();
    }

    /** All elements, depth-first (parents before their children). */
    public List<RequestElement> allElements() {
        List<RequestElement> result = new ArrayList<>();
        topLevelElements.forEach(element -> collect(element, result, ignored -> true));
        return Collections.unmodifiableList(result);
    }

    /** All reachable elements, depth-first. */
    public List<RequestElement> reachableElements() {
        List<RequestElement> result = new ArrayList<>();
        topLevelElements.forEach(element -> collect(element, result, RequestElement::isReachable));
        return Collections.unmodifiableList(result);
    }

    /** All elements that are not reachable, depth-first. */
    public List<RequestElement> notReachableElements() {
        List<RequestElement> result = new ArrayList<>();
        topLevelElements.forEach(element -> collect(element, result, e -> !e.isReachable()));
        return Collections.unmodifiableList(result);
    }

    public Optional<RequestElement> find(String name) {
        return allElements().stream().filter(element -> element.getName().equals(name)).findFirst();
    }

    private static void collect(RequestElement element, List<RequestElement> result, Predicate<RequestElement> filter) {
        if (filter.test(element)) {
            result.add(element);
        }
        element.getChildren().forEach(child -> collect(child, result, filter));
    }

    @Override
    public String toString() {
        return "RequestElementTree" + topLevelElements + (bodySpecMissing ? " (no body spec)" : "");
    }
}
