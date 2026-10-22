package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.legregor.microservice.coverage.analysis.cache.ServiceAnalysisCache;
import com.github.legregor.microservice.coverage.analysis.model.*;
import com.github.legregor.microservice.coverage.analysis.util.ServiceLocator;
import com.github.legregor.microservice.coverage.config.model.EntryEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recursive static analyzer for microservice communication paths.
 * 
 * This analyzer performs recursive analysis of microservices starting from a root service.
 * For each discovered service, it:
 * 1. Analyzes endpoints and downstream targets
 * 2. Discovers destination services from downstream targets
 * 3. Recursively analyzes discovered services
 * 4. Maintains cycle detection and duplicate path removal
 * 
 * The result is a hierarchical map of all possible REST communication paths from the root.
 */
@Slf4j
public class RecursiveStaticServiceTraceAnalyzer {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    private final StaticServiceTraceAnalyzer baseAnalyzer;
    private final ServiceLocator serviceLocator;
    private final ServiceAnalysisCache serviceAnalysisCache;
    private boolean forceReanalysis = false; // Set to true to ignore cache and re-analyze services
    
    /**
     * Constructs the analyzer with auto-initialized dependencies.
     */
    public RecursiveStaticServiceTraceAnalyzer() {
        this(new StaticServiceTraceAnalyzer(), new ServiceLocator(), new ServiceAnalysisCache());
    }

    /**
     * Constructs the analyzer with provided dependencies (useful for testing).
     */
    public RecursiveStaticServiceTraceAnalyzer(StaticServiceTraceAnalyzer baseAnalyzer,
                                                ServiceLocator serviceLocator) {
        this(baseAnalyzer, serviceLocator, new ServiceAnalysisCache());
    }

    public RecursiveStaticServiceTraceAnalyzer(StaticServiceTraceAnalyzer baseAnalyzer,
                                               ServiceLocator serviceLocator,
                                               ServiceAnalysisCache serviceAnalysisCache) {
        this.baseAnalyzer = Objects.requireNonNull(baseAnalyzer, "baseAnalyzer");
        this.serviceLocator = Objects.requireNonNull(serviceLocator, "serviceLocator");
        this.serviceAnalysisCache = Objects.requireNonNull(serviceAnalysisCache, "serviceAnalysisCache");
    }



    public void analyzeFromRootService(Path rootServicePath) throws IOException {
        String rootServiceName = extractServiceName(rootServicePath);
        Set<Microservice> analyzedServices = new HashSet<>();
        Set<String> visitedServices = new HashSet<>();
        visitedServices.add(rootServiceName);

        analyzeService(rootServicePath, rootServiceName, visitedServices);


    }

    /**
     * Analyzes a single service.
     */
    public void analyzeService(Path servicePath, String serviceName, Set<String> visitedServices) throws IOException {
        // Avoid re-analyzing already visited services
        if (visitedServices.contains(serviceName)) {
            log.debug("Service {} already analyzed, skipping", serviceName);
            return;
        }
        /*
        // Analyze the current service
         */
        visitedServices.add(serviceName);
    }
    /**
     * Performs recursive analysis starting from a root service.
     * 
     * @param rootServicePath Path to the root microservice
     * @return Result containing all discovered communication paths
     * @throws IOException if analysis fails
     */
    public CommunicationPathResult analyzeRecursively(Path rootServicePath) throws IOException {
        return analyzeRecursively(rootServicePath, List.of());
    }

