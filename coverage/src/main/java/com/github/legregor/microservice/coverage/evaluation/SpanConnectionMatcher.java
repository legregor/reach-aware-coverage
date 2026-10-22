package com.github.legregor.microservice.coverage.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.AntPathMatcher;

import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.trace.model.CoverageClientSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageServerSpan;

/**
 * Helper class to connect Client with Server spans and match spans from trace
 * data with REST connections.
 */
public class SpanConnectionMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Checks if a client span matches a REST call from static analysis.
     *
     * @param clientSpan        The client span from trace data
     * @param restCall          The REST call from static analysis
     * @param sourceServiceName The source service name from the REST connection
     * @return true if they match, false otherwise
     */
    public boolean isClientSpanMatchingRestCall(CoverageClientSpan clientSpan,
            RestCall restCall, String sourceServiceName) {
        if (sourceServiceName == null || sourceServiceName.isEmpty()) {
            return false;
        }

        if (!sourceServiceName.equals(extractServiceName(clientSpan.getService()))) {
            return false;
        }

        String spanPath = extractPathFromUrl(clientSpan.getUrl());
        String restCallPath = extractPathFromUrl(restCall.getUrl());

        return clientSpan.getHttpMethod().equalsIgnoreCase(restCall.getMethod())
                && (pathMatcher.match(restCallPath, spanPath) || pathMatcher.match(spanPath, restCallPath));
    }

    /**
     * Checks if a server span matches a REST endpoint from static analysis.
     *
     * @param serverSpan             The server span from trace data
     * @param restEndpoint           The REST endpoint from static analysis
     * @param destinationServiceName The destination service name from the REST
     *                               connection
     * @return true if they match, false otherwise
     */
    public boolean isServerSpanMatchingRestEndpoint(CoverageServerSpan serverSpan,
            RestEndpoint restEndpoint, String destinationServiceName) {
        if (destinationServiceName == null || destinationServiceName.isEmpty()) {
            return false;
        }
        if (!destinationServiceName.equals(extractServiceName(serverSpan.getService()))) {
            return false;
        }

        String spanMethod = serverSpan.getHttpMethod();
        String endpointMethod = restEndpoint.getMethod();

        String spanRoute = serverSpan.getRoute();
        String endpointPath = restEndpoint.getPath();

        return spanMethod != null && endpointMethod != null
                && spanMethod.equalsIgnoreCase(endpointMethod)
                && spanRoute != null && endpointPath != null
                && (pathMatcher.match(endpointPath, spanRoute) || pathMatcher.match(spanRoute, endpointPath));
    }

    /**
     * Connects client spans to their corresponding server spans based on
     * the equality of the clients span ID and the server span's parent span ID.
     *
     * @param clientSpans List of client spans from trace data
     * @param serverSpans List of server spans from trace data
     * @return Map of client spans to their corresponding server spans
     */
    public Map<CoverageClientSpan, CoverageServerSpan> connectClientToServerSpans(
            List<CoverageClientSpan> clientSpans,
            List<CoverageServerSpan> serverSpans) {

        Map<CoverageClientSpan, CoverageServerSpan> connections = new HashMap<>();
        for (CoverageClientSpan clientSpan : clientSpans) {
            String clientSpanId = clientSpan.getSpanId();
            if (clientSpanId != null && !clientSpanId.isEmpty()) {
                CoverageServerSpan connectedServerSpan = serverSpans.stream()
                        .filter(serverSpan -> clientSpanId.equals(serverSpan.getParentSpanId()))
                        .findAny()
                        .orElse(null);
                connections.put(clientSpan, connectedServerSpan);
            }
        }
        return connections;
    }

    private String extractPathFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "/";
        }

        try {
            return new java.net.URL(url).getPath();
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Extracts the actual microservice name from a otel-span service-name field.
     * Handles cases where service names contain test paths like:
     * "ts-seat-service/seat.integration.post.DistributeSeatTest/testDistributeSeatTrainNumberG"
     * and extracts just "ts-seat-service"
     *
     * @param spanServiceName The full service name from the span
     * @return The extracted service name (part before the first slash)
     */
    private String extractServiceName(String spanServiceName) {
        if (spanServiceName == null || spanServiceName.isEmpty()) {
            return spanServiceName;
        }

        int firstSlashIndex = spanServiceName.indexOf('/');
        if (firstSlashIndex != -1) {
            return spanServiceName.substring(0, firstSlashIndex);
        }

        return spanServiceName;
    }
}
