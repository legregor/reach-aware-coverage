package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;
import com.github.legregor.microservice.coverage.evaluation.requestElement.NotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.RequestValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseValuePartition.ResponseValuePartitionNotApplicableReason;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a comprehensive integration test coverage report for REST connections in a microservice system.
 * 
 * This class contains all information needed for detailed reporting and the calculation of coverage metrics 
 * for the executed microservice integration tests. 
 */
@Getter
@ToString
public class CoverageReport {
    private final List<RestConnection> coveredConnections;
    private final List<RestConnection> uncoveredConnections;
    private final List<ConnectionReport> connectionReports;
    /**
     * Connections of the static analysis that the config excludes ({@code "reachable": false}) or that are only
     * reachable through such connections; not part of any coverage value.
     */
    private List<ConnectionExclusions.ExcludedConnection> excludedConnections = new ArrayList<>();
    /** Excluded connections that the tests executed nevertheless: the exclusion is probably wrong. */
    private List<String> executedExcludedConnections = new ArrayList<>();
    /** Exclusions of the config that match no reachable connection. */
    private List<String> exclusionWarnings = new ArrayList<>();

    /** Sets the exclusions applied before the coverage was computed. */
    public void setExclusions(List<ConnectionExclusions.ExcludedConnection> excludedConnections,
                              List<String> executedExcludedConnections, List<String> exclusionWarnings) {
        this.excludedConnections = excludedConnections == null ? new ArrayList<>() : excludedConnections;
        this.executedExcludedConnections = executedExcludedConnections == null ? new ArrayList<>()
                : executedExcludedConnections;
        this.exclusionWarnings = exclusionWarnings == null ? new ArrayList<>() : exclusionWarnings;
    }

    /**
     * Main constructor with detailed reports
     */
    public CoverageReport(List<RestConnection> coveredConnections,
            List<RestConnection> uncoveredConnections,
            List<ConnectionReport> connectionReports) {
        this.coveredConnections = coveredConnections;
        this.uncoveredConnections = uncoveredConnections;
        this.connectionReports = connectionReports;
    }

    /**
     * Constructor without detailed reports (for backward compatibility)
     */
    public CoverageReport(List<RestConnection> coveredConnections,
            List<RestConnection> uncoveredConnections) {
        this.coveredConnections = coveredConnections;
        this.uncoveredConnections = uncoveredConnections;
        this.connectionReports = new ArrayList<>();
    }

    /**
     * Gets the total number of connections (covered + uncovered)
     * 
     * @return Total number of connections
     */
    public int getTotalConnections() {
        return coveredConnections.size() + uncoveredConnections.size();
    }

    /**
     * Gets the connection coverage percentage
     * 
     * @return Connection coverage percentage (0-100%)
     */
    public double getConnectionCoveragePercentage() {
        int total = getTotalConnections();
        if (total == 0) {
            return 0.0;
        }
        return (double) coveredConnections.size() / total * 100.0;
    }



    /**
     * Calculates the aggregate status code class coverage percentage across all
     * connections
     * (both covered and uncovered)
     * 
     * @return Aggregate status code class coverage percentage (average across all
     *         connections)
     */
    public double getStatusCodeClassCoveragePercentage() {
        int totalConnections = getTotalConnections();
        if (totalConnections == 0) {
            return 0.0;
        }

        double coveredConnectionsTotal = connectionReports.stream()
                .mapToDouble(ConnectionReport::getStatusCodeClassCoveragePercentage)
                .sum();

        return coveredConnectionsTotal / totalConnections;
    }

    /**
     * Calculates the status code class coverage percentage for only the covered
     * connections
     * 
     * @return Status code class coverage percentage (average of covered connections
     *         only)
     */
    public double getCoveredConnectionsStatusCodeClassCoveragePercentage() {
        if (connectionReports.isEmpty()) {
            return 0.0;
        }

        double totalCoverage = connectionReports.stream()
                .mapToDouble(ConnectionReport::getStatusCodeClassCoveragePercentage)
                .sum();

        return totalCoverage / connectionReports.size();
    }

    /** Lowest status code class coverage of a called connection; null if no connection was called. */
    public Double getStatusCodeClassMinCoveragePercentage() {
        return connectionReports.stream()
                .map(ConnectionReport::getStatusCodeClassCoveragePercentage)
                .min(Double::compare)
                .orElse(null);
    }

