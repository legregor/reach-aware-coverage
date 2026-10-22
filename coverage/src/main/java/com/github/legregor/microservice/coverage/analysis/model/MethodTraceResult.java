package com.github.legregor.microservice.coverage.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MethodTraceResult {
    private String methodKey;
    private Set<String> calledMethodKeys = new LinkedHashSet<>();
    private Set<TraceTarget> downstreamTargets = new LinkedHashSet<>();
}