package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.legregor.microservice.coverage.output.MarkdownCoverageReportWriter;
import com.github.legregor.microservice.coverage.output.TextCoverageReportWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.responseField.ObservedResponse;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldTreeBuilder;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;
import com.github.legregor.microservice.coverage.openapi.OpenApiResponseResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;

/**
 * Totals and report sections of response value partition coverage in {@link CoverageReport}: two applicable
 * connections (quotes 12/18, hierarchical 13/18, constraint-only 8/13; prices 4/8, hierarchical 1/2, no constraint
 * partitions) and one connection to a mocked callee (n/a). Values as in ResponseValuePartitionCoverageTest.
 */
class CoverageReportResponseValuePartitionTest {
    private static final double DELTA = 1e-9;
    private static final List<String> DEFAULT_EXPECTED = List.of("2xx", "4xx");
    private static final String UUID_1 = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String UUID_2 = "3fa85f64-5717-4562-b3fc-2c963f66afa7";

    @TempDir
    Path specDir;

    private ResponseFieldTreeBuilder builder;

    @BeforeEach
    void writeSpec() throws IOException {
        Files.writeString(specDir.resolve("pricing.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/quotes": {"post": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["quoteId", "currency", "total", "items"], "properties": {
                         "quoteId": {"type": "string", "format": "uuid"},
                         "currency": {"type": "string", "enum": ["EUR", "CHF", "USD"]},
                         "total": {"type": "number", "minimum": 0},
                         "discountApplied": {"type": "number", "minimum": 0, "maximum": 50},
                         "items": {"type": "array", "items": {"type": "object", "required": ["sku", "quantity"],
                           "properties": {"sku": {"type": "string"},
                             "quantity": {"type": "integer", "minimum": 1, "maximum": 99}}}}}}}}}}}},
                   "/prices": {"get": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "array", "items": {"type": "object", "required": ["sku", "price"], "properties": {
                         "sku": {"type": "string"}, "price": {"type": "number"}}}}}}}}}}}}
                """);
        builder = new ResponseFieldTreeBuilder(new OpenApiResponseResolver(new OpenApiSpecRegistry(specDir)));
    }

    @Test
    void totalsOverApplicableConnections() {
        CoverageReport coverageReport = report();

        assertEquals(16.0 / 26 * 100, coverageReport.getResponseValuePartitionPooledCoveragePercentage(), DELTA);
        assertEquals(8.0 / 13 * 100, coverageReport.getResponseValuePartitionConstraintPooledCoveragePercentage(),
                DELTA);
        assertEquals(13.0 / 26 * 100, coverageReport.getResponseValuePartitionConstraintSharePercentage(), DELTA);
        assertEquals((12.0 / 18 * 100 + 50) / 2, coverageReport.getResponseValuePartitionMeanPooledCoveragePercentage(),
                DELTA);
        assertEquals((13.0 / 18 * 100 + 50) / 2,
                coverageReport.getResponseValuePartitionMeanHierarchicalCoveragePercentage(), DELTA);
        assertEquals(50.0, coverageReport.getResponseValuePartitionMinPooledCoveragePercentage(), DELTA);
        assertEquals((12.0 / 18 * 100 + 50) / 2,
                coverageReport.getResponseValuePartitionMedianPooledCoveragePercentage(), DELTA);
        assertEquals(2, coverageReport.getConnectionsWithResponseValuePartitions());
        assertEquals(1, coverageReport.getConnectionsWithResponseValuePartitionCoverageNotApplicable());
        assertEquals(Map.of(ResponseValuePartitionNotApplicableReason.CALLEE_NOT_TRACED, 1L),
                coverageReport.getResponseValuePartitionNotApplicableReasons());
        assertEquals(0, coverageReport.getResponseValuePartitionOutOfSpecValueCount());
    }

    @Test
    void nothingApplicableIsNotApplicable() {
        ConnectionReport mocked = connectionReport("mock", "/quotes", "POST", List.of());
        CoverageReport coverageReport = new CoverageReport(List.of(mocked.getConnection()), List.of(),
                List.of(mocked));

        assertNull(coverageReport.getResponseValuePartitionPooledCoveragePercentage());
        assertNull(coverageReport.getResponseValuePartitionConstraintSharePercentage());
        assertNull(coverageReport.getResponseValuePartitionMeanHierarchicalCoveragePercentage());
        assertEquals(0, coverageReport.getConnectionsWithResponseValuePartitions());
    }

    @Test
    void writersShowTheSectionAfterResponseFieldCoverage() {
        CoverageReport coverageReport = report();
        // the writers format numbers with the default locale (e.g. 61,54% on a German system)
        String pooled = String.format("%.2f%%", 16.0 / 26 * 100);

        String markdown = new MarkdownCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(markdown.contains("| **Response Value Partition Coverage (pooled)** | **" + pooled + "** |"),
                markdown);
        assertTrue(markdown.contains("| `200:currency` | 1 of 3 | `#enum=EUR` | `#enum=CHF`, `#enum=USD` | none |"),
                markdown);
        assertTrue(markdown.contains("*n/a (called service not traced"), markdown);
        assertInOrder(markdown,
                "\n## Response Field Coverage Statistics\n",
                "\n## Response Value Partition Coverage Statistics\n",
                "\n# Detailed Statistics for Covered REST Connections\n",
                "\n### Response Field Coverage\n",
                "\n### Response Value Partition Coverage\n");

        String text = new TextCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(text.contains("Response Value Partition Coverage (pooled): " + pooled), text);
        assertTrue(text.contains("  200:currency: 1 of 3 | covered: #enum=EUR | uncovered: #enum=CHF, #enum=USD"),
                text);
        assertInOrder(text,
                "Response Field Coverage Statistics",
                "Response Value Partition Coverage Statistics",
                "Response Field Coverage:",
                "Response Value Partition Coverage:");
    }

    private CoverageReport report() {
        ConnectionReport quotes = connectionReport("quotes", "/quotes", "POST", List.of(
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_1 + "\", \"currency\": \"EUR\", \"total\": 0, "
                        + "\"items\": []}"),
                new ObservedResponse(200, "{\"quoteId\": \"" + UUID_2 + "\", \"currency\": \"EUR\", \"total\": 12.5, "
                        + "\"discountApplied\": 10, \"items\": [{\"sku\": \"ABC\", \"quantity\": 1}, "
                        + "{\"sku\": \"\", \"quantity\": 5}]}")));
        ConnectionReport prices = connectionReport("prices", "/prices", "GET", List.of(
                new ObservedResponse(200, "[]"),
                new ObservedResponse(200, "[{\"sku\": \"ABC\", \"price\": 1.5}]")));
        ConnectionReport mocked = connectionReport("mock", "/quotes", "POST", List.of());
        List<ConnectionReport> reports = List.of(quotes, prices, mocked);
        return new CoverageReport(reports.stream().map(ConnectionReport::getConnection).toList(), List.of(),
                reports);
    }

    private ConnectionReport connectionReport(String name, String path, String method,
                                              List<ObservedResponse> observed) {
        ResponseFieldCoverage fieldCoverage = ResponseFieldCoverage.of(observed, DEFAULT_EXPECTED, builder,
                "pricing", method, path);
        ConnectionReport report = new ConnectionReport();
        report.setConnection(new RestConnection("caller", name, new RestCall(method, "http://" + name + path, "X"),
                new RestEndpoint(method, path, new ArrayList<>(), "X")));
        report.setCount(1);
        report.setStatusCodes(new ArrayList<>(List.of(200)));
        report.setResponseFieldCoverage(fieldCoverage);
        report.setResponseValuePartitionCoverage(ResponseValuePartitionCoverage.of(fieldCoverage,
                new PartitionGenerator(), new ValueClassifier()));
        return report;
    }

    private static void assertInOrder(String report, String... sections) {
        int position = -1;
        for (String section : sections) {
            int next = report.indexOf(section, position + 1);
            assertTrue(next > position, "missing or out of order: " + section.strip() + "\n" + report);
            position = next;
        }
    }
}