    /**
     * Performs recursive analysis starting from selected endpoints of a root service.
     *
     * @param rootServicePath Path to the root microservice
     * @param entryEndpoints  the endpoints of the root service the tests call; empty: every endpoint of the root
     *                        service is an entry point. Entries that match no endpoint are listed in
     *                        {@link CommunicationPathResult#getUnmatchedEntryEndpoints()}.
     */
    public CommunicationPathResult analyzeRecursively(Path rootServicePath, List<EntryEndpoint> entryEndpoints)
            throws IOException {
        String rootServiceName = extractServiceName(rootServicePath);
        log.info("Starting recursive analysis from root service: {}", rootServiceName);
        
        CommunicationPathResult result = new CommunicationPathResult();
        result.setRootService(rootServiceName);
        
        // Map to store all analyzed services and their trace results
        Map<String, StaticServiceTraceResult> serviceTraceMap = new HashMap<>();
        
        // Track visited services to prevent re-analyzing (cycle detection)
        Set<String> visitedServices = new HashSet<>();
        
        // Track paths being constructed to detect cycles in path chains
        Deque<String> currentPath = new ArrayDeque<>();
        
        // Recursively analyze services
        analyzeServiceRecursively(rootServicePath, rootServiceName, serviceTraceMap,
                visitedServices, currentPath, result);

        // The entry points: the selected endpoints of the root service, or all of them
        StaticServiceTraceResult rootTrace = serviceTraceMap.get(rootServiceName);
        List<EndpointTrace> rootEndpoints = rootTrace == null ? List.of() : rootTrace.getEndpointTraces();
        List<EndpointTrace> entryPoints = rootEndpoints;
        if (entryEndpoints != null && !entryEndpoints.isEmpty()) {
            entryPoints = new ArrayList<>();
            for (EntryEndpoint entry : entryEndpoints) {
                List<EndpointTrace> matched = selectEntryEndpoints(rootEndpoints, entry);
                if (matched.isEmpty()) {
                    result.getUnmatchedEntryEndpoints().add(entry.label());
                }
                for (EndpointTrace endpoint : matched) {
                    if (!entryPoints.contains(endpoint)) {
                        entryPoints.add(endpoint);
                        result.getEntryEndpoints().add(endpoint.getHttpMethod() + " " + endpoint.getPath());
                    }
                }
            }
            log.info("Entry endpoints of {}: {}", rootServiceName, result.getEntryEndpoints());
        }

        // Build communication paths from the trace map
        List<CommunicationPath> allPaths = buildCommunicationPaths(serviceTraceMap, rootServiceName, entryPoints);
        
        // Remove duplicate paths (same service sequence)
        List<CommunicationPath> deduplicatedPaths = deduplicatePaths(allPaths);
        
        result.setPaths(deduplicatedPaths);
        result.setAnalyzedServices(new LinkedHashSet<>(serviceTraceMap.keySet()));
        
        // Backwards compatibility: expose the root service's StaticServiceTraceResult
        result.setStaticTraceResult(serviceTraceMap.get(rootServiceName));
        // Also expose full per-service trace results for all analyzed services
        result.setStaticTraceResults(serviceTraceMap);

        log.info("Recursive analysis complete. Analyzed {} services, found {} paths",
                serviceTraceMap.size(), deduplicatedPaths.size());
        
        return result;
    }
    
