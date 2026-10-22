package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;

/** Status code summary of {@link CoverageReport}: lowest / median value and the missing codes per connection. */
class CoverageReportStatusCodeTest {

    @Test
    void lowestMedianAndMissingCodesOfTheCalledConnections() {
        ConnectionReport reservations = report(List.of(201, 400, 409), "2xx", "4xx");      // 100 %
        ConnectionReport quotes = report(List.of(200, 500), "2xx", "4xx");                  // 50 %, 5xx not counted
        ConnectionReport prices = report(List.of(200, 404), "2xx", "4xx");                  // 100 %
        ConnectionReport discounts = report(List.of(200, 404), "200", "400", "404");        // 66.67 %
        CoverageReport coverageReport = new CoverageReport(List.of(), List.of(),
                List.of(reservations, quotes, prices, discounts));

        assertEquals(50.0, coverageReport.getStatusCodeClassMinCoveragePercentage(), 0.001);
        assertEquals((66.6667 + 100.0) / 2, coverageReport.getStatusCodeClassMedianCoveragePercentage(), 0.001);
        assertEquals(List.of(quotes, discounts), coverageReport.findConnectionsMissingExpectedStatusCodes());
        assertEquals(List.of("4xx"), quotes.getMissingExpectedStatusCodes());
        assertEquals(List.of("400"), discounts.getMissingExpectedStatusCodes());
        assertTrue(reservations.getMissingExpectedStatusCodes().isEmpty());
    }

    @Test
    void noCalledConnectionGivesNoValues() {
        CoverageReport coverageReport = new CoverageReport(List.of(), List.of(), List.of());

        assertNull(coverageReport.getStatusCodeClassMinCoveragePercentage());
        assertNull(coverageReport.getStatusCodeClassMedianCoveragePercentage());
        assertTrue(coverageReport.findConnectionsMissingExpectedStatusCodes().isEmpty());
    }

    private static ConnectionReport report(List<Integer> statusCodes, String... expected) {
        ConnectionReport report = new ConnectionReport();
        report.setStatusCodes(new ArrayList<>(statusCodes));
        report.setExpectedStatusCodes(new ArrayList<>(List.of(expected)));
        return report;
    }
}
