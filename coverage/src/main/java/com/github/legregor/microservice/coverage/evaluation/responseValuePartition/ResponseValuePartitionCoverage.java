package com.github.legregor.microservice.coverage.evaluation.responseValuePartition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.legregor.microservice.coverage.evaluation.HierarchicalCoverage;
import com.github.legregor.microservice.coverage.evaluation.requestValuePartition.ElementPartitionCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ObservedResponse;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseField;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.StatusResponse;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.PartitionGenerator;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ValueClassifier;

/**
 * Response value partition coverage of one called connection.
 * <p>
 * Scope: the covered response fields of the judged status codes of {@link ResponseFieldCoverage} (relevant returned
 * status codes with a documented schema and a captured body); fields never returned with a value are gaps of
 * response field coverage and are skipped here. Coverage items are the value partitions of these fields, derived
 * from the callee's response schema of the status code with the same generator as for requests; every partition is
 * reachable (no analysis of the callee). A partition is covered if at least one value of the field, in a response
 * with that status code, falls into it. Values the schema does not allow are listed per field and not counted.
 * <ul>
 * <li>Pooled: covered / all partitions.</li>
 * <li>Hierarchical: per status code, a field's own partitions count as one value (covered / all), averaged with the
 * values of its covered children; the value of a status code is the mean over its top-level fields, the connection
 * value the mean over its status codes.</li>
 * </ul>
 * Items are named {@code <status>:<path>}, their partitions {@code <status>:<path>#<partition>}.
 * All percentages are 0-100, or null (n/a).
 */
public class ResponseValuePartitionCoverage {

    /** The partitions of the covered fields of one status code, depth-first. */
    public record StatusPartitionCoverage(int statusCode, String specKey, List<ElementPartitionCoverage> fields) {
    }

    private final List<StatusPartitionCoverage> statusCodes;
    private final Double hierarchical;
    private final ResponseValuePartitionNotApplicableReason notApplicableReason;

    private ResponseValuePartitionCoverage(List<StatusPartitionCoverage> statusCodes, Double hierarchical,
                                           ResponseValuePartitionNotApplicableReason notApplicableReason) {
        this.statusCodes = List.copyOf(statusCodes);
        this.hierarchical = hierarchical;
        this.notApplicableReason = notApplicableReason;
    }

    /**
     * @param fieldCoverage response field coverage of the connection: defines the covered fields and the responses
     *                      whose values are classified (those of a relevant response class)
     */
    public static ResponseValuePartitionCoverage of(ResponseFieldCoverage fieldCoverage, PartitionGenerator generator,
                                                    ValueClassifier classifier) {
        if (fieldCoverage == null) {
            return new ResponseValuePartitionCoverage(List.of(), null,
                    ResponseValuePartitionNotApplicableReason.NO_RESPONSE_SCHEMA);
        }
        if (fieldCoverage.isNotApplicable()) {
            return new ResponseValuePartitionCoverage(List.of(), null,
                    ResponseValuePartitionNotApplicableReason.from(fieldCoverage.getNotApplicableReason()));
        }
        ExecutedResponseValuesCollector collector = new ExecutedResponseValuesCollector();
        List<StatusPartitionCoverage> statusCodes = new ArrayList<>();
        List<Double> statusValues = new ArrayList<>();
        for (StatusResponse response : fieldCoverage.getJudgedResponses()) {
            int statusCode = response.getStatusCode();
            List<String> bodies = fieldCoverage.getSelectedResponses().stream()
                    .filter(candidate -> candidate.statusCode() == statusCode && candidate.hasBody())
                    .map(ObservedResponse::body).collect(Collectors.toList());
            Map<String, List<JsonNode>> values = collector.collect(response.getBodySchema(), bodies);

            Map<String, ElementPartitionCoverage> byPath = new LinkedHashMap<>();
            for (ResponseField field : response.allFields()) {
                if (fieldCoverage.isCovered(statusCode, field.getName())) {
                    byPath.put(field.getName(), ElementPartitionCoverage.ofField(statusCode + ":" + field.getName(),
                            field.getSchemaField(), values.getOrDefault(field.getName(), List.of()), generator,
                            classifier));
                }
            }
            if (byPath.isEmpty()) {
                continue;
            }
            statusCodes.add(new StatusPartitionCoverage(statusCode, response.getSpecKey(),
                    List.copyOf(byPath.values())));

            List<Double> topLevel = new ArrayList<>();
            for (ResponseField field : response.getTopLevelFields()) {
                topLevel.add(hierarchical(field, byPath));
            }
            statusValues.add(HierarchicalCoverage.meanOfDefined(topLevel));
        }
        Double hierarchical = HierarchicalCoverage.meanOfDefined(statusValues);

        ResponseValuePartitionNotApplicableReason reason = null;
        if (statusCodes.isEmpty()) {
            reason = ResponseValuePartitionNotApplicableReason.NO_COVERED_FIELDS;
        } else if (statusCodes.stream().flatMap(status -> status.fields().stream())
                .mapToInt(ElementPartitionCoverage::getReachableCount).sum() == 0) {
            reason = ResponseValuePartitionNotApplicableReason.NO_PARTITIONS;
        }
        return new ResponseValuePartitionCoverage(statusCodes, hierarchical == null ? null : hierarchical * 100.0,
                reason);
    }

