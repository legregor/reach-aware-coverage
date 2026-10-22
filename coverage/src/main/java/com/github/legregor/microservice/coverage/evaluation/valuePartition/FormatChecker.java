package com.github.legregor.microservice.coverage.evaluation.valuePartition;

import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Checks string formats of the OpenAPI / JSON Schema format registry. Unknown formats are always valid (the spec
 * allows custom formats).
 */
final class FormatChecker {

    private static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern HOSTNAME = Pattern.compile(
            "^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]*:[0-9a-fA-F:.]*$");

    private static final BigInteger INT32_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT32_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private FormatChecker() {
    }

    /** True if the string satisfies the format; unknown formats are always valid. */
    static boolean isValidString(String format, String value) {
        if (format == null) {
            return true;
        }
        try {
            return switch (format) {
                case "date" -> parses(() -> LocalDate.parse(value));
                case "date-time" -> parses(() -> OffsetDateTime.parse(value));
                case "time" -> parses(() -> OffsetTime.parse(value)) || parses(() -> LocalTime.parse(value));
                case "uuid" -> UUID.matcher(value).matches();
                case "email", "idn-email" -> EMAIL.matcher(value).matches();
                case "uri", "url" -> new URI(value).isAbsolute();
                case "uri-reference" -> parses(() -> new URI(value));
                case "hostname" -> HOSTNAME.matcher(value).matches();
                case "ipv4" -> IPV4.matcher(value).matches();
                case "ipv6" -> IPV6.matcher(value).matches();
                case "byte" -> parses(() -> Base64.getDecoder().decode(value));
                default -> true;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /** Range check of the integer formats int32 / int64; other formats are not checked. */
    static boolean isValidInteger(String format, BigInteger value) {
        if ("int32".equals(format)) {
            return value.compareTo(INT32_MIN) >= 0 && value.compareTo(INT32_MAX) <= 0;
        }
        if ("int64".equals(format)) {
            return value.compareTo(INT64_MIN) >= 0 && value.compareTo(INT64_MAX) <= 0;
        }
        return true;
    }

    private interface Parse {
        Object run() throws Exception;
    }

    private static boolean parses(Parse parse) {
        try {
            parse.run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
