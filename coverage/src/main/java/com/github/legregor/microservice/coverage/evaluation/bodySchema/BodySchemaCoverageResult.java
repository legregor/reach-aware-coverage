package com.github.legregor.microservice.coverage.evaluation.bodySchema;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
/**
 * Aggregated result of request-body schema coverage for one connection.
 */
public class BodySchemaCoverageResult {
    private final boolean applicable;
    private final int checkedCount;
    private final int matchedCount;
    private final int missingCount;
    private final String mismatchReason;
    private final JsonNode schema;
    private final List<RequestBodySchemaCheck> requestChecks;

    public BodySchemaCoverageResult(boolean applicable, int checkedCount, int matchedCount, int missingCount,
            String mismatchReason, JsonNode schema, List<RequestBodySchemaCheck> requestChecks) {
        this.applicable = applicable;
        this.checkedCount = checkedCount;
        this.matchedCount = matchedCount;
        this.missingCount = missingCount;
        this.mismatchReason = mismatchReason;
        this.schema = schema;
        this.requestChecks = requestChecks == null ? new ArrayList<>() : requestChecks;
    }

    /**
     * Standard result used when no body-schema evaluation can be performed.
     */
    public static BodySchemaCoverageResult notApplicable() {
        return new BodySchemaCoverageResult(false, 0, 0, 0, null, null, new ArrayList<>());
    }

}
