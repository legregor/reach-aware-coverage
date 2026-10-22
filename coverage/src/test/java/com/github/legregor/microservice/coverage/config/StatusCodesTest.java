package com.github.legregor.microservice.coverage.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests for {@link StatusCodes}: HTTP entries and body outcome entries ({@code field=value}). */
class StatusCodesTest {

    private static final String SUCCESS = "{\"status\": 1, \"msg\": \"Success\", \"data\": {\"id\": \"a\"}}";
    private static final String FAILURE = "{\"status\": 0, \"msg\": \"Order Not Found\", \"data\": null}";

    @Test
    void kindsOfEntries() {
        assertTrue(StatusCodes.isHttpEntry("404"));
        assertTrue(StatusCodes.isHttpEntry("4XX"));
        assertFalse(StatusCodes.isHttpEntry("status=0"));
        assertTrue(StatusCodes.isBodyEntry("status=0"));
        assertTrue(StatusCodes.isBodyEntry("result.code=OK"));
        assertFalse(StatusCodes.isBodyEntry("=0"));
        assertFalse(StatusCodes.isBodyEntry("status="));
        assertFalse(StatusCodes.isBodyEntry("success"));
    }

    @Test
    void httpEntriesDoNotMatchBodyEntries() {
        assertTrue(StatusCodes.matches(404, "4xx"));
        assertFalse(StatusCodes.matches(200, "status=0"));
        assertEquals(List.of("2xx", "404"), StatusCodes.httpEntries(List.of("2xx", "status=0", "404")));
    }

    /** TrainTicket: always HTTP 200, the outcome is the body field status (1 = success, 0 = failure). */
    @Test
    void bodyOutcomes() {
        assertTrue(StatusCodes.matchesBody(SUCCESS, "status=1"));
        assertFalse(StatusCodes.matchesBody(SUCCESS, "status=0"));
        assertTrue(StatusCodes.matchesBody(FAILURE, "status=0"));
        assertTrue(StatusCodes.matchesBody(FAILURE, "data=null"));
        assertTrue(StatusCodes.matchesBody(SUCCESS, "data.id=a"));
        assertTrue(StatusCodes.matchesBody("{\"status\": 1.0}", "status=1"));
        assertTrue(StatusCodes.matchesBody("{\"ok\": true, \"code\": \"OK\"}", "ok=true"));
        assertTrue(StatusCodes.matchesBody("{\"ok\": true, \"code\": \"OK\"}", "code=OK"));
        assertFalse(StatusCodes.matchesBody("{\"other\": 1}", "status=1"));
        assertFalse(StatusCodes.matchesBody("not json", "status=1"));
        assertFalse(StatusCodes.matchesBody(null, "status=1"));
    }

    @Test
    void observedBodyOutcomesOfTheExpectedFields() {
        assertEquals(List.of("status=1", "status=0"),
                StatusCodes.bodyOutcomes(List.of(SUCCESS, FAILURE, SUCCESS), List.of("status=1", "status=0")));
        assertEquals(List.of(), StatusCodes.bodyOutcomes(List.of(SUCCESS), List.of("2xx", "4xx")));
    }
}
