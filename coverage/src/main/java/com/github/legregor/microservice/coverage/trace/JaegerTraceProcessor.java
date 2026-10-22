package com.github.legregor.microservice.coverage.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.legregor.microservice.coverage.trace.model.CoverageClientSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageInternalSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageServerSpan;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;
import com.github.legregor.microservice.coverage.trace.model.SpanKind;

import io.jaegertracing.api_v2.Model.KeyValue;
import io.jaegertracing.api_v2.Model.Span;

/**
 * Processes raw Jaeger spans and converts them to CoverageSpan objects.
 */
public class JaegerTraceProcessor {

    private JaegerTraceProcessor() {
    }

    /**
     * Process a list of Jaeger spans into CoverageSpan instances.
     * 
     * @param jaegerSpans The Jaeger spans to process
     * @return A list of CoverageSpan objects
     */
    public static List<CoverageSpan> processSpans(List<Span> jaegerSpans) {
        List<CoverageSpan> coverageSpans = new ArrayList<>();

        for (Span jaegerSpan : jaegerSpans) {
            coverageSpans.add(processSpan(jaegerSpan));
        }

        return coverageSpans;
    }

    /**
     * Process a single Jaeger span into a CoverageSpan instance.
     * 
     * @param jaegerSpan The Jaeger span to process
     * @return An Optional containing the CoverageSpan if processing was successful,
     *         or an empty Optional otherwise
     */
    public static CoverageSpan processSpan(Span jaegerSpan) {
        Map<String, Object> tags = extractTags(jaegerSpan);

        SpanKind kind = determineSpanKind(tags);

        // attributes for all CoverageSpan types
        String traceId = bytesToHex(jaegerSpan.getTraceId().toByteArray());
        String spanId = bytesToHex(jaegerSpan.getSpanId().toByteArray());
        String parentSpanId = jaegerSpan.getReferencesCount() > 0
                ? bytesToHex(jaegerSpan.getReferences(0).getSpanId().toByteArray())
                : null;
        String operationName = jaegerSpan.getOperationName();
        String service = jaegerSpan.getProcess().getServiceName();

        if (kind == SpanKind.INTERNAL) {
            return new CoverageInternalSpan(
                    traceId, spanId, parentSpanId, operationName, service);
        } else if (kind == SpanKind.CLIENT) {
            String httpMethod = (String) tags.getOrDefault("http.method", "");
            String url = (String) tags.getOrDefault("http.url", "");

            // default to 0 if "Connection to server failed: Connection refused" and
            // http.status_code is not present
            Integer statusCode = (Integer) tags.getOrDefault("http.status_code", 0);

            return new CoverageClientSpan(
                    traceId, spanId, parentSpanId, operationName, service,
                    httpMethod, statusCode, url);
        } else {
            String httpMethod = (String) tags.getOrDefault("http.method", "");
            Integer statusCode = (Integer) tags.getOrDefault("http.status_code", 0);
            String route = (String) tags.getOrDefault("http.route", "");
            String requestBody = (String) tags.getOrDefault("http.request.body", null);
            String responseBody = (String) tags.getOrDefault("http.response.body", null);

            return new CoverageServerSpan(
                    traceId, spanId, parentSpanId, operationName, service,
                    httpMethod, route, statusCode, requestBody, responseBody);
        }
    }

    /**
     * Converts generic Jaeger span tags to typed key value pairs
     * 
     * @param jaegerSpan The Jaeger span
     * @return A map of tag keys to values
     */
    private static Map<String, Object> extractTags(Span jaegerSpan) {
        Map<String, Object> tags = new HashMap<>();

        for (KeyValue tag : jaegerSpan.getTagsList()) {
            String key = tag.getKey();

            switch (tag.getVType()) {
                case STRING:
                    tags.put(key, tag.getVStr());
                    break;
                case BOOL:
                    tags.put(key, tag.getVBool());
                    break;
                case INT64:
                    tags.put(key, (int) tag.getVInt64());
                    break;
                case FLOAT64:
                    tags.put(key, tag.getVFloat64());
                    break;
                case BINARY:
                    tags.put(key, tag.getVBinary().toByteArray());
                    break;
                default:
                    break;
            }
        }

        return tags;
    }

    /**
     * Determines the type of a span
     * 
     * @param tags
     * @return
     */
    private static SpanKind determineSpanKind(Map<String, Object> tags) {
        if (tags.containsKey("span.kind")) {
            String kindStr = ((String) tags.get("span.kind")).toUpperCase();

            switch (kindStr) {
                case "CLIENT":
                    return SpanKind.CLIENT;
                case "SERVER":
                    return SpanKind.SERVER;
                default:
                    return SpanKind.INTERNAL;
            }
        }

        return SpanKind.INTERNAL;
    }

    public static List<CoverageSpan> filterSpansByKind(List<CoverageSpan> spans, SpanKind kind) {
        List<CoverageSpan> filtered = new ArrayList<>();

        for (CoverageSpan span : spans) {
            if (span.getKind() == kind) {
                filtered.add(span);
            }
        }

        return filtered;
    }

    public static List<CoverageClientSpan> getClientSpans(List<CoverageSpan> spans) {
        List<CoverageClientSpan> clients = new ArrayList<>();
        for (CoverageSpan span : spans) {
            if (span instanceof CoverageClientSpan) {
                clients.add((CoverageClientSpan) span);
            }
        }
        return clients;
    }

    public static List<CoverageServerSpan> getServerSpans(List<CoverageSpan> spans) {
        List<CoverageServerSpan> servers = new ArrayList<>();
        for (CoverageSpan span : spans) {
            if (span instanceof CoverageServerSpan) {
                servers.add((CoverageServerSpan) span);
            }
        }
        return servers;
    }

    public static List<CoverageInternalSpan> getInternalSpans(List<CoverageSpan> spans) {
        List<CoverageInternalSpan> internals = new ArrayList<>();
        for (CoverageSpan span : spans) {
            if (span instanceof CoverageInternalSpan) {
                internals.add((CoverageInternalSpan) span);
            }
        }
        return internals;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
