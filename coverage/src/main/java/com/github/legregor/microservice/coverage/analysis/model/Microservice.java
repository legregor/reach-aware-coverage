package com.github.legregor.microservice.coverage.analysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
// Derived getters are written to JSON but must be ignored when reading it back
@JsonIgnoreProperties(value = {"rootService", "communicationPathResultPresent"}, allowGetters = true)
public class Microservice {
    private String name;
    private List<RestCall> restCalls;
    private List<RestEndpoint> restEndpoints;
    private CommunicationPathResult communicationPathResult;

    public Microservice(String name, List<RestCall> restCalls, List<RestEndpoint> restEndpoints) {
        this.name = name;
        this.restCalls = restCalls;
        this.restEndpoints = restEndpoints;
        this.communicationPathResult = null;
    }

    // Helper methods that may be useful for coverage analysis
    public boolean isRootService() {
        return communicationPathResult != null && Objects.equals(communicationPathResult.getRootService(), name);
    }

    public boolean isCommunicationPathResultPresent() {
        return communicationPathResult != null;
    }
}