    /**
     * Recursively analyzes a service and all its dependencies.
     */
    private void analyzeServiceRecursively(Path servicePath, String serviceName,
                                          Map<String, StaticServiceTraceResult> serviceTraceMap,
                                          Set<String> visitedServices,
                                          Deque<String> currentPath,
                                          CommunicationPathResult result) throws IOException {
        
        // Avoid re-analyzing already visited services
        if (visitedServices.contains(serviceName)) {
            log.debug("Service {} already analyzed, skipping", serviceName);
            return;
        }
        
        // Detect cycles in path chains
        if (currentPath.contains(serviceName)) {
            log.debug("Cycle detected in path: {}", currentPath);
            return;
        }
        
        visitedServices.add(serviceName);
        currentPath.push(serviceName);
        
        log.debug("Analyzing service: {}", serviceName);
        
        try {
            // Analyze the current service
            StaticServiceTraceResult traceResult = null;
            if(!forceReanalysis) {
                try {
                    traceResult = serviceAnalysisCache.load(servicePath).orElse(null);
                } catch (IOException e) {
                    log.warn("Failed to load cached analysis for {}", serviceName, e);
                }
            }

            if (traceResult == null || forceReanalysis) {
                traceResult = baseAnalyzer.analyze(List.of(servicePath));
                try {
                    serviceAnalysisCache.store(servicePath, traceResult);
                } catch (IOException e) {
                    log.warn("Failed to store cached analysis for {}", serviceName, e);
                }
            }
            serviceTraceMap.put(serviceName, traceResult);
            
            // Extract downstream targets and recurse only through matching entrypoints.
            for (TraceTarget target : extractDownstreamTargets(traceResult)) {
                String destService = target.getDestinationService();
                if (destService == null || destService.isEmpty()) {
                    continue;
                }

                log.debug("Processing destination service: {}", destService);

                if (serviceLocator.serviceExists(destService)) {
                    Path destServicePath = serviceLocator.locateService(destService).get();
                    analyzeServiceRecursively(destServicePath, destService, serviceTraceMap,
                            visitedServices, currentPath, result);
                } else {
                    log.warn("Destination service '{}' not found in MICROSERVICES_ROOT", destService);
                    result.getUnresolvedServices().add(destService);
                }
            }
            
        } finally {
            currentPath.pop();
        }
    }
    
    /**
     * Extracts unique destination service names from a trace result.
     */
    private Set<TraceTarget> extractDownstreamTargets(StaticServiceTraceResult traceResult) {
        Set<TraceTarget> targets = new LinkedHashSet<>();
        
        for (EndpointTrace endpoint : traceResult.getEndpointTraces()) {
            for (TraceTarget target : endpoint.getDownstreamTargets()) {
                if (target.getDestinationService() != null && !target.getDestinationService().isEmpty()) {
                    targets.add(target);
                }
            }
        }
        
        return targets;
    }
    
    /**
     * Builds hierarchical communication paths from service trace map.
     * Only includes paths reachable from the root service.
     */
    private List<CommunicationPath> buildCommunicationPaths(
            Map<String, StaticServiceTraceResult> serviceTraceMap,
            String rootServiceName, List<EndpointTrace> entryPoints) {
        
        List<CommunicationPath> allPaths = new ArrayList<>();
        
        // Only process the entry endpoints of the root service and their reachable descendants
        buildPathsFromService(rootServiceName, serviceTraceMap, new HashSet<>(), allPaths, null, entryPoints);
        
        return allPaths;
    }

    /**
     * The endpoints of the root service that a configured entry endpoint denotes (same HTTP method, if given).
     * <ul>
     * <li>A path template ({@code /cancel/refound/{orderId}}) denotes the endpoint with the same template only. It is
     * not used as a concrete path: {@code /cancel/{orderId}/{loginId}} would otherwise match it, with "refound" as
     * order id.</li>
     * <li>A concrete path ({@code /cancel/refound/42}) denotes the endpoint Spring routes it to: of the matching
     * templates only the most specific one per HTTP method (ties: all of them).</li>
     * </ul>
     */
    private List<EndpointTrace> selectEntryEndpoints(List<EndpointTrace> rootEndpoints, EntryEndpoint entry) {
        if (entry == null || entry.getPath() == null || entry.getPath().isBlank()) {
            return List.of();
        }
        String entryPath = normalizePath(entry.getPath());
        List<EndpointTrace> sameMethod = rootEndpoints.stream()
                .filter(endpoint -> endpoint.getPath() != null)
                .filter(endpoint -> entry.getMethod() == null || entry.getMethod().isBlank()
                        || (endpoint.getHttpMethod() != null
                        && endpoint.getHttpMethod().equalsIgnoreCase(entry.getMethod())))
                .toList();
        if (entryPath.contains("{")) {
            String entryTemplate = normalizeTemplatedPath(entryPath);
            return sameMethod.stream()
                    .filter(endpoint -> normalizeTemplatedPath(endpoint.getPath()).equals(entryTemplate))
                    .toList();
        }
        List<EndpointTrace> matching = sameMethod.stream()
                .filter(endpoint -> pathMatcher.match(normalizeTemplatedPath(endpoint.getPath()), entryPath))
                .toList();
        List<EndpointTrace> selected = new ArrayList<>();
        matching.stream()
                .map(EndpointTrace::getHttpMethod)
                .distinct()
                .forEach(method -> selected.addAll(mostSpecific(matching.stream()
                        .filter(endpoint -> java.util.Objects.equals(endpoint.getHttpMethod(), method))
                        .toList(), entryPath)));
        return selected;
    }

