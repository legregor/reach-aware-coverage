package com.github.legregor.microservice.coverage.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Matching of the expected entries of the coverage config against the recorded responses. An entry is
 * <ul>
 * <li>an HTTP status code ({@code "404"}) or class ({@code "4xx"}), matched against the status codes, or</li>
 * <li>a body outcome {@code "<field>=<value>"} ({@code "status=0"}, nested: {@code "result.code=OK"}), matched against
 * the response bodies, for systems that report their outcome in the body instead of the HTTP status code (e.g.
 * TrainTicket: always HTTP 200, {@code "status": 1} on success and {@code 0} on failure).</li>
 * </ul>
 */
public final class StatusCodes {

    /** An HTTP status code ({@code 404}) or class ({@code 4xx}). */
    private static final Pattern HTTP_ENTRY = Pattern.compile("^[1-5](\\d\\d|xx)$", Pattern.CASE_INSENSITIVE);

    /** A body outcome: a (dotted) field path, "=", a value. */
    private static final Pattern BODY_ENTRY = Pattern.compile("^[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*=.+$");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StatusCodes() {
    }

    /** True if the entry is an HTTP status code or class. */
    public static boolean isHttpEntry(String entry) {
        return entry != null && HTTP_ENTRY.matcher(entry.trim()).matches();
    }

    /** True if the entry is a body outcome ({@code field=value}). */
    public static boolean isBodyEntry(String entry) {
        return entry != null && BODY_ENTRY.matcher(entry.trim()).matches();
    }

    /** True if the code equals the expected code or belongs to the expected class (case-insensitive). */
    public static boolean matches(int code, String expected) {
        if (!isHttpEntry(expected)) {
            return false;
        }
        String normalized = expected.trim().toLowerCase();
        if (normalized.equals(String.valueOf(code))) {
            return true;
        }
        if (normalized.endsWith("xx")) {
            return Integer.parseInt(normalized.substring(0, 1)) == code / 100;
        }
        return false;
    }

    /** True if the code matches at least one of the expected entries. */
    public static boolean matchesAny(int code, List<String> expected) {
        return expected != null && expected.stream().anyMatch(entry -> matches(code, entry));
    }

    /**
     * True if the response body has the field of a body outcome entry with its value. Numbers are compared by value
     * ({@code status=1} matches {@code 1} and {@code 1.0}), everything else by its text ({@code ok=true},
     * {@code code=OK}); {@code field=null} matches an explicit null.
     */
    public static boolean matchesBody(String body, String expected) {
        if (!isBodyEntry(expected) || body == null) {
            return false;
        }
        String entry = expected.trim();
        String path = entry.substring(0, entry.indexOf('='));
        String value = entry.substring(entry.indexOf('=') + 1).trim();
        JsonNode node = field(body, path);
        if (node == null || node.isMissingNode()) {
            return false;
        }
        if (node.isNull()) {
            return value.equals("null");
        }
        if (node.isNumber()) {
            try {
                return node.decimalValue().compareTo(new BigDecimal(value)) == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return (node.isValueNode() ? node.asText() : node.toString()).equals(value);
    }

    /**
     * Whether a response belongs to at least one relevant response class: an HTTP entry ({@code 2xx}, {@code 404})
     * by its status code, a body entry ({@code status=0}) by its body.
     */
    public static boolean matchesAnyClass(int code, String body, List<String> expected) {
        if (expected == null) {
            return false;
        }
        boolean hasBody = body != null && !body.isBlank();
        return expected.stream().anyMatch(entry -> matches(code, entry) || (hasBody && matchesBody(body, entry)));
    }

    /**
     * Whether the class of a response cannot be decided: the expected entries contain body entries, the body was not
     * captured, and no HTTP entry matches the status code.
     */
    public static boolean classUnknownWithoutBody(int code, String body, List<String> expected) {
        return (body == null || body.isBlank()) && !matchesAny(code, expected)
                && expected != null && expected.stream().anyMatch(StatusCodes::isBodyEntry);
    }

    /** The HTTP entries of a list of expected entries (body outcome entries removed). */
    public static List<String> httpEntries(List<String> expected) {
        List<String> result = new ArrayList<>();
        if (expected != null) {
            expected.stream().filter(StatusCodes::isHttpEntry).forEach(result::add);
        }
        return result;
    }

    /**
     * The body outcomes ({@code field=value}) found in the bodies, for the fields of the body outcome entries of the
     * expected list (e.g. expected {@code status=1}, {@code status=0}; bodies with status 1 and 2 -> {@code status=1},
     * {@code status=2}).
     */
    public static List<String> bodyOutcomes(List<String> bodies, List<String> expected) {
        Set<String> paths = new LinkedHashSet<>();
        if (expected != null) {
            expected.stream().filter(StatusCodes::isBodyEntry)
                    .forEach(entry -> paths.add(entry.trim().substring(0, entry.trim().indexOf('='))));
        }
        Set<String> outcomes = new LinkedHashSet<>();
        for (String body : bodies == null ? List.<String>of() : bodies) {
            for (String path : paths) {
                JsonNode node = field(body, path);
                if (node != null && !node.isMissingNode()) {
                    outcomes.add(path + "=" + (node.isValueNode() || node.isNull() ? node.asText() : node.toString()));
                }
            }
        }
        return new ArrayList<>(outcomes);
    }

    /** A (dotted) field of a JSON body; null if the body is not JSON. */
    private static JsonNode field(String body, String path) {
        JsonNode node;
        try {
            node = MAPPER.readTree(body);
        } catch (Exception e) {
            return null;
        }
        for (String name : path.split("\\.")) {
            if (node == null || !node.isObject()) {
                return null;
            }
            node = node.get(name);
        }
        return node;
    }
}
