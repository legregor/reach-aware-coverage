package com.github.legregor.microservice.coverage.analysis;

import com.github.legregor.microservice.coverage.analysis.model.*;

import java.util.*;

/**
 * Converter for transforming CommunicationPathResult into internal microservice system models.
 * <p>
 * This class bridges the recursive static analysis output (CommunicationPathResult) with the
 * internal model representation (MicroserviceSystem).
 * <p>
 * Backwards compatibility is maintained by converting all communication paths into
 * RestConnection objects that can be used by existing coverage evaluation logic.
 * At some point, the system might be refactored to fully utilize the enhanced CommunicationPathResult structure.
 */
public class CommunicationPathConverter {

    /**
     * Converts a CommunicationPathResult (from recursive analyzer) into a MicroserviceSystem.
     * <p>
     * The conversion maintains backwards compatibility by:
     * 1. Deriving RestConnections from communication paths
     * 2. Building Microservice objects from the service trace results
     * 3. Enriching the MicroserviceSystem with additional path information
     * <p>
     * Note: The root service also contains the CommunicationPathResult for enhanced analysis
     * @param result The result from RecursiveStaticServiceTraceAnalyzer
     * @return MicroserviceSystem containing both legacy and enhanced data
     */
    public MicroserviceSystem convertToSystem(CommunicationPathResult result) {
        if (result == null) {
            return new MicroserviceSystem();
        }

        // Convert communication paths to RestConnections for backwards compatibility
        List<RestConnection> restConnections = convertPathsToRestConnections(result.getPaths());

        // Convert service traces to Microservice objects
        List<Microservice> microservices = buildMicroservicesFromPathResult(result);

        // Create MicroserviceSystem with both legacy and enhanced data
        MicroserviceSystem system = new MicroserviceSystem(microservices, restConnections);

        // Store the raw CommunicationPathResult for enhanced analysis
        system.setCommunicationPathResult(result);

        return system;
    }

    /**
     * Converts CommunicationPath objects to RestConnection objects.
     * <p>
     * Every path segment is exactly one hop (one call from a source service to an endpoint of a
     * destination service), so all segments of all paths are flattened and grouped per connection.
     * Following the definition k = (s_n, p), a connection is identified by the calling service and the
     * called endpoint (destination service + endpoint HTTP method + endpoint path).
     * If different call implementations in the source service reach the same endpoint, they are all
     * kept in {@link RestConnection#getCalls()}; {@link RestConnection#getCall()} is the first one.
     *
     * @param paths List of CommunicationPath objects from recursive analysis
     * @return List of RestConnection objects (insertion order preserved)
     */
    private List<RestConnection> convertPathsToRestConnections(List<CommunicationPath> paths) {
        if (paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, RestConnection> connectionsByKey = new LinkedHashMap<>();

        for (CommunicationPath path : paths) {
            if (path.getSegments() == null || path.getSegments().isEmpty()) {
                continue;
            }

            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                RestConnection connection = convertSegmentToRestConnection(segment);
                if (connection == null) {
                    continue;
                }

                String key = connectionKey(connection);
                RestConnection existing = connectionsByKey.get(key);
                if (existing == null) {
                    connectionsByKey.put(key, connection);
                    mergeRequestElements(connection, segment, true);
                } else {
                    if (connection.getCall() != null && !existing.getCalls().contains(connection.getCall())) {
                        existing.getCalls().add(connection.getCall());
                    }
                    mergeRequestElements(existing, segment, false);
                }
            }
        }

        return new ArrayList<>(connectionsByKey.values());
    }

