package com.github.legregor.microservice.coverage.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.analysis.model.Microservice;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Reads a previously written microservice-system.json file into the internal model.
 */
public class MicroserviceSystemReader {

    private final ObjectMapper mapper;

    public MicroserviceSystemReader() {
        this.mapper = new ObjectMapper();
    }

    public MicroserviceSystem read(Path filePath) throws IOException {
        MicroserviceSystem system = mapper.readValue(filePath.toFile(), MicroserviceSystem.class);
        normalize(system);
        return system;
    }

    private void normalize(MicroserviceSystem system) {
        if (system.getMicroservices() == null) {
            system.setMicroservices(new ArrayList<>());
        }
        if (system.getRestConnections() == null) {
            system.setRestConnections(new ArrayList<>());
        }

        for (Microservice microservice : system.getMicroservices()) {
            if (microservice.getRestCalls() == null) {
                microservice.setRestCalls(new ArrayList<>());
            }
            if (microservice.getRestEndpoints() == null) {
                microservice.setRestEndpoints(new ArrayList<>());
            }
            for (RestEndpoint endpoint : microservice.getRestEndpoints()) {
                if (endpoint.getArguments() == null) {
                    endpoint.setArguments(new ArrayList<>());
                }
            }
        }

        system.getRestConnections().forEach(connection -> {
            if (connection.getEndpoint() != null && connection.getEndpoint().getArguments() == null) {
                connection.getEndpoint().setArguments(new ArrayList<>());
            }
            // Older files have no "calls" list: the single call is the only call variant
            if (connection.getCalls() == null) {
                connection.setCalls(new ArrayList<>());
            }
            if (connection.getCalls().isEmpty() && connection.getCall() != null) {
                connection.getCalls().add(connection.getCall());
            }
            // Older files have no request element information
            if (connection.getRequestElements() == null) {
                connection.setRequestElements(new ArrayList<>());
            }
            if (connection.getReachableRequestElements() == null) {
                connection.setReachableRequestElements(new ArrayList<>());
            }
            if (connection.getNotReachableRequestElements() == null) {
                connection.setNotReachableRequestElements(new ArrayList<>());
            }
            if (connection.getRequestBodyOrigins() == null) {
                connection.setRequestBodyOrigins(new ArrayList<>());
            }
            if (connection.getRequestElementConstants() == null) {
                connection.setRequestElementConstants(new LinkedHashMap<>());
            }
            if (connection.getNeverNullRequestElements() == null) {
                connection.setNeverNullRequestElements(new ArrayList<>());
            }
            if (connection.getNeverSentArguments() == null) {
                connection.setNeverSentArguments(new ArrayList<>());
            }
            if (connection.getStaticallyMissingArguments() == null) {
                connection.setStaticallyMissingArguments(new ArrayList<>());
            }
        });
    }
}
