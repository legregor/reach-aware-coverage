package com.github.legregor.microservice.coverage.trace.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

/**
 * Represents an internal span in the coverage system.
 * Internal spans are created for operations that occur within a service,
 * without external communication.
 */
@ToString(callSuper = true)
public class CoverageInternalSpan extends CoverageSpan {
    @JsonCreator
    public CoverageInternalSpan(
            @JsonProperty("traceId") String traceId,
            @JsonProperty("spanId") String spanId,
            @JsonProperty("parentSpanId") String parentSpanId,
            @JsonProperty("name") String name,
            @JsonProperty("service") String service) {
        super(traceId, spanId, parentSpanId, name, service);
    }

    @Override
    public SpanKind getKind() {
        return SpanKind.INTERNAL;
    }
}
