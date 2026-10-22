package com.github.legregor.microservice.coverage.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EndpointTrace {
    private String sourceService;
    private String sourceRoot;
    private String endpointMethodKey;
    private String httpMethod;
    private String path;
    private String returnType;
    private List<String> endpointArguments = new ArrayList<>();
    private Set<String> reachableMethodKeys = new LinkedHashSet<>();
    private Set<TraceTarget> downstreamTargets = new LinkedHashSet<>();
}