package com.github.legregor.microservice.coverage.output;

import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.ConnectionExclusions;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageReport;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.ElementPartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.OutOfSpecValue;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.RequestValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;

/**
 * Writer class responsible for generating text-based coverage reports
 */
public class TextCoverageReportWriter {

    /**
     * Generates a basic coverage summary in text format
     * 
     * @param report The coverage report to format
     * @return A text formatted summary of the coverage report
     */
    public String generateCoverageSummary(CoverageReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("MICROSERVICE INTEGRATION TEST COVERAGE REPORT:\n");
        sb.append("\n");
        // Order as in the paper: request side (connections, elements, values), then response side
        sb.append("REQUEST COVERAGE METRICS\n");
        sb.append("========================\n");
        sb.append("\n");
        sb.append("Connection Coverage Statistics:\n");
        sb.append("-------------------------------\n");
        sb.append(String.format("Total REST connections: %d\n", report.getTotalConnections()));
        sb.append(String.format("Covered REST connections: %d\n", report.getCoveredConnections().size()));
        sb.append(String.format("Uncovered REST connections: %d\n", report.getUncoveredConnections().size()));
        sb.append("\n");
        sb.append(String.format("REST Connection Coverage percentage: %.2f%%\n",
                report.getConnectionCoveragePercentage()));
        if (!report.getExcludedConnections().isEmpty()) {
            sb.append(String.format("Excluded REST connections (config, not counted): %d\n",
                    report.getExcludedConnections().size()));
        }
        appendConnectionLists(sb, report);
        appendRequestElementCoverage(sb, report);
        appendRequestValuePartitionCoverage(sb, report);
        appendRequestBodySchemaCoverage(sb, report);

        sb.append("RESPONSE COVERAGE METRICS\n");
        sb.append("=========================\n");
        sb.append("\n");
        appendStatusCodeClassCoverage(sb, report);
        appendResponseFieldCoverage(sb, report);
        appendResponseValuePartitionCoverage(sb, report);

        return sb.toString();
    }

    /**
     * Appends the lists of covered and uncovered connections
     *
     * @param sb     The StringBuilder to append to
     * @param report The coverage report
     */
    private void appendConnectionLists(StringBuilder sb, CoverageReport report) {
        sb.append("\nUncovered REST connections:\n");
        if (report.getUncoveredConnections().isEmpty()) {
            sb.append("No uncovered connections\n\n");
        } else {
            report.getUncoveredConnections().forEach(connection -> sb.append(formatConnectionLine(connection)));
        }

        sb.append("\nCovered REST connections:\n");
        if (report.getCoveredConnections().isEmpty()) {
            sb.append("No covered connections\n\n");
        } else {
            report.getCoveredConnections().forEach(connection -> sb.append(formatConnectionLine(connection)));
        }
        if (!report.getExcludedConnections().isEmpty() || !report.getExclusionWarnings().isEmpty()) {
            sb.append("\nExcluded REST connections (config \"reachable\": false, not counted):\n");
            for (ConnectionExclusions.ExcludedConnection excluded : report.getExcludedConnections()) {
                sb.append(formatConnectionLine(excluded.connection()).stripTrailing());
                if (excluded.byConfig()) {
                    sb.append(" (excluded by the config");
                    if (!excluded.reasons().isEmpty()) {
                        sb.append(": ").append(String.join("; ", excluded.reasons()));
                    }
                    sb.append(")");
                } else {
                    sb.append(" (only reachable through ").append(String.join(", ", excluded.through())).append(")");
                }
                sb.append("\n");
            }
            report.getExecutedExcludedConnections().forEach(executed -> sb.append("WARNING: the tests executed the "
                    + "excluded connection ").append(executed).append(": the exclusion is probably wrong\n"));
            report.getExclusionWarnings().forEach(warning -> sb.append("WARNING: ").append(warning).append("\n"));
        }
        sb.append("\n");
    }

    /**
     * Formats a single connection line
     *
     * @param connection The REST connection to format
     * @return A formatted string representing the connection
     */
    private String formatConnectionLine(RestConnection connection) {
        return String.format("- %s -> %s %s %s\n",
                connection.getSource(),
                connection.getDestination(),
                connection.getCall().getMethod(),
                connection.getCall().getUrl());
    }

