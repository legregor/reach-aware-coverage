package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReportGenerator;
import com.github.legregor.microservice.coverage.trace.JaegerTraceProcessor;
import com.github.legregor.microservice.coverage.trace.model.CoverageClientSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageServerSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;

/**
 * Evaluates REST connection coverage by comparing static analysis results with
 * dynamic runtime traces.
 * Determines which connections are covered/uncovered during testing
 * and generates comprehensive coverage reports with detailed statistics.
 */
public class CoverageEvaluator {

    private final ConnectionReportGenerator reportGenerator;

    public CoverageEvaluator(java.nio.file.Path openApiDir, java.nio.file.Path configFilePath) {
        this.reportGenerator = new ConnectionReportGenerator(openApiDir, configFilePath);
    }

    /** @param config the coverage config (expected status codes); null = defaults (2xx and 4xx) */
    public CoverageEvaluator(java.nio.file.Path openApiDir,
            CoverageConfig config) {
        this.reportGenerator = new ConnectionReportGenerator(openApiDir, config);
    }

    //todo: do we need this? what's the use case for this?
    //public CoverageEvaluator() {
    //    this(null);
    //}

    /**
     * Evaluates how much of the REST connections are covered by the REST calls in
     * the
     * captured spans.
     *
     * @param system   The microservice system containing REST connections from
     *                 static analysis
     * @param allSpans Map of test names to their corresponding spans
     * @return Coverage evaluation results with detailed usage statistics
     */
    public CoverageReport evaluateCoverage(MicroserviceSystem system,
                                           Map<String, List<CoverageSpan>> allSpans) {
        if (system == null || system.getRestConnections() == null) {
            return new CoverageReport(new ArrayList<>(), new ArrayList<>());
        }

        // Connections the config declares as not reachable (and those only reachable through them) are removed
        // before the coverage is computed; the static analysis result is not changed
        ConnectionExclusions.Result exclusions = ConnectionExclusions.apply(system, reportGenerator.getConfig());
        List<RestConnection> allRestConnections = exclusions.remaining();

        List<ConnectionReport> connectionReports = getConnectionReports(allSpans, allRestConnections);

        // Extract covered connections from reports
        List<RestConnection> coveredConnections = connectionReports.stream()
                .map(ConnectionReport::getConnection)
                .collect(Collectors.toList());

        List<RestConnection> uncoveredConnections = allRestConnections.stream()
                .filter(con -> !coveredConnections.contains(con))
                .collect(Collectors.toList());

        // Create coverage report with detailed statistics
        CoverageReport report = new CoverageReport(coveredConnections, uncoveredConnections, connectionReports);
        List<String> executedExcluded = exclusions.excluded().isEmpty() ? List.of()
                : getConnectionReports(allSpans, exclusions.excluded().stream()
                        .map(ConnectionExclusions.ExcludedConnection::connection).toList()).stream()
                .map(connectionReport -> ConnectionExclusions.label(connectionReport.getConnection()))
                .toList();
        report.setExclusions(exclusions.excluded(), executedExcluded, exclusions.warnings());
        exclusions.warnings().forEach(warning -> System.out.println("[WARNING] coverage config: " + warning));
        executedExcluded.forEach(label -> System.out.println("[WARNING] excluded connection " + label
                + " was executed by the tests: the exclusion in the config is probably wrong"));
        if (!exclusions.excluded().isEmpty()) {
            System.out.println("Excluded connections (config \"reachable\": false and downstream): "
                    + exclusions.excluded().size());
        }
        return report;
    }

    /**
     * Generates connection reports for all REST connections found in the traces.
     *
     * @param allSpans           Map of test names to their corresponding spans
     * @param allRestConnections All REST connections from static analysis
     * @return List of reports for connections that were used during testing
     */
    private List<ConnectionReport> getConnectionReports(Map<String, List<CoverageSpan>> allSpans,
                                                        List<RestConnection> allRestConnections) {
        List<CoverageSpan> allSpansList = allSpans.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<CoverageClientSpan> clientSpans = JaegerTraceProcessor.getClientSpans(allSpansList);
        List<CoverageServerSpan> serverSpans = JaegerTraceProcessor.getServerSpans(allSpansList);

        // Generate detailed connection reports
        List<ConnectionReport> connectionReports = reportGenerator.generateDetailedConnectionReports(
                allRestConnections, clientSpans, serverSpans);
        return connectionReports;
    }
}
