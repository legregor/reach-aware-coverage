package com.github.legregor.microservice.coverage.evaluation.requestElement;

/**
 * Why request elements of a called connection cannot be judged. Used for the whole connection
 * ({@link RequestElementCoverage#getNotApplicableReason()}) and for its body elements
 * ({@link RequestElementCoverage#getBodyNotRecordedReason()}).
 */
public enum NotApplicableReason {

    /** The endpoint has neither path parameters nor a request body. */
    NO_ELEMENTS("no request elements (no path parameters, no request body)", "no elements"),

    /** The endpoint has request elements, but the caller can never send any of them with a value. */
    NONE_REACHABLE("no reachable request elements", "none reachable"),

    /**
     * The called service produced no server spans (e.g. it is replaced by a mock in the tests), so the request body
     * is not in the traces.
     */
    CALLEE_NOT_TRACED("called service not traced (no server span, e.g. a mock): request body not recorded",
            "callee not traced (mock)"),

    /** Server spans exist, but they contain no request body (body capture disabled or not supported). */
    BODY_NOT_CAPTURED("request body not captured in the server spans", "body not captured"),

    /** Value partitions: no request element was sent with a value (all reachable elements are uncovered). */
    NO_EXECUTED_ELEMENTS("no request element was sent with a value", "nothing sent with a value"),

    /** Value partitions: the executed elements have no value partitions (e.g. no spec for the body). */
    NO_PARTITIONS("the executed request elements have no value partitions", "no partitions");

    private final String description;
    private final String label;

    NotApplicableReason(String description, String label) {
        this.description = description;
        this.label = label;
    }

    /** Short label for counts in summaries, e.g. "callee not traced (mock)". */
    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
