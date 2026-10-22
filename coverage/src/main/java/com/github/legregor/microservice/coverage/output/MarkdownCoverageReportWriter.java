package com.github.legregor.microservice.coverage.output;

import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.ConnectionExclusions;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageReport;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.ElementPartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.RequestValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;

/**
 * Writer class responsible for generating markdown-based coverage reports
 */
public class MarkdownCoverageReportWriter {

    /**
     * Generates a coverage summary in markdown format
     * 
     * @param report The coverage report to format
     * @return A markdown formatted summary of the coverage report
     */
    public String generateCoverageSummary(CoverageReport report) {
        StringBuilder sb = new StringBuilder();

        appendMarkdownHeader(sb);

        // Order as in the paper: request side (connections, elements, values), then response side
        //sb.append("## Request Coverage Metrics\n\n");
        appendBasicConnectionStatisticsMarkdown(sb, report);
        appendConnectionListsMarkdown(sb, report);
        appendRequestElementCoverageMarkdown(sb, report);
        appendRequestValuePartitionCoverageMarkdown(sb, report);
        appendRequestBodySchemaCoverageMarkdown(sb, report);

        //sb.append("\n## Response Coverage Metrics\n\n");
        appendStatusCodeClassCoverageMarkdown(sb, report);
        appendResponseFieldCoverageMarkdown(sb, report);
        appendResponseValuePartitionCoverageMarkdown(sb, report);

        return sb.toString();
    }

    /**
     * Generates a detailed coverage report in markdown format
     * 
     * @param report The coverage report to format
     * @return A comprehensive markdown formatted summary with usage details
     */
    public String generateDetailedCoverageReport(CoverageReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateCoverageSummary(report));

        if (!report.getConnectionReports().isEmpty()) {
            sb.append("\n\n\n\n");

            sb.append("# Detailed Statistics for Covered REST Connections\n\n");

            for (ConnectionReport connectionReport : report.getConnectionReports()) {
                sb.append(formatConnectionReportMarkdown(connectionReport));
                sb.append("\n\n---\n\n");
            }
        }

