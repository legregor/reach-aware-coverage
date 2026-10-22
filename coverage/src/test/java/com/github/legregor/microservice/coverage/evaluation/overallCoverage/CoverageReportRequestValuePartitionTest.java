package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.requestElement.ExecutedRequestElementsCollector;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTestTrees;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTree;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.ExecutedRequestValuesCollector;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.RequestValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;
import com.github.legregor.microservice.coverage.openapi.OpenApiSchema;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaFieldReader;
import com.github.legregor.microservice.coverage.output.MarkdownCoverageReportWriter;
import com.github.legregor.microservice.coverage.output.TextCoverageReportWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the request value partition aggregation in {@link CoverageReport} and its output. */
class CoverageReportRequestValuePartitionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double DELTA = 1e-9;
    private static final String QUANTITY = """
            {"type": "object", "required": ["quantity"], "properties": {
              "quantity": {"type": "integer", "minimum": 1, "maximum": 99}}}
            """;

    @Test
    void pooledAndMeanOverApplicableConnections() throws IOException {
        // a: quantity #min, #interior, #max -> 2 of 3 covered; b: 1 of 3 covered; c: nothing sent with a value
        ConnectionReport a = report("a", QUANTITY, List.of("{\"quantity\": 1}", "{\"quantity\": 5}"));
        ConnectionReport b = report("b", QUANTITY, List.of("{\"quantity\": 99}", "{\"quantity\": 0}"));
        ConnectionReport c = report("c", """
                {"type": "object", "properties": {"comment": {"type": "string"}}}
                """, List.of("{}"));
        CoverageReport coverageReport = new CoverageReport(
                List.of(a.getConnection(), b.getConnection(), c.getConnection()), List.of(), List.of(a, b, c));

        assertEquals(3.0 / 6.0 * 100.0, coverageReport.getRequestValuePartitionPooledCoveragePercentage(), DELTA);
        assertEquals((2.0 / 3.0 * 100.0 + 1.0 / 3.0 * 100.0) / 2.0,
                coverageReport.getRequestValuePartitionMeanPooledCoveragePercentage(), DELTA);
        assertEquals(1.0 / 3.0 * 100.0, coverageReport.getRequestValuePartitionMinPooledCoveragePercentage(), DELTA);
        assertEquals(100.0, coverageReport.getRequestValuePartitionConstraintSharePercentage(), DELTA);
        assertEquals(2, coverageReport.getConnectionsWithRequestValuePartitions());
        assertEquals(1, coverageReport.getConnectionsWithRequestValuePartitionCoverageNotApplicable());
        assertEquals(Map.of(NotApplicableReason.NO_EXECUTED_ELEMENTS, 1L),
                coverageReport.getRequestValuePartitionNotApplicableReasons());
        // quantity 0 in b
        assertEquals(1, coverageReport.getRequestValuePartitionOutOfSpecValueCount());
    }

    @Test
    void nothingApplicableIsNotApplicable() throws IOException {
        ConnectionReport c = report("c", """
                {"type": "object", "properties": {"comment": {"type": "string"}}}
                """, List.of("{}"));
        CoverageReport coverageReport = new CoverageReport(List.of(c.getConnection()), List.of(), List.of(c));

        assertNull(coverageReport.getRequestValuePartitionPooledCoveragePercentage());
        assertNull(coverageReport.getRequestValuePartitionMeanHierarchicalCoveragePercentage());
        assertNull(coverageReport.getRequestValuePartitionMedianPooledCoveragePercentage());
    }

    @Test
    void writersShowTheValuePartitionSection() throws IOException {
        ConnectionReport b = report("b", QUANTITY, List.of("{\"quantity\": 99}", "{\"quantity\": 0}"));
        CoverageReport coverageReport = new CoverageReport(List.of(b.getConnection()), List.of(), List.of(b));
        // the writers format numbers with the default locale
        String third = String.format("%.2f%%", 100.0 / 3.0);

        String text = new TextCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(text.contains("Request Value Partition Coverage (pooled): " + third), text);
        assertTrue(text.contains("body:quantity: 1 of 3 | covered: #max | uncovered: #min, #interior"), text);
        assertTrue(text.contains("outside the spec: 0 (below minimum 1), 1x"), text);

        String markdown = new MarkdownCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(markdown.contains("| **Request Value Partition Coverage (pooled)** | **" + third + "** |"), markdown);
        assertTrue(markdown.contains("| `body:quantity` | 1 of 3 | `#max` | `#min`, `#interior` | none | `0` (below minimum 1, 1x) |"),
                markdown);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static ConnectionReport report(String name, String bodySchema, List<String> bodies) throws IOException {
        SchemaField body = SchemaFieldReader.read(new OpenApiSchema(MAPPER.readTree(bodySchema), null,
                MAPPER.createObjectNode()));
        RequestElementTree tree = RequestElementTestTrees.tree(List.of(), body);
        RequestElementCoverage elementCoverage = new RequestElementCoverage(tree,
                new ExecutedRequestElementsCollector().collect("/x", bodies.size(), bodies));
        PartitionGenerator generator = new PartitionGenerator();
        RequestValuePartitionCoverage valueCoverage = new RequestValuePartitionCoverage(tree, elementCoverage,
                new ExecutedRequestValuesCollector().collect(tree, "/x", List.of(), bodies), generator,
                new ValueClassifier(generator));

        ConnectionReport report = new ConnectionReport();
        report.setConnection(new RestConnection("caller", name, new RestCall("POST", "http://" + name + "/x", "X"),
                new RestEndpoint("POST", "/x", new ArrayList<>(), "X")));
        report.setCount(bodies.size());
        report.setStatusCodes(new ArrayList<>(List.of(200)));
        report.setRequestElementCoverage(elementCoverage);
        report.setRequestValuePartitionCoverage(valueCoverage);
        return report;
    }
}
