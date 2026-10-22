package com.github.legregor.microservice.coverage.config;

import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import com.github.legregor.microservice.coverage.config.model.EntryEndpoint;
import com.github.legregor.microservice.coverage.config.model.TestConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Determines the entry endpoints of a run from the {@code entryEndpoints} of the tests in the config.
 * <p>
 * A run has one coverage space, so its entry endpoints are the union of the entry endpoints of the tests it executes
 * (the config tests that match a test file of the command line; without test files, e.g. with {@code --static}, all
 * config tests). If one of these tests has no entry endpoints, or a test file is not listed in the config, that test
 * may call any endpoint, so every endpoint of the entry service is an entry point.
 */
public final class EntryEndpointResolver {

    /**
     * @param entryEndpoints the entry endpoints of the run; empty: every endpoint of the entry service
     * @param notes          explanations for the log (e.g. why the restriction does not apply)
     */
    public record Resolution(List<EntryEndpoint> entryEndpoints, List<String> notes) {
    }

    private EntryEndpointResolver() {
    }

    /**
     * @param config      the coverage config, or null
     * @param testFiles   the test files of the command line (may be empty)
     * @param rootService the folder name of the entry service (the first service folder), or null
     * @throws CoverageConfigException if a test with entry endpoints belongs to another service than the entry service
     */
    public static Resolution resolve(CoverageConfig config, List<Path> testFiles, String rootService)
            throws CoverageConfigException {
        List<String> notes = new ArrayList<>();
        if (config == null || config.getTests() == null
                || config.getTests().stream().allMatch(test -> test == null || test.getEntryEndpoints().isEmpty())) {
            return new Resolution(List.of(), notes);
        }
        List<TestConfig> configTests = config.getTests().stream().filter(test -> test != null).toList();

        List<TestConfig> selected = new ArrayList<>();
        boolean unrestricted = false;
        if (testFiles == null || testFiles.isEmpty()) {
            selected.addAll(configTests);
        } else {
            for (Path testFile : testFiles) {
                List<TestConfig> matching = configTests.stream().filter(test -> test.matches(testFile)).toList();
                if (matching.isEmpty()) {
                    unrestricted = true;
                    notes.add("test " + testFile + " is not listed in the tests of the config, so every endpoint of "
                            + "the entry service is an entry point");
                }
                for (TestConfig test : matching) {
                    if (!selected.contains(test)) {
                        selected.add(test);
                    }
                }
            }
        }

        Map<String, EntryEndpoint> union = new LinkedHashMap<>();
        for (TestConfig test : selected) {
            if (test.getEntryEndpoints().isEmpty()) {
                unrestricted = true;
                notes.add("test " + test.label() + " has no entryEndpoints, so every endpoint of the entry service is "
                        + "an entry point");
                continue;
            }
            if (rootService != null && test.getServiceName() != null && !test.getServiceName().isBlank()
                    && !test.getServiceName().equals(rootService)) {
                throw new CoverageConfigException("coverage config: test " + test.label() + " has entryEndpoints, "
                        + "but belongs to " + test.getServiceName() + ", while the reachability analysis starts at the "
                        + "first service folder " + rootService + " (list " + test.getServiceName() + " first)", null);
            }
            for (EntryEndpoint entry : test.getEntryEndpoints()) {
                if (entry != null) {
                    union.putIfAbsent(entry.label(), entry);
                }
            }
        }
        return new Resolution(unrestricted ? List.of() : List.copyOf(union.values()), notes);
    }
}
