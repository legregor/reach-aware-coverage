package com.github.legregor.microservice.coverage.openapi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Loads and caches the OpenAPI spec of each service from a spec folder.
 *
 * <p>A service's spec is looked up by name, in this order (JSON before YAML): {@code <service>.json},
 * {@code <service>-openapi.json}, {@code <service>-slim.json}, {@code <service>/openapi.json}, each also with
 * {@code .yaml} and {@code .yml}. The first existing file is used.</p>
 */
public class OpenApiSpecRegistry {
    private static final List<String> NAME_PATTERNS = List.of(
            "%s", "%s-openapi", "%s-slim", "%s/openapi");
    private static final List<String> EXTENSIONS = List.of(".json", ".yaml", ".yml");

    private final Path openApiDir;
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    private final Map<String, Optional<JsonNode>> cache;

    public OpenApiSpecRegistry(Path openApiDir) {
        this.openApiDir = openApiDir;
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.cache = new HashMap<>();
    }

    /** The file names (relative to the spec folder) tried for a service, in lookup order. */
    public static List<String> candidateFileNames(String serviceName) {
        List<String> names = new ArrayList<>();
        for (String pattern : NAME_PATTERNS) {
            for (String extension : EXTENSIONS) {
                names.add(String.format(pattern, serviceName) + extension);
            }
        }
        return names;
    }

    /** The spec file of a service, if one exists in the spec folder. */
    public Optional<Path> findSpecFile(String serviceName) {
        if (serviceName == null || serviceName.isBlank() || openApiDir == null) {
            return Optional.empty();
        }
        return candidateFileNames(serviceName).stream()
                .map(openApiDir::resolve)
                .filter(Files::isRegularFile)
                .findFirst();
    }

    /**
     * Returns a cached spec when available, otherwise reads and caches it once.
     */
    public Optional<JsonNode> getSpec(String serviceName) {
        if (serviceName == null || serviceName.isBlank() || openApiDir == null) {
            return Optional.empty();
        }

        Optional<JsonNode> cached = cache.get(serviceName);
        if (cached != null) {
            return cached;
        }

        Optional<JsonNode> result = findSpecFile(serviceName).flatMap(path -> read(serviceName, path));
        if (result.isEmpty()) {
            System.out.println("[INFO] No OpenAPI spec for " + serviceName + " in " + openApiDir
                    + " (looked for " + candidateFileNames(serviceName).stream()
                            .filter(name -> name.endsWith(".json"))
                            .collect(Collectors.joining(", "))
                    + ", also .yaml/.yml)");
        }
        cache.put(serviceName, result);
        return result;
    }

    private Optional<JsonNode> read(String serviceName, Path specPath) {
        String fileName = specPath.getFileName().toString().toLowerCase();
        ObjectMapper mapper = fileName.endsWith(".json") ? jsonMapper : yamlMapper;
        try {
            return Optional.ofNullable(mapper.readTree(specPath.toFile()));
        } catch (Exception e) {
            System.err.println("Warning: Failed to read OpenAPI spec for " + serviceName + " (" + specPath + "): "
                    + e.getMessage());
            return Optional.empty();
        }
    }
}
