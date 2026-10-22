package com.github.legregor.microservice.coverage.evaluation.requestElement;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The request elements of one connection that were assigned a value during test execution, collected over all
 * recorded calls of the connection (set union).
 */
public class ExecutedRequestElements {

    private final Set<String> names;
    private final int callCount;
    private final int callsWithServerSpan;
    private final int callsWithBody;

    /** All calls are assumed to have a server span. */
    public ExecutedRequestElements(Set<String> names, int callCount, int callsWithBody) {
        this(names, callCount, callCount, callsWithBody);
    }

    public ExecutedRequestElements(Set<String> names, int callCount, int callsWithServerSpan, int callsWithBody) {
        this.names = Collections.unmodifiableSet(new LinkedHashSet<>(names));
        this.callCount = callCount;
        this.callsWithServerSpan = callsWithServerSpan;
        this.callsWithBody = callsWithBody;
    }

    /** Names of the executed elements, e.g. {@code path:orderId}, {@code body:trip.tripId}, {@code body:items[].name}. */
    public Set<String> getNames() {
        return names;
    }

    public boolean contains(String elementName) {
        return names.contains(elementName);
    }

    /** Number of recorded calls of the connection. */
    public int getCallCount() {
        return callCount;
    }

    /**
     * Number of calls with a matching server span of the called service. 0 means the called service is not traced
     * (e.g. it is a mock), so no request body can be recorded.
     */
    public int getCallsWithServerSpan() {
        return callsWithServerSpan;
    }

    /**
     * Number of calls whose request body was captured in the traces. If a connection sends a body but no body was
     * captured, the body elements cannot be judged from the traces.
     */
    public int getCallsWithBody() {
        return callsWithBody;
    }
}
