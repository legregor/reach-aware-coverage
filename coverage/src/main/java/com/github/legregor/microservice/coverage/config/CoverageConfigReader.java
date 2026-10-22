package com.github.legregor.microservice.coverage.config;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.github.legregor.microservice.coverage.config.model.ConnectionOverride;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Reads a {@link CoverageConfig} from a JSON file and warns about entries that can never match a status code. */
public class CoverageConfigReader {


    private final ObjectMapper mapper;

    public CoverageConfigReader() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Reads and validates the config file.
     *
     * @throws CoverageConfigException if the file cannot be read, is no valid JSON or contains an unknown field
     */
    public CoverageConfig read(Path filePath) throws CoverageConfigException {
        CoverageConfig config;
        try {
            config = mapper.readValue(filePath.toFile(), CoverageConfig.class);
        } catch (UnrecognizedPropertyException e) {
            String known = e.getKnownPropertyIds() == null ? "" : e.getKnownPropertyIds().stream()
                    .map(Objects::toString).sorted().collect(Collectors.joining(", "));
            throw new CoverageConfigException(String.format("coverage config %s: unknown field '%s' in %s%s%s"
                    + " (allowed: %s)", filePath, e.getPropertyName(), jsonPath(e), location(e),
                    suggestion(e.getPropertyName(), e.getKnownPropertyIds()), known), e);
        } catch (JsonProcessingException e) {
            throw new CoverageConfigException(String.format("coverage config %s is not valid%s: %s", filePath,
                    location(e), e.getOriginalMessage()), e);
        } catch (IOException e) {
            throw new CoverageConfigException("coverage config " + filePath + " cannot be read: " + e.getMessage(), e);
        }
        if (config == null) {
            throw new CoverageConfigException("coverage config " + filePath + " is empty", null);
        }
        rejectTopLevelEntryEndpoints(filePath);
        List<String> errors = errors(config);
        if (!errors.isEmpty()) {
            throw new CoverageConfigException("coverage config " + filePath + ": " + String.join("; ", errors), null);
        }
        for (String warning : validate(config)) {
            System.err.println("Warning: coverage config " + filePath.getFileName() + ": " + warning);
        }
        return config;
    }

    /**
     * Entry endpoints belong to the tests that call them ({@code tests[].entryEndpoints}); a top-level list would
     * otherwise be ignored silently (unknown top-level fields are allowed for the web UI) and widen the reachability.
     */
    private void rejectTopLevelEntryEndpoints(Path filePath) throws CoverageConfigException {
        try {
            if (mapper.readTree(filePath.toFile()).has("entryEndpoints")) {
                throw new CoverageConfigException("coverage config " + filePath + ": 'entryEndpoints' belongs to a "
                        + "test, e.g. \"tests\": [{\"path\": \"...Test.java\", \"serviceName\": \"...\", "
                        + "\"entryEndpoints\": [...]}]", null);
            }
        } catch (CoverageConfigException e) {
            throw e;
        } catch (IOException e) {
            throw new CoverageConfigException("coverage config " + filePath + " cannot be read: " + e.getMessage(), e);
        }
    }

    /** The position in the JSON document, e.g. {@code connectionOverrides[0]}, of the object with the bad field. */
    private static String jsonPath(JsonMappingException e) {
        StringBuilder path = new StringBuilder();
        List<JsonMappingException.Reference> references = e.getPath();
        // the last reference is the unknown field itself
        for (int i = 0; i < references.size() - 1; i++) {
            JsonMappingException.Reference reference = references.get(i);
            if (reference.getFieldName() != null) {
                path.append(path.length() == 0 ? "" : ".").append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.length() == 0 ? "the top level" : path.toString();
    }

    /** "; did you mean 'sourceService'?" if a known field contains the unknown name (ignoring case), else "". */
    private static String suggestion(String unknown, java.util.Collection<Object> known) {
        if (unknown == null || unknown.isBlank() || known == null) {
            return "";
        }
        String lower = unknown.toLowerCase(java.util.Locale.ROOT);
        return known.stream().map(Objects::toString)
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).contains(lower))
                .sorted()
                .findFirst()
                .map(name -> "; did you mean '" + name + "'?")
                .orElse("");
    }

    private static String location(JsonProcessingException e) {
        JsonLocation location = e.getLocation();
        return location == null ? "" : " (line " + location.getLineNr() + ", column " + location.getColumnNr() + ")";
    }

    /**
     * Exclusions ({@code "reachable": false}) must name exactly one connection (sourceService, destinationService,
     * method and path) and must not set expected status codes.
     */
    static List<String> errors(CoverageConfig config) {
        List<String> errors = new ArrayList<>();
        if (config.getConnectionOverrides() == null) {
            return errors;
        }
        for (int i = 0; i < config.getConnectionOverrides().size(); i++) {
            ConnectionOverride override = config.getConnectionOverrides().get(i);
            if (override == null || !override.isExclusion()) {
                continue;
            }
            String where = "connectionOverrides[" + i + "]";
            List<String> missing = new ArrayList<>();
            if (isBlank(override.getSourceService())) {
                missing.add("sourceService");
            }
            if (isBlank(override.getDestinationService())) {
                missing.add("destinationService");
            }
            if (isBlank(override.getMethod())) {
                missing.add("method");
            }
            if (isBlank(override.getPath())) {
                missing.add("path");
            }
            if (!missing.isEmpty()) {
                errors.add(where + " has \"reachable\": false, but no " + String.join(", ", missing)
                        + " (an exclusion must name the connection with sourceService, destinationService, method"
                        + " and path)");
            }
            if (override.getExpectedStatusCodes() != null && !override.getExpectedStatusCodes().isEmpty()) {
                errors.add(where + " has \"reachable\": false and expectedStatusCodes (an excluded connection has no"
                        + " expected status codes; remove one of them)");
            }
        }
        return errors;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Entries that are neither a status code, a status code class nor a body outcome (they never match). */
    static List<String> validate(CoverageConfig config) {
        List<String> warnings = new ArrayList<>();
        check(config.getGlobalExpectedStatusCodes(), "globalExpectedStatusCodes", warnings);
        if (config.getConnectionOverrides() != null) {
            for (int i = 0; i < config.getConnectionOverrides().size(); i++) {
                ConnectionOverride override = config.getConnectionOverrides().get(i);
                if (override != null) {
                    check(override.getExpectedStatusCodes(), "connectionOverrides[" + i + "].expectedStatusCodes",
                            warnings);
                    if (!override.isExclusion() && !isBlank(override.getReason())) {
                        warnings.add("connectionOverrides[" + i + "].reason is only used with \"reachable\": false");
                    }
                }
            }
        }
        return warnings;
    }

    private static void check(List<String> codes, String where, List<String> warnings) {
        if (codes == null) {
            return;
        }
        for (String code : codes) {
            if (!StatusCodes.isHttpEntry(code) && !StatusCodes.isBodyEntry(code)) {
                warnings.add(where + ": '" + code + "' is neither a status code (e.g. 404), a class (e.g. 4xx) nor a "
                        + "body outcome (e.g. status=0)");
            }
        }
    }
}
