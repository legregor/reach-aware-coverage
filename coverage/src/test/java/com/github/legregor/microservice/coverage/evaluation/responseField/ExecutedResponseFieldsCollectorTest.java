package com.github.legregor.microservice.coverage.evaluation.responseField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Tests for {@link ExecutedResponseFieldsCollector}: field paths with a value, per status code. */
class ExecutedResponseFieldsCollectorTest {

    @Test
    void nestedObjectsListsAndNullValues() {
        Set<String> paths = ExecutedResponseFieldsCollector.fromBody("""
                {"message": "invalid", "hint": null, "address": {"zip": "80333", "city": null},
                 "fieldErrors": [{"field": "items[0].quantity", "message": "must be >= 1"}, {"field": "x"}]}
                """);

        assertEquals(Set.of("message", "address", "address.zip", "fieldErrors", "fieldErrors[].field",
                "fieldErrors[].message"), paths);
    }

    @Test
    void topLevelListAndNonJsonBody() {
        assertEquals(Set.of("[]", "[].sku"), ExecutedResponseFieldsCollector.fromBody("[{\"sku\": \"ABC-0001\"}]"));
        assertTrue(ExecutedResponseFieldsCollector.fromBody("Internal Server Error").isEmpty());
    }

    @Test
    void valuesAndBodiesArePerStatusCode() {
        ExecutedResponseFields executed = new ExecutedResponseFieldsCollector().collect(List.of(
                new ObservedResponse(201, "{\"reservationId\": \"r-1\", \"estimatedDelivery\": \"2026-10-11\"}"),
                new ObservedResponse(201, "{\"reservationId\": \"r-2\", \"estimatedDelivery\": null}"),
                new ObservedResponse(409, null)));

        assertTrue(executed.contains(201, "estimatedDelivery"));
        assertFalse(executed.contains(409, "message"));
        assertEquals(2, executed.getResponseCount(201));
        assertEquals(2, executed.getBodyCount(201));
        assertEquals(1, executed.getResponseCount(409));
        assertEquals(0, executed.getBodyCount(409));
    }
}