    /** Per judged status code with covered fields: the partitions of each covered field. */
    public List<StatusPartitionCoverage> getStatusCodes() {
        return statusCodes;
    }

    /** All covered fields of all status codes, named {@code <status>:<path>} (in the JSON via the status codes). */
    @JsonIgnore
    public List<ElementPartitionCoverage> getFields() {
        return statusCodes.stream().flatMap(status -> status.fields().stream()).collect(Collectors.toList());
    }

    /** Covered / all partitions of the covered fields, null if n/a. */
    public Double getPooledCoveragePercentage() {
        return percentage(sum(ElementPartitionCoverage::getCoveredCount), sum(ElementPartitionCoverage::getReachableCount));
    }

    /** Pooled over the partitions that come from spec constraints only (no type defaults), null if there are none. */
    public Double getConstraintPooledCoveragePercentage() {
        return percentage(sum(ElementPartitionCoverage::getConstraintCoveredCount),
                sum(ElementPartitionCoverage::getConstraintReachableCount));
    }

    public Double getHierarchicalCoveragePercentage() {
        return isNotApplicable() ? null : hierarchical;
    }

    public boolean isNotApplicable() {
        return notApplicableReason != null;
    }

    /** Why the coverage is n/a, null if it is applicable. */
    public ResponseValuePartitionNotApplicableReason getNotApplicableReason() {
        return notApplicableReason;
    }

    /** Number of partitions (all are reachable: no analysis of the callee). */
    public int getPartitionCount() {
        return sum(ElementPartitionCoverage::getReachableCount);
    }

    public int getCoveredCount() {
        return sum(ElementPartitionCoverage::getCoveredCount);
    }

    public int getConstraintPartitionCount() {
        return sum(ElementPartitionCoverage::getConstraintReachableCount);
    }

    public int getConstraintCoveredCount() {
        return sum(ElementPartitionCoverage::getConstraintCoveredCount);
    }

    /** Number of distinct values returned outside the spec (per field, value and reason). */
    public int getOutOfSpecValueCount() {
        return getFields().stream().mapToInt(field -> field.getOutOfSpecValues().size()).sum();
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Hierarchical value (0-1) of a field, null if it is not covered or has nothing to judge. */
    private static Double hierarchical(ResponseField field, Map<String, ElementPartitionCoverage> byPath) {
        ElementPartitionCoverage coverage = byPath.get(field.getName());
        if (coverage == null) {
            return null;
        }
        List<Double> parts = new ArrayList<>();
        if (coverage.getReachableCount() > 0) {
            parts.add((double) coverage.getCoveredCount() / coverage.getReachableCount());
        }
        for (ResponseField child : field.getChildren()) {
            Double childValue = hierarchical(child, byPath);
            if (childValue != null) {
                parts.add(childValue);
            }
        }
        return HierarchicalCoverage.meanOfDefined(parts);
    }

    private int sum(ToIntFunction<ElementPartitionCoverage> count) {
        return getFields().stream().mapToInt(count).sum();
    }

    private Double percentage(int covered, int all) {
        return isNotApplicable() || all == 0 ? null : covered * 100.0 / all;
    }
}
