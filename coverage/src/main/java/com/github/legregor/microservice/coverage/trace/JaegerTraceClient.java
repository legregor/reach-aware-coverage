package com.github.legregor.microservice.coverage.trace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Timestamp;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.jaegertracing.api_v2.Model.Span;
import io.jaegertracing.api_v2.Query.FindTracesRequest;
import io.jaegertracing.api_v2.Query.SpansResponseChunk;
import io.jaegertracing.api_v2.Query.TraceQueryParameters;
import io.jaegertracing.api_v2.QueryServiceGrpc;

/**
 * A utility client for interacting with Jaeger system.
 * 
 * This class provides methods to query distributed traces and spans from a Jaeger backend
 * using gRPC. Furthermore, HTTP functionality is available to retrieve traces by service name, time range,
 * and trace ID, as well as listing available services.

 * 
 * HTTP API endpoint is fixed at {@code http://localhost:16686/api}
 */
public final class JaegerTraceClient {

    private static String JAEGER_URL = "http://localhost:16686/api";

    private static final String HOST = System.getenv().getOrDefault("JAEGER_QUERY_HOST", "localhost");
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("JAEGER_QUERY_GRPC_PORT", "16685"));

    private static final ManagedChannel CHANNEL = ManagedChannelBuilder
            .forAddress(HOST, PORT)
            .usePlaintext()
            .build();

    private static final QueryServiceGrpc.QueryServiceBlockingStub STUB = QueryServiceGrpc.newBlockingStub(CHANNEL);

    private JaegerTraceClient() {
    }

    /**
     * Finds spans from Jaeger using the gRPC API within a specified time range.
     * 
     * <p>Queries Jaeger for spans belonging to the specified service within the given
     * time lookback period. The method converts the current time and lookback duration
     * to the appropriate timestamp format required by Jaeger's gRPC API.</p>
     * 
     * @param serviceName the name of the service to query traces for (must not be null)
     * @param lookback the duration to look back from the current time (must not be null)
     * @param limit the maximum number of traces to retrieve (must be positive)
     * @return a list of spans matching the criteria, or an empty list if limit is non-positive
     * @throws NullPointerException if serviceName or lookback is null
     */
    public static List<Span> findSpans(String serviceName, Duration lookback, int limit) {
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(lookback, "lookback");
        if (limit <= 0)
            return Collections.emptyList();

        TraceQueryParameters params = buildQueryParams(serviceName, lookback, limit);
        FindTracesRequest req = FindTracesRequest.newBuilder().setQuery(params).build();

        Iterator<SpansResponseChunk> chunkStream = STUB.findTraces(req);
        List<Span> result = new ArrayList<>();
        chunkStream.forEachRemaining(chunk -> result.addAll(chunk.getSpansList()));
        return result;
    }

    /**
     * Builds query for Jaeger traces.
     * 
     * @param serviceName the service name to filter by
     * @param lookback the time duration to look back from current time
     * @param limit the maximum number of results (used as search depth)
     * @return configured TraceQueryParameters for the gRPC request
     */
    private static TraceQueryParameters buildQueryParams(String serviceName, Duration lookback, int limit) {
        long nowMicros = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());
        long sinceMicros = nowMicros - TimeUnit.SECONDS.toMicros(lookback.getSeconds());

        return TraceQueryParameters.newBuilder()
                .setServiceName(serviceName)
                .setStartTimeMin(toTimestamp(sinceMicros))
                .setStartTimeMax(toTimestamp(nowMicros))
                .setSearchDepth(limit)
                .build();
    }

    /**
     * Converts microseconds timestamp to Protobuf Timestamp.
     * 
     * @param micros timestamp in microseconds 
     * @return Protobuf Timestamp object
     */
    private static Timestamp toTimestamp(long micros) {
        long seconds = TimeUnit.MICROSECONDS.toSeconds(micros);
        int nanos = (int) ((micros - TimeUnit.SECONDS.toMicros(seconds)) * 1_000);
        return Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
    }

    // HTTP API methods

    public static String getTraces(String serviceName, String lookback, int limit) throws Exception {
        String queryParams = String.format("?service=%s&lookback=%s&limit=%d", serviceName, lookback, limit);
        return fetchFromJaeger(JAEGER_URL + "/traces" + queryParams);
    }

    public static String getTraceById(String traceId) throws Exception {
        return fetchFromJaeger(JAEGER_URL + "/traces/" + traceId);
    }

    public static String getServices() throws Exception {
        return fetchFromJaeger(JAEGER_URL + "/services");
    }

    /**
     * Performs an generic HTTP GET request to the specified Jaeger endpoint.
     * 
     * @param endpoint the complete URL endpoint to fetch data from
     * @return the response body as a string
     * @throws Exception if the HTTP request fails or returns a non-200 status code
     */
    private static String fetchFromJaeger(String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to fetch data: HTTP response code " + connection.getResponseCode());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

}
