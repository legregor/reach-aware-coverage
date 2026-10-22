package com.github.legregor.microservice.coverage.evaluation.responseField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.legregor.microservice.coverage.config.StatusCodes;
import com.github.legregor.microservice.coverage.evaluation.HierarchicalCoverage;

/**
 * Response field coverage of one called connection.
 * <p>
 * Only responses of a <em>relevant response class</em> count: a response that matches an expected entry of the
 * connection (coverage config, default {@code 2xx} and {@code 4xx}), an HTTP entry by its status code, a body entry
 * ({@code status=0}) by its body. They are grouped by HTTP status code (the response variant): coverage items are the
 * fields of the documented response schemas of these status codes. A status code that no relevant response had
 * contributes no fields (whether that is a gap is decided by response class coverage). An item is covered if at least
 * one relevant response with its status code carried the field with a non-null value. A response without captured
 * body whose class depends on the body counts as "body not recorded"; a status code whose relevant responses carried
 * no captured body is not judged.
 * <p>
 * All percentages are 0-100, or {@code null} (n/a) if there is no item to judge.
 */
public class ResponseFieldCoverage {

    /** The coverage of one relevant status code, for reports and the JSON output. */
    public record StatusFieldCoverage(int statusCode, String specKey, int responseCount, boolean bodyRecorded,
                                      List<String> coveredFields, List<String> uncoveredFields,
                                      List<String> requiredFields, List<String> returnedNotInSpec) {
    }

    private final List<StatusResponse> responses;
    private final ExecutedResponseFields executed;
    private final boolean anyServerSpan;
    private final List<Integer> ignoredStatusCodes;
    private final List<ObservedResponse> selectedResponses;

    /**
     * @param responses          the documented responses of the relevant status codes
     * @param executed           the recorded response values per status code
     * @param anyServerSpan      whether at least one call of the connection has a server span of the callee
     * @param ignoredStatusCodes returned status codes that are not expected for the connection (not judged)
     */
    public ResponseFieldCoverage(List<StatusResponse> responses, ExecutedResponseFields executed,
                                 boolean anyServerSpan, List<Integer> ignoredStatusCodes) {
        this(responses, executed, anyServerSpan, ignoredStatusCodes, List.of());
    }

    /**
     * @param selectedResponses the recorded responses of a relevant response class (the basis of the coverage, also
     *                          of response value partition coverage)
     */
    public ResponseFieldCoverage(List<StatusResponse> responses, ExecutedResponseFields executed,
                                 boolean anyServerSpan, List<Integer> ignoredStatusCodes,
                                 List<ObservedResponse> selectedResponses) {
        this.responses = List.copyOf(responses);
        this.executed = executed;
        this.anyServerSpan = anyServerSpan;
        this.ignoredStatusCodes = List.copyOf(ignoredStatusCodes);
        this.selectedResponses = List.copyOf(selectedResponses);
    }

    /**
     * Response field coverage of a connection from its recorded responses.
     *
     * @param observed  responses recorded in the callee's server spans
     * @param expected  expected status codes / classes of the connection
     * @param builder   builds the documented responses of the relevant status codes
     * @param service   called service
     * @param method    HTTP method of the called endpoint
     * @param path      path (template) of the called endpoint
     */
    public static ResponseFieldCoverage of(List<ObservedResponse> observed, List<String> expected,
                                           ResponseFieldTreeBuilder builder, String service, String method,
                                           String path) {
        return of(observed, List.of(), expected, builder, service, method, path);
    }

