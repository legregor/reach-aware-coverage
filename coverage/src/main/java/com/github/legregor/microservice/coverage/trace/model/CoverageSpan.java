package com.github.legregor.microservice.coverage.trace.model;

import lombok.*;

import com.fasterxml.jackson.annotation.*;

/**
 * Abstract base class for all types of coverage spans.
 * Contains common properties needed by all span types.
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CoverageInternalSpan.class, name = "INTERNAL"),
        @JsonSubTypes.Type(value = CoverageServerSpan.class, name = "SERVER"),
        @JsonSubTypes.Type(value = CoverageClientSpan.class, name = "CLIENT")
})
@ToString
@Getter
public abstract class CoverageSpan {
    // Common identifiers for all span types
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String name;
    private final String service;

    @JsonCreator
    public CoverageSpan(
            @JsonProperty("traceId") String traceId,
            @JsonProperty("spanId") String spanId,
            @JsonProperty("parentSpanId") String parentSpanId,
            @JsonProperty("name") String name,
            @JsonProperty("service") String service){

        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.name = name;
        this.service = service;
    }

    public abstract SpanKind getKind();
}