    /** Median status code class coverage of the called connections; null if no connection was called. */
    public Double getStatusCodeClassMedianCoveragePercentage() {
        return median(connectionReports.stream()
                .map(ConnectionReport::getStatusCodeClassCoveragePercentage)
                .collect(Collectors.toList()));
    }

    /**
     * Called connections that did not return all expected status codes or classes, lowest coverage first. (Not a
     * getter on purpose: the connection reports are already part of the JSON report.)
     */
    public List<ConnectionReport> findConnectionsMissingExpectedStatusCodes() {
        return connectionReports.stream()
                .filter(report -> !report.getMissingExpectedStatusCodes().isEmpty())
                .sorted(java.util.Comparator.comparingDouble(ConnectionReport::getStatusCodeClassCoveragePercentage))
                .collect(Collectors.toList());
    }

    /**
     * Gets the number of connections that have covered the correct status code
     * class (2xx)
     * 
     * @return Count of connections with 2xx status code coverage
     */
    public long getConnectionsWithCorrectStatusCodeClass() {
        return connectionReports.stream()
                .filter(ConnectionReport::hasCorrectStatusCodeClass)
                .count();
    }

    /**
     * Gets the number of connections that have covered client error status code
     * classes (4xx)
     * 
     * @return Count of connections with 4xx status code coverage
     */
    public long getConnectionsWithClientErrorStatusCodeClasses() {
        return connectionReports.stream()
                .filter(ConnectionReport::hasClientErrorStatusCodeClasses)
                .count();
    }

    /**
     * Gets the number of connections that have encountered server error status code
     * classes (5xx)
     * 
     * @return Count of connections with 5xx status code coverage
     */
    public long getConnectionsWithServerErrorStatusCodeClasses() {
        return connectionReports.stream()
                .filter(ConnectionReport::hasServerErrorStatusCodeClasses)
                .count();
    }

    public long getConnectionsWithBodySchema() {
        return connectionReports.stream()
                .filter(ConnectionReport::isBodySchemaApplicable)
                .count();
    }

    public int getBodySchemaCheckedCount() {
        return connectionReports.stream()
                .mapToInt(ConnectionReport::getBodySchemaCheckedCount)
                .sum();
    }

    public int getBodySchemaMatchedCount() {
        return connectionReports.stream()
                .mapToInt(ConnectionReport::getBodySchemaMatchedCount)
                .sum();
    }

    public int getBodySchemaMissingCount() {
        return connectionReports.stream()
                .mapToInt(ConnectionReport::getBodySchemaMissingCount)
                .sum();
    }


    // ---------------------------------------------------------------------------------------------
    // Request element coverage (over the called connections only)
    // ---------------------------------------------------------------------------------------------

    /**
     * Pooled request element coverage: covered request elements / reachable request elements over all called
     * connections. Every element has the same weight. Null (n/a) if no called connection has request elements.
     */
    public Double getRequestElementPooledCoveragePercentage() {
        int reachable = applicableRequestElementCoverages().stream()
                .mapToInt(RequestElementCoverage::getReachableCount).sum();
        int covered = applicableRequestElementCoverages().stream()
                .mapToInt(RequestElementCoverage::getCoveredCount).sum();
        return reachable == 0 ? null : (double) covered / reachable * 100.0;
    }

    /** Pooled request element coverage of the optional elements only. Null (n/a) if there are none. */
    public Double getRequestElementOptionalPooledCoveragePercentage() {
        int reachable = applicableRequestElementCoverages().stream()
                .mapToInt(RequestElementCoverage::getOptionalReachableCount).sum();
        int covered = applicableRequestElementCoverages().stream()
                .mapToInt(RequestElementCoverage::getOptionalCoveredCount).sum();
        return reachable == 0 ? null : (double) covered / reachable * 100.0;
    }

