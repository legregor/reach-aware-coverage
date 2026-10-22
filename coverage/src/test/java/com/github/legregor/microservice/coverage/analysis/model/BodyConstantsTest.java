package com.github.legregor.microservice.coverage.analysis.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for {@link BodyConstants}: merging constant values and never-null properties of several calls. */
class BodyConstantsTest {

    @Test
    void constantsAreUnitedAndAnyValueWins() {
        Map<String, List<String>> merged = BodyConstants.merge(
                List.of("type", "note", "level"), Map.of("type", List.of("\"A\""), "level", List.of("1")),
                List.of("type", "level"), Map.of("type", List.of("\"B\"")));

        // type: constants in both calls; level: any value in the second call; note: any value in the first
        assertEquals(Map.of("type", List.of("\"A\"", "\"B\"")), merged);
    }

    @Test
    void aPropertyOnlyOneCallCanSendKeepsItsConstants() {
        Map<String, List<String>> merged = BodyConstants.merge(
                List.of("type"), Map.of("type", List.of("\"A\"")), List.of(), Map.of());

        assertEquals(Map.of("type", List.of("\"A\"")), merged);
    }

    @Test
    void neverNullOnlyIfEveryCallWithThePropertySaysSo() {
        Set<String> merged = BodyConstants.mergeNeverNull(
                List.of("count", "status", "flag"), List.of("count", "status"),
                List.of("count", "status"), List.of("count"));

        // status may be null in the second call; flag is never null nowhere
        assertEquals(Set.of("count"), merged);
    }
}
