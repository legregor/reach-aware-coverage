package com.github.legregor.microservice.coverage.analysis;

import com.github.legregor.microservice.coverage.analysis.model.CommunicationPath;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility for building service dependency graphs from communication path results.
 * 
 * Supports multiple graph representations:
 * 1. Adjacency list (for algorithms and traversal)
 * 2. Graphviz DOT format (for visualization)
 * 3. Matrix representation (for analysis)
 */
public class ServiceGraphBuilder {
    
    private final CommunicationPathResult pathResult;
    
    public ServiceGraphBuilder(CommunicationPathResult pathResult) {
        this.pathResult = pathResult;
    }
    
    /**
     * Builds an adjacency list representation of the service graph.
     * Each service maps to its direct downstream dependencies.
     * 
     * @return Map where key=service, value=set of destination services
     */
    public Map<String, Set<String>> buildAdjacencyList() {
        Map<String, Set<String>> graph = new HashMap<>();
        
        // Initialize all analyzed services
        for (String service : pathResult.getAnalyzedServices()) {
            graph.putIfAbsent(service, new HashSet<>());
        }
        
        // Add edges for all paths
        for (CommunicationPath path : pathResult.getPaths()) {
            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                String source = segment.getSourceService();
                String destination = segment.getDestinationService();
                
                graph.putIfAbsent(source, new HashSet<>());
                graph.get(source).add(destination);
            }
        }
        
        return graph;
    }
    
    /**
     * Generates Graphviz DOT format for visualization.
     * Can be visualized with: graphviz, online tools, or Mermaid
     * 
     * @return DOT format string
     */
    public String generateDotFormat() {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph ServiceDependencies {\n");
        dot.append("    rankdir=LR;\n");
        dot.append("    node [shape=box, style=rounded];\n");
        dot.append("    bgcolor=white;\n\n");
        
        // Highlight root service
        String rootService = pathResult.getRootService();
        dot.append("    \"").append(rootService).append("\" [style=\"rounded,filled\", fillcolor=lightblue];\n\n");
        
        // Add all edges from paths
        Set<String> addedEdges = new HashSet<>();
        for (CommunicationPath path : pathResult.getPaths()) {
            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                String source = segment.getSourceService();
                String dest = segment.getDestinationService();
                String edgeKey = source + "->" + dest;
                
                if (!addedEdges.contains(edgeKey)) {
                    dot.append("    \"").append(source).append("\" -> \"")
                       .append(dest).append("\";\n");
                    addedEdges.add(edgeKey);
                }
            }
        }
        
        dot.append("}\n");
        return dot.toString();
    }
    
    /**
     * Generates Mermaid graph format (GitHub-compatible markdown).
     * Can be embedded directly in markdown files.
     * 
     * @return Mermaid format string
     */
    public String generateMermaidFormat() {
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("graph LR\n");
        
        String rootService = pathResult.getRootService();
        
        // Add style for root service
        Set<String> addedEdges = new HashSet<>();
        for (CommunicationPath path : pathResult.getPaths()) {
            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                String source = segment.getSourceService();
                String dest = segment.getDestinationService();
                String edgeKey = source + " --> " + dest;
                
                if (!addedEdges.contains(edgeKey)) {
                    mermaid.append("    ").append(source).append(" --> ").append(dest).append("\n");
                    addedEdges.add(edgeKey);
                }
            }
        }
        
        // Add styling for root
        mermaid.append("    style ").append(rootService).append(" fill:#lightblue\n");
        
        return mermaid.toString();
    }
    
    /**
     * Generates an adjacency matrix for the service graph.
     * Useful for algorithmic analysis (connectivity, transitive closure, etc.)
     * 
     * @return Matrix where matrix[i][j] > 0 means service i calls service j
     */
    public int[][] generateAdjacencyMatrix() {
        Set<String> services = pathResult.getAnalyzedServices();
        int size = services.size();
        
        // Create index mapping
        Map<String, Integer> serviceIndex = new HashMap<>();
        int idx = 0;
        for (String service : services) {
            serviceIndex.put(service, idx++);
        }
        
        // Initialize matrix
        int[][] matrix = new int[size][size];
        
        // Fill matrix with edge counts
        for (CommunicationPath path : pathResult.getPaths()) {
            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                String source = segment.getSourceService();
                String dest = segment.getDestinationService();
                
                int i = serviceIndex.getOrDefault(source, -1);
                int j = serviceIndex.getOrDefault(dest, -1);
                
                if (i >= 0 && j >= 0) {
                    matrix[i][j]++;
                }
            }
        }
        
        return matrix;
    }
    
    /**
     * Gets all services at a specific depth from root service.
     * Depth 0 = root service only
     * Depth 1 = services directly called by root
     * Depth n = services n hops away
     * 
     * @param depth The distance from root service
     * @return Set of services at that depth
     */
    public Set<String> getServicesAtDepth(int depth) {
        Set<String> servicesAtDepth = new HashSet<>();
        
        if (depth == 0) {
            servicesAtDepth.add(pathResult.getRootService());
            return servicesAtDepth;
        }
        
        for (CommunicationPath path : pathResult.getPaths()) {
            if (path.getSegments().size() == depth) {
                servicesAtDepth.add(path.getFinalDestinationService());
            }
        }
        
        return servicesAtDepth;
    }
    
    /**
     * Prints a tree-like representation starting from root service.
     * Shows the hierarchical structure of service calls.
     * 
     * @return Tree format string
     */
    public String generateTreeFormat() {
        StringBuilder tree = new StringBuilder();
        tree.append("Service Communication Tree\n");
        tree.append("===========================\n\n");
        tree.append(pathResult.getRootService()).append(" (ROOT)\n");
        
        Map<Integer, Set<String>> depthMap = new HashMap<>();
        for (CommunicationPath path : pathResult.getPaths()) {
            int depth = path.getSegments().size();
            depthMap.putIfAbsent(depth, new HashSet<>());
            depthMap.get(depth).add(path.getFinalDestinationService());
        }
        
        for (int depth = 1; depth <= depthMap.keySet().stream().max(Integer::compare).orElse(0); depth++) {
            Set<String> services = depthMap.get(depth);
            if (services != null) {
                for (String service : services) {
                    tree.append("  ".repeat(depth)).append("└── ").append(service)
                        .append(" (depth ").append(depth).append(")\n");
                }
            }
        }
        
        return tree.toString();
    }
}
