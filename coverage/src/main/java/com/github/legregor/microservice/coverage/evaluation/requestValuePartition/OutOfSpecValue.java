package com.github.legregor.microservice.coverage.evaluation.requestValuePartition;

/**
 * A value sent during the tests that the spec does not allow (developer information, not counted in the coverage).
 *
 * @param value  the value as JSON text, or "(no value)" for absent / null
 * @param reason why the value is out of spec, e.g. "below minimum 1"
 * @param count  how often it was sent
 */
public record OutOfSpecValue(String value, String reason, int count) {
}
