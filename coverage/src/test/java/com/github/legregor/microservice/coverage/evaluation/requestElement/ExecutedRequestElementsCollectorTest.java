package com.github.legregor.microservice.coverage.evaluation.requestElement;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link ExecutedRequestElementsCollector}: which request elements got a value during the tests. */
class ExecutedRequestElementsCollectorTest {

    private final ExecutedRequestElementsCollector collector = new ExecutedRequestElementsCollector();

    @Test
    void pathParametersAreExecutedWhenTheConnectionWasCalled() {
        ExecutedRequestElements called = collector.collect("/drawback/{userId}/{money}", 2, List.of());
        assertEquals(Set.of("path:userId", "path:money"), called.getNames());
        assertEquals(2, called.getCallCount());

        ExecutedRequestElements notCalled = collector.collect("/drawback/{userId}/{money}", 0, List.of());
        assertTrue(notCalled.getNames().isEmpty());
    }

    @Test
    void callsWithServerSpanAreKept() {
        ExecutedRequestElements mocked = collector.collect("/notification", 1, 0, List.of());
        assertEquals(1, mocked.getCallCount());
        assertEquals(0, mocked.getCallsWithServerSpan());

        // without the count, every call is assumed to have a server span
        assertEquals(2, collector.collect("/orders", 2, List.of()).getCallsWithServerSpan());
    }

    @Test
    void nonNullTopLevelFieldsAreExecutedNullOnesAreNot() {
        ExecutedRequestElements executed = collector.collect("/orders", 1, List.of("""
                {"id": "e9cb", "contactsName": null, "coachNumber": 0, "seatNumber": ""}
                """));

        assertEquals(Set.of("body:id", "body:coachNumber", "body:seatNumber"), executed.getNames());
        assertEquals(1, executed.getCallsWithBody());
    }

    @Test
    void nestedObjectsAndListItems() {
        ExecutedRequestElements executed = collector.collect("/travel", 1, List.of("""
                {"trip": {"tripId": {"type": "G", "number": null}, "routeId": "r1"},
                 "items": [{"name": "a", "amount": null}, {"name": null, "amount": 2}],
                 "tags": [],
                 "address": {}}
                """));

        assertEquals(Set.of("body:trip", "body:trip.tripId", "body:trip.tripId.type", "body:trip.routeId",
                "body:items", "body:items[].name", "body:items[].amount",
                "body:tags", "body:address"), executed.getNames());
    }

    @Test
    void elementsAreUnitedOverAllCalls() {
        ExecutedRequestElements executed = collector.collect("/notify/{type}", 3, Arrays.asList(
                "{\"email\": \"a@b.c\", \"username\": null}",
                null,
                "{\"email\": null, \"username\": \"lena\"}"));

        assertEquals(Set.of("path:type", "body:email", "body:username"), executed.getNames());
        assertEquals(3, executed.getCallCount());
        assertEquals(2, executed.getCallsWithBody(), "one call had no captured body");
    }

    @Test
    void nonObjectBodies() {
        assertEquals(Set.of("body"), collector.collect("/namelist", 1, List.of("[\"shanghai\", \"beijing\"]")).getNames());
        assertEquals(Set.of("body", "body[].name"),
                collector.collect("/batch", 1, List.of("[{\"name\": \"x\"}, {\"name\": null}]")).getNames());
        assertEquals(Set.of("body"), collector.collect("/text", 1, List.of("plain text, not JSON")).getNames());
        assertTrue(collector.collect("/null", 1, List.of("null")).getNames().isEmpty());
    }

    @Test
    void trainTicketOrderBodyFromTheSpans() {
        // request body of PUT /api/v1/orderservice/order as recorded in processed-spans.json
        String body = "{\"id\":\"e9cb042e-54d3-4b56-b5fa-67ff1c1c992b\",\"boughtDate\":\"2026-05-12T16:01:15.094+00:00\","
                + "\"travelDate\":\"1970-01-03T23:24:49.287+00:00\",\"travelTime\":\"1970-01-02T10:17:36.799+00:00\","
                + "\"accountId\":\"4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f\",\"contactsName\":\"Contacts_One\",\"documentType\":1,"
                + "\"contactsDocumentNumber\":\"DocumentNumber_One\",\"trainNumber\":\"G1235\",\"coachNumber\":5,"
                + "\"seatClass\":0,\"seatNumber\":\"FirstClass-30\",\"from\":\"shanghai\",\"to\":\"beijing\",\"status\":4,"
                + "\"price\":\"100.0\"}";

        ExecutedRequestElements executed = collector.collect("/api/v1/orderservice/order", 1, List.of(body));

        assertEquals(16, executed.getNames().size());
        assertTrue(executed.contains("body:travelTime"));
    }
}