    /**
     * Appends status code class coverage statistics
     *
     * @param sb     The StringBuilder to append to
     * @param report The coverage report
     */
    private void appendStatusCodeClassCoverage(StringBuilder sb, CoverageReport report) {
        sb.append("Status Code Class Coverage Statistics:\n");
        sb.append("--------------------------------------\n");

        // Coverage percentages
        sb.append(String.format("Status Code Class Coverage percentage (all connections): %.2f%%\n",
                report.getStatusCodeClassCoveragePercentage()));
        sb.append(String.format("Status Code Class Coverage percentage (covered connections only): %.2f%%\n",
                report.getCoveredConnectionsStatusCodeClassCoveragePercentage()));

        sb.append(String.format("Lowest / median connection value (called connections): %s / %s\n",
                percent(report.getStatusCodeClassMinCoveragePercentage()),
                percent(report.getStatusCodeClassMedianCoveragePercentage())));

        // What is missing per called connection (the connections that were never called are listed above)
        java.util.List<ConnectionReport> withGaps = report.findConnectionsMissingExpectedStatusCodes();
        sb.append("Connections missing expected status codes:");
        if (withGaps.isEmpty()) {
            sb.append(" none\n");
        } else {
            sb.append("\n");
            for (ConnectionReport connectionReport : withGaps) {
                sb.append(formatConnectionLine(connectionReport.getConnection()).stripTrailing())
                        .append(": missing ").append(String.join(", ", connectionReport.getMissingExpectedStatusCodes()))
                        .append(" (seen: ").append(seenStatusCodes(connectionReport)).append(")\n");
            }
        }

        // Add note about server errors if any connections have them
        long serverErrorConnections = report.getConnectionsWithServerErrorStatusCodeClasses();
        if (serverErrorConnections > 0) {
            sb.append(String.format(
                    "Note: %d connections encountered server errors (5xx) - not counted towards coverage\n",
                    serverErrorConnections));
        }

        sb.append("\n");
    }