        return sb.toString();
    }

    private void appendMarkdownHeader(StringBuilder sb) {
        sb.append("# Microservice Integration Test Coverage Report\n\n");
    }

    private void appendBasicConnectionStatisticsMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("## Connection Coverage Statistics\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append(String.format("| Total REST connections | %d |\n", report.getTotalConnections()));
        sb.append(String.format("| Covered REST connections | %d |\n", report.getCoveredConnections().size()));
        sb.append(String.format("| Uncovered REST connections | %d |\n", report.getUncoveredConnections().size()));
        sb.append(String.format("| **REST Connection Coverage** | **%.2f%%** |\n", report.getConnectionCoveragePercentage()));
        if (!report.getExcludedConnections().isEmpty()) {
            sb.append(String.format("| Excluded REST connections (config, not counted) | %d |\n",
                    report.getExcludedConnections().size()));
        }
        sb.append("\n");
    }

    private void appendConnectionListsMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("### Uncovered REST Connections\n\n");
        if (report.getUncoveredConnections().isEmpty()) {
            sb.append("*No uncovered connections*\n\n");
        } else {
            report.getUncoveredConnections().forEach(connection -> sb.append(formatConnectionLineMarkdown(connection)));
        }

        sb.append("\n### Covered REST Connections\n\n");
        if (report.getCoveredConnections().isEmpty()) {
            sb.append("*No covered connections*\n\n");
        } else {
            report.getCoveredConnections().forEach(connection -> sb.append(formatConnectionLineMarkdown(connection)));
        }
        appendExcludedConnectionsMarkdown(sb, report);
    }

    /** The connections excluded by the config ("reachable": false) and those only reachable through them. */
    private void appendExcludedConnectionsMarkdown(StringBuilder sb, CoverageReport report) {
        if (report.getExcludedConnections().isEmpty() && report.getExclusionWarnings().isEmpty()) {
            return;
        }
        sb.append("\n### Excluded REST Connections\n\n");
        sb.append("*Reachable according to the static analysis, but excluded by the config (\"reachable\": false) or "
                + "only reachable through such connections; not part of any coverage value.*\n\n");
        for (ConnectionExclusions.ExcludedConnection excluded : report.getExcludedConnections()) {
            sb.append(formatConnectionLineMarkdown(excluded.connection()).stripTrailing());
            if (excluded.byConfig()) {
                sb.append(" — excluded by the config");
                if (!excluded.reasons().isEmpty()) {
                    sb.append(": ").append(String.join("; ", excluded.reasons()));
                }
            } else {
                sb.append(" — only reachable through ").append(String.join(", ", excluded.through()));
            }
            sb.append("\n");
        }
        for (String executed : report.getExecutedExcludedConnections()) {
            sb.append("\n> **Warning:** the tests executed the excluded connection ").append(executed)
                    .append(": the exclusion in the config is probably wrong.\n");
        }
        for (String warning : report.getExclusionWarnings()) {
            sb.append("\n> **Warning:** ").append(warning).append(".\n");
        }
    }

    private String formatConnectionLineMarkdown(RestConnection connection) {
        return String.format("- **%s** → **%s** `%s %s`\n",
                connection.getSource(),
                connection.getDestination(),
                connection.getCall().getMethod(),
                connection.getCall().getUrl());
    }

    private void appendStatusCodeClassCoverageMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("## Status Code Class Coverage Statistics\n\n");

        sb.append("| Coverage Metric | Value |\n");
        sb.append("|-----------------|-------|\n");
        sb.append(String.format("| **Status Code Class Coverage (all connections)** | **%.2f%%** |\n",
                report.getStatusCodeClassCoveragePercentage()));
        sb.append(String.format("| **Status Code Class Coverage (covered connections only)** | **%.2f%%** |\n",
                report.getCoveredConnectionsStatusCodeClassCoveragePercentage()));
        sb.append(String.format("| Lowest / median connection value (called connections) | %s / %s |\n",
                percent(report.getStatusCodeClassMinCoveragePercentage()),
                percent(report.getStatusCodeClassMedianCoveragePercentage())));

        // What is missing per called connection (the connections that were never called are listed above)
        java.util.List<ConnectionReport> withGaps = report.findConnectionsMissingExpectedStatusCodes();
        sb.append("\n### Connections Missing Expected Status Codes\n\n");
        if (withGaps.isEmpty()) {
            sb.append("None: every called connection returned all expected status codes.\n");
        } else {
            for (ConnectionReport connectionReport : withGaps) {
                sb.append(formatConnectionLineMarkdown(connectionReport.getConnection()).stripTrailing())
                        .append(": missing ").append(list(connectionReport.getMissingExpectedStatusCodes()))
                        .append(" (seen: ").append(seenStatusCodes(connectionReport)).append(")\n");
            }
        }

        // Add note about server errors if any connections have them
        long serverErrorConnections = report.getConnectionsWithServerErrorStatusCodeClasses();
        if (serverErrorConnections > 0) {
            sb.append(String.format("\n**Note:** %d connections encountered server errors (5xx) - not counted towards coverage\n",
                    serverErrorConnections));
        }

        sb.append("\n");
    }

    private void appendRequestElementCoverageMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("\n## Request Element Coverage Statistics\n\n");
        sb.append("*Computed over the called connections only.*\n\n");

        sb.append("| Coverage Metric | Value |\n");
        sb.append("|-----------------|-------|\n");
        sb.append(String.format("| **Request Element Coverage (pooled)** | **%s** |\n",
                percent(report.getRequestElementPooledCoveragePercentage())));
        sb.append(String.format("| Mean over connections (pooled) | %s |\n",
                percent(report.getRequestElementMeanPooledCoveragePercentage())));
        sb.append(String.format("| Mean over connections (hierarchical) | %s |\n",
                percent(report.getRequestElementMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("| Optional elements only (pooled) | %s |\n",
                percent(report.getRequestElementOptionalPooledCoveragePercentage())));
        sb.append(String.format("| Lowest / median connection value (pooled) | %s / %s |\n",
                percent(report.getRequestElementMinPooledCoveragePercentage()),
                percent(report.getRequestElementMedianPooledCoveragePercentage())));
        sb.append(String.format("| Connections with request elements | %d |\n",
                report.getConnectionsWithRequestElements()));
        sb.append(String.format("| Connections with request element coverage n/a | %d%s |\n",
                report.getConnectionsWithRequestElementCoverageNotApplicable(),
                reasons(report.getRequestElementNotApplicableReasons())));
        sb.append(String.format("| Connections whose request body was not recorded | %d |\n",
                report.getConnectionsWithBodyNotRecorded()));
        sb.append(String.format("| Connections with interface mismatches | %d |\n",
                report.getConnectionsWithInterfaceMismatch()));
        sb.append("\n");
    }

    private void appendRequestValuePartitionCoverageMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("\n## Request Value Partition Coverage Statistics\n\n");
        sb.append("*Computed over the executed request elements of the called connections only.*\n\n");

        sb.append("| Coverage Metric | Value |\n");
        sb.append("|-----------------|-------|\n");
        sb.append(String.format("| **Request Value Partition Coverage (pooled)** | **%s** |\n",
                percent(report.getRequestValuePartitionPooledCoveragePercentage())));
        sb.append(String.format("| Pooled, spec constraints only | %s |\n",
                percent(report.getRequestValuePartitionConstraintPooledCoveragePercentage())));
        sb.append(String.format("| Share of partitions from spec constraints | %s |\n",
                percent(report.getRequestValuePartitionConstraintSharePercentage())));
        sb.append(String.format("| Mean over connections (pooled) | %s |\n",
                percent(report.getRequestValuePartitionMeanPooledCoveragePercentage())));
        sb.append(String.format("| Mean over connections (hierarchical) | %s |\n",
                percent(report.getRequestValuePartitionMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("| Lowest / median connection value (pooled) | %s / %s |\n",
                percent(report.getRequestValuePartitionMinPooledCoveragePercentage()),
                percent(report.getRequestValuePartitionMedianPooledCoveragePercentage())));
        sb.append(String.format("| Connections with value partitions | %d |\n",
                report.getConnectionsWithRequestValuePartitions()));
        sb.append(String.format("| Connections with value partition coverage n/a | %d%s |\n",
                report.getConnectionsWithRequestValuePartitionCoverageNotApplicable(),
                reasons(report.getRequestValuePartitionNotApplicableReasons())));
        sb.append(String.format("| Values sent outside the spec | %d |\n",
                report.getRequestValuePartitionOutOfSpecValueCount()));
        sb.append("\n");
    }

    private void appendResponseFieldCoverageMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("\n## Response Field Coverage Statistics\n\n");
        sb.append("*Computed over the called connections and their returned, expected status codes only.*\n\n");
        sb.append("| Coverage Metric | Value |\n");
        sb.append("|-----------------|-------|\n");
        sb.append(String.format("| **Response Field Coverage (pooled)** | **%s** |\n",
                percent(report.getResponseFieldPooledCoveragePercentage())));
        sb.append(String.format("| Mean over connections (pooled) | %s |\n",
                percent(report.getResponseFieldMeanPooledCoveragePercentage())));
        sb.append(String.format("| Mean over connections (hierarchical) | %s |\n",
                percent(report.getResponseFieldMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("| Optional fields only (pooled) | %s |\n",
                percent(report.getResponseFieldOptionalPooledCoveragePercentage())));
        sb.append(String.format("| Lowest / median connection value (pooled) | %s / %s |\n",
                percent(report.getResponseFieldMinPooledCoveragePercentage()),
                percent(report.getResponseFieldMedianPooledCoveragePercentage())));
        sb.append(String.format("| Connections with response fields | %d |\n", report.getConnectionsWithResponseFields()));
        sb.append(String.format("| Connections with response field coverage n/a | %d%s |\n",
                report.getConnectionsWithResponseFieldCoverageNotApplicable(),
                responseReasons(report.getResponseFieldNotApplicableReasons())));
        sb.append(String.format("| Fields returned but not in the spec | %d |\n",
                report.getResponseFieldReturnedNotInSpecCount()));
        sb.append("\n");
    }

    private void appendResponseValuePartitionCoverageMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("\n## Response Value Partition Coverage Statistics\n\n");
        sb.append("*Computed over the covered response fields of the called connections only.*\n\n");
        sb.append("| Coverage Metric | Value |\n");
        sb.append("|-----------------|-------|\n");
        sb.append(String.format("| **Response Value Partition Coverage (pooled)** | **%s** |\n",
                percent(report.getResponseValuePartitionPooledCoveragePercentage())));
        sb.append(String.format("| Pooled, spec constraints only | %s |\n",
                percent(report.getResponseValuePartitionConstraintPooledCoveragePercentage())));
        sb.append(String.format("| Share of partitions from spec constraints | %s |\n",
                percent(report.getResponseValuePartitionConstraintSharePercentage())));
        sb.append(String.format("| Mean over connections (pooled) | %s |\n",
                percent(report.getResponseValuePartitionMeanPooledCoveragePercentage())));
        sb.append(String.format("| Mean over connections (hierarchical) | %s |\n",
                percent(report.getResponseValuePartitionMeanHierarchicalCoveragePercentage())));
        sb.append(String.format("| Lowest / median connection value (pooled) | %s / %s |\n",
                percent(report.getResponseValuePartitionMinPooledCoveragePercentage()),
                percent(report.getResponseValuePartitionMedianPooledCoveragePercentage())));
        sb.append(String.format("| Connections with response value partitions | %d |\n",
                report.getConnectionsWithResponseValuePartitions()));
        sb.append(String.format("| Connections with response value partition coverage n/a | %d%s |\n",
                report.getConnectionsWithResponseValuePartitionCoverageNotApplicable(),
                responseValueReasons(report.getResponseValuePartitionNotApplicableReasons())));
        sb.append(String.format("| Values returned outside the spec | %d |\n",
                report.getResponseValuePartitionOutOfSpecValueCount()));
        sb.append("\n");
    }

    private void appendRequestBodySchemaCoverageMarkdown(StringBuilder sb, CoverageReport report) {
        sb.append("\n## Request Body Schema Validation Statistics\n\n");

        sb.append("| Information | Value |\n");
        sb.append("|-----------------|-------|\n");
        sb.append(String.format("| Connections with body schema | %d |\n",
                report.getConnectionsWithBodySchema()));
        sb.append(String.format("| Bodies checked against schema | %d |\n",
                report.getBodySchemaCheckedCount()));
        sb.append(String.format("| Bodies matching schema | %d |\n",
                report.getBodySchemaMatchedCount()));
        sb.append(String.format("| Bodies missing from traces | %d |\n",
                report.getBodySchemaMissingCount()));

        sb.append("\n");
    }

    private String formatConnectionReportMarkdown(ConnectionReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("## %s → %s\n\n",
                report.getConnection().getSource(),
                report.getConnection().getDestination()));

        sb.append(String.format("**Endpoint:** `%s %s`\n\n",
                report.getConnection().getCall().getMethod(),
                report.getConnection().getCall().getUrl()));

        // Basic statistics
        sb.append("### Statistics\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append(String.format("| Usage Count | %d |\n", report.getCount()));

        // Same order as the summary: request side, then response side
        //sb.append("\n### Request Coverage Metrics\n");
        appendConnectionRequestElementsMarkdown(sb, report.getRequestElementCoverage());
        appendConnectionValuePartitionsMarkdown(sb, report.getRequestValuePartitionCoverage());
        appendConnectionBodySchemaMarkdown(sb, report);

        //sb.append("\n### Response Coverage Metrics\n");
        appendConnectionStatusCodesMarkdown(sb, report);
        appendConnectionResponseFieldsMarkdown(sb, report.getResponseFieldCoverage());
        appendConnectionResponseValuePartitionsMarkdown(sb, report.getResponseValuePartitionCoverage());

        appendConnectionBodiesMarkdown(sb, report);

        return sb.toString();
    }

    private void appendConnectionBodySchemaMarkdown(StringBuilder sb, ConnectionReport report) {
        if (!report.isBodySchemaApplicable()) {
            return;
        }
        sb.append("\n### Request Body Schema Validation\n\n");
        sb.append(String.format("- **Bodies checked against schema:** %d\n",
                report.getBodySchemaCheckedCount()));
        sb.append(String.format("- **Bodies missing from traces:** %d\n",
                report.getBodySchemaMissingCount()));
        if (report.getBodySchemaMismatchReason() != null) {
            sb.append(String.format("- **Example mismatch:** %s\n",
                    report.getBodySchemaMismatchReason()));
        }
        sb.append("\n**Request body schema:**\n\n");
        if (report.getBodySchema() != null) {
            sb.append("```json\n");
            sb.append(report.getBodySchema().toPrettyString());
            sb.append("\n```\n");
        } else {
            sb.append("*Schema not available*\n\n");
        }
    }

    private void appendConnectionStatusCodesMarkdown(StringBuilder sb, ConnectionReport report) {
        sb.append("\n### Status Code Class Coverage\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append(String.format("| Expected | %s |\n", list(report.getExpectedStatusCodes())));
        sb.append(String.format("| Status Code Classes Covered | %s |\n",
                String.join(", ", report.getUniqueStatusCodeClasses())));
        if (!report.getObservedBodyOutcomes().isEmpty()) {
            sb.append(String.format("| Body Outcomes Seen | %s |\n", list(report.getObservedBodyOutcomes())));
        }
        sb.append(String.format("| Status Code Class Coverage | %.0f%% |\n",
                report.getStatusCodeClassCoveragePercentage()));

        sb.append("\n| Status Code | Count |\n");
        sb.append("|-------------|-------|\n");

        // Count occurrences of each status code
        java.util.Map<Integer, Long> statusCodeCounts = report.getStatusCodes().stream()
                .collect(java.util.stream.Collectors.groupingBy(code -> code, java.util.stream.Collectors.counting()));

        statusCodeCounts.forEach((code, count) -> sb.append(String.format("| %d | %d |\n", code, count)));
    }

    private void appendConnectionBodiesMarkdown(StringBuilder sb, ConnectionReport report) {
        if (report.getRequestBodies().isEmpty() && report.getResponseBodies().isEmpty()) {
            return;
        }
        sb.append("\n### Recorded Bodies\n");

        if (!report.getRequestBodies().isEmpty()) {
            sb.append("\n#### Request Bodies\n\n");
            sb.append(String.format("- **Number of Request Bodies:** %d\n",
                    report.getRequestBodies().size()));
            sb.append(String.format("- **Number of Unique Request Bodies:** %d\n",
                    report.getUniqueRequestBodies().size()));

            sb.append("\n**List of unique Request Bodies:**\n\n");
            report.getUniqueRequestBodies().forEach(body -> {
                sb.append("```json\n");
                sb.append(body);
                sb.append("\n```\n\n");
            });
        }

        if (!report.getResponseBodies().isEmpty()) {
            sb.append("\n#### Response Bodies\n\n");
            sb.append(String.format("- **Number of Response Bodies:** %d\n",
                    report.getResponseBodies().size()));
            sb.append(String.format("- **Number of Unique Response Bodies:** %d\n",
                    report.getUniqueResponseBodies().size()));

            sb.append("\n**List of unique Response Bodies:**\n\n");
            report.getUniqueResponseBodies().forEach(body -> {
                sb.append("```json\n");
                sb.append(body);
                sb.append("\n```\n\n");
            });
        }
    }

    private void appendConnectionRequestElementsMarkdown(StringBuilder sb, RequestElementCoverage coverage) {
        sb.append("\n### Request Element Coverage\n\n");
        if (coverage == null || coverage.isNotApplicable()) {
            NotApplicableReason reason = coverage == null ? NotApplicableReason.NO_ELEMENTS
                    : coverage.getNotApplicableReason();
            sb.append("*n/a (").append(reason.getDescription()).append(")*\n");
            if (coverage != null && !coverage.getNotRecordedElements().isEmpty()) {
                sb.append("\n- **Not recorded:** ").append(list(coverage.getNotRecordedElements())).append("\n");
            }
            if (coverage != null && !coverage.getNotReachableElements().isEmpty()) {
                sb.append("- **Not reachable (never sent by the caller):** ")
                        .append(list(coverage.getNotReachableElements())).append("\n");
            }
            return;
        }
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append(String.format("| Pooled | %s (%d of %d) |\n", percent(coverage.getPooledCoveragePercentage()),
                coverage.getCoveredCount(), coverage.getReachableCount()));
        sb.append(String.format("| Hierarchical | %s |\n", percent(coverage.getHierarchicalCoveragePercentage())));
        sb.append(String.format("| Optional elements only | %s |\n",
                percent(coverage.getOptionalPooledCoveragePercentage())));
        sb.append("\n");
        sb.append("- **Covered:** ").append(list(coverage.getCoveredElements())).append("\n");
        sb.append("- **Uncovered:** ").append(list(coverage.getUncoveredElements())).append("\n");
        sb.append("- **Not reachable (never sent by the caller):** ").append(list(coverage.getNotReachableElements()))
                .append("\n");
        if (coverage.isBodyNotRecorded()) {
            sb.append("- **Not recorded (").append(coverage.getBodyNotRecordedReason().getDescription()).append("):** ")
                    .append(list(coverage.getNotRecordedElements())).append("\n");
        }
        if (coverage.isBodySpecMissing()) {
            sb.append("- *No body schema in the OpenAPI spec: body elements taken from the caller's DTO*\n");
        }
        if (coverage.isInterfaceMismatch()) {
            sb.append(String.format("- **Interface mismatch:** only in spec %s; only in caller %s\n",
                    list(coverage.getOnlyInSpec()), list(coverage.getOnlyInCaller())));
        }
        if (!coverage.getUnexpectedElements().isEmpty()) {
            sb.append("- **Sent but not in the spec:** ").append(list(coverage.getUnexpectedElements())).append("\n");
        }
    }

    private void appendConnectionResponseFieldsMarkdown(StringBuilder sb, ResponseFieldCoverage coverage) {
        sb.append("\n### Response Field Coverage\n\n");
        if (coverage == null || coverage.isNotApplicable()) {
            sb.append("*n/a (").append(coverage == null ? ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA
                    .getDescription() : coverage.getNotApplicableReason().getDescription()).append(")*\n");
        } else {
            sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Optional fields only: %s\n",
                    percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                    coverage.getFieldCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                    percent(coverage.getOptionalPooledCoveragePercentage())));
        }
        if (coverage == null || coverage.getStatusCodes().isEmpty()) {
            if (coverage != null && !coverage.getIgnoredStatusCodes().isEmpty()) {
                sb.append("\n- **Ignored status codes (not expected):** ")
                        .append(coverage.getIgnoredStatusCodes()).append("\n");
            }
            return;
        }
        sb.append("\n| Status | Spec | Responses | Covered fields | Uncovered fields | Returned but not in the spec |\n");
        sb.append("|--------|------|-----------|----------------|------------------|------------------------------|\n");
        for (ResponseFieldCoverage.StatusFieldCoverage status : coverage.getStatusCodes()) {
            String uncovered = status.bodyRecorded() ? list(status.uncoveredFields()) : "*body not recorded*";
            sb.append(String.format("| %d | %s | %d | %s | %s | %s |\n", status.statusCode(),
                    status.specKey() == null ? "*not documented*" : "`" + status.specKey() + "`",
                    status.responseCount(), list(status.coveredFields()), uncovered,
                    list(status.returnedNotInSpec())));
        }
        if (!coverage.getIgnoredStatusCodes().isEmpty()) {
            sb.append("\n- **Ignored status codes (not expected):** ").append(coverage.getIgnoredStatusCodes())
                    .append("\n");
        }
    }

    private void appendConnectionResponseValuePartitionsMarkdown(StringBuilder sb,
                                                                 ResponseValuePartitionCoverage coverage) {
        sb.append("\n### Response Value Partition Coverage\n\n");
        if (coverage == null || coverage.isNotApplicable()) {
            sb.append("*n/a (").append(coverage == null ? ResponseValuePartitionNotApplicableReason.NO_RESPONSE_SCHEMA
                    .getDescription() : coverage.getNotApplicableReason().getDescription()).append(")*\n");
            return;
        }
        sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Spec constraints only: %s\n",
                percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                coverage.getPartitionCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                percent(coverage.getConstraintPooledCoveragePercentage())));
        sb.append("\n| Field | Covered | Covered partitions | Uncovered partitions | Returned outside the spec |\n");
        sb.append("|-------|---------|--------------------|----------------------|---------------------------|\n");
        for (ElementPartitionCoverage field : coverage.getFields()) {
            if (field.getReachableCount() == 0 && field.getOutOfSpecValues().isEmpty()) {
                continue;
            }
            java.util.List<String> outOfSpecTexts = new java.util.ArrayList<>();
            field.getOutOfSpecValues().forEach(value ->
                    outOfSpecTexts.add("`" + value.value() + "` (" + value.reason() + ", " + value.count() + "x)"));
            String outOfSpec = outOfSpecTexts.isEmpty() ? "none" : String.join("; ", outOfSpecTexts);
            sb.append(String.format("| `%s` | %d of %d | %s | %s | %s |\n", field.getElement(),
                    field.getCoveredCount(), field.getReachableCount(), list(field.getCoveredPartitions()),
                    list(field.getUncoveredPartitions()), outOfSpec.replace("|", "\\|")));
        }
    }

    private void appendConnectionValuePartitionsMarkdown(StringBuilder sb, RequestValuePartitionCoverage coverage) {
        sb.append("\n### Request Value Partition Coverage\n\n");
        if (coverage == null || coverage.isNotApplicable()) {
            sb.append("*n/a (").append(coverage == null ? NotApplicableReason.NO_ELEMENTS.getDescription()
                    : coverage.getNotApplicableReason().getDescription()).append(")*\n");
        } else {
            sb.append(String.format("Pooled: %s (%d of %d) | Hierarchical: %s | Spec constraints only: %s\n",
                    percent(coverage.getPooledCoveragePercentage()), coverage.getCoveredCount(),
                    coverage.getReachableCount(), percent(coverage.getHierarchicalCoveragePercentage()),
                    percent(coverage.getConstraintPooledCoveragePercentage())));
        }
        if (coverage == null || coverage.getElements().isEmpty()) {
            return;
        }
        sb.append("\n| Element | Covered | Covered partitions | Uncovered partitions | Not reachable | Outside the spec |\n");
        sb.append("|---------|---------|--------------------|----------------------|---------------|------------------|\n");
        for (ElementPartitionCoverage element : coverage.getElements()) {
            if (element.getReachableCount() == 0 && element.getOutOfSpecValues().isEmpty()) {
                continue;
            }
            java.util.List<String> outOfSpecTexts = new java.util.ArrayList<>();
            element.getOutOfSpecValues().forEach(value ->
                    outOfSpecTexts.add("`" + value.value() + "` (" + value.reason() + ", " + value.count() + "x)"));
            element.getOutOfSpecConstants().forEach(value ->
                    outOfSpecTexts.add("caller constant `" + value.value() + "` (" + value.reason() + ")"));
            String outOfSpec = outOfSpecTexts.isEmpty() ? "none" : String.join("; ", outOfSpecTexts);
            String notReachable = list(element.getNotReachablePartitions());
            if (element.getStaticConstants() != null) {
                notReachable += " (caller only sends " + String.join(", ", element.getStaticConstants()) + ")";
            }
            sb.append(String.format("| `%s` | %d of %d | %s | %s | %s | %s |\n", element.getElement(),
                    element.getCoveredCount(), element.getReachableCount(), list(element.getCoveredPartitions()),
                    list(element.getUncoveredPartitions()), notReachable.replace("|", "\\|"),
                    outOfSpec.replace("|", "\\|")));
        }
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

    /** " (reason: n; ...)" or "" if there is no n/a connection. */
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
        connectionReport.getUniqueStatusCodes().stream().sorted().forEach(code -> seen.add("`" + code + "`"));
        connectionReport.getObservedBodyOutcomes().forEach(outcome -> seen.add("`" + outcome + "`"));
        return seen.isEmpty() ? "none" : String.join(", ", seen);
    }

    private static String percent(Double value) {
        return value == null ? "n/a" : String.format("%.2f%%", value);
    }

    private static String list(java.util.List<String> values) {
        return values == null || values.isEmpty() ? "none" : values.stream()
                .map(value -> "`" + value + "`").collect(java.util.stream.Collectors.joining(", "));
    }
}
