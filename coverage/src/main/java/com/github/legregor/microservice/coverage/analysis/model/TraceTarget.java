package com.github.legregor.microservice.coverage.analysis.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = { "httpMethod", "url", "destinationService", "returnType" })
public class TraceTarget {
    private String httpMethod;
    private String url;
    private String destinationService;
    private String returnType;
    private Set<String> sentPathVariables = new LinkedHashSet<>();
    private Set<String> sentQueryParameters = new LinkedHashSet<>();
    private boolean requestBodySent;
    private boolean headersSent;

    // --- request body reachability (top-level properties, JSON names) ---

    /** Kind of the request body. */
    private BodyKind bodyKind = BodyKind.NONE;
    /** Type of the sent object (as resolved from the caller's source), null if unknown. */
    private String bodyType;
    /** All serializable top-level properties of the caller's DTO (upper bound). Empty for NONE / VALUE. */
    private Set<String> bodyProperties = new LinkedHashSet<>();
    /** Properties the caller can send with a non-null value. */
    private Set<String> reachableBodyProperties = new LinkedHashSet<>();
    /** bodyProperties minus reachableBodyProperties. */
    private Set<String> notReachableBodyProperties = new LinkedHashSet<>();
    /** Short explanations where the sent object comes from. */
    private Set<String> bodyOrigins = new LinkedHashSet<>();
    /** Reachable properties whose every assignment is a constant: the constants (JSON text). */
    private Map<String, List<String>> bodyConstantValues = new LinkedHashMap<>();
    /** Properties that can never be null (primitive fields). */
    private Set<String> neverNullBodyProperties = new LinkedHashSet<>();

    /**
     * Merges two targets of the same call (same method, URL, destination and return type), e.g. two call sites
     * in one endpoint. Neither input is changed; sets are united, flags or-ed, "all reachable" wins naturally.
     */
    public static TraceTarget merge(TraceTarget first, TraceTarget second) {
        TraceTarget merged = new TraceTarget();
        merged.setHttpMethod(first.getHttpMethod());
        merged.setUrl(first.getUrl());
        merged.setDestinationService(first.getDestinationService());
        merged.setReturnType(first.getReturnType());
        merged.setSentPathVariables(union(first.getSentPathVariables(), second.getSentPathVariables()));
        merged.setSentQueryParameters(union(first.getSentQueryParameters(), second.getSentQueryParameters()));
        merged.setRequestBodySent(first.isRequestBodySent() || second.isRequestBodySent());
        merged.setHeadersSent(first.isHeadersSent() || second.isHeadersSent());
        merged.setBodyKind(mergeKind(first.getBodyKind(), second.getBodyKind()));
        merged.setBodyType(first.getBodyType() != null ? first.getBodyType() : second.getBodyType());
        merged.setBodyProperties(union(first.getBodyProperties(), second.getBodyProperties()));
        merged.setReachableBodyProperties(union(first.getReachableBodyProperties(), second.getReachableBodyProperties()));
        Set<String> notReachable = new LinkedHashSet<>(merged.getBodyProperties());
        notReachable.removeAll(merged.getReachableBodyProperties());
        merged.setNotReachableBodyProperties(notReachable);
        merged.setBodyOrigins(union(first.getBodyOrigins(), second.getBodyOrigins()));
        merged.setBodyConstantValues(BodyConstants.merge(first.getReachableBodyProperties(),
                first.getBodyConstantValues(), second.getReachableBodyProperties(), second.getBodyConstantValues()));
        merged.setNeverNullBodyProperties(BodyConstants.mergeNeverNull(first.getBodyProperties(),
                first.getNeverNullBodyProperties(), second.getBodyProperties(), second.getNeverNullBodyProperties()));
        return merged;
    }

    private static BodyKind mergeKind(BodyKind first, BodyKind second) {
        if (first == BodyKind.OBJECT || second == BodyKind.OBJECT) {
            return BodyKind.OBJECT;
        }
        if (first == BodyKind.VALUE || second == BodyKind.VALUE) {
            return BodyKind.VALUE;
        }
        return BodyKind.NONE;
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new LinkedHashSet<>();
        if (first != null) {
            result.addAll(first);
        }
        if (second != null) {
            result.addAll(second);
        }
        return result;
    }
}
