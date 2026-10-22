package com.github.legregor.microservice.coverage.evaluation.responseValuePartition;

import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldNotApplicableReason;

/** Why the response value partition coverage of a called connection cannot be computed (n/a). */
public enum ResponseValuePartitionNotApplicableReason {

    /** Response field coverage is n/a: the callee produced no server spans (e.g. it is a mock). */
    CALLEE_NOT_TRACED(ResponseFieldNotApplicableReason.CALLEE_NOT_TRACED),

    /** Response field coverage is n/a: every returned status code is unexpected for the connection. */
    NO_EXPECTED_STATUS_RETURNED(ResponseFieldNotApplicableReason.NO_EXPECTED_STATUS_RETURNED),

    /** Response field coverage is n/a: the spec documents none of the returned status codes. */
    NO_RESPONSE_SCHEMA(ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA),

    /** Response field coverage is n/a: the documented responses have no fields. */
    NO_RESPONSE_FIELDS(ResponseFieldNotApplicableReason.NO_RESPONSE_FIELDS),

    /** Response field coverage is n/a: no response body was captured. */
    BODY_NOT_CAPTURED(ResponseFieldNotApplicableReason.BODY_NOT_CAPTURED),

    /** Responses were judged, but no response field was returned with a value. */
    NO_COVERED_FIELDS("no response field was returned with a value", "no covered response fields"),

    /** The covered response fields have no value partitions (e.g. only objects without own partitions). */
    NO_PARTITIONS("the covered response fields have no value partitions", "no partitions");

    private final String description;
    private final String label;

    ResponseValuePartitionNotApplicableReason(ResponseFieldNotApplicableReason reason) {
        this(reason.getDescription(), reason.getLabel());
    }

    ResponseValuePartitionNotApplicableReason(String description, String label) {
        this.description = description;
        this.label = label;
    }

    /** The reason that follows from an n/a response field coverage. */
    public static ResponseValuePartitionNotApplicableReason from(ResponseFieldNotApplicableReason reason) {
        return valueOf(reason.name());
    }

    /** Short label for counts in summaries. */
    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
