package com.github.legregor.microservice.coverage.analysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
// Derived getters are written to JSON but must be ignored when reading it back
@JsonIgnoreProperties(value = {"allTraceTargets"}, allowGetters = true)
public class StaticServiceTraceResult {
    private List<EndpointTrace> endpointTraces = new ArrayList<>();
    private Map<String, MethodTraceResult> methodTraceCache = new LinkedHashMap<>();

    public Set<TraceTarget> getAllTraceTargets() {
        Set<TraceTarget> traceTargets = new LinkedHashSet<>();
        for (EndpointTrace endpointTrace : endpointTraces) {
            traceTargets.addAll(endpointTrace.getDownstreamTargets());
        }
        return traceTargets;
    }
}