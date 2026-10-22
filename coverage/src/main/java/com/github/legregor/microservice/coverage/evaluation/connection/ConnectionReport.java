package com.github.legregor.microservice.coverage.evaluation.connection;

import com.github.legregor.microservice.coverage.config.StatusCodes;
import com.github.legregor.microservice.coverage.evaluation.bodySchema.RequestBodySchemaCheck;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.RequestValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionCoverage;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.legregor.microservice.coverage.analysis.model.RestConnection;

/**
 * Detailed report that aggregates information about the integration test
 * executions of a single REST connection.
 * 
 * This class captures information about how a specific REST connection
 * was exercised during microservice integration testing. It provides detailed
 * metrics for
 * analyzing the quality and completeness of test coverage for individual
 * connections.
 * 
 * <h3>Detailed Reporting Information:</h3>
 * <ul>
 * <li><strong>Call Frequency:</strong> Number of times the connection was
 * invoked</li>
 * <li><strong>Status Codes:</strong> All HTTP status codes encountered during
 * testing</li>
 * <li><strong>Request/Response Bodies:</strong> Collection of unique request
 * and response bodies</li>
 * <li><strong>Status Code Class Coverage:</strong> On demand coverage
 * percentage calculation based on 2xx/4xx status code classes</li>
 * <li><strong>Request Body Schema Coverage:</strong> Validation against OpenAPI
 * request body schemas (if applicable)</li>
 * </ul>
 * 
 * <h3>Status Code Class Coverage Rules:</h3>
 * <ul>
 * <li><strong>0%:</strong> No relevant status codes covered</li>
 * <li><strong>50%:</strong> Either correct (2xx) OR client error (4xx) status
 * codes covered</li>
 * <li><strong>100%:</strong> Both correct (2xx) AND client error (4xx) status
 * codes covered</li>
 * <li><strong>Server errors (5xx):</strong> Tracked separately, don't
 * contribute to coverage percentage</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionReport {
    /** The REST connection this report is analyzing */
    private RestConnection connection;

    /** Number of times this connection was invoked during testing */
    private int count;

    /**
     * List of all HTTP status codes encountered during testing (may contain
     * duplicates)
     */
    private List<Integer> statusCodes = new ArrayList<>();

    /** List of all request bodies sent during testing (may contain duplicates) */
    private List<String> requestBodies = new ArrayList<>();

    /**
     * List of all response bodies received during testing (may contain duplicates)
     */
    private List<String> responseBodies = new ArrayList<>();

    /** Whether a request body schema exists for this connection */
    private boolean bodySchemaApplicable;

    /** Number of request bodies checked against schema */
    private int bodySchemaCheckedCount;

    /** Number of request bodies matching schema */
    private int bodySchemaMatchedCount;

    /** Number of calls with missing captured request bodies */
    private int bodySchemaMissingCount;

    /** Example mismatch reason for request body schema validation */
    private String bodySchemaMismatchReason;

    /** The OpenAPI request body schema for this connection (if applicable) */
    private com.fasterxml.jackson.databind.JsonNode bodySchema;

    /** Per-request schema validation details for request bodies */
    private List<RequestBodySchemaCheck> requestBodySchemaChecks = new ArrayList<>();

    /** List of expected status codes/classes (e.g. ["2xx", "4xx", "500"]), set by a user per connection */
    private List<String> expectedStatusCodes = new ArrayList<>();

    /** Request element coverage of this connection (path parameters and request body fields). */
    private RequestElementCoverage requestElementCoverage;

    private RequestValuePartitionCoverage requestValuePartitionCoverage;

    /** Response field coverage of the connection (fields of the documented responses of the relevant status codes) */
    private ResponseFieldCoverage
            responseFieldCoverage;

    /** Response value partition coverage of the connection (partitions of the covered response fields) */
    private ResponseValuePartitionCoverage
            responseValuePartitionCoverage;


    //todo: delete later! Or do we want to have this default option, where we assign the expected status codes a default value? LG
    //Constructor for all variables except expectedStatusCodes
    public ConnectionReport(RestConnection connection, int count, List<Integer> statusCodes,
                            List<String> requestBodies, List<String> responseBodies, boolean bodySchemaApplicable,
                            int bodySchemaCheckedCount, int bodySchemaMatchedCount, int bodySchemaMissingCount,
                            String bodySchemaMismatchReason, com.fasterxml.jackson.databind.JsonNode bodySchema, List<RequestBodySchemaCheck> requestBodySchemaChecks) {
        this.connection = connection;
        this.count = count;
        this.statusCodes = statusCodes;
        this.requestBodies = requestBodies;
        this.responseBodies = responseBodies;
        this.bodySchemaApplicable = bodySchemaApplicable;
        this.bodySchemaCheckedCount = bodySchemaCheckedCount;
        this.bodySchemaMatchedCount = bodySchemaMatchedCount;
        this.bodySchemaMissingCount = bodySchemaMissingCount;
        this.bodySchemaMismatchReason = bodySchemaMismatchReason;
        this.bodySchema = bodySchema;
        this.requestBodySchemaChecks = requestBodySchemaChecks;
        this.expectedStatusCodes = new ArrayList<>();
    }


    /**
     * Gets unique status codes that occurred during testing
     * 
     * @return List of unique status codes
     */
    public List<Integer> getUniqueStatusCodes() {
        return this.statusCodes.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Gets unique request bodies that were sent during testing
     * 
     * @return List of unique request bodies
     */
    public List<String> getUniqueRequestBodies() {
        return this.requestBodies.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Gets unique response bodies that were received during testing
     * 
     * @return List of unique response bodies
     */
    public List<String> getUniqueResponseBodies() {
        return this.responseBodies.stream().distinct().collect(Collectors.toList());
    }

    public com.fasterxml.jackson.databind.JsonNode getBodySchema() {
        return bodySchema;
    }

    public void setBodySchema(com.fasterxml.jackson.databind.JsonNode bodySchema) {
        this.bodySchema = bodySchema;
    }

    /**
     * Gets unique status code classes (2xx, 4xx, 5xx) that occurred during testing
     * 
     * @return List of unique status code classes
     */
    public List<String> getUniqueStatusCodeClasses() {
        return this.statusCodes.stream()
                .map(this::getStatusCodeClass)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Calculates status code class coverage.
     * Coverage is based on whether the expected status codes/classes were encountered during testing.
     *
     * @return Status code class coverage (percentage)
     */
    public double getStatusCodeClassCoveragePercentage(){
        if (expectedStatusCodes == null || expectedStatusCodes.isEmpty()) {
            return 0.0;
        }

        long matchedCount = expectedStatusCodes.stream()
                .filter(this::isStatusCodeExpected)
                .count();

        return (double) matchedCount / expectedStatusCodes.size() * 100.0;
    }

    /** The expected status codes or classes (e.g. "4xx", "404") that no call of this connection returned. */
    public List<String> getMissingExpectedStatusCodes() {
        if (expectedStatusCodes == null) {
            return new ArrayList<>();
        }
        return expectedStatusCodes.stream()
                .filter(expected -> !isStatusCodeExpected(expected))
                .collect(Collectors.toList());
    }

    /**
     * Checks if an expected entry was encountered: an HTTP status code or class (e.g. "200" or "2xx") by the status
     * codes of the calls, a body outcome (e.g. "status=0") by the recorded response bodies.
     */
    public boolean isStatusCodeExpected(String expected) {
        if (StatusCodes.isBodyEntry(expected)) {
            return responseBodies != null && responseBodies.stream().anyMatch(body -> StatusCodes.matchesBody(body, expected));
        }
        return statusCodes.stream().anyMatch(code -> matchesExpected(code, expected));
    }

    /**
     * The body outcomes (e.g. "status=1") found in the recorded response bodies, for the fields used by body outcome
     * entries of the expected list; empty if the connection expects HTTP status codes only.
     */
    public List<String> getObservedBodyOutcomes() {
        return StatusCodes.bodyOutcomes(responseBodies, expectedStatusCodes);
    }

    public boolean matchesExpected(int code, String expected) {
        return StatusCodes.matches(code, expected);
    }

    /**
     * Checks if this connection has covered the correct status code class (2xx)
     * 
     * @return true if 2xx status codes were encountered, false otherwise
     */
    public boolean hasCorrectStatusCodeClass() {
        return statusCodes.stream().anyMatch(code -> code >= 200 && code < 300);
    }

    /**
     * Checks if this connection has covered client error status code classes (4xx)
     * 
     * @return true if 4xx status codes were encountered, false otherwise
     */
    public boolean hasClientErrorStatusCodeClasses() {
        return statusCodes.stream().anyMatch(code -> code >= 400 && code < 500);
    }

    /**
     * Checks if this connection has encountered server error status code classes
     * (5xx)
     * 
     * @return true if 5xx status codes were encountered, false otherwise
     */
    public boolean hasServerErrorStatusCodeClasses() {
        return statusCodes.stream().anyMatch(code -> code >= 500 && code < 600);
    }

    /**
     * Maps a status code to its class category
     * 
     * @param statusCode The HTTP status code
     * @return The status code class (2xx, 4xx, 5xx, or Other)
     */
    private String getStatusCodeClass(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "2xx";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "4xx";
        } else if (statusCode >= 500 && statusCode < 600) {
            return "5xx";
        } else {
            return "Other";
        }
    }
}
