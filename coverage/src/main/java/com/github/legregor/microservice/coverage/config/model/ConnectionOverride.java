package com.github.legregor.microservice.coverage.config.model;

import com.github.legregor.microservice.coverage.config.CoverageConfigReader;
import lombok.*;

import java.util.List;

/**
 * Expected status codes for the connections that match sourceService, destinationService, method and path
 * (missing = any), or, with {@code "reachable": false}, a connection that the static analysis reports as reachable
 * but that is not (e.g. because of a runtime dependency it cannot see). Such an exclusion must name the connection
 * with all four fields, must not set expectedStatusCodes, and can give a {@code reason} for the report; it applies
 * independent of its position in the list. Other field names are rejected by
 * {@link CoverageConfigReader}.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionOverride {
    private String sourceService;
    private String destinationService;
    private String method;
    private String path;
    private List<String> expectedStatusCodes;
    /** false: the matching connection is excluded from the coverage (null or true: no effect). */
    private Boolean reachable;
    /** Why the connection is not reachable (only used with reachable = false). */
    private String reason;

    /** Whether this override excludes a connection ({@code "reachable": false}). */
    public boolean isExclusion() {
        return Boolean.FALSE.equals(reachable);
    }

    /** E.g. "ts-cancel-service -> ts-order-service PUT /api/v1/orderservice/order". */
    public String label() {
        return sourceService + " -> " + destinationService + " " + method + " " + path;
    }
}