    /**
     * Adds the request elements of one segment (one call site) to its connection.
     * <ul>
     * <li>Path parameters: the variables of the endpoint's path template; always reachable when the endpoint is
     * called.</li>
     * <li>Body: the top-level properties of the caller's DTO ({@code body:<name>}), reachable as found by the static
     * analysis; a non-object body is the single element {@code body}.</li>
     * </ul>
     * Several segments of one connection: elements and reachable elements are united, so an element is not
     * reachable only if no call site can send it. Arguments that are never sent are intersected for the same reason;
     * statically missing arguments are united (each one is a warning).
     */
    private void mergeRequestElements(RestConnection connection, CommunicationPath.PathSegment segment, boolean first) {
        Set<String> elements = new LinkedHashSet<>(connection.getRequestElements());
        Set<String> reachable = new LinkedHashSet<>(connection.getReachableRequestElements());

        String endpointPath = connection.getEndpoint() != null ? connection.getEndpoint().getPath() : null;
        for (String variable : RequestElements.pathVariables(endpointPath)) {
            elements.add(RequestElements.path(variable));
            reachable.add(RequestElements.path(variable));
        }

        BodyKind bodyKind = segment.getBodyKind() == null ? BodyKind.NONE : segment.getBodyKind();
        Set<String> reachableBefore = new LinkedHashSet<>(reachable);
        Set<String> elementsBefore = new LinkedHashSet<>(elements);
        if (bodyKind == BodyKind.OBJECT) {
            nullSafe(segment.getBodyProperties()).forEach(property -> elements.add(RequestElements.body(property)));
            nullSafe(segment.getReachableBodyProperties()).forEach(property -> {
                elements.add(RequestElements.body(property));
                reachable.add(RequestElements.body(property));
            });
        } else if (bodyKind == BodyKind.VALUE) {
            elements.add(RequestElements.WHOLE_BODY);
            reachable.add(RequestElements.WHOLE_BODY);
        }
        if (connection.getRequestBodyType() == null && bodyKind != BodyKind.NONE) {
            connection.setRequestBodyType(segment.getBodyType());
        }

        // constants and never-null elements of this segment, as element names
        Set<String> segmentReachable = new LinkedHashSet<>();
        Set<String> segmentElements = new LinkedHashSet<>();
        Map<String, List<String>> segmentConstants = new LinkedHashMap<>();
        Set<String> segmentNeverNull = new LinkedHashSet<>();
        if (bodyKind == BodyKind.OBJECT) {
            nullSafe(segment.getReachableBodyProperties()).forEach(p -> segmentReachable.add(RequestElements.body(p)));
            nullSafe(segment.getBodyProperties()).forEach(p -> segmentElements.add(RequestElements.body(p)));
            if (segment.getBodyConstantValues() != null) {
                segment.getBodyConstantValues().forEach((p, values) ->
                        segmentConstants.put(RequestElements.body(p), values));
            }
            nullSafe(segment.getNeverNullBodyProperties()).forEach(p -> segmentNeverNull.add(RequestElements.body(p)));
        }
        Map<String, List<String>> constantsBefore = first ? Map.of() : connection.getRequestElementConstants();
        connection.setRequestElementConstants(new LinkedHashMap<>(BodyConstants.merge(
                first ? List.of() : bodyElements(reachableBefore), constantsBefore, segmentReachable, segmentConstants)));
        connection.setNeverNullRequestElements(new ArrayList<>(BodyConstants.mergeNeverNull(
                first ? List.of() : bodyElements(elementsBefore), first ? List.of() : connection.getNeverNullRequestElements(),
                segmentElements, segmentNeverNull)));

        Set<String> notReachable = new LinkedHashSet<>(elements);
        notReachable.removeAll(reachable);
        connection.setRequestElements(new ArrayList<>(elements));
        connection.setReachableRequestElements(new ArrayList<>(reachable));
        connection.setNotReachableRequestElements(new ArrayList<>(notReachable));

        Set<String> origins = new LinkedHashSet<>(connection.getRequestBodyOrigins());
        origins.addAll(nullSafe(segment.getBodyOrigins()));
        connection.setRequestBodyOrigins(new ArrayList<>(origins));

        Set<String> neverSent = new LinkedHashSet<>(first
                ? nullSafe(segment.getStaticallyOptionalButUnsentArguments())
                : connection.getNeverSentArguments());
        if (!first) {
            neverSent.retainAll(nullSafe(segment.getStaticallyOptionalButUnsentArguments()));
        }
        connection.setNeverSentArguments(new ArrayList<>(neverSent));

        Set<String> missing = new LinkedHashSet<>(connection.getStaticallyMissingArguments());
        missing.addAll(nullSafe(segment.getStaticallyMissingArguments()));
        connection.setStaticallyMissingArguments(new ArrayList<>(missing));
    }

    /** Only the body elements (constants and never-null apply to body properties). */
    private static Set<String> bodyElements(Set<String> elements) {
        Set<String> result = new LinkedHashSet<>();
        elements.stream().filter(e -> e.startsWith(RequestElements.BODY_PREFIX)).forEach(result::add);
        return result;
    }

