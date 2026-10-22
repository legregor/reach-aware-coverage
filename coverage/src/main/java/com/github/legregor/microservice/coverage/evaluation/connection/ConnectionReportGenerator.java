package com.github.legregor.microservice.coverage.evaluation.connection;

import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTree;
import com.github.legregor.microservice.coverage.evaluation.responseField.ObservedResponse;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldTreeBuilder;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionCoverage;
import com.github.legregor.microservice.coverage.openapi.OpenApiResponseResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.ExecutedRequestValuesCollector;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.RequestValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.config.CoverageConfigException;
import com.github.legregor.microservice.coverage.config.CoverageConfigReader;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import com.github.legregor.microservice.coverage.evaluation.SpanConnectionMatcher;
import com.github.legregor.microservice.coverage.evaluation.bodySchema.BodySchemaCoverageEvaluator;
import com.github.legregor.microservice.coverage.evaluation.bodySchema.BodySchemaCoverageResult;
import com.github.legregor.microservice.coverage.evaluation.requestElement.ExecutedRequestElements;
import com.github.legregor.microservice.coverage.evaluation.requestElement.ExecutedRequestElementsCollector;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTreeBuilder;
import com.github.legregor.microservice.coverage.trace.model.CoverageClientSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageServerSpan;

/**
 * Generates detailed reports for REST connections by matching static analysis data
 * with dynamic runtime trace information. 
 * Provides usage information including request/response bodies, status codes, and connection frequencies.
 */
public class ConnectionReportGenerator {

    private final SpanConnectionMatcher spanMatcher;
    private final BodySchemaCoverageEvaluator bodySchemaEvaluator;
    private final CoverageConfig config;
    private final RequestElementTreeBuilder requestElementTreeBuilder;
    private final ResponseFieldTreeBuilder responseFieldTreeBuilder;
    private final ExecutedRequestElementsCollector executedRequestElementsCollector;
    private final ExecutedRequestValuesCollector executedRequestValuesCollector = new ExecutedRequestValuesCollector();
    private final PartitionGenerator partitionGenerator = new PartitionGenerator();
    private final ValueClassifier valueClassifier = new ValueClassifier(partitionGenerator);

    public ConnectionReportGenerator(java.nio.file.Path openApiDir, java.nio.file.Path configFilePath) {
        this(openApiDir, readConfig(configFilePath));
    }

    /** @param config the coverage config (expected status codes); null = defaults (2xx and 4xx) */
    public ConnectionReportGenerator(java.nio.file.Path openApiDir, CoverageConfig config) {
        this.spanMatcher = new SpanConnectionMatcher();
        this.bodySchemaEvaluator = new BodySchemaCoverageEvaluator(openApiDir);
        this.requestElementTreeBuilder = new RequestElementTreeBuilder(openApiDir);
        this.responseFieldTreeBuilder = new ResponseFieldTreeBuilder(openApiDir == null ? null
                : new OpenApiResponseResolver(new OpenApiSpecRegistry(openApiDir)));
        this.executedRequestElementsCollector = new ExecutedRequestElementsCollector();
        this.config = config == null ? new CoverageConfig() : config;
    }

    /** The coverage config used (defaults if none was given). */
    public CoverageConfig getConfig() {
        return config;
    }

