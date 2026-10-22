package com.github.legregor.microservice.coverage.evaluation;

import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectionReportTests {

    @Test
    void testCalculateStatusCodeClassCoverageWithExactCodes(){

            ConnectionReport connectionReport = new ConnectionReport();
            connectionReport.setStatusCodes(new ArrayList<>(List.of(200, 201, 400, 404, 500)));
            connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("200", "201", "400", "404", "500")));

            double result = connectionReport.getStatusCodeClassCoveragePercentage();
            assertTrue (result == 100.0);
    }

    @Test
    void testCalculateStatusCodeClassCoverageWithCodeClasses(){

        ConnectionReport connectionReport = new ConnectionReport();

        connectionReport.setStatusCodes(new ArrayList<>(List.of(200, 404, 500)));
        connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("2xx", "4XX", "500")));

        double result = connectionReport.getStatusCodeClassCoveragePercentage();
        assertTrue (result == 100.0);
    }

    @Test
    void testCalculateStatusCodeClassCoverageWithMissingCodes(){

        ConnectionReport connectionReport = new ConnectionReport();

        connectionReport.setStatusCodes(new ArrayList<>(List.of(200, 404)));
        connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("2xx", "4XX", "500")));

        double result = connectionReport.getStatusCodeClassCoveragePercentage();
        assertTrue (result == 66.66666666666666);
    }

    @Test
    void testCalculateStatusCodeClassCoverageWithInvalidExpected(){

        ConnectionReport connectionReport = new ConnectionReport();

        connectionReport.setStatusCodes(new ArrayList<>(List.of(200, 404)));
        connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("ERROR", "SUCCESS", "500")));

        double result = connectionReport.getStatusCodeClassCoveragePercentage();
        assertTrue (result == 0.0);
    }

    @Test
    void testCalculateStatusCodeClassCoverageWithNoExpected(){

        ConnectionReport connectionReport = new ConnectionReport();

        connectionReport.setStatusCodes(new ArrayList<>(List.of(299)));
        connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("2xx")));

        double result = connectionReport.getStatusCodeClassCoveragePercentage();
        assertTrue (result == 100.0);
    }

    @Test
    void testCalculateStatusCodeClassCoverageWithLimits(){

        ConnectionReport connectionReport = new ConnectionReport();

        connectionReport.setStatusCodes(new ArrayList<>(List.of(200, 404)));
        connectionReport.setExpectedStatusCodes(new ArrayList<>());

        double result = connectionReport.getStatusCodeClassCoveragePercentage();
        assertTrue (result == 0.0);
    }

    /** TrainTicket-like connection: always HTTP 200, the outcome in the body field status. */
    @Test
    void testBodyOutcomesAsExpectedEntries() {
        ConnectionReport connectionReport = new ConnectionReport();
        connectionReport.setStatusCodes(new ArrayList<>(List.of(200, 200)));
        connectionReport.setResponseBodies(new ArrayList<>(List.of(
                "{\"status\": 1, \"msg\": \"Success\"}", "{\"status\": 1, \"msg\": \"Success\"}")));
        connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("status=1", "status=0")));

        assertTrue(connectionReport.getStatusCodeClassCoveragePercentage() == 50.0);
        assertTrue(connectionReport.getMissingExpectedStatusCodes().equals(List.of("status=0")));
        assertTrue(connectionReport.getObservedBodyOutcomes().equals(List.of("status=1")));

        connectionReport.getResponseBodies().add("{\"status\": 0, \"msg\": \"Order Not Found\"}");
        assertTrue(connectionReport.getStatusCodeClassCoveragePercentage() == 100.0);
    }

    /** HTTP and body entries can be mixed; without recorded bodies (mocked callee), body entries are not covered. */
    @Test
    void testMixedEntriesWithoutBodies() {
        ConnectionReport connectionReport = new ConnectionReport();
        connectionReport.setStatusCodes(new ArrayList<>(List.of(200)));
        connectionReport.setExpectedStatusCodes(new ArrayList<>(List.of("2xx", "status=0")));

        assertTrue(connectionReport.getStatusCodeClassCoveragePercentage() == 50.0);
        assertTrue(connectionReport.getMissingExpectedStatusCodes().equals(List.of("status=0")));
    }
}