    /**
     * Of the endpoints whose templates match a path, the ones Spring would route the path to: the most specific
     * templates by {@link AntPathMatcher#getPatternComparator(String)} (identical template first, then fewer path
     * variables, ...); all of them if several are equally specific.
     */
    private List<EndpointTrace> mostSpecific(List<EndpointTrace> matchingEndpoints, String path) {
        if (matchingEndpoints.size() <= 1) {
            return matchingEndpoints;
        }
        java.util.Comparator<String> byPattern = pathMatcher.getPatternComparator(path);
        java.util.Comparator<EndpointTrace> bySpecificity = (a, b) -> byPattern.compare(
                normalizeTemplatedPath(a.getPath()), normalizeTemplatedPath(b.getPath()));
        EndpointTrace best = matchingEndpoints.stream().min(bySpecificity).orElseThrow();
        return matchingEndpoints.stream()
                .filter(endpoint -> bySpecificity.compare(endpoint, best) == 0)
                .toList();
    }

    /**
     * Recursively builds paths starting from a specific service.
     * Only includes paths reachable from the current service.
     */
    private void buildPathsFromService(String serviceName,
                                      Map<String, StaticServiceTraceResult> serviceTraceMap,
                                      Set<String> visitedInCurrentPath,
                                      List<CommunicationPath> allPaths) {
        buildPathsFromService(serviceName, serviceTraceMap, visitedInCurrentPath, allPaths, null, null);
    }

