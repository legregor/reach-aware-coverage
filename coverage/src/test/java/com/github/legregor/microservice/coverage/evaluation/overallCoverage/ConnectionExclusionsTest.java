package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import com.github.legregor.microservice.coverage.analysis.model.CommunicationPath;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.config.model.ConnectionOverride;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionExclusionsTest {

    // x: a -> b GET /b1      (excluded by the config)
    // w: b -> g GET /g       (only reachable through x: excluded downstream)
    // z: a -> b GET /b2
    // y: b -> c GET /c       (reachable through x and through z: stays)
    private static final String[] X = {"a", "b", "GET", "/b1"};
    private static final String[] W = {"b", "g", "GET", "/g"};
    private static final String[] Z = {"a", "b", "GET", "/b2"};
    private static final String[] Y = {"b", "c", "GET", "/c"};

    private static RestConnection connection(String[] c) {
        return new RestConnection(c[0], c[1], new RestCall(c[2], "http://" + c[1] + ":8080" + c[3], null),
                new RestEndpoint(c[2], c[3], new ArrayList<>(), null));
    }

    private static CommunicationPath.PathSegment segment(String[] c) {
        CommunicationPath.PathSegment segment = new CommunicationPath.PathSegment();
        segment.setSourceService(c[0]);
        segment.setDestinationService(c[1]);
        segment.setDestinationHttpMethod(c[2]);
        segment.setDestinationUrl("http://" + c[1] + ":8080" + c[3]);
        segment.setDestinationEndpointHttpMethod(c[2]);
        segment.setDestinationEndpointPath(c[3]);
        return segment;
    }

    private static CommunicationPath path(String[]... connections) {
        CommunicationPath path = new CommunicationPath();
        List<CommunicationPath.PathSegment> segments = new ArrayList<>();
        for (String[] c : connections) {
            segments.add(segment(c));
        }
        path.setSegments(segments);
        return path;
    }

    private static MicroserviceSystem system() {
        CommunicationPathResult result = new CommunicationPathResult();
        result.setPaths(new ArrayList<>(List.of(path(X, Y), path(X, W), path(Z, Y))));
        MicroserviceSystem system = new MicroserviceSystem();
        system.setRestConnections(new ArrayList<>(List.of(connection(X), connection(Y), connection(W),
                connection(Z))));
        system.setCommunicationPathResult(result);
        return system;
    }

    private static ConnectionOverride exclusion(String[] c, String reason) {
        ConnectionOverride override = new ConnectionOverride();
        override.setSourceService(c[0]);
        override.setDestinationService(c[1]);
        override.setMethod(c[2]);
        override.setPath(c[3]);
        override.setReachable(false);
        override.setReason(reason);
        return override;
    }

    private static CoverageConfig config(ConnectionOverride... overrides) {
        CoverageConfig config = new CoverageConfig();
        config.setConnectionOverrides(new ArrayList<>(List.of(overrides)));
        return config;
    }

    private static List<String> labels(List<RestConnection> connections) {
        return connections.stream().map(ConnectionExclusions::label).toList();
    }

    @Test
    void withoutExclusionsNothingChanges() {
        var result = ConnectionExclusions.apply(system(), new CoverageConfig());
        assertEquals(4, result.remaining().size());
        assertTrue(result.excluded().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void excludesTheConnectionAndWhatIsOnlyReachableThroughIt() {
        var result = ConnectionExclusions.apply(system(), config(exclusion(X, "only called for legacy orders")));

        assertEquals(List.of("b -> c GET /c", "a -> b GET /b2"), labels(result.remaining()));
        assertEquals(2, result.excluded().size());
        var byConfig = result.excluded().get(0);
        assertEquals("a -> b GET /b1", ConnectionExclusions.label(byConfig.connection()));
        assertTrue(byConfig.byConfig());
        assertEquals(List.of("only called for legacy orders"), byConfig.reasons());
        var downstream = result.excluded().get(1);
        assertEquals("b -> g GET /g", ConnectionExclusions.label(downstream.connection()));
        assertEquals(List.of("a -> b GET /b1"), downstream.through());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void theCalledUrlNamesTheConnectionToo() {
        var result = ConnectionExclusions.apply(system(), config(exclusion(
                new String[]{"a", "b", "get", "http://b:8080/b1"}, null)));
        assertEquals(2, result.excluded().size());
        assertTrue(result.excluded().get(0).reasons().isEmpty());
    }

    @Test
    void anExclusionMatchingNothingIsAWarning() {
        var result = ConnectionExclusions.apply(system(), config(exclusion(new String[]{"a", "b", "POST", "/b1"},
                null)));
        assertEquals(4, result.remaining().size());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void statusCodesIgnoreExclusionsWhateverTheirPosition() {
        ConnectionOverride statusCodes = new ConnectionOverride();
        statusCodes.setSourceService("a");
        statusCodes.setExpectedStatusCodes(List.of("200", "404"));
        CoverageConfig config = config(exclusion(X, null), statusCodes);
        assertEquals(List.of("200", "404"),
                config.getExpectedStatusCodesFor("a", "b", "GET", List.of("/b1")));
        assertEquals(1, ConnectionExclusions.apply(system(), config(statusCodes, exclusion(X, null)))
                .excluded().stream().filter(ConnectionExclusions.ExcludedConnection::byConfig).count());
    }
}