    /**
     * Appends request element coverage statistics (computed over the called connections only)
     *
     * @param sb     The StringBuilder to append to
     * @param report The coverage report
     */
    private void appendRequestElementCoverage(StringBuilder sb, CoverageReport report) {
        sb.append("Request Element Coverage Statistics (called connections only):\n");
        sb.append("--------------------------------------------------------------\n");
        sb.append(String.format("Request Element Coverage (pooled): %s\n",
                percent(report.getRequestElementPooledCoveragePercentage())));
        sb.append(String.format("Request Element Coverage (mean over connections, pooled): %s\n",
                percent(report.getRequestElementMeanPooledCoveragePercentage())));
        sb.append(String.format("Request Element Coverage (mean over connections, hierarchical): %s\n",
                percent(report.getRequestElementMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("Request Element Coverage (optional elements only, pooled): %s\n",
                percent(report.getRequestElementOptionalPooledCoveragePercentage())));
        sb.append(String.format("Lowest / median connection value (pooled): %s / %s\n",
                percent(report.getRequestElementMinPooledCoveragePercentage()),
                percent(report.getRequestElementMedianPooledCoveragePercentage())));
        sb.append(String.format("Connections with request elements: %d\n",
                report.getConnectionsWithRequestElements()));
        sb.append(String.format("Connections with request element coverage n/a: %d%s\n",
                report.getConnectionsWithRequestElementCoverageNotApplicable(),
                reasons(report.getRequestElementNotApplicableReasons())));
        sb.append(String.format("Connections whose request body was not recorded (body elements n/a): %d\n",
                report.getConnectionsWithBodyNotRecorded()));
        sb.append(String.format("Connections with interface mismatches (caller DTO vs. spec): %d\n",
                report.getConnectionsWithInterfaceMismatch()));
        sb.append("\n");
    }

    /**
     * Appends request value partition coverage statistics (executed elements of the called connections only)
     */
    private void appendRequestValuePartitionCoverage(StringBuilder sb, CoverageReport report) {
        sb.append("Request Value Partition Coverage Statistics (executed elements of called connections only):\n");
        sb.append("----------------------------------------------------------------------------------------\n");
        sb.append(String.format("Request Value Partition Coverage (pooled): %s\n",
                percent(report.getRequestValuePartitionPooledCoveragePercentage())));
        sb.append(String.format("Pooled, spec constraints only (without type defaults): %s\n",
                percent(report.getRequestValuePartitionConstraintPooledCoveragePercentage())));
        sb.append(String.format("Share of partitions from spec constraints: %s\n",
                percent(report.getRequestValuePartitionConstraintSharePercentage())));
        sb.append(String.format("Mean over connections (pooled / hierarchical): %s / %s\n",
                percent(report.getRequestValuePartitionMeanPooledCoveragePercentage()),
                percent(report.getRequestValuePartitionMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("Lowest / median connection value (pooled): %s / %s\n",
                percent(report.getRequestValuePartitionMinPooledCoveragePercentage()),
                percent(report.getRequestValuePartitionMedianPooledCoveragePercentage())));
        sb.append(String.format("Connections with value partitions: %d\n",
                report.getConnectionsWithRequestValuePartitions()));
        sb.append(String.format("Connections with value partition coverage n/a: %d%s\n",
                report.getConnectionsWithRequestValuePartitionCoverageNotApplicable(),
                reasons(report.getRequestValuePartitionNotApplicableReasons())));
        sb.append(String.format("Values sent outside the spec: %d\n",
                report.getRequestValuePartitionOutOfSpecValueCount()));
        sb.append("\n");
    }

    /**
     * Appends response field coverage statistics (relevant returned status codes of the called connections only)
     */
    private void appendResponseFieldCoverage(StringBuilder sb, CoverageReport report) {
        sb.append("Response Field Coverage Statistics (called connections only):\n");
        sb.append("--------------------------------------------------------------\n");
        sb.append(String.format("Response Field Coverage (pooled): %s\n",
                percent(report.getResponseFieldPooledCoveragePercentage())));
        sb.append(String.format("Mean over connections (pooled / hierarchical): %s / %s\n",
                percent(report.getResponseFieldMeanPooledCoveragePercentage()),
                percent(report.getResponseFieldMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("Response Field Coverage (optional fields only, pooled): %s\n",
                percent(report.getResponseFieldOptionalPooledCoveragePercentage())));
        sb.append(String.format("Lowest / median connection value (pooled): %s / %s\n",
                percent(report.getResponseFieldMinPooledCoveragePercentage()),
                percent(report.getResponseFieldMedianPooledCoveragePercentage())));
        sb.append(String.format("Connections with response fields: %d\n", report.getConnectionsWithResponseFields()));
        sb.append(String.format("Connections with response field coverage n/a: %d%s\n",
                report.getConnectionsWithResponseFieldCoverageNotApplicable(),
                responseReasons(report.getResponseFieldNotApplicableReasons())));
        sb.append(String.format("Fields returned but not in the spec: %d\n",
                report.getResponseFieldReturnedNotInSpecCount()));
        sb.append("\n");
    }

    /**
     * Appends response value partition coverage statistics (covered response fields of the called connections only)
     */
    private void appendResponseValuePartitionCoverage(StringBuilder sb, CoverageReport report) {
        sb.append("Response Value Partition Coverage Statistics (covered response fields of called connections only):\n");
        sb.append("-----------------------------------------------------------------------------------------------\n");
        sb.append(String.format("Response Value Partition Coverage (pooled): %s\n",
                percent(report.getResponseValuePartitionPooledCoveragePercentage())));
        sb.append(String.format("Pooled, spec constraints only (without type defaults): %s\n",
                percent(report.getResponseValuePartitionConstraintPooledCoveragePercentage())));
        sb.append(String.format("Share of partitions from spec constraints: %s\n",
                percent(report.getResponseValuePartitionConstraintSharePercentage())));
        sb.append(String.format("Mean over connections (pooled / hierarchical): %s / %s\n",
                percent(report.getResponseValuePartitionMeanPooledCoveragePercentage()),
                percent(report.getResponseValuePartitionMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("Lowest / median connection value (pooled): %s / %s\n",
                percent(report.getResponseValuePartitionMinPooledCoveragePercentage()),
                percent(report.getResponseValuePartitionMedianPooledCoveragePercentage())));
        sb.append(String.format("Connections with response value partitions: %d\n",
                report.getConnectionsWithResponseValuePartitions()));
        sb.append(String.format("Connections with response value partition coverage n/a: %d%s\n",
                report.getConnectionsWithResponseValuePartitionCoverageNotApplicable(),
                responseValueReasons(report.getResponseValuePartitionNotApplicableReasons())));
        sb.append(String.format("Values returned outside the spec: %d\n",
                report.getResponseValuePartitionOutOfSpecValueCount()));
        sb.append("\n");
    }

    private void appendRequestBodySchemaCoverage(StringBuilder sb, CoverageReport report) {
        sb.append("Request Body Schema Validation Statistics:\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("Connections with body schema: %d\n",
                report.getConnectionsWithBodySchema()));
        sb.append(String.format("Bodies checked against schema: %d\n",
                report.getBodySchemaCheckedCount()));
        sb.append(String.format("Bodies matching schema: %d\n",
                report.getBodySchemaMatchedCount()));
        sb.append(String.format("Bodies missing from traces: %d\n",
                report.getBodySchemaMissingCount()));
        sb.append("\n");
    }

    /**
     * Generates a detailed coverage report
     * 
     * @param report The coverage report to format
     * @return A comprehensive text formatted summary with usage details
     */
    public String generateDetailedCoverageReport(CoverageReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateCoverageSummary(report));

        if (!report.getConnectionReports().isEmpty()) {
            sb.append("\n\n");
            sb.append("Detailed Statistics for covered Rest Connections:\n");
            sb.append("=================================================\n");
            sb.append("\n");

            for (ConnectionReport connectionReport : report.getConnectionReports()) {
                sb.append(generateConnectionSummary(connectionReport));
                sb.append("\n\n");
                sb.append("-------------------------------------------------\n");
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * Generates a summary
     * 
     * @param connectionReport The connection report to format
     * @return A formatted summary string for the connection
     */
    private String generateConnectionSummary(ConnectionReport connectionReport) {
        StringBuilder sb = new StringBuilder();

        sb.append(generateConnectionHeader(connectionReport));

        // Same order as the summary: request side, then response side
        sb.append("\n[Request coverage metrics]\n");
        sb.append(generateRequestElementSummary(connectionReport.getRequestElementCoverage()));
        sb.append(generateRequestValuePartitionSummary(connectionReport.getRequestValuePartitionCoverage()));
        if (connectionReport.isBodySchemaApplicable()) {
            sb.append(generateBodySchemaSummary(connectionReport));
            sb.append("Request Body Schema:\n");
            if (connectionReport.getBodySchema() != null) {
                sb.append(connectionReport.getBodySchema().toPrettyString()).append("\n");
            } else {
                sb.append("Schema not available\n");
            }
            sb.append("\n");
        }

        sb.append("[Response coverage metrics]\n");
        sb.append(generateStatusCodeSummary(connectionReport));
        sb.append(generateResponseFieldSummary(connectionReport.getResponseFieldCoverage()));
        sb.append(generateResponseValuePartitionSummary(connectionReport.getResponseValuePartitionCoverage()));

        String method = connectionReport.getConnection().getCall().getMethod().toUpperCase();

        // Include request bodies only for methods that typically have them
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            sb.append(generateBodiesSummary("REQUEST BODIES",
                    connectionReport.getRequestBodies(),
                    connectionReport.getUniqueRequestBodies().size()));
        }

        // Include response bodies only for methods that typically have them
        if ("GET".equals(method) || "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            sb.append(generateBodiesSummary("RESPONSE BODIES",
                    connectionReport.getResponseBodies(),
                    connectionReport.getUniqueResponseBodies().size()));
        }

        return sb.toString();
    }

    /**
     * Generates the request element coverage section of a connection
     *
     * @param coverage The request element coverage of the connection (may be null)
     * @return Formatted request element summary string
     */
    private String generateRequestElementSummary(RequestElementCoverage coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request Element Coverage:\n");
        if (coverage == null || coverage.isNotApplicable()) {
            NotApplicableReason reason = coverage == null ? NotApplicableReason.NO_ELEMENTS
                    : coverage.getNotApplicableReason();
            sb.append("n/a (").append(reason.getDescription()).append(")\n");
            if (coverage != null && !coverage.getNotRecordedElements().isEmpty()) {
                sb.append("Not recorded: ").append(list(coverage.getNotRecordedElements())).append("\n");
            }
            if (coverage != null && !coverage.getNotReachableElements().isEmpty()) {
                sb.append("Not reachable (never sent by the caller): ")
                        .append(list(coverage.getNotReachableElements())).append("\n");
            }
            sb.append("\n");
            return sb.toString();
        }
        sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Optional only: %s\n",
                percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                coverage.getReachableCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                percent(coverage.getOptionalPooledCoveragePercentage())));
        sb.append("Covered elements: ").append(list(coverage.getCoveredElements())).append("\n");
        sb.append("Uncovered elements: ").append(list(coverage.getUncoveredElements())).append("\n");
        sb.append("Not reachable (never sent by the caller): ").append(list(coverage.getNotReachableElements()))
                .append("\n");
        if (coverage.isBodyNotRecorded()) {
            sb.append("Not recorded (").append(coverage.getBodyNotRecordedReason().getDescription()).append("): ")
                    .append(list(coverage.getNotRecordedElements())).append("\n");
        }
        if (coverage.isBodySpecMissing()) {
            sb.append("Note: no body schema in the OpenAPI spec, body elements taken from the caller's DTO\n");
        }
        if (coverage.isInterfaceMismatch()) {
            sb.append(String.format("Interface mismatch: only in spec %s, only in caller %s\n",
                    list(coverage.getOnlyInSpec()), list(coverage.getOnlyInCaller())));
        }
        if (!coverage.getUnexpectedElements().isEmpty()) {
            sb.append("Sent but not in the spec: ").append(list(coverage.getUnexpectedElements())).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Generates the request value partition section of a connection
     *
     * @param coverage The value partition coverage of the connection (may be null)
     * @return Formatted summary string
     */
    private String generateResponseFieldSummary(ResponseFieldCoverage coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Response Field Coverage:\n");
        if (coverage == null || coverage.isNotApplicable()) {
            sb.append("n/a (").append(coverage == null ? ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA
                    .getDescription() : coverage.getNotApplicableReason().getDescription()).append(")\n");
        } else {
            sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Optional fields only: %s\n",
                    percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                    coverage.getFieldCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                    percent(coverage.getOptionalPooledCoveragePercentage())));
        }
        if (coverage != null) {
            for (ResponseFieldCoverage.StatusFieldCoverage status : coverage.getStatusCodes()) {
                sb.append(String.format("  %d (spec: %s, %d responses): covered: %s | uncovered: %s",
                        status.statusCode(), status.specKey() == null ? "not documented" : status.specKey(),
                        status.responseCount(), list(status.coveredFields()), list(status.uncoveredFields())));
                if (!status.bodyRecorded()) {
                    sb.append(" | body not recorded");
                }
                if (!status.returnedNotInSpec().isEmpty()) {
                    sb.append(" | returned but not in the spec: ").append(list(status.returnedNotInSpec()));
                }
                sb.append("\n");
            }
            if (!coverage.getIgnoredStatusCodes().isEmpty()) {
                sb.append("  Ignored (not expected): ").append(coverage.getIgnoredStatusCodes()).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private String generateResponseValuePartitionSummary(ResponseValuePartitionCoverage coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Response Value Partition Coverage:\n");
        if (coverage == null || coverage.isNotApplicable()) {
            sb.append("n/a (").append(coverage == null ? ResponseValuePartitionNotApplicableReason.NO_RESPONSE_SCHEMA
                    .getDescription() : coverage.getNotApplicableReason().getDescription()).append(")\n\n");
            return sb.toString();
        }
        sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Spec constraints only: %s\n",
                percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                coverage.getPartitionCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                percent(coverage.getConstraintPooledCoveragePercentage())));
        for (ElementPartitionCoverage field : coverage.getFields()) {
            if (field.getReachableCount() == 0 && field.getOutOfSpecValues().isEmpty()) {
                continue;
            }
            sb.append(String.format("  %s: %d of %d | covered: %s | uncovered: %s\n", field.getElement(),
                    field.getCoveredCount(), field.getReachableCount(), list(field.getCoveredPartitions()),
                    list(field.getUncoveredPartitions())));
            for (OutOfSpecValue value : field.getOutOfSpecValues()) {
                sb.append(String.format("    returned outside the spec: %s (%s), %dx\n", value.value(),
                        value.reason(), value.count()));
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private String generateRequestValuePartitionSummary(RequestValuePartitionCoverage coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request Value Partition Coverage:\n");
        if (coverage == null || coverage.isNotApplicable()) {
            sb.append("n/a (").append(coverage == null ? NotApplicableReason.NO_ELEMENTS.getDescription()
                    : coverage.getNotApplicableReason().getDescription()).append(")\n");
        } else {
            sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Spec constraints only: %s\n",
                    percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                    coverage.getReachableCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                    percent(coverage.getConstraintPooledCoveragePercentage())));
        }
        if (coverage != null) {
            for (ElementPartitionCoverage element : coverage.getElements()) {
                if (element.getReachableCount() == 0 && element.getOutOfSpecValues().isEmpty()) {
                    continue;
                }
                sb.append(String.format("  %s: %d of %d | covered: %s | uncovered: %s", element.getElement(),
                        element.getCoveredCount(), element.getReachableCount(), list(element.getCoveredPartitions()),
                        list(element.getUncoveredPartitions())));
                if (!element.getNotReachablePartitions().isEmpty()) {
                    sb.append(" | not reachable: ").append(list(element.getNotReachablePartitions()));
                }
                if (element.getStaticConstants() != null) {
                    sb.append(" | caller only sends: ").append(list(element.getStaticConstants()));
                }
                sb.append("\n");
                for (OutOfSpecValue constant : element.getOutOfSpecConstants()) {
                    sb.append(String.format("    caller constant outside the spec: %s (%s)\n", constant.value(),
                            constant.reason()));
                }
                for (OutOfSpecValue value : element.getOutOfSpecValues()) {
                    sb.append(String.format("    outside the spec: %s (%s), %dx\n", value.value(), value.reason(),
                            value.count()));
                }
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /** " (reason: n; ...)" or "" if there is no n/a connection. */
    private static String responseValueReasons(
            java.util.Map<ResponseValuePartitionNotApplicableReason, Long> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        return reasons.entrySet().stream()
                .map(entry -> entry.getKey().getLabel() + ": " + entry.getValue())
                .collect(java.util.stream.Collectors.joining("; ", " (", ")"));
    }

    /** " (reason: n, ...)" or "" if there is no n/a connection. */
    private static String responseReasons(java.util.Map<ResponseFieldNotApplicableReason, Long> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        return reasons.entrySet().stream()
                .map(entry -> entry.getKey().getLabel() + ": " + entry.getValue())
                .collect(java.util.stream.Collectors.joining("; ", " (", ")"));
    }

    private static String reasons(java.util.Map<NotApplicableReason, Long> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "";
        }
        return reasons.entrySet().stream()
                .map(entry -> entry.getKey().getLabel() + ": " + entry.getValue())
                .collect(java.util.stream.Collectors.joining("; ", " (", ")"));
    }

    /** The status codes of the calls and, if body outcomes are expected, the body outcomes found (e.g. status=0). */
    private static String seenStatusCodes(ConnectionReport connectionReport) {
        java.util.List<String> seen = new java.util.ArrayList<>();
        connectionReport.getUniqueStatusCodes().stream().sorted().forEach(code -> seen.add(String.valueOf(code)));
        seen.addAll(connectionReport.getObservedBodyOutcomes());
        return seen.isEmpty() ? "none" : String.join(", ", seen);
    }

    private static String percent(Double value) {
        return value == null ? "n/a" : String.format("%.2f%%", value);
    }

    private static String list(java.util.List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }

    private String generateBodySchemaSummary(ConnectionReport connectionReport) {
        StringBuilder sb = new StringBuilder();

        sb.append("Request Body Schema Validation:\n");
        sb.append(String.format("Bodies checked: %d\n",
                connectionReport.getBodySchemaCheckedCount()));
        sb.append(String.format("Bodies missing from traces: %d\n",
                connectionReport.getBodySchemaMissingCount()));
        if (connectionReport.getBodySchemaMismatchReason() != null) {
            sb.append(String.format("Example mismatch: %s\n",
                    connectionReport.getBodySchemaMismatchReason()));
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Generates the connection header section
     * 
     * @param connectionReport The connection report
     * @return Formatted connection header string
     */
    private String generateConnectionHeader(ConnectionReport connectionReport) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("REST Connection: %s -> %s %s %s\n",
                connectionReport.getConnection().getSource(),
                connectionReport.getConnection().getDestination(),
                connectionReport.getConnection().getCall().getMethod(),
                connectionReport.getConnection().getCall().getUrl()));

        sb.append(String.format("Usage Count: %d\n", connectionReport.getCount()));

        return sb.toString();
    }

    /**
     * Generates the status codes section
     * 
     * @param connectionReport The connection report
     * @return Formatted status codes summary string
     */
    private String generateStatusCodeSummary(ConnectionReport connectionReport) {
        StringBuilder sb = new StringBuilder();

        // Count occurrences of each status code
        java.util.Map<Integer, Long> statusCodeCounts = connectionReport.getStatusCodes().stream()
                .collect(java.util.stream.Collectors.groupingBy(code -> code, java.util.stream.Collectors.counting()));

        if (statusCodeCounts.isEmpty()) {
            sb.append("List of covered Status Codes: None\n");
        } else {
            sb.append("List of covered Status Codes: ");
            String statusSummary = statusCodeCounts.entrySet().stream()
                    .map(e -> String.format("%d x %d", e.getValue(), e.getKey()))
                    .collect(java.util.stream.Collectors.joining(", "));
            sb.append(statusSummary).append("\n");
        }

        // Add status code class coverage information
        java.util.List<String> uniqueClasses = connectionReport.getUniqueStatusCodeClasses();
        sb.append(String.format("Status Code Classes Covered: %s\n",
                uniqueClasses.isEmpty() ? "None" : String.join(", ", uniqueClasses)));
        if (!connectionReport.getObservedBodyOutcomes().isEmpty()) {
            sb.append(String.format("Body Outcomes Seen: %s\n",
                    String.join(", ", connectionReport.getObservedBodyOutcomes())));
        }

        boolean hasCorrect = connectionReport.hasCorrectStatusCodeClass();
        boolean hasClientError = connectionReport.hasClientErrorStatusCodeClasses();
        boolean hasServerError = connectionReport.hasServerErrorStatusCodeClasses();

        String coverageDescription;
        if (hasCorrect && hasClientError) {
            coverageDescription = "Both correct (2xx) and client error (4xx) covered";
        } else if (hasCorrect) {
            coverageDescription = "Only correct (2xx) covered";
        } else if (hasClientError) {
            coverageDescription = "Only client error (4xx) covered";
        } else {
            coverageDescription = "No status code classes covered";
        }

        sb.append(String.format("Status Code Class Coverage for Connection: %.0f%% (%s)\n",
                connectionReport.getStatusCodeClassCoveragePercentage(), coverageDescription));

        // Add note about server errors if present
        if (hasServerError) {
            sb.append("Note: Server errors (5xx) detected\n");
        }

        sb.append("\n");

        return sb.toString();
    }

    /**
     * Generate a bodies summary section
     * 
     * @param sectionTitle Title of the section (REQUEST BODIES or RESPONSE BODIES)
     * @param bodies       List of bodies to include in the summary
     * @param uniqueCount  Count of unique bodies
     * @return Formatted bodies summary string
     */
    private String generateBodiesSummary(String sectionTitle, java.util.List<String> bodies, int uniqueCount) {
        StringBuilder sb = new StringBuilder();

        sb.append(sectionTitle).append("\n");
        sb.append(String.format("Number of %s: %d\n", sectionTitle.toLowerCase(), bodies.size()));
        sb.append(String.format("Number of Unique %s: %d\n", sectionTitle.toLowerCase(), uniqueCount));

        if (bodies.isEmpty()) {
            sb.append(String.format("List of %s: None\n", sectionTitle)).append("\n");
        } else {
            sb.append(String.format("List of %s:\n", sectionTitle));
            for (String body : bodies) {
                sb.append("   - ").append(body).append("\n");
            }
        }

        if (!sectionTitle.equals("RESPONSE BODIES")) {
            sb.append("\n");
        }

        return sb.toString();
    }

}
