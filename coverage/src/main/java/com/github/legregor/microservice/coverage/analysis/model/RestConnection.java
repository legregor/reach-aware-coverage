package com.github.legregor.microservice.coverage.analysis.model;

import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A connection from a calling service (source) to an endpoint of another service (destination).
 * <p>
 * - call: the REST call as implemented in the source service (method, called URL, expected return type)
 * - endpoint: the endpoint of the destination service that receives the call
 * - calls: all distinct call implementations in the source service that reach this endpoint.
 *   {@code call} is the first (representative) one; {@code calls} always contains it.
 * <p>
 * Request elements (see {@link RequestElements} for the names) come from the static analysis of the caller:
 * path parameters of the endpoint and the top-level properties of the request body the caller sends.
 * Equality only uses source, destination, call and endpoint.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class RestConnection {
    private String source;
    private String destination;
    private RestCall call;
    private RestEndpoint endpoint;

    @EqualsAndHashCode.Exclude
    private List<RestCall> calls = new ArrayList<>();

    /** All request elements of this connection as seen from the caller, e.g. "path:orderId", "body:email". */
    @EqualsAndHashCode.Exclude
    private List<String> requestElements = new ArrayList<>();

    /** Request elements the caller can send with a value (reachable). */
    @EqualsAndHashCode.Exclude
    private List<String> reachableRequestElements = new ArrayList<>();

    /** Request elements the caller never sends with a value (requestElements minus reachableRequestElements). */
    @EqualsAndHashCode.Exclude
    private List<String> notReachableRequestElements = new ArrayList<>();

    /** Type of the request body as sent by the caller, null if no body is sent. */
    @EqualsAndHashCode.Exclude
    private String requestBodyType;

    /** Where the sent request body comes from in the caller's code (explanations for developers). */
    @EqualsAndHashCode.Exclude
    private List<String> requestBodyOrigins = new ArrayList<>();

    /**
     * Reachable request elements whose every assignment in the caller is a constant, with the constants as JSON
     * text, e.g. {@code "body:seatClass" -> ["\"FIRST\""]}. A reachable element missing here can get any value.
     */
    @EqualsAndHashCode.Exclude
    private Map<String, List<String>> requestElementConstants = new LinkedHashMap<>();

    /** Request elements that can never be null (primitive fields of the caller's DTO). */
    @EqualsAndHashCode.Exclude
    private List<String> neverNullRequestElements = new ArrayList<>();

    /** Endpoint arguments that no call over this connection ever sends (developer information, all kinds). */
    @EqualsAndHashCode.Exclude
    private List<String> neverSentArguments = new ArrayList<>();

    /** Required endpoint arguments that a call does not send: likely an analysis gap or a bug. */
    @EqualsAndHashCode.Exclude
    private List<String> staticallyMissingArguments = new ArrayList<>();

    public RestConnection(String source, String destination, RestCall call, RestEndpoint endpoint) {
        this.source = source;
        this.destination = destination;
        this.call = call;
        this.endpoint = endpoint;
        this.calls = new ArrayList<>();
        if (call != null) {
            this.calls.add(call);
        }
    }
}
