package com.github.legregor.microservice.coverage.analysis.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Result of recursive static analysis of microservice communication paths.
 * 
 * Contains all discovered REST communication paths starting from a root/origin service,
 * including paths through intermediary services.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
// Derived getters are written to JSON but must be ignored when reading it back
@JsonIgnoreProperties(value = {"allServiceNames"}, allowGetters = true)
public class CommunicationPathResult {
    
    /**
     * The root/origin service from which analysis started.
     */
    private String rootService;
    
    /**
     * All discovered communication paths from the root service.
     */
    private List<CommunicationPath> paths = new ArrayList<>();
    
    /**
     * Set of all services that were analyzed (including origin).
     */
    private Set<String> analyzedServices = new LinkedHashSet<>();
    
    /**
     * Services that were discovered but not analyzed (e.g., missing from filesystem).
     */
    private Set<String> unresolvedServices = new LinkedHashSet<>();

    /**
     * The endpoints of the root service the analysis started from ("METHOD path"); empty if it started from every
     * endpoint of the root service.
     */
    private List<String> entryEndpoints = new ArrayList<>();

    /** Configured entry endpoints that match no endpoint of the root service ("METHOD path"). */
    private List<String> unmatchedEntryEndpoints = new ArrayList<>();
    
    /**
     * The underlying static service trace result for backward compatibility.
     *
     * @deprecated This field is deprecated and will be removed.
     */
    @Deprecated
    private StaticServiceTraceResult staticTraceResult;

    /**
     * Per-service static trace results for all analyzed services. Populated by the recursive analyzer.
     */
    private Map<String, StaticServiceTraceResult> staticTraceResults = new HashMap<>();
    
    /**
     * Gets all unique services involved in the communication paths.
     * 
     * @return Set of all service names in the paths
     */
    public Set<String> getAllServiceNames() {
        Set<String> services = new LinkedHashSet<>();
        for (CommunicationPath path : paths) {
            services.add(path.getOriginService());
            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                services.add(segment.getDestinationService());
            }
        }
        return services;
    }
    
    /**
     * Gets all paths that start from a specific service.
     * 
     * @param serviceName The service name to filter by
     * @return List of paths originating from the given service
     */
    public List<CommunicationPath> getPathsFrom(String serviceName) {
        List<CommunicationPath> result = new ArrayList<>();
        for (CommunicationPath path : paths) {
            if (serviceName.equals(path.getOriginService())) {
                result.add(path);
            }
        }
        return result;
    }
    
    /**
     * Gets all paths that reach a specific service as destination.
     * 
     * @param serviceName The service name to filter by
     * @return List of paths that lead to the given service
     */
    public List<CommunicationPath> getPathsTo(String serviceName) {
        List<CommunicationPath> result = new ArrayList<>();
        for (CommunicationPath path : paths) {
            if (serviceName.equals(path.getFinalDestinationService())) {
                result.add(path);
            }
        }
        return result;
    }
    
    /**
     * Gets paths of a specific length (number of segments).
     * 
     * @param segmentCount Number of segments in the path
     * @return List of paths with exactly that many segments
     */
    public List<CommunicationPath> getPathsByLength(int segmentCount) {
        List<CommunicationPath> result = new ArrayList<>();
        for (CommunicationPath path : paths) {
            if (path.getSegments().size() == segmentCount) {
                result.add(path);
            }
        }
        return result;
    }
}
