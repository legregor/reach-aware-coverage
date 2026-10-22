package com.github.legregor.microservice.coverage.analysis.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.github.legregor.microservice.coverage.analysis.model.StaticServiceTraceResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Stores and loads static service analysis results from a local cache.
 */
public class ServiceAnalysisCache {
    private static final String CACHE_SCHEMA_VERSION = "v6"; // v6: runtime URL parts as path placeholders, URL variables assigned in branches (v5: body constants, never-null properties)

    private final Path cacheRoot;
    private final ObjectMapper mapper;

    public ServiceAnalysisCache() {
        this(Paths.get("storage", "cache", "static-analysis"));
    }

    public ServiceAnalysisCache(Path cacheRoot) {
        this.cacheRoot = cacheRoot.toAbsolutePath();
        this.mapper = new ObjectMapper(new SmileFactory());
    }

    public Optional<StaticServiceTraceResult> load(Path serviceRoot) throws IOException {
        Path cacheFile = resolveCacheFile(serviceRoot);
        if (!Files.exists(cacheFile)) {
            return Optional.empty();
        }

        return Optional.of(mapper.readValue(cacheFile.toFile(), StaticServiceTraceResult.class));
    }

    public void store(Path serviceRoot, StaticServiceTraceResult result) throws IOException {
        Path cacheFile = resolveCacheFile(serviceRoot);
        Files.createDirectories(cacheFile.getParent());
        mapper.writeValue(cacheFile.toFile(), result);
    }

    public Path resolveCacheFile(Path serviceRoot) throws IOException {
        String serviceName = serviceRoot.getFileName().toString();
        String fingerprint = fingerprintJavaSources(serviceRoot);
        return cacheRoot.resolve(CACHE_SCHEMA_VERSION).resolve(serviceName).resolve(fingerprint + ".smile");
    }

    private String fingerprintJavaSources(Path serviceRoot) throws IOException {
        MessageDigest digest = newMessageDigest();
        try (Stream<Path> paths = Files.walk(serviceRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isJavaSource)
                    .sorted()
                    .forEach(path -> updateDigest(digest, serviceRoot, path));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void updateDigest(MessageDigest digest, Path serviceRoot, Path path) {
        digest.update(serviceRoot.relativize(path).toString().getBytes());
        try {
            digest.update(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fingerprint service source: " + path, e);
        }
    }

    private boolean isJavaSource(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java");
    }

    private MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }
}
