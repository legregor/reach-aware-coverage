package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import com.github.legregor.microservice.coverage.analysis.CommunicationPathConverter;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPath;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.config.model.ConnectionOverride;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes the connections that the config declares as not reachable ({@code "reachable": false} in
 * {@code connectionOverrides}) from the reachable connections of the static analysis, before the coverage is
 * computed. The static analysis result itself is not changed.
 * <p>
 * A connection is excluded
 * <ul>
 * <li><b>by the config</b> if an exclusion names it, or</li>
 * <li><b>downstream</b> if every communication path that reaches it runs through a connection excluded by the
 * config (a connection that is still reachable on another path stays).</li>
 * </ul>
 */
public final class ConnectionExclusions {

    /**
     * A connection excluded from the coverage.
     *
     * @param connection the connection
     * @param byConfig   true: an exclusion of the config names it; false: only reachable through excluded ones
     * @param reasons    the reasons of the matching exclusions (by config; may be empty)
     * @param through    the excluded connections it is only reachable through (downstream)
     */
    public record ExcludedConnection(RestConnection connection, boolean byConfig, List<String> reasons,
                                     List<String> through) {
    }

    /**
     * @param remaining the reachable connections the coverage is computed on
     * @param excluded  the excluded connections, those excluded by the config first
     * @param warnings  exclusions of the config that match no reachable connection (e.g. a typo)
     */
    public record Result(List<RestConnection> remaining, List<ExcludedConnection> excluded, List<String> warnings) {
    }

    private ConnectionExclusions() {
    }

    public static Result apply(MicroserviceSystem system, CoverageConfig config) {
        List<RestConnection> connections = system == null || system.getRestConnections() == null
                ? List.of() : system.getRestConnections();
        List<ConnectionOverride> exclusions = config == null ? List.of() : config.getExclusions();
        if (exclusions.isEmpty()) {
            return new Result(connections, List.of(), List.of());
        }

        // excluded by the config
        Map<String, List<ConnectionOverride>> byConfig = new LinkedHashMap<>();
        Set<ConnectionOverride> used = new HashSet<>();
        for (RestConnection connection : connections) {
            List<ConnectionOverride> matching = config.getExclusionsFor(connection.getSource(),
                    connection.getDestination(), method(connection), paths(connection));
            if (!matching.isEmpty()) {
                byConfig.put(CommunicationPathConverter.connectionKey(connection), matching);
                used.addAll(matching);
            }
        }
        List<String> warnings = new ArrayList<>();
        for (ConnectionOverride exclusion : exclusions) {
            if (!used.contains(exclusion)) {
                warnings.add("exclusion " + exclusion.label() + " matches no reachable connection");
            }
        }

        // downstream: a connection is still reachable if it lies on a path before the first excluded connection
        Set<String> stillReachable = new HashSet<>();
        Map<String, Set<String>> through = new LinkedHashMap<>();
        List<CommunicationPath> paths = system == null || system.getCommunicationPathResult() == null
                || system.getCommunicationPathResult().getPaths() == null
                ? List.of() : system.getCommunicationPathResult().getPaths();
        for (CommunicationPath path : paths) {
            if (path.getSegments() == null) {
                continue;
            }
            String firstExcluded = null;
            for (CommunicationPath.PathSegment segment : path.getSegments()) {
                String key = CommunicationPathConverter.connectionKey(segment);
                if (key == null) {
                    continue;
                }
                if (firstExcluded == null && byConfig.containsKey(key)) {
                    firstExcluded = key;
                } else if (firstExcluded == null) {
                    stillReachable.add(key);
                } else if (!byConfig.containsKey(key)) {
                    through.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(firstExcluded);
                }
            }
        }

        List<RestConnection> remaining = new ArrayList<>();
        List<ExcludedConnection> excludedByConfig = new ArrayList<>();
        List<ExcludedConnection> excludedDownstream = new ArrayList<>();
        Map<String, String> labels = new LinkedHashMap<>();
        connections.forEach(connection -> labels.put(CommunicationPathConverter.connectionKey(connection),
                label(connection)));
        for (RestConnection connection : connections) {
            String key = CommunicationPathConverter.connectionKey(connection);
            if (byConfig.containsKey(key)) {
                excludedByConfig.add(new ExcludedConnection(connection, true, byConfig.get(key).stream()
                        .map(ConnectionOverride::getReason)
                        .filter(reason -> reason != null && !reason.isBlank())
                        .distinct()
                        .toList(), List.of()));
            } else if (through.containsKey(key) && !stillReachable.contains(key)) {
                excludedDownstream.add(new ExcludedConnection(connection, false, List.of(), through.get(key).stream()
                        .map(excludedKey -> labels.getOrDefault(excludedKey, excludedKey))
                        .toList()));
            } else {
                remaining.add(connection);
            }
        }
        List<ExcludedConnection> excluded = new ArrayList<>(excludedByConfig);
        excluded.addAll(excludedDownstream);
        return new Result(remaining, excluded, warnings);
    }

    /** E.g. "ts-cancel-service -> ts-order-service PUT /api/v1/orderservice/order". */
    public static String label(RestConnection connection) {
        String path = connection.getEndpoint() != null && connection.getEndpoint().getPath() != null
                ? connection.getEndpoint().getPath()
                : connection.getCall() != null ? connection.getCall().getUrl() : null;
        return connection.getSource() + " -> " + connection.getDestination() + " " + method(connection) + " " + path;
    }

    private static String method(RestConnection connection) {
        if (connection.getEndpoint() != null && connection.getEndpoint().getMethod() != null) {
            return connection.getEndpoint().getMethod();
        }
        return connection.getCall() != null ? connection.getCall().getMethod() : null;
    }

    /** The ways the connection's path can be written: endpoint template, called URLs. */
    private static List<String> paths(RestConnection connection) {
        List<String> paths = new ArrayList<>();
        if (connection.getEndpoint() != null && connection.getEndpoint().getPath() != null) {
            paths.add(connection.getEndpoint().getPath());
        }
        if (connection.getCall() != null && connection.getCall().getUrl() != null) {
            paths.add(connection.getCall().getUrl());
        }
        if (connection.getCalls() != null) {
            connection.getCalls().stream().filter(call -> call.getUrl() != null)
                    .forEach(call -> paths.add(call.getUrl()));
        }
        return paths;
    }
}
