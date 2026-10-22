package com.github.legregor.microservice.coverage.evaluation.bodySchema;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
/**
 * Per-request validation outcome including field-level presence diagnostics.
 */
public class RequestBodySchemaCheck {
    private String requestBody;
    private boolean matches;
    private String mismatchReason;
    private List<String> presentFields = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();
}
