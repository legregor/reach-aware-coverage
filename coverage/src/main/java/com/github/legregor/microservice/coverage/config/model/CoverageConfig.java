package com.github.legregor.microservice.coverage.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.legregor.microservice.coverage.config.EntryEndpointResolver;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Coverage configuration of a run: the expected status codes or status code classes per connection.
 * <p>
 * It is the config file the web UI writes to {@code storage/configs/<name>.json} (the selected services are ignored
 * here), extended by:
 * <pre>
 * "globalExpectedStatusCodes": ["2xx", "4xx"],
 * "connectionOverrides": [
 *   {"sourceService": "ts-cancel-service", "destinationService": "ts-order-service",
 *    "method": "PUT", "path": "/api/v1/orderservice/order", "expectedStatusCodes": ["200", "404"]}
 * ]
 * </pre>
 * Entries are status codes ({@code "404"}) or classes ({@code "4xx"}). An override applies to every connection it
 * matches: a missing source, destination, method or path matches any; the path is compared with the endpoint path
 * template and with the called URL (or its path). The first matching override wins; without one, the global list
 * applies (default 2xx and 4xx).
 * <p>
 * An override with {@code "reachable": false} (and all four fields, no expected status codes) excludes a connection
 * that the static analysis reports as reachable from the coverage, see {@link ConnectionOverride}.
 * <p>
 * Each entry of {@code "tests"} can list the endpoints of the entry service that the test calls
 * ({@code "entryEndpoints"}, see {@link TestConfig}); they restrict the reachability analysis, see
 * {@link EntryEndpointResolver}.
 */
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverageConfig {

    /** Expected status code classes when the config does not define any. */
    public static final List<String> DEFAULT_EXPECTED_STATUS_CODES = List.of("2xx", "4xx");

    private String configName;

    private List<String> globalExpectedStatusCodes = DEFAULT_EXPECTED_STATUS_CODES;

    private List<ConnectionOverride> connectionOverrides = new ArrayList<>();

    private GraphSelection graphSelection = new GraphSelection();

    /** The test classes of the run (written by the web UI), optionally with their entry endpoints. */
    private List<TestConfig> tests = new ArrayList<>();

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /** A missing or empty list keeps the defaults (2xx, 4xx). */
    public void setGlobalExpectedStatusCodes(List<String> globalExpectedStatusCodes) {
        this.globalExpectedStatusCodes = globalExpectedStatusCodes == null || globalExpectedStatusCodes.isEmpty()
                ? DEFAULT_EXPECTED_STATUS_CODES : globalExpectedStatusCodes;
    }

    public void setTests(List<TestConfig> tests) {
        this.tests = tests == null ? new ArrayList<>() : tests;
    }

    public void setConnectionOverrides(List<ConnectionOverride> connectionOverrides) {
        this.connectionOverrides = connectionOverrides == null ? new ArrayList<>() : connectionOverrides;
    }

    public void setGraphSelection(GraphSelection graphSelection) {
        this.graphSelection = graphSelection == null ? new GraphSelection() : graphSelection;
    }

    /** Kept for existing callers: the path is compared as given. */
    public List<String> getExpectedStatusCodesFor(String sourceService, String destinationService, String method,
                                                  String path) {
        return getExpectedStatusCodesFor(sourceService, destinationService, method,
                path == null ? List.of() : List.of(path));
    }

    /**
     * @param paths the ways the connection's path can be written: endpoint path template, called URL, ...
     * @return the expected status codes of the first matching override, otherwise the global list
     */
    public List<String> getExpectedStatusCodesFor(String sourceService, String destinationService, String method,
                                                  Collection<String> paths) {
        if (connectionOverrides != null) {
            for (ConnectionOverride override : connectionOverrides) {
                if (override == null || override.getExpectedStatusCodes() == null
                        || override.getExpectedStatusCodes().isEmpty()) {
                    continue;
                }
                if (matches(override.getSourceService(), sourceService)
                        && matches(override.getDestinationService(), destinationService)
                        && (override.getMethod() == null || override.getMethod().equalsIgnoreCase(method))
                        && matchesPath(override.getPath(), paths)) {
                    return override.getExpectedStatusCodes();
                }
            }
        }
        return globalExpectedStatusCodes == null || globalExpectedStatusCodes.isEmpty()
                ? DEFAULT_EXPECTED_STATUS_CODES : globalExpectedStatusCodes;
    }

    /** The overrides with {@code "reachable": false}. */
    public List<ConnectionOverride> getExclusions() {
        return connectionOverrides == null ? List.of() : connectionOverrides.stream()
                .filter(override -> override != null && override.isExclusion())
                .toList();
    }

    /**
     * The exclusions ({@code "reachable": false}) that name this connection: same source, destination and method
     * (ignoring case), and a path equal to one of the connection's paths (endpoint template, called URL or its
     * path).
     */
    public List<ConnectionOverride> getExclusionsFor(String sourceService, String destinationService, String method,
                                                     Collection<String> paths) {
        return getExclusions().stream()
                .filter(exclusion -> exclusion.getSourceService() != null
                        && exclusion.getSourceService().equals(sourceService)
                        && exclusion.getDestinationService() != null
                        && exclusion.getDestinationService().equals(destinationService)
                        && exclusion.getMethod() != null && exclusion.getMethod().equalsIgnoreCase(method)
                        && exclusion.getPath() != null && !exclusion.getPath().isBlank()
                        && matchesPath(exclusion.getPath(), paths))
                .toList();
    }

    private static boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    /**
     * Whether the configured path names one of the connection's paths (endpoint template, called URL or its path).
     * A missing leading or a trailing slash and the names of path variables do not matter:
     * {@code api/v1/routes/{trainNumber}} names the endpoint {@code /api/v1/routes/{tripId}}.
     */
    private static boolean matchesPath(String expected, Collection<String> paths) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        String normalizedExpected = comparable(expected);
        for (String path : paths) {
            if (path == null) {
                continue;
            }
            if (comparable(path).equals(normalizedExpected) || comparable(pathOf(path)).equals(normalizedExpected)) {
                return true;
            }
        }
        return false;
    }

    private static String comparable(String path) {
        String trimmed = path.trim();
        if (!trimmed.contains("://") && !trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return trimSlash(trimmed).replaceAll("\\{[^/}]*\\}", "{}");
    }

    /** The path of a URL ({@code http://host:8080/a/b?x=1} -> {@code /a/b}); other strings unchanged. */
    private static String pathOf(String url) {
        String withoutQuery = url.split("[?#]", 2)[0];
        int scheme = withoutQuery.indexOf("://");
        if (scheme < 0) {
            return withoutQuery;
        }
        int pathStart = withoutQuery.indexOf('/', scheme + 3);
        return pathStart < 0 ? "/" : withoutQuery.substring(pathStart);
    }

    private static String trimSlash(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
