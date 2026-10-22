package com.github.legregor.microservice.coverage.trace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a client span in the coverage system.
 * Client spans are created when a service initiates a request to another service.
 */
@ToString(callSuper = true)
@Getter
public class CoverageClientSpan extends CoverageSpan {
    private final String httpMethod;
    private final int statusCode;
    private final String url;

    /**
     * Constructor for CoverageClientSpan.
     *
     * @param traceId      The unique identifier for the trace
     * @param spanId       The unique identifier for this span
     * @param parentSpanId The identifier of the parent span (null if root span)
     * @param name         The name of the span
     * @param service      The service this span belongs to
     * @param httpMethod   The HTTP method used (GET, POST, etc.)
     * @param statusCode   The HTTP status code returned
     * @param url          The full URL of the request
     */
    public CoverageClientSpan(
            @JsonProperty("traceId") String traceId,
            @JsonProperty("spanId") String spanId,
            @JsonProperty("parentSpanId") String parentSpanId,
            @JsonProperty("name") String name,
            @JsonProperty("service") String service,
            @JsonProperty("httpMethod") String httpMethod,
            @JsonProperty("statusCode") int statusCode,
            @JsonProperty("url") String url) {

        super(traceId, spanId, parentSpanId, name, service);

        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.url = url;
    }

    @Override
    public SpanKind getKind() {
        return SpanKind.CLIENT;
    }
}
