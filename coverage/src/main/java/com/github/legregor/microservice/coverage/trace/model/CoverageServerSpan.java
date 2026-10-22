package com.github.legregor.microservice.coverage.trace.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import lombok.Setter;

/**
 * Represents a server span in the coverage system.
 * Server spans are created when a service receives and processes a request.
 */
@ToString(callSuper = true)
@Getter
@Setter
public class CoverageServerSpan extends CoverageSpan {
    private final String httpMethod;
    private final String route;
    private final int statusCode;
    private String requestBody;
    private String responseBody;

    /**
     * Constructor for CoverageServerSpan.
     *
     * @param traceId      The unique identifier for the trace
     * @param spanId       The unique identifier for this span
     * @param parentSpanId The identifier of the parent span (null if root span)
     * @param name         The name of the span
     * @param service      The service this span belongs to
     * @param httpMethod   The HTTP method used (GET, POST, etc.)
     * @param route        The route pattern used
     * @param statusCode   The HTTP status code returned
     * @param requestBody  The HTTP request body (may be null)
     * @param responseBody The HTTP response body (may be null)
     */
    public CoverageServerSpan(
            @JsonProperty("traceId") String traceId,
            @JsonProperty("spanId") String spanId,
            @JsonProperty("parentSpanId") String parentSpanId,
            @JsonProperty("name") String name,
            @JsonProperty("service") String service,
            @JsonProperty("httpMethod") String httpMethod,
            @JsonProperty("route") String route,
            @JsonProperty("statusCode") int statusCode,
            @JsonProperty("requestBody") String requestBody,
            @JsonProperty("responseBody") String responseBody) {

        super(traceId, spanId, parentSpanId, name, service);

        this.httpMethod = httpMethod;
        this.route = route;
        this.statusCode = statusCode;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
    }

    /**
     * Constructor for CoverageServerSpan without request and response bodies.
     * 
     * @param traceId      The unique identifier for the trace
     * @param spanId       The unique identifier for this span
     * @param parentSpanId The identifier of the parent span (null if root span)
     * @param name         The name of the span
     * @param service      The service this span belongs to
     * @param httpMethod   The HTTP method used (GET, POST, etc.)
     * @param route        The route pattern used
     * @param statusCode   The HTTP status code returned
     */
    public CoverageServerSpan(
            String traceId,
            String spanId,
            String parentSpanId,
            String name,
            String service,
            String httpMethod,
            String route,
            int statusCode) {
        this(traceId, spanId, parentSpanId, name, service, httpMethod, route, statusCode, null, null);
    }

    @Override
    public SpanKind getKind() {
        return SpanKind.SERVER;
    }
}
