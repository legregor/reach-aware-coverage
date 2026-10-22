package com.github.legregor.microservice.coverage.analysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single communication path in a microservice system.
 * 
 * A path is a chain of REST calls from an origin service through one or more
 * intermediate services. Each step in the path records:
 * - The service making the call
 * - The endpoint being invoked
 * - The downstream target service being called
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
// Derived getters are written to JSON but must be ignored when reading it back
@JsonIgnoreProperties(value = {"originService", "finalDestinationService"}, allowGetters = true)
public class CommunicationPath {
    
    /**
     * List of path segments, ordered from origin to final destination.
     * Each segment represents a single REST call in the communication chain.
     */
    private List<PathSegment> segments = new LinkedList<>();
    
    /**
     * Unique identifier for this path (computed hash of service chain).
     * Used for deduplication of identical service sequences.
     */
    private String pathHash;
    
    /**
     * Metadata about the path discovery.
     */
    private Map<String, String> metadata = new LinkedHashMap<>();
    
    /**
     * Represents a single segment in a communication path.
     * A segment is one REST call from a source service to a destination service.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class PathSegment {
        private String sourceService;
        private String sourceRoot;
        private String sourceEndpointMethodKey;
        private String sourceHttpMethod;
        private String sourcePath;
        private String sourceReturnType;
        private String destinationService;
        private String destinationHttpMethod;
        private String destinationUrl;
        private String destinationReturnType;
        // The endpoint of the destination service that receives the call (matched by the recursive analyzer)
        private String destinationEndpointMethodKey;
        private String destinationEndpointHttpMethod;
        private String destinationEndpointPath;
        private String destinationEndpointReturnType;
        private List<String> destinationEndpointArguments = new LinkedList<>();
        private Set<String> sentPathVariables;
        private Set<String> sentQueryParameters;
        private boolean requestBodySent;
        private boolean headersSent;
        private List<String> staticallyMissingArguments = new LinkedList<>();
        private List<String> staticallyOptionalButUnsentArguments = new LinkedList<>();
        // Request body reachability of this call (top-level properties, JSON names), see TraceTarget
        private BodyKind bodyKind = BodyKind.NONE;
        private String bodyType;
        private Set<String> bodyProperties = new LinkedHashSet<>();
        private Set<String> reachableBodyProperties = new LinkedHashSet<>();
        private Set<String> notReachableBodyProperties = new LinkedHashSet<>();
        private Set<String> bodyOrigins = new LinkedHashSet<>();
        private Map<String, List<String>> bodyConstantValues = new LinkedHashMap<>();
        private Set<String> neverNullBodyProperties = new LinkedHashSet<>();
    }
    
    /**
     * Computes a hash representing the service chain in this path.
     * Used for identifying duplicate paths (same sequence of services).
     * 
     * @return A hash string based on the service sequence
     */
    public String computeServiceChainHash() {
        if (segments.isEmpty()) {
            return "";
        }
        
        StringBuilder chainBuilder = new StringBuilder();
        chainBuilder.append(segments.get(0).getSourceService())
                .append('|').append(nullToEmpty(segments.get(0).getSourcePath()));
        
        for (PathSegment segment : segments) {
            chainBuilder.append("->")
                    .append(segment.getDestinationService())
                    .append('|').append(nullToEmpty(segment.getDestinationHttpMethod()))
                    .append('|').append(nullToEmpty(segment.getDestinationUrl()));
        }
        
        return chainBuilder.toString();
    }
    
    /**
     * Gets the origin service (first in the chain).
     * 
     * @return The origin service name, or empty string if path is empty
     */
    public String getOriginService() {
        if (segments.isEmpty()) {
            return "";
        }
        return segments.get(0).getSourceService();
    }
    
    /**
     * Gets the final destination service (last in the chain).
     * 
     * @return The destination service name, or empty string if path is empty
     */
    public String getFinalDestinationService() {
        if (segments.isEmpty()) {
            return "";
        }
        return segments.get(segments.size() - 1).getDestinationService();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