    /** Mean of the pooled connection values: every connection has the same weight. */
    public Double getRequestElementMeanPooledCoveragePercentage() {
        return mean(applicableRequestElementCoverages().stream()
                .map(RequestElementCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    /** Mean of the hierarchical connection values: every connection and every sibling element has the same weight. */
    public Double getRequestElementMeanHierarchicalCoveragePercentage() {
        return mean(applicableRequestElementCoverages().stream()
                .map(RequestElementCoverage::getHierarchicalCoveragePercentage).collect(Collectors.toList()));
    }

    /** Lowest pooled connection value (part of the distribution that a single value hides). */
    public Double getRequestElementMinPooledCoveragePercentage() {
        return applicableRequestElementCoverages().stream()
                .map(RequestElementCoverage::getPooledCoveragePercentage)
                .filter(Objects::nonNull)
                .min(Double::compare)
                .orElse(null);
    }

    /** Median of the pooled connection values. */
    public Double getRequestElementMedianPooledCoveragePercentage() {
        List<Double> values = applicableRequestElementCoverages().stream()
                .map(RequestElementCoverage::getPooledCoveragePercentage)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        if (values.isEmpty()) {
            return null;
        }
        int middle = values.size() / 2;
        return values.size() % 2 == 1 ? values.get(middle) : (values.get(middle - 1) + values.get(middle)) / 2.0;
    }

    /** Called connections with at least one request element that can be judged. */
    public long getConnectionsWithRequestElements() {
        return applicableRequestElementCoverages().size();
    }

    /**
     * Called connections whose request element coverage is n/a (no elements, none reachable, or only body elements
     * whose body was not recorded). See {@link #getRequestElementNotApplicableReasons()} for the reasons.
     */
    public long getConnectionsWithRequestElementCoverageNotApplicable() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestElementCoverage)
                .filter(coverage -> coverage == null || coverage.isNotApplicable())
                .count();
    }

    /** Number of called connections with n/a request element coverage, per reason (only reasons that occur). */
    public Map<NotApplicableReason, Long> getRequestElementNotApplicableReasons() {
        Map<NotApplicableReason, Long> reasons = new EnumMap<>(NotApplicableReason.class);
        for (ConnectionReport connectionReport : connectionReports) {
            RequestElementCoverage coverage = connectionReport.getRequestElementCoverage();
            NotApplicableReason reason = coverage == null ? NotApplicableReason.NO_ELEMENTS
                    : coverage.getNotApplicableReason();
            if (reason != null) {
                reasons.merge(reason, 1L, Long::sum);
            }
        }
        return reasons;
    }

    /** Called connections that send a body which was not recorded in the traces (body elements n/a). */
    public long getConnectionsWithBodyNotRecorded() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestElementCoverage)
                .filter(Objects::nonNull)
                .filter(RequestElementCoverage::isBodyNotRecorded)
                .count();
    }

    /** Called connections whose caller DTO and endpoint spec do not have the same top-level properties. */
    public long getConnectionsWithInterfaceMismatch() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestElementCoverage)
                .filter(Objects::nonNull)
                .filter(RequestElementCoverage::isInterfaceMismatch)
                .count();
    }

    // ---------------------------------------------------------------------------------------------
    // Request value partition coverage (over the executed elements of the called connections)
    // ---------------------------------------------------------------------------------------------

    /** Pooled: covered / reachable value partitions over all applicable connections; null (n/a) if there are none. */
    public Double getRequestValuePartitionPooledCoveragePercentage() {
        return pooled(RequestValuePartitionCoverage::getCoveredCount, RequestValuePartitionCoverage::getReachableCount);
    }

    /** Pooled over the partitions from spec constraints only (without type defaults); null if there are none. */
    public Double getRequestValuePartitionConstraintPooledCoveragePercentage() {
        return pooled(RequestValuePartitionCoverage::getConstraintCoveredCount,
                RequestValuePartitionCoverage::getConstraintReachableCount);
    }

    /** Share of the reachable partitions that come from spec constraints (0-100); null if there are none. */
    public Double getRequestValuePartitionConstraintSharePercentage() {
        int reachable = sum(RequestValuePartitionCoverage::getReachableCount);
        return reachable == 0 ? null
                : sum(RequestValuePartitionCoverage::getConstraintReachableCount) * 100.0 / reachable;
    }

    public Double getRequestValuePartitionMeanPooledCoveragePercentage() {
        return mean(applicableRequestValuePartitionCoverages().stream()
                .map(RequestValuePartitionCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    public Double getRequestValuePartitionMeanHierarchicalCoveragePercentage() {
        return mean(applicableRequestValuePartitionCoverages().stream()
                .map(RequestValuePartitionCoverage::getHierarchicalCoveragePercentage).collect(Collectors.toList()));
    }

    public Double getRequestValuePartitionMinPooledCoveragePercentage() {
        return applicableRequestValuePartitionCoverages().stream()
                .map(RequestValuePartitionCoverage::getPooledCoveragePercentage)
                .filter(Objects::nonNull)
                .min(Double::compare)
                .orElse(null);
    }

    public Double getRequestValuePartitionMedianPooledCoveragePercentage() {
        return median(applicableRequestValuePartitionCoverages().stream()
                .map(RequestValuePartitionCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    /** Called connections with at least one executed element that has value partitions. */
    public long getConnectionsWithRequestValuePartitions() {
        return applicableRequestValuePartitionCoverages().size();
    }

    /** Called connections whose value partition coverage is n/a; see {@link #getRequestValuePartitionNotApplicableReasons()}. */
    public long getConnectionsWithRequestValuePartitionCoverageNotApplicable() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestValuePartitionCoverage)
                .filter(coverage -> coverage == null || coverage.isNotApplicable())
                .count();
    }

    /** Number of called connections with n/a value partition coverage, per reason (only reasons that occur). */
    public Map<NotApplicableReason, Long> getRequestValuePartitionNotApplicableReasons() {
        Map<NotApplicableReason, Long> reasons = new EnumMap<>(NotApplicableReason.class);
        for (ConnectionReport connectionReport : connectionReports) {
            RequestValuePartitionCoverage coverage = connectionReport.getRequestValuePartitionCoverage();
            NotApplicableReason reason = coverage == null ? NotApplicableReason.NO_ELEMENTS
                    : coverage.getNotApplicableReason();
            if (reason != null) {
                reasons.merge(reason, 1L, Long::sum);
            }
        }
        return reasons;
    }

    /** Distinct values sent that the spec does not allow, over all called connections (developer information). */
    public long getRequestValuePartitionOutOfSpecValueCount() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestValuePartitionCoverage)
                .filter(Objects::nonNull)
                .mapToInt(RequestValuePartitionCoverage::getOutOfSpecValueCount)
                .sum();
    }

    // ---------------------------------------------------------------------------------------------
    // Response field coverage (relevant returned status codes of the called connections)
    // ---------------------------------------------------------------------------------------------

    /** Pooled: covered / all response fields over all applicable connections; null (n/a) if there are none. */
    public Double getResponseFieldPooledCoveragePercentage() {
        return responseFieldPooled(ResponseFieldCoverage::getCoveredCount, ResponseFieldCoverage::getFieldCount);
    }

    /** Pooled over the optional response fields only. */
    public Double getResponseFieldOptionalPooledCoveragePercentage() {
        return responseFieldPooled(ResponseFieldCoverage::getOptionalCoveredCount,
                ResponseFieldCoverage::getOptionalFieldCount);
    }

    public Double getResponseFieldMeanPooledCoveragePercentage() {
        return mean(applicableResponseFieldCoverages().stream()
                .map(ResponseFieldCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    public Double getResponseFieldMeanHierarchicalCoveragePercentage() {
        return mean(applicableResponseFieldCoverages().stream()
                .map(ResponseFieldCoverage::getHierarchicalCoveragePercentage).collect(Collectors.toList()));
    }

    public Double getResponseFieldMinPooledCoveragePercentage() {
        return applicableResponseFieldCoverages().stream()
                .map(ResponseFieldCoverage::getPooledCoveragePercentage)
                .filter(Objects::nonNull)
                .min(Double::compare)
                .orElse(null);
    }

    public Double getResponseFieldMedianPooledCoveragePercentage() {
        return median(applicableResponseFieldCoverages().stream()
                .map(ResponseFieldCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    /** Called connections with at least one judged response field. */
    public long getConnectionsWithResponseFields() {
        return applicableResponseFieldCoverages().size();
    }

    /** Called connections whose response field coverage is n/a; see {@link #getResponseFieldNotApplicableReasons()}. */
    public long getConnectionsWithResponseFieldCoverageNotApplicable() {
        return connectionReports.stream()
                .map(ConnectionReport::getResponseFieldCoverage)
                .filter(coverage -> coverage == null || coverage.isNotApplicable())
                .count();
    }

    /** Number of called connections with n/a response field coverage, per reason (only reasons that occur). */
    public Map<ResponseFieldNotApplicableReason, Long> getResponseFieldNotApplicableReasons() {
        Map<ResponseFieldNotApplicableReason, Long> reasons = new EnumMap<>(ResponseFieldNotApplicableReason.class);
        for (ConnectionReport connectionReport : connectionReports) {
            ResponseFieldCoverage coverage = connectionReport.getResponseFieldCoverage();
            ResponseFieldNotApplicableReason reason = coverage == null
                    ? ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA : coverage.getNotApplicableReason();
            if (reason != null) {
                reasons.merge(reason, 1L, Long::sum);
            }
        }
        return reasons;
    }

    /** Fields returned but not documented in the spec, over all called connections (developer information). */
    public long getResponseFieldReturnedNotInSpecCount() {
        return connectionReports.stream()
                .map(ConnectionReport::getResponseFieldCoverage)
                .filter(Objects::nonNull)
                .mapToInt(ResponseFieldCoverage::getReturnedNotInSpecCount)
                .sum();
    }

    // ---------------------------------------------------------------------------------------------
    // Response value partition coverage (over the covered response fields of the called connections)
    // ---------------------------------------------------------------------------------------------

    /** Pooled: covered / all response value partitions over all applicable connections; null (n/a) if none. */
    public Double getResponseValuePartitionPooledCoveragePercentage() {
        return responseValuePartitionPooled(ResponseValuePartitionCoverage::getCoveredCount,
                ResponseValuePartitionCoverage::getPartitionCount);
    }

    /** Pooled over the partitions from spec constraints only (without type defaults); null if there are none. */
    public Double getResponseValuePartitionConstraintPooledCoveragePercentage() {
        return responseValuePartitionPooled(ResponseValuePartitionCoverage::getConstraintCoveredCount,
                ResponseValuePartitionCoverage::getConstraintPartitionCount);
    }

    /** Share of the response value partitions that come from spec constraints (0-100); null if there are none. */
    public Double getResponseValuePartitionConstraintSharePercentage() {
        int all = applicableResponseValuePartitionCoverages().stream()
                .mapToInt(ResponseValuePartitionCoverage::getPartitionCount).sum();
        return all == 0 ? null : applicableResponseValuePartitionCoverages().stream()
                .mapToInt(ResponseValuePartitionCoverage::getConstraintPartitionCount).sum() * 100.0 / all;
    }

    public Double getResponseValuePartitionMeanPooledCoveragePercentage() {
        return mean(applicableResponseValuePartitionCoverages().stream()
                .map(ResponseValuePartitionCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    public Double getResponseValuePartitionMeanHierarchicalCoveragePercentage() {
        return mean(applicableResponseValuePartitionCoverages().stream()
                .map(ResponseValuePartitionCoverage::getHierarchicalCoveragePercentage).collect(Collectors.toList()));
    }

    public Double getResponseValuePartitionMinPooledCoveragePercentage() {
        return applicableResponseValuePartitionCoverages().stream()
                .map(ResponseValuePartitionCoverage::getPooledCoveragePercentage)
                .filter(Objects::nonNull)
                .min(Double::compare)
                .orElse(null);
    }

    public Double getResponseValuePartitionMedianPooledCoveragePercentage() {
        return median(applicableResponseValuePartitionCoverages().stream()
                .map(ResponseValuePartitionCoverage::getPooledCoveragePercentage).collect(Collectors.toList()));
    }

    /** Called connections with at least one covered response field that has value partitions. */
    public long getConnectionsWithResponseValuePartitions() {
        return applicableResponseValuePartitionCoverages().size();
    }

    /**
     * Called connections whose response value partition coverage is n/a; see
     * {@link #getResponseValuePartitionNotApplicableReasons()}.
     */
    public long getConnectionsWithResponseValuePartitionCoverageNotApplicable() {
        return connectionReports.stream()
                .map(ConnectionReport::getResponseValuePartitionCoverage)
                .filter(coverage -> coverage == null || coverage.isNotApplicable())
                .count();
    }

    /** Number of called connections with n/a response value partition coverage, per reason (only reasons that occur). */
    public Map<ResponseValuePartitionNotApplicableReason, Long> getResponseValuePartitionNotApplicableReasons() {
        Map<ResponseValuePartitionNotApplicableReason, Long> reasons =
                new EnumMap<>(ResponseValuePartitionNotApplicableReason.class);
        for (ConnectionReport connectionReport : connectionReports) {
            ResponseValuePartitionCoverage coverage = connectionReport.getResponseValuePartitionCoverage();
            ResponseValuePartitionNotApplicableReason reason = coverage == null
                    ? ResponseValuePartitionNotApplicableReason.NO_RESPONSE_SCHEMA : coverage.getNotApplicableReason();
            if (reason != null) {
                reasons.merge(reason, 1L, Long::sum);
            }
        }
        return reasons;
    }

    /** Distinct values returned that the response schema does not allow, over all called connections. */
    public long getResponseValuePartitionOutOfSpecValueCount() {
        return connectionReports.stream()
                .map(ConnectionReport::getResponseValuePartitionCoverage)
                .filter(Objects::nonNull)
                .mapToInt(ResponseValuePartitionCoverage::getOutOfSpecValueCount)
                .sum();
    }

    private Double responseValuePartitionPooled(ToIntFunction<ResponseValuePartitionCoverage> covered,
                                                ToIntFunction<ResponseValuePartitionCoverage> all) {
        int allSum = applicableResponseValuePartitionCoverages().stream().mapToInt(all).sum();
        return allSum == 0 ? null
                : applicableResponseValuePartitionCoverages().stream().mapToInt(covered).sum() * 100.0 / allSum;
    }

    private List<ResponseValuePartitionCoverage> applicableResponseValuePartitionCoverages() {
        return connectionReports.stream()
                .map(ConnectionReport::getResponseValuePartitionCoverage)
                .filter(Objects::nonNull)
                .filter(coverage -> !coverage.isNotApplicable())
                .collect(Collectors.toList());
    }

    private Double responseFieldPooled(ToIntFunction<ResponseFieldCoverage> covered,
                                       ToIntFunction<ResponseFieldCoverage> all) {
        int allSum = applicableResponseFieldCoverages().stream().mapToInt(all).sum();
        return allSum == 0 ? null
                : applicableResponseFieldCoverages().stream().mapToInt(covered).sum() * 100.0 / allSum;
    }

    private List<ResponseFieldCoverage> applicableResponseFieldCoverages() {
        return connectionReports.stream()
                .map(ConnectionReport::getResponseFieldCoverage)
                .filter(Objects::nonNull)
                .filter(coverage -> !coverage.isNotApplicable())
                .collect(Collectors.toList());
    }

    private Double pooled(ToIntFunction<RequestValuePartitionCoverage> covered,
                          ToIntFunction<RequestValuePartitionCoverage> reachable) {
        int reachableSum = sum(reachable);
        return reachableSum == 0 ? null : sum(covered) * 100.0 / reachableSum;
    }

    private int sum(ToIntFunction<RequestValuePartitionCoverage> count) {
        return applicableRequestValuePartitionCoverages().stream().mapToInt(count).sum();
    }

    private List<RequestValuePartitionCoverage> applicableRequestValuePartitionCoverages() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestValuePartitionCoverage)
                .filter(Objects::nonNull)
                .filter(coverage -> !coverage.isNotApplicable())
                .collect(Collectors.toList());
    }

    private static Double median(List<Double> values) {
        List<Double> sorted = values.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
        if (sorted.isEmpty()) {
            return null;
        }
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private List<RequestElementCoverage> applicableRequestElementCoverages() {
        return connectionReports.stream()
                .map(ConnectionReport::getRequestElementCoverage)
                .filter(Objects::nonNull)
                .filter(coverage -> !coverage.isNotApplicable())
                .collect(Collectors.toList());
    }

    private static Double mean(List<Double> values) {
        List<Double> defined = values.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (defined.isEmpty()) {
            return null;
        }
        return defined.stream().mapToDouble(Double::doubleValue).sum() / defined.size();
    }
}