    /**
     * @param inboundTarget the call into this service (null for the root service)
     * @param entryPoints   for the root service: the endpoints to start from (null: all endpoints)
     */
    private void buildPathsFromService(String serviceName,
                                      Map<String, StaticServiceTraceResult> serviceTraceMap,
                                      Set<String> visitedInCurrentPath,
                                      List<CommunicationPath> allPaths,
                                      TraceTarget inboundTarget,
                                      List<EndpointTrace> entryPoints) {
        
        StaticServiceTraceResult traceResult = serviceTraceMap.get(serviceName);
        if (traceResult == null || traceResult.getEndpointTraces().isEmpty()) {
            return;
        }
        
        // Track visited services in current path to detect cycles
        Set<String> currentPath = new HashSet<>(visitedInCurrentPath);
        if (!currentPath.add(serviceName)) {
            return; // Cycle detected
        }
        
        List<EndpointTrace> matchingEndpoints = inboundTarget == null && entryPoints != null
                ? entryPoints : findMatchingEndpoints(traceResult, inboundTarget);
        for (EndpointTrace endpoint : matchingEndpoints) {
            // For each downstream target of that endpoint
            for (TraceTarget target : endpoint.getDownstreamTargets()) {
                String destService = target.getDestinationService();
                
                if (destService == null || destService.isEmpty()) {
                    continue;
                }
                
                // Create segment from current service to destination
                CommunicationPath.PathSegment segment = new CommunicationPath.PathSegment();
                segment.setSourceService(serviceName);
                segment.setSourceRoot(endpoint.getSourceRoot());
                segment.setSourceEndpointMethodKey(endpoint.getEndpointMethodKey());
                segment.setSourceHttpMethod(endpoint.getHttpMethod());
                segment.setSourcePath(endpoint.getPath());
                segment.setSourceReturnType(endpoint.getReturnType());
                segment.setDestinationService(destService);
                segment.setDestinationHttpMethod(target.getHttpMethod());
                segment.setDestinationUrl(target.getUrl());
                segment.setDestinationReturnType(target.getReturnType());
                segment.setSentPathVariables(new LinkedHashSet<>(target.getSentPathVariables()));
                segment.setSentQueryParameters(new LinkedHashSet<>(target.getSentQueryParameters()));
                segment.setRequestBodySent(target.isRequestBodySent());
                segment.setHeadersSent(target.isHeadersSent());
                segment.setBodyKind(target.getBodyKind());
                segment.setBodyType(target.getBodyType());
                segment.setBodyProperties(new LinkedHashSet<>(target.getBodyProperties()));
                segment.setReachableBodyProperties(new LinkedHashSet<>(target.getReachableBodyProperties()));
                segment.setNotReachableBodyProperties(new LinkedHashSet<>(target.getNotReachableBodyProperties()));
                segment.setBodyOrigins(new LinkedHashSet<>(target.getBodyOrigins()));
                segment.setBodyConstantValues(new LinkedHashMap<>(target.getBodyConstantValues()));
                segment.setNeverNullBodyProperties(new LinkedHashSet<>(target.getNeverNullBodyProperties()));

                // Endpoint of the destination service that receives this call
                EndpointTrace destinationEndpoint = findFirstMatchingEndpoint(serviceTraceMap.get(destService), target);
                if (destinationEndpoint != null) {
                    segment.setDestinationEndpointMethodKey(destinationEndpoint.getEndpointMethodKey());
                    segment.setDestinationEndpointHttpMethod(destinationEndpoint.getHttpMethod());
                    segment.setDestinationEndpointPath(destinationEndpoint.getPath());
                    segment.setDestinationEndpointReturnType(destinationEndpoint.getReturnType());
                } else {
                    // Destination not analyzed (e.g. unresolved service): best effort from the call URL
                    segment.setDestinationEndpointHttpMethod(target.getHttpMethod());
                    segment.setDestinationEndpointPath(extractPathFromUrl(target.getUrl()));
                }

                EndpointArgumentAnalysis argumentAnalysis = analyzeEndpointArguments(target,
                        serviceTraceMap.get(destService));
                segment.setDestinationEndpointArguments(argumentAnalysis.endpointArguments);
                segment.setStaticallyMissingArguments(argumentAnalysis.staticallyMissingArguments);
                segment.setStaticallyOptionalButUnsentArguments(argumentAnalysis.staticallyOptionalButUnsentArguments);
                
                // Create single-segment path
                CommunicationPath path = new CommunicationPath();
                path.setSegments(List.of(segment));
                path.setPathHash(path.computeServiceChainHash());
                allPaths.add(path);
                
                // If destination service is also analyzed, build extended paths
                if (serviceTraceMap.containsKey(destService) && !currentPath.contains(destService)) {
                    // Get all paths from destination service
                    List<CommunicationPath> destPaths = new ArrayList<>();
                    buildPathsFromService(destService, serviceTraceMap, currentPath, destPaths, target, null);
                    
                    // Extend each destination path by prepending current segment
                    for (CommunicationPath destPath : destPaths) {
                        CommunicationPath extendedPath = new CommunicationPath();
                        List<CommunicationPath.PathSegment> extendedSegments = new ArrayList<>();
                        extendedSegments.add(segment);
                        extendedSegments.addAll(destPath.getSegments());
                        
                        extendedPath.setSegments(extendedSegments);
                        extendedPath.setPathHash(extendedPath.computeServiceChainHash());
                        allPaths.add(extendedPath);
                    }
                }
            }
        }
    }

