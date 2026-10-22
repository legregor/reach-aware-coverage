package com.github.legregor.microservice.coverage.analysis.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MicroserviceSystem {
    private List<Microservice> microservices;
    private List<RestConnection> restConnections;
    
    // Enhanced fields from recursive static analysis - maintains backwards compatibility
    private CommunicationPathResult communicationPathResult;
    
    /**
     * Constructs MicroserviceSystem with legacy fields only.
     * Used for backwards compatibility with existing code.
     */
    public MicroserviceSystem(List<Microservice> microservices, List<RestConnection> restConnections) {
        this.microservices = microservices;
        this.restConnections = restConnections;
        this.communicationPathResult = null;
    }
}
