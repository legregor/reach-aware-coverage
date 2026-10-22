package com.github.legregor.microservice.coverage.analysis.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merging of the static value information of request body properties over several calls (call sites, segments of
 * one connection). Over-approximation: a property keeps a constant set only if every call that can send it has
 * one; it is never null only if every call that has it says so.
 */
public final class BodyConstants {

    private BodyConstants() {
    }

    /**
     * @param reachableA properties call A can send with a value
     * @param constantsA constant values per property of call A (a reachable property missing here: any value)
     * @return constants per property after merging (union of the constants; any value wins)
     */
    public static Map<String, List<String>> merge(Collection<String> reachableA, Map<String, List<String>> constantsA,
                                                  Collection<String> reachableB, Map<String, List<String>> constantsB) {
        Map<String, List<String>> safeA = constantsA == null ? Map.of() : constantsA;
        Map<String, List<String>> safeB = constantsB == null ? Map.of() : constantsB;
        Set<String> reachable = new LinkedHashSet<>(nullSafe(reachableA));
        reachable.addAll(nullSafe(reachableB));
        Map<String, List<String>> merged = new LinkedHashMap<>();
        for (String property : reachable) {
            boolean knownA = !nullSafe(reachableA).contains(property) || safeA.containsKey(property);
            boolean knownB = !nullSafe(reachableB).contains(property) || safeB.containsKey(property);
            if (!knownA || !knownB) {
                continue;
            }
            Set<String> values = new LinkedHashSet<>(safeA.getOrDefault(property, List.of()));
            values.addAll(safeB.getOrDefault(property, List.of()));
            if (!values.isEmpty()) {
                merged.put(property, new ArrayList<>(values));
            }
        }
        return merged;
    }

    /**
     * @param propertiesA all properties of call A's body
     * @param neverNullA  properties of call A that are never null
     * @return properties that are never null in every call that has them
     */
    public static Set<String> mergeNeverNull(Collection<String> propertiesA, Collection<String> neverNullA,
                                             Collection<String> propertiesB, Collection<String> neverNullB) {
        Set<String> candidates = new LinkedHashSet<>(nullSafe(neverNullA));
        candidates.addAll(nullSafe(neverNullB));
        Set<String> merged = new LinkedHashSet<>();
        for (String property : candidates) {
            boolean okA = !nullSafe(propertiesA).contains(property) || nullSafe(neverNullA).contains(property);
            boolean okB = !nullSafe(propertiesB).contains(property) || nullSafe(neverNullB).contains(property);
            if (okA && okB) {
                merged.add(property);
            }
        }
        return merged;
    }

    private static Collection<String> nullSafe(Collection<String> values) {
        return values == null ? List.of() : values;
    }
}