    /**
     * Response field coverage of a connection from its recorded responses, with the status codes of all calls.
     *
     * @param observed          responses recorded in the callee's server spans
     * @param returnedByClients status codes of all calls as seen by the caller (client spans), including calls
     *                          whose callee was not traced; only used to list the ignored status codes
     * @param expected          expected status codes / classes of the connection
     * @param builder           builds the documented responses of the relevant status codes
     * @param service           called service
     * @param method            HTTP method of the called endpoint
     * @param path              path (template) of the called endpoint
     */
    public static ResponseFieldCoverage of(List<ObservedResponse> observed, List<Integer> returnedByClients,
                                           List<String> expected, ResponseFieldTreeBuilder builder, String service,
                                           String method, String path) {
        // responses of a relevant class; a response whose class depends on its body, but whose body was not captured,
        // is kept, so that its status code is reported as "body not recorded" instead of disappearing
        List<ObservedResponse> selected = observed.stream()
                .filter(response -> StatusCodes.matchesAnyClass(response.statusCode(), response.body(), expected)
                        || StatusCodes.classUnknownWithoutBody(response.statusCode(), response.body(), expected))
                .collect(Collectors.toList());
        Set<Integer> relevantCodes = new TreeSet<>();
        selected.forEach(response -> relevantCodes.add(response.statusCode()));
        List<Integer> relevant = new ArrayList<>(relevantCodes);

        // ignored: returned status codes without a response of a relevant class; a code seen only by the caller
        // (callee not traced) is unknown, not ignored, if its class depends on the body
        boolean bodyEntries = expected != null && expected.stream().anyMatch(StatusCodes::isBodyEntry);
        Set<Integer> observedCodes = new TreeSet<>();
        observed.forEach(response -> observedCodes.add(response.statusCode()));
        Set<Integer> allReturned = new TreeSet<>(observedCodes);
        returnedByClients.stream().filter(java.util.Objects::nonNull).forEach(allReturned::add);
        List<Integer> ignored = allReturned.stream()
                .filter(code -> !relevantCodes.contains(code))
                .filter(code -> observedCodes.contains(code) || !(StatusCodes.matchesAny(code, expected) || bodyEntries))
                .collect(Collectors.toList());
        return new ResponseFieldCoverage(builder.build(service, method, path, relevant),
                new ExecutedResponseFieldsCollector().collect(selected), !observed.isEmpty(), ignored, selected);
    }

    // ---------------------------------------------------------------------------------------------
    // Coverage values
    // ---------------------------------------------------------------------------------------------

    /** Pooled: covered fields / fields of the judged status codes. */
    public Double getPooledCoveragePercentage() {
        return pooled(false);
    }

    /** Pooled over the optional fields only (a valid response always carries its required fields). */
    public Double getOptionalPooledCoveragePercentage() {
        return pooled(true);
    }

    /**
     * Hierarchical: one tree per judged status code (computed as for request elements); the connection value is the
     * mean over the status codes, so every returned response weighs the same.
     */
    public Double getHierarchicalCoveragePercentage() {
        List<Double> values = new ArrayList<>();
        for (StatusResponse response : judgedResponses()) {
            values.add(HierarchicalCoverage.of(response.getTopLevelFields(), ResponseField::getChildren,
                    field -> true, field -> isCovered(response, field)));
        }
        Double mean = HierarchicalCoverage.meanOfDefined(values);
        return mean == null ? null : mean * 100.0;
    }

    public boolean isNotApplicable() {
        return getFieldCount() == 0;
    }

    /** Why the coverage of this connection is n/a, or null if it is applicable. */
    public ResponseFieldNotApplicableReason getNotApplicableReason() {
        if (!isNotApplicable()) {
            return null;
        }
        if (!anyServerSpan) {
            return ResponseFieldNotApplicableReason.CALLEE_NOT_TRACED;
        }
        if (responses.isEmpty()) {
            return ResponseFieldNotApplicableReason.NO_EXPECTED_STATUS_RETURNED;
        }
        if (responses.stream().noneMatch(StatusResponse::isDocumented)) {
            return ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA;
        }
        if (responses.stream().allMatch(response -> response.getTopLevelFields().isEmpty())) {
            return ResponseFieldNotApplicableReason.NO_RESPONSE_FIELDS;
        }
        return ResponseFieldNotApplicableReason.BODY_NOT_CAPTURED;
    }

    // ---------------------------------------------------------------------------------------------
    // Counts and lists
    // ---------------------------------------------------------------------------------------------

    public int getFieldCount() {
        return items(false).size();
    }

    public int getCoveredCount() {
        return (int) items(false).stream().filter(Item::covered).count();
    }

    public int getOptionalFieldCount() {
        return items(true).size();
    }

    public int getOptionalCoveredCount() {
        return (int) items(true).stream().filter(Item::covered).count();
    }

    /** Covered fields as {@code status:path}, e.g. {@code 201:reservationId}. */
    public List<String> getCoveredFields() {
        return items(false).stream().filter(Item::covered).map(Item::label).collect(Collectors.toList());
    }

    /** Uncovered fields (test gaps) as {@code status:path}. */
    public List<String> getUncoveredFields() {
        return items(false).stream().filter(item -> !item.covered()).map(Item::label).collect(Collectors.toList());
    }