    private static CoverageConfig readConfig(java.nio.file.Path configFilePath) {
        if (configFilePath == null) {
            // No config file given: use the defaults (expected status code classes 2xx and 4xx)
            return null;
        }
        try {
            return new CoverageConfigReader().read(configFilePath);
        } catch (CoverageConfigException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Generates detailed connection reports for all REST connections found in the
     * traces
     * 
     * @param restConnections All REST connections from static analysis
     * @param clientSpans     List of client spans from traces
     * @param serverSpans     List of server spans from traces
     * @return List of reports for connections that were used during testing
     */
    public List<ConnectionReport> generateDetailedConnectionReports(
            List<RestConnection> restConnections,
            List<CoverageClientSpan> clientSpans,
            List<CoverageServerSpan> serverSpans) {
        List<ConnectionReport> reports = new ArrayList<>();

        Map<CoverageClientSpan, CoverageServerSpan> spanConnections = spanMatcher
                .connectClientToServerSpans(clientSpans, serverSpans);

        for (RestConnection connection : restConnections) {
            List<CoverageClientSpan> matchingClientSpans = findMatchingClientSpans(
                    clientSpans, connection);

            // Create report from the matching spans
            if (!matchingClientSpans.isEmpty()) {
                ConnectionReport report = createReport(
                        connection, matchingClientSpans, spanConnections);
                reports.add(report);
            }
        }

        return reports;
    }

    /**
     * Creates a detailed report for a REST connection based on the collected client
     * and server spans.
     *
     * @param connection      The REST connection to report on
     * @param clientSpans     List of client spans that match the connection
     * @param serverSpans     List of server spans that match the connection
     * @param spanConnections Map of client to server span connections
     * @return A detailed report containing usage statistics for the connection
     */
    private ConnectionReport createReport(RestConnection connection,
                                          List<CoverageClientSpan> clientSpans,
                                          Map<CoverageClientSpan, CoverageServerSpan> spanConnections) {

        List<Integer> statusCodes = new ArrayList<>();
        List<String> requestBodies = new ArrayList<>();
        List<String> responseBodies = new ArrayList<>();
        List<ObservedResponse> observedResponses = new ArrayList<>();
        int requestBodyMissingCount = 0;
        int callsWithServerSpan = 0;

        for (CoverageClientSpan clientSpan : clientSpans) {
            statusCodes.add(clientSpan.getStatusCode());

            CoverageServerSpan connectedServerSpan = spanConnections.get(clientSpan);
            if (connectedServerSpan != null && spanMatcher.isServerSpanMatchingRestEndpoint(connectedServerSpan,
                    connection.getEndpoint(), connection.getDestination())) {
                callsWithServerSpan++;
                // status code and body of the same server span, so that each body belongs to its status code
                observedResponses.add(new ObservedResponse(connectedServerSpan.getStatusCode(),
                        connectedServerSpan.getResponseBody()));
                if (isValidBody(connectedServerSpan.getRequestBody())) {
                    requestBodies.add(connectedServerSpan.getRequestBody());
                } else {
                    requestBodyMissingCount++;
                }

                if (isValidBody(connectedServerSpan.getResponseBody())) {
                    responseBodies.add(connectedServerSpan.getResponseBody());
                }
            }
        }

        BodySchemaCoverageResult bodySchemaCoverage = bodySchemaEvaluator.evaluate(connection, requestBodies,
                requestBodyMissingCount);

        List<String> expectedStatusCodes = config.getExpectedStatusCodesFor(
                connection.getSource(),
                connection.getDestination(),
                endpointMethod(connection),
                connectionPaths(connection));
        // only responses of a relevant response class count (an HTTP entry by the status code, a body entry such as
        // "status=0" by the body); they are grouped by HTTP status code (the response variant). Response value
        // partitions classify the values of exactly these responses.
        ResponseFieldCoverage responseFieldCoverage = ResponseFieldCoverage.of(observedResponses, statusCodes,
                expectedStatusCodes, responseFieldTreeBuilder, connection.getDestination(),
                endpointMethod(connection), endpointPathOrUrl(connection));
        ResponseValuePartitionCoverage responseValuePartitionCoverage = ResponseValuePartitionCoverage.of(
                responseFieldCoverage, partitionGenerator, valueClassifier);


        RequestElementTree tree = requestElementTreeBuilder.build(connection);
        RequestElementCoverage elementCoverage = requestElementCoverage(connection, tree, clientSpans.size(),
                callsWithServerSpan, requestBodies);
        List<String> callUrls = clientSpans.stream().map(CoverageClientSpan::getUrl).collect(Collectors.toList());
        RequestValuePartitionCoverage valuePartitionCoverage = new RequestValuePartitionCoverage(tree, elementCoverage,
                executedRequestValuesCollector.collect(tree, endpointPath(connection), callUrls, requestBodies),
                partitionGenerator, valueClassifier);

        return new ConnectionReport(connection, clientSpans.size(),
                statusCodes, requestBodies, responseBodies,
                bodySchemaCoverage.isApplicable(),
                bodySchemaCoverage.getCheckedCount(),
                bodySchemaCoverage.getMatchedCount(),
                bodySchemaCoverage.getMissingCount(),
                bodySchemaCoverage.getMismatchReason(),
                getConnectionSchemaIfApplicable(bodySchemaCoverage),
                bodySchemaCoverage.getRequestChecks(),
                expectedStatusCodes,
                elementCoverage,
                valuePartitionCoverage,
                responseFieldCoverage,
                responseValuePartitionCoverage);
    }

    /**
     * Request element coverage of a called connection: reachable elements from the spec and the static analysis,
     * executed elements from the recorded calls (request bodies of the connected server spans).
     */
    private RequestElementCoverage requestElementCoverage(RestConnection connection, RequestElementTree tree,
                                                          int callCount, int callsWithServerSpan,
                                                          List<String> requestBodies) {
        ExecutedRequestElements executed = executedRequestElementsCollector.collect(endpointPath(connection), callCount,
                callsWithServerSpan, requestBodies);
        return new RequestElementCoverage(tree, executed);
    }

    /** The ways a connection's path can be written in a config: endpoint path template and called URLs. */
    private static List<String> connectionPaths(RestConnection connection) {
        List<String> paths = new ArrayList<>();
        if (connection.getEndpoint() != null && connection.getEndpoint().getPath() != null) {
            paths.add(connection.getEndpoint().getPath());
        }
        if (connection.getCall() != null && connection.getCall().getUrl() != null) {
            paths.add(connection.getCall().getUrl());
        }
        if (connection.getCalls() != null) {
            connection.getCalls().stream().filter(call -> call.getUrl() != null)
                    .forEach(call -> paths.add(call.getUrl()));
        }
        return paths;
    }

    private static String endpointMethod(RestConnection connection) {
        if (connection.getEndpoint() != null && connection.getEndpoint().getMethod() != null) {
            return connection.getEndpoint().getMethod();
        }
        return connection.getCall() != null ? connection.getCall().getMethod() : null;
    }

    /** The endpoint's path template, or the called URL if the endpoint is unknown. */
    private static String endpointPathOrUrl(RestConnection connection) {
        String path = endpointPath(connection);
        if (path == null && connection.getCall() != null) {
            path = connection.getCall().getUrl();
        }
        return path;
    }

    private static String endpointPath(RestConnection connection) {
        return connection.getEndpoint() != null ? connection.getEndpoint().getPath() : null;
    }

    private JsonNode getConnectionSchemaIfApplicable(BodySchemaCoverageResult bodySchemaCoverageResult) {
        return bodySchemaCoverageResult.isApplicable() ? bodySchemaCoverageResult.getSchema() : null;
    }

    private boolean isValidBody(String body) {
        return body != null && !body.trim().isEmpty();
    }

    /**
     * Finds all client spans that match the given REST connection.
     * Filters out clientSpans that have a status code of 0, due to an error
     *
     * @param clientSpans List of all client spans
     * @param connection  The REST connection to match against
     * @return List of matching client spans
     */
    private List<CoverageClientSpan> findMatchingClientSpans(
            List<CoverageClientSpan> clientSpans,
            RestConnection connection) {
        return clientSpans.stream()
                .filter(clientSpan -> clientSpan.getStatusCode() != 0)
                .filter(clientSpan -> callsOf(connection).stream().anyMatch(call ->
                        spanMatcher.isClientSpanMatchingRestCall(clientSpan, call, connection.getSource())))
                .collect(Collectors.toList());
    }

    /**
     * All call implementations of a connection (several call sites in the source service can reach
     * the same endpoint). Falls back to the single call if no call list is present.
     */
    private List<RestCall> callsOf(RestConnection connection) {
        if (connection.getCalls() != null && !connection.getCalls().isEmpty()) {
            return connection.getCalls();
        }
        return connection.getCall() != null ? List.of(connection.getCall()) : List.of();
    }

}