    private List<EndpointTrace> findMatchingEndpoints(StaticServiceTraceResult traceResult, TraceTarget inboundTarget) {
        if (inboundTarget == null) {
            return traceResult.getEndpointTraces();
        }

        String inboundPath = extractPathFromUrl(inboundTarget.getUrl());
        String inboundMethod = inboundTarget.getHttpMethod();
        if (inboundPath == null || inboundPath.isBlank()) {
            return List.of();
        }

        // Only the endpoint Spring routes the call to: the most specific matching template (e.g. a call to
        // /cancel/refound/{id} reaches /cancel/refound/{orderId}, not also /cancel/{orderId}/{loginId}, and a call to
        // /trains/welcome reaches /trains/welcome, not also /trains/{id}); several only if equally specific.
        String normalizedInboundPath = normalizeTemplatedPath(inboundPath);
        List<EndpointTrace> matching = traceResult.getEndpointTraces().stream()
                .filter(endpoint -> isMatchingEndpoint(endpoint, inboundPath, inboundMethod))
                .toList();
        return mostSpecific(matching, normalizedInboundPath);
    }

    private EndpointTrace findFirstMatchingEndpoint(StaticServiceTraceResult destinationTraceResult, TraceTarget target) {
        if (destinationTraceResult == null || target == null) {
            return null;
        }
        List<EndpointTrace> matchingEndpoints = findMatchingEndpoints(destinationTraceResult, target);
        return matchingEndpoints.isEmpty() ? null : matchingEndpoints.get(0);
    }

    private boolean isMatchingEndpoint(EndpointTrace endpoint, String inboundPath, String inboundMethod) {
        if (endpoint.getPath() == null || endpoint.getHttpMethod() == null || inboundMethod == null) {
            return false;
        }

        if (!endpoint.getHttpMethod().equalsIgnoreCase(inboundMethod)) {
            return false;
        }

        String endpointPath = normalizeTemplatedPath(endpoint.getPath());
        String normalizedInboundPath = normalizeTemplatedPath(inboundPath);
        // Only the endpoint's template is used as pattern. The other direction (the call's template as pattern)
        // wrongly matched a call to /contacts/{contactsId} with the literal endpoint /contacts/welcome.
        return endpointPath.equals(normalizedInboundPath)
                || pathMatcher.match(endpointPath, normalizedInboundPath);
    }

