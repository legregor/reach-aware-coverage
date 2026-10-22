package com.github.legregor.microservice.coverage.evaluation.responseField;

/** Why the response field coverage of a called connection cannot be computed (n/a). */
public enum ResponseFieldNotApplicableReason {

    /** The called service produced no server spans (e.g. it is a mock), so no response is recorded. */
    CALLEE_NOT_TRACED("called service not traced (no server span, e.g. a mock): responses not recorded",
            "callee not traced (mock)"),

    /** Every returned status code is unexpected for the connection (e.g. only 5xx). */
    NO_EXPECTED_STATUS_RETURNED("no expected status code was returned", "no expected status code returned"),

    /** The spec documents none of the returned status codes (or there is no spec for the callee). */
    NO_RESPONSE_SCHEMA("the spec documents no response for the returned status codes", "no response schema"),

    /** The documented responses have no fields (no body, or a body without properties). */
    NO_RESPONSE_FIELDS("the documented responses have no fields", "no response fields"),

    /** Responses with fields were returned, but no response body was captured in the server spans. */
    BODY_NOT_CAPTURED("response body not captured in the server spans", "body not captured");

    private final String description;
    private final String label;

    ResponseFieldNotApplicableReason(String description, String label) {
        this.description = description;
        this.label = label;
    }

    /** Short label for counts in summaries. */
    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
