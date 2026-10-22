package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.requestElement.ExecutedRequestElements;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElement;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTree;
import com.github.legregor.microservice.coverage.output.MarkdownCoverageReportWriter;
import com.github.legregor.microservice.coverage.output.TextCoverageReportWriter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the request element aggregation in {@link CoverageReport} (over the called connections only). */
class CoverageReportRequestElementTest {

    private static final double DELTA = 1e-9;

    @Test
    void pooledMeanAndDistributionOverCalledConnections() {
        // connection A: 1 of 4 covered (25%), connection B: 3 of 3 covered (100%), connection C: n/a
        ConnectionReport a = report("a", coverage(
                List.of(optional("body:a1"), optional("body:a2"), optional("body:a3"), required("body:a4")),
                1, "body:a1"));
        ConnectionReport b = report("b", coverage(
                List.of(path("path:id"), optional("body:b1"), optional("body:b2")),
                1, "path:id", "body:b1", "body:b2"));
        ConnectionReport c = report("c", coverage(List.of(), 0));
        CoverageReport coverageReport = new CoverageReport(
                List.of(a.getConnection(), b.getConnection(), c.getConnection()),
                List.of(connection("uncalled")),
                List.of(a, b, c));

        // pooled: (1 + 3) / (4 + 3); uncalled connections and n/a connections do not count
        assertEquals(4.0 / 7.0 * 100.0, coverageReport.getRequestElementPooledCoveragePercentage(), DELTA);
        assertEquals((25.0 + 100.0) / 2.0, coverageReport.getRequestElementMeanPooledCoveragePercentage(), DELTA);
        assertEquals(25.0, coverageReport.getRequestElementMinPooledCoveragePercentage(), DELTA);
        assertEquals(62.5, coverageReport.getRequestElementMedianPooledCoveragePercentage(), DELTA);
        // optional only: a1, a2, a3 (1 covered) + b1, b2 (2 covered) = 3 of 5
        assertEquals(60.0, coverageReport.getRequestElementOptionalPooledCoveragePercentage(), DELTA);
        assertEquals(2, coverageReport.getConnectionsWithRequestElements());
        assertEquals(1, coverageReport.getConnectionsWithRequestElementCoverageNotApplicable());
        assertEquals(Map.of(NotApplicableReason.NO_ELEMENTS, 1L), coverageReport.getRequestElementNotApplicableReasons());
    }

    @Test
    void noRequestElementsAtAllIsNotApplicable() {
        ConnectionReport c = report("c", coverage(List.of(), 1));
        CoverageReport coverageReport = new CoverageReport(List.of(c.getConnection()), List.of(), List.of(c));

        assertNull(coverageReport.getRequestElementPooledCoveragePercentage());
        assertNull(coverageReport.getRequestElementMeanPooledCoveragePercentage());
        assertNull(coverageReport.getRequestElementMeanHierarchicalCoveragePercentage());
        assertNull(coverageReport.getRequestElementMinPooledCoveragePercentage());
    }

    @Test
    void writersShowTheRequestElementSection() {
        ConnectionReport a = report("a", coverage(List.of(optional("body:a1"), optional("body:a2")), 1, "body:a1"));
        ConnectionReport c = report("c", coverage(List.of(), 1));
        CoverageReport coverageReport = new CoverageReport(List.of(a.getConnection(), c.getConnection()), List.of(),
                List.of(a, c));

        String text = new TextCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        // the writers format numbers with the default locale (e.g. 50,00% on a German system)
        String fifty = String.format("%.2f%%", 50.0);
        assertTrue(text.contains("Request Element Coverage (pooled): " + fifty), text);
        assertTrue(text.contains("Uncovered elements: body:a2"), text);
        assertTrue(text.contains("n/a (no request elements (no path parameters, no request body))"), text);
        assertTrue(text.contains("Connections with request element coverage n/a: 1 (no elements: 1)"), text);

        String markdown = new MarkdownCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(markdown.contains("| **Request Element Coverage (pooled)** | **" + fifty + "** |"), markdown);
        assertTrue(markdown.contains("- **Uncovered:** `body:a2`"), markdown);
    }

    @Test
    void notApplicableReasonsAreCountedAndShown() {
        // a: body only, called service not traced (no server span, e.g. a WireMock stub)
        RequestElementTree bodyOnly = new RequestElementTree(List.of(optional("body:email")), false, List.of(),
                List.of());
        ConnectionReport a = report("a", new RequestElementCoverage(bodyOnly,
                new ExecutedRequestElements(Set.of(), 1, 0, 0)));
        // b: no elements at all
        ConnectionReport b = report("b", coverage(List.of(), 1));
        CoverageReport coverageReport = new CoverageReport(List.of(a.getConnection(), b.getConnection()), List.of(),
                List.of(a, b));

        assertEquals(2, coverageReport.getConnectionsWithRequestElementCoverageNotApplicable());
        assertEquals(Map.of(NotApplicableReason.CALLEE_NOT_TRACED, 1L, NotApplicableReason.NO_ELEMENTS, 1L),
                coverageReport.getRequestElementNotApplicableReasons());

        String text = new TextCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(text.contains("n/a (called service not traced"), text);
        assertTrue(text.contains("Not recorded: body:email"), text);

        String markdown = new MarkdownCoverageReportWriter().generateDetailedCoverageReport(coverageReport);
        assertTrue(markdown.contains("*n/a (called service not traced"), markdown);
        assertTrue(markdown.contains("- **Not recorded:** `body:email`"), markdown);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static RequestElement path(String name) {
        return new RequestElement(name, RequestElement.Kind.PATH_PARAMETER, "string", true, true);
    }

    private static RequestElement optional(String name) {
        return new RequestElement(name, RequestElement.Kind.BODY_PROPERTY, "string", false, true);
    }

    private static RequestElement required(String name) {
        return new RequestElement(name, RequestElement.Kind.BODY_PROPERTY, "string", true, true);
    }

    private static RequestElementCoverage coverage(List<RequestElement> elements, int callsWithBody, String... executed) {
        RequestElementTree tree = new RequestElementTree(elements, false, List.of(), List.of());
        return new RequestElementCoverage(tree, new ExecutedRequestElements(Set.of(executed), 1, callsWithBody));
    }

    private static RestConnection connection(String name) {
        return new RestConnection("caller", name, new RestCall("POST", "http://" + name + "/x", "X"),
                new RestEndpoint("POST", "/x", new ArrayList<>(), "X"));
    }

    private static ConnectionReport report(String name, RequestElementCoverage coverage) {
        ConnectionReport report = new ConnectionReport();
        report.setConnection(connection(name));
        report.setCount(1);
        report.setStatusCodes(new ArrayList<>(List.of(200)));
        report.setRequestElementCoverage(coverage);
        return report;
    }
}
