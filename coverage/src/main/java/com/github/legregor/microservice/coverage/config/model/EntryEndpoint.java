package com.github.legregor.microservice.coverage.config.model;

import com.github.legregor.microservice.coverage.config.CoverageConfigReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * An endpoint of the entry service that a test calls ({@link TestConfig#getEntryEndpoints()}): the reachable
 * connections are only those that can be reached from the entry endpoints of the executed tests (instead of from
 * every endpoint of the entry service).
 * <pre>
 * "entryEndpoints": [{"method": "POST", "path": "/api/v1/routeplanservice/routePlan/cheapestRoute"}]
 * </pre>
 * {@code method} left out: any HTTP method. {@code path}: the endpoint's path template, or a concrete path that the
 * template matches. Other field names are rejected by
 * {@link CoverageConfigReader}.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class EntryEndpoint {
    private String method;
    private String path;

    /** E.g. "POST /api/v1/orders" or "* /api/v1/orders". */
    public String label() {
        return (method == null || method.isBlank() ? "*" : method.toUpperCase()) + " " + path;
    }
}