    private String extractPathFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String raw = url.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            String withoutScheme = raw.replaceFirst("^https?://[^/]+", "");
            return normalizePath(withoutScheme.split("\\?")[0].split("#")[0]);
        }

        return normalizePath(raw.split("\\?")[0].split("#")[0]);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeTemplatedPath(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("\\{[^/]+\\}", "{var}");
    }

    private EndpointArgumentAnalysis analyzeEndpointArguments(TraceTarget target,
            StaticServiceTraceResult destinationTraceResult) {
        if (target == null || destinationTraceResult == null) {
            return EndpointArgumentAnalysis.empty();
        }
        EndpointTrace endpoint = findFirstMatchingEndpoint(destinationTraceResult, target);
        if (endpoint == null) {
            return EndpointArgumentAnalysis.empty();
        }
        List<String> endpointArguments = endpoint.getEndpointArguments() == null
                ? List.of()
                : new ArrayList<>(endpoint.getEndpointArguments());

        List<String> missingRequired = new ArrayList<>();
        List<String> unsentOptional = new ArrayList<>();
        for (String argument : endpointArguments) {
            EndpointArgument endpointArgument = parseEndpointArgument(argument);
            if (endpointArgument == null) {
                continue;
            }
            Optional<Boolean> sent = isArgumentSent(endpointArgument, target);
            if (sent.isEmpty()) {
                continue;
            }
            if (sent.get()) {
                continue;
            }
            if (endpointArgument.required) {
                missingRequired.add(argument);
            } else {
                unsentOptional.add(argument);
            }
        }
        return new EndpointArgumentAnalysis(endpointArguments, missingRequired, unsentOptional);
    }

    private Optional<Boolean> isArgumentSent(EndpointArgument argument, TraceTarget target) {
        if (target == null) {
            return Optional.empty();
        }
        return switch (argument.type) {
            // The call was matched to this endpoint by its path template, so every path variable position is
            // filled. Names are not compared: the caller's template uses its own variable names
            // (e.g. /users/id/{accountId} for the endpoint /users/id/{userId}).
            case PATH -> Optional.of(true);
            case QUERY -> Optional.of(target.getSentQueryParameters() != null
                    && target.getSentQueryParameters().contains(argument.name));
            case BODY -> Optional.of(target.isRequestBodySent());
            case HEADER -> Optional.of(target.isHeadersSent());
            case OTHER -> Optional.empty();
        };
    }

    private EndpointArgument parseEndpointArgument(String argument) {
        if (argument == null || argument.isBlank()) {
            return null;
        }
        String trimmed = argument.trim();
        if (trimmed.contains("@PathVariable")) {
            return new EndpointArgument(ArgumentType.PATH, resolveAnnotationName(trimmed, "PathVariable"), true);
        }
        if (trimmed.contains("@RequestParam")) {
            return new EndpointArgument(ArgumentType.QUERY, resolveAnnotationName(trimmed, "RequestParam"),
                    !trimmed.contains("required = false") && !trimmed.contains("required=false"));
        }
        if (trimmed.contains("@RequestBody")) {
            return new EndpointArgument(ArgumentType.BODY, "body",
                    !trimmed.contains("required = false") && !trimmed.contains("required=false"));
        }
        if (trimmed.contains("@RequestHeader")) {
            return new EndpointArgument(ArgumentType.HEADER, resolveAnnotationName(trimmed, "RequestHeader"),
                    !trimmed.contains("required = false") && !trimmed.contains("required=false"));
        }
        return new EndpointArgument(ArgumentType.OTHER, "", false);
    }

    private String resolveAnnotationName(String argument, String annotationName) {
        Pattern explicitName = Pattern.compile("@" + annotationName + "\\s*\\(([^)]*)\\)");
        Matcher matcher = explicitName.matcher(argument);
        if (matcher.find()) {
            String annotationArgs = matcher.group(1);
            Matcher namedMatcher = Pattern.compile("(value|name)\\s*=\\s*\"([^\"]+)\"").matcher(annotationArgs);
            if (namedMatcher.find()) {
                return namedMatcher.group(2);
            }
            Matcher positionalMatcher = Pattern.compile("^\\s*\"([^\"]+)\"\\s*$").matcher(annotationArgs);
            if (positionalMatcher.find()) {
                return positionalMatcher.group(1);
            }
        }
        String[] tokens = argument.split("\\s+");
        return tokens.length == 0 ? "" : tokens[tokens.length - 1];
    }
    
    /**
     * Removes duplicate paths that have the same service chain sequence.
     */
    private List<CommunicationPath> deduplicatePaths(List<CommunicationPath> paths) {
        Map<String, CommunicationPath> uniquePathsByHash = new HashMap<>();
        
        for (CommunicationPath path : paths) {
            String hash = path.computeServiceChainHash();
            if (!uniquePathsByHash.containsKey(hash)) {
                uniquePathsByHash.put(hash, path);
                log.debug("Added unique path: {}", hash);
            } else {
                log.debug("Duplicate path detected (same service chain): {}", hash);
            }
        }
        
        return new ArrayList<>(uniquePathsByHash.values());
    }
    
    /**
     * Extracts service name from a service path.
     * Assumes service path is the directory name.
     */
    private String extractServiceName(Path servicePath) {
        return servicePath.getFileName().toString();
    }

    public boolean isForceReanalysis() {
        return forceReanalysis;
    }

    public void setForceReanalysis(boolean forceReanalysis) {
        this.forceReanalysis = forceReanalysis;
    }

    private enum ArgumentType {
        PATH, QUERY, HEADER, BODY, OTHER
    }

    private record EndpointArgument(ArgumentType type, String name, boolean required) {
    }

    private record EndpointArgumentAnalysis(List<String> endpointArguments, List<String> staticallyMissingArguments,
                                            List<String> staticallyOptionalButUnsentArguments) {
        static EndpointArgumentAnalysis empty() {
            return new EndpointArgumentAnalysis(List.of(), List.of(), List.of());
        }
    }


}