    /** Per relevant status code: documented key, covered and uncovered fields, fields returned but not in the spec. */
    public List<StatusFieldCoverage> getStatusCodes() {
        List<StatusFieldCoverage> result = new ArrayList<>();
        for (StatusResponse response : responses) {
            List<String> covered = new ArrayList<>();
            List<String> uncovered = new ArrayList<>();
            List<String> required = new ArrayList<>();
            boolean judged = isJudged(response);
            for (ResponseField field : response.allFields()) {
                if (field.isRequired()) {
                    required.add(field.getName());
                }
                if (judged) {
                    (isCovered(response, field) ? covered : uncovered).add(field.getName());
                }
            }
            result.add(new StatusFieldCoverage(response.getStatusCode(), response.getSpecKey(),
                    executed.getResponseCount(response.getStatusCode()),
                    executed.getBodyCount(response.getStatusCode()) > 0, covered, uncovered, required,
                    returnedNotInSpec(response)));
        }
        return result;
    }

    /** Returned status codes without a response of a relevant class; their responses are not judged. */
    public List<Integer> getIgnoredStatusCodes() {
        return ignoredStatusCodes;
    }

    /** Relevant status codes for which the spec documents no response. */
    public List<Integer> getUndocumentedStatusCodes() {
        return responses.stream().filter(response -> !response.isDocumented()).map(StatusResponse::getStatusCode)
                .collect(Collectors.toList());
    }

    /** Relevant status codes with fields whose responses carried no captured body (not judged). */
    public List<Integer> getBodyNotRecordedStatusCodes() {
        return responses.stream()
                .filter(response -> !response.getTopLevelFields().isEmpty())
                .filter(response -> executed.getBodyCount(response.getStatusCode()) == 0)
                .map(StatusResponse::getStatusCode).collect(Collectors.toList());
    }

    /** Number of fields returned but not in the spec, over all documented relevant status codes. */
    public int getReturnedNotInSpecCount() {
        return responses.stream().mapToInt(response -> returnedNotInSpec(response).size()).sum();
    }

    /**
     * The judged responses (documented status codes with fields and at least one captured body), whose fields are
     * the coverage items. Used by response value partition coverage.
     */
    @JsonIgnore
    public List<StatusResponse> getJudgedResponses() {
        return judgedResponses();
    }

    /**
     * The recorded responses of a relevant response class, the basis of this coverage. Response value partition
     * coverage classifies the values of exactly these responses.
     */
    @JsonIgnore
    public List<ObservedResponse> getSelectedResponses() {
        return selectedResponses;
    }

    /** True if the field had a value in at least one response with this status code (a covered response field). */
    public boolean isCovered(int statusCode, String fieldPath) {
        return executed.contains(statusCode, fieldPath);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private record Item(StatusResponse response, ResponseField field, boolean covered) {
        String label() {
            return response.getStatusCode() + ":" + field.getName();
        }
    }

    private List<Item> items(boolean optionalOnly) {
        List<Item> items = new ArrayList<>();
        for (StatusResponse response : judgedResponses()) {
            for (ResponseField field : response.allFields()) {
                if (!optionalOnly || !field.isRequired()) {
                    items.add(new Item(response, field, isCovered(response, field)));
                }
            }
        }
        return items;
    }

    private Double pooled(boolean optionalOnly) {
        List<Item> items = items(optionalOnly);
        if (items.isEmpty()) {
            return null;
        }
        long covered = items.stream().filter(Item::covered).count();
        return (double) covered / items.size() * 100.0;
    }

    /** Documented status codes with fields and at least one captured body. */
    private List<StatusResponse> judgedResponses() {
        return responses.stream().filter(this::isJudged).collect(Collectors.toList());
    }

    private boolean isJudged(StatusResponse response) {
        return response.isDocumented() && !response.getTopLevelFields().isEmpty()
                && executed.getBodyCount(response.getStatusCode()) > 0;
    }

    private boolean isCovered(StatusResponse response, ResponseField field) {
        return executed.contains(response.getStatusCode(), field.getName());
    }

    private List<String> returnedNotInSpec(StatusResponse response) {
        if (!response.isDocumented()) {
            return List.of();
        }
        Set<String> known = new LinkedHashSet<>();
        response.allFields().forEach(field -> known.add(field.getName()));
        return executed.getPaths(response.getStatusCode()).stream().filter(path -> !known.contains(path))
                .collect(Collectors.toList());
    }
}
