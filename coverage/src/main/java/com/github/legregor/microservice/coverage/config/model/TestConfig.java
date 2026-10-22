package com.github.legregor.microservice.coverage.config.model;

import com.github.legregor.microservice.coverage.config.CoverageConfigReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A test class of a run, as the web UI writes it to the {@code tests} list of the config, optionally with the
 * endpoints of the entry service that it calls:
 * <pre>
 * "tests": [{
 *   "path": "plan/integration/post/SearchCheapestResultTest.java",
 *   "fileName": "SearchCheapestResultTest.java",
 *   "serviceName": "ts-route-plan-service",
 *   "entryEndpoints": [{"method": "POST", "path": "/api/v1/routeplanservice/routePlan/cheapestRoute"}]
 * }]
 * </pre>
 * {@code path} is relative to {@code <serviceName>/src/test/java}. Other field names are rejected by
 * {@link CoverageConfigReader}.
 */
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TestConfig {
    private String path;
    private String fileName;
    private String serviceName;
    /** Empty: the test may call every endpoint of the entry service. */
    private List<EntryEndpoint> entryEndpoints = new ArrayList<>();

    public void setPath(String path) {
        this.path = path;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setEntryEndpoints(List<EntryEndpoint> entryEndpoints) {
        this.entryEndpoints = entryEndpoints == null ? new ArrayList<>() : entryEndpoints;
    }

    /**
     * Whether a test file of the command line (optionally with a {@code #method} suffix) is this test: its path ends
     * with {@code <serviceName>/src/test/java/<path>} (without service name: {@code src/test/java/<path>}, or only
     * {@code <path>} if neither is set).
     */
    public boolean matches(Path testFile) {
        if (testFile == null || path == null || path.isBlank()) {
            return false;
        }
        String file = testFile.toString().replace('\\', '/');
        int methodSeparator = file.indexOf('#', Math.max(0, file.lastIndexOf(".java")));
        if (methodSeparator > 0) {
            file = file.substring(0, methodSeparator);
        }
        String relative = path.replace('\\', '/').replaceFirst("^/+", "");
        String suffix = (serviceName == null || serviceName.isBlank() ? "" : serviceName + "/")
                + "src/test/java/" + relative;
        return file.equals(suffix) || file.endsWith("/" + suffix);
    }

    /** The test file for messages, e.g. "ts-cancel-service/cancel/integration/get/CancelOrderTest.java". */
    public String label() {
        return (serviceName == null || serviceName.isBlank() ? "" : serviceName + "/") + path;
    }
}