    private static <T> Collection<T> nullSafe(Collection<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * Key of a connection k = (s_n, p): source service, destination service and the called endpoint.
     * Falls back to the call if no endpoint information is available.
     */
    public static String connectionKey(RestConnection connection) {
        String method = null;
        String path = null;
        if (connection.getEndpoint() != null) {
            method = connection.getEndpoint().getMethod();
            path = connection.getEndpoint().getPath();
        }
        if ((method == null || path == null) && connection.getCall() != null) {
            method = method != null ? method : connection.getCall().getMethod();
            path = path != null ? path : connection.getCall().getUrl();
        }
        return connection.getSource() + "|" + connection.getDestination() + "|"
                + (method == null ? "" : method.toUpperCase(Locale.ROOT)) + " " + (path == null ? "" : path);
    }

    /**
     * Converts a single PathSegment to a RestConnection.
     * <p>
     * The mapping is:
     * - source = PathSegment.sourceService
     * - destination = PathSegment.destinationService
     * - call = the REST call implemented in the source service (destinationHttpMethod, destinationUrl,
     *   destinationReturnType = type the caller expects)
     * - endpoint = the endpoint of the destination service that receives the call
     *   (destinationEndpointHttpMethod, destinationEndpointPath, destinationEndpointArguments,
     *   destinationEndpointReturnType)
     * <p>
     * The source* fields of the segment describe the source service's own endpoint whose handler makes
     * the call; they are not part of the connection.
     *
     * @param segment The path segment to convert
     * @return RestConnection or null if segment is invalid
     */
    /** The key of the connection a path segment belongs to (as {@link #connectionKey(RestConnection)}), or null. */
    public static String connectionKey(CommunicationPath.PathSegment segment) {
        RestConnection connection = convertSegmentToRestConnection(segment);
        return connection == null ? null : connectionKey(connection);
    }

    private static RestConnection convertSegmentToRestConnection(CommunicationPath.PathSegment segment) {
        if (segment == null || segment.getSourceService() == null || segment.getDestinationService() == null) {
            return null;
        }

        RestCall call = new RestCall(
                segment.getDestinationHttpMethod(),
                segment.getDestinationUrl(),
                segment.getDestinationReturnType()
        );

        RestEndpoint endpoint = new RestEndpoint(
                segment.getDestinationEndpointHttpMethod(),
                segment.getDestinationEndpointPath(),
                segment.getDestinationEndpointArguments() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(segment.getDestinationEndpointArguments()),
                segment.getDestinationEndpointReturnType()
        );

        return new RestConnection(
                segment.getSourceService(),
                segment.getDestinationService(),
                call,
                endpoint
        );
    }

    /**
     * Builds Microservice objects from the CommunicationPathResult.
     * <p>
     * Microservices are constructed from the analyzed services and their endpoint traces.
     * For each service, we extract its endpoints and outgoing calls.
     *
     * @param result The CommunicationPathResult containing analyzed services
     * @return List of Microservice objects
     */
    private List<Microservice> buildMicroservicesFromPathResult(CommunicationPathResult result) {
        if (result.getAnalyzedServices() == null || result.getAnalyzedServices().isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, StaticServiceTraceResult> serviceTraces = extractServiceTraces(result);
        List<Microservice> microservices = new ArrayList<>();

        for (String serviceName : result.getAnalyzedServices()) {
            StaticServiceTraceResult traceResult = serviceTraces.get(serviceName);
            if (traceResult != null) {
                Microservice microservice = buildMicroserviceFromTrace(serviceName, traceResult);
                //store the CommunicationPathResult in the Microservice for enhanced analysis
                if (Objects.equals(microservice.getName(), result.getRootService())) {
                    microservice.setCommunicationPathResult(result);
                }
                microservices.add(microservice);
            }
        }

        return microservices;
    }

    /**
     * Extracts service traces from the CommunicationPathResult.
     * <p>
     * Currently, the staticTraceResult field in CommunicationPathResult is used.
     * In the future, this can be enhanced to store per-service trace results.
     *
     * @param result The CommunicationPathResult
     * @return Map of service names to their StaticServiceTraceResult
     */
    private Map<String, StaticServiceTraceResult> extractServiceTraces(CommunicationPathResult result) {
        Map<String, StaticServiceTraceResult> traces = new HashMap<>();

        // Prefer per-service trace results if available
        if (result.getStaticTraceResults() != null && !result.getStaticTraceResults().isEmpty()) {
            traces.putAll(result.getStaticTraceResults());
            return traces;
        }

        // Fallback to the legacy single staticTraceResult for the root service
        if (result.getStaticTraceResult() != null) {
            String rootService = result.getRootService();
            if (rootService != null) {
                traces.put(rootService, result.getStaticTraceResult());
            }
        }

        return traces;
    }

    /**
     * Builds a Microservice object from a StaticServiceTraceResult.
     * <p>
     * Extracts endpoints and outgoing calls from the trace result.
     *
     * @param serviceName The name of the service
     * @param traceResult The static service trace result
     * @return A Microservice object
     */
    private Microservice buildMicroserviceFromTrace(String serviceName, StaticServiceTraceResult traceResult) {
        if (traceResult == null || traceResult.getEndpointTraces() == null) {
            return new Microservice(serviceName, new ArrayList<>(), new ArrayList<>());
        }

        List<RestEndpoint> endpoints = new ArrayList<>();
        List<RestCall> calls = new ArrayList<>();

        // Extract endpoints from endpoint traces
        Set<RestEndpoint> uniqueEndpoints = new LinkedHashSet<>();
        Set<RestCall> uniqueCalls = new LinkedHashSet<>();

        for (EndpointTrace trace : traceResult.getEndpointTraces()) {
            // Convert endpoint trace to RestEndpoint
            RestEndpoint endpoint = new RestEndpoint(
                    trace.getHttpMethod(),
                    trace.getPath(),
                    trace.getEndpointArguments(),
                    trace.getReturnType()
            );
            uniqueEndpoints.add(endpoint);

            // Extract calls from downstream targets
            for (TraceTarget target : trace.getDownstreamTargets()) {
                RestCall call = new RestCall(
                        target.getHttpMethod(),
                        target.getUrl(),
                        target.getReturnType()
                );
                uniqueCalls.add(call);
            }
        }

        endpoints.addAll(uniqueEndpoints);
        calls.addAll(uniqueCalls);

        return new Microservice(serviceName, calls, endpoints);
    }
}
