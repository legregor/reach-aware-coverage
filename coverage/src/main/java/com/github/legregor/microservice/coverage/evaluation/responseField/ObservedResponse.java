package com.github.legregor.microservice.coverage.evaluation.responseField;

/**
 * A response of a called connection as recorded in the callee's server span: its status code and its body (null if
 * no body was captured).
 */
public record ObservedResponse(int statusCode, String body) {

    /** True if a body was captured (not null, not blank). */
    public boolean hasBody() {
        return body != null && !body.isBlank();
    }
}
