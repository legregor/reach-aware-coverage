package com.github.legregor.microservice.coverage.config;

import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import com.github.legregor.microservice.coverage.config.model.EntryEndpoint;
import com.github.legregor.microservice.coverage.config.model.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryEndpointResolverTest {

    private static final String ROOT = "ts-route-plan-service";
    private static final Path CHEAPEST =
            Path.of("/ms/ts-route-plan-service/src/test/java/plan/integration/post/SearchCheapestResultTest.java");
    private static final Path QUICKEST =
            Path.of("/ms/ts-route-plan-service/src/test/java/plan/integration/post/SearchQuickestResultTest.java");

    private static TestConfig test(String path, String serviceName, EntryEndpoint... entries) {
        return new TestConfig(path, Path.of(path).getFileName().toString(), serviceName,
                new ArrayList<>(List.of(entries)));
    }

    private static CoverageConfig config(TestConfig... tests) {
        CoverageConfig config = new CoverageConfig();
        config.setTests(new ArrayList<>(List.of(tests)));
        return config;
    }

    private static List<String> labels(EntryEndpointResolver.Resolution resolution) {
        return resolution.entryEndpoints().stream().map(EntryEndpoint::label).toList();
    }

    private final CoverageConfig twoTests = config(
            test("plan/integration/post/SearchCheapestResultTest.java", ROOT,
                    new EntryEndpoint("POST", "/api/v1/routeplanservice/routePlan/cheapestRoute")),
            test("plan/integration/post/SearchQuickestResultTest.java", ROOT,
                    new EntryEndpoint("POST", "/api/v1/routeplanservice/routePlan/quickestRoute"),
                    new EntryEndpoint("POST", "/api/v1/routeplanservice/routePlan/cheapestRoute")));

    @Test
    void usesTheEntryEndpointsOfTheExecutedTestOnly() throws Exception {
        var resolution = EntryEndpointResolver.resolve(twoTests, List.of(CHEAPEST), ROOT);
        assertEquals(List.of("POST /api/v1/routeplanservice/routePlan/cheapestRoute"), labels(resolution));
        assertTrue(resolution.notes().isEmpty());
    }

    @Test
    void methodSuffixOfATestFileStillMatches() throws Exception {
        var resolution = EntryEndpointResolver.resolve(twoTests, List.of(Path.of(CHEAPEST + "#testCheapest")), ROOT);
        assertEquals(List.of("POST /api/v1/routeplanservice/routePlan/cheapestRoute"), labels(resolution));
    }

    @Test
    void unitesTheEntryEndpointsOfSeveralTestsWithoutDuplicates() throws Exception {
        var resolution = EntryEndpointResolver.resolve(twoTests, List.of(CHEAPEST, QUICKEST), ROOT);
        assertEquals(List.of("POST /api/v1/routeplanservice/routePlan/cheapestRoute",
                "POST /api/v1/routeplanservice/routePlan/quickestRoute"), labels(resolution));
    }

    @Test
    void withoutTestFilesAllConfigTestsCount() throws Exception {
        assertEquals(2, EntryEndpointResolver.resolve(twoTests, List.of(), ROOT).entryEndpoints().size());
    }

    @Test
    void aTestWithoutEntryEndpointsLiftsTheRestriction() throws Exception {
        CoverageConfig config = config(
                test("plan/integration/post/SearchCheapestResultTest.java", ROOT,
                        new EntryEndpoint("POST", "/api/v1/routeplanservice/routePlan/cheapestRoute")),
                test("plan/integration/post/SearchQuickestResultTest.java", ROOT));
        var resolution = EntryEndpointResolver.resolve(config, List.of(CHEAPEST, QUICKEST), ROOT);
        assertTrue(resolution.entryEndpoints().isEmpty());
        assertFalse(resolution.notes().isEmpty());
    }

    @Test
    void aTestFileMissingInTheConfigLiftsTheRestriction() throws Exception {
        Path other = Path.of("/ms/ts-route-plan-service/src/test/java/plan/integration/post/OtherTest.java");
        var resolution = EntryEndpointResolver.resolve(twoTests, List.of(CHEAPEST, other), ROOT);
        assertTrue(resolution.entryEndpoints().isEmpty());
        assertTrue(resolution.notes().get(0).contains("OtherTest.java"));
    }

    @Test
    void noEntryEndpointsAnywhereMeansAllEndpointsWithoutNotes() throws Exception {
        CoverageConfig config = config(test("plan/integration/post/SearchCheapestResultTest.java", ROOT));
        var resolution = EntryEndpointResolver.resolve(config, List.of(QUICKEST), ROOT);
        assertTrue(resolution.entryEndpoints().isEmpty());
        assertTrue(resolution.notes().isEmpty());
        assertTrue(EntryEndpointResolver.resolve(null, List.of(CHEAPEST), ROOT).entryEndpoints().isEmpty());
    }

    @Test
    void entryEndpointsOfATestOfAnotherServiceStopTheRun() {
        CoverageConfig config = config(test("cancel/integration/get/CancelOrderTest.java", "ts-cancel-service",
                new EntryEndpoint("GET", "/api/v1/cancelservice/cancel/{orderId}/{loginId}")));
        assertThrows(CoverageConfigException.class, () -> EntryEndpointResolver.resolve(config,
                List.of(Path.of("/ms/ts-cancel-service/src/test/java/cancel/integration/get/CancelOrderTest.java")),
                ROOT));
    }

    @Test
    void readerAcceptsEntryEndpointsInTestsAndRejectsThemAtTheTopLevel(@TempDir Path dir) throws Exception {
        Path inTests = dir.resolve("inTests.json");
        Files.writeString(inTests, """
                {"configName": "x", "tests": [{"path": "plan/integration/post/SearchCheapestResultTest.java",
                  "fileName": "SearchCheapestResultTest.java", "serviceName": "ts-route-plan-service",
                  "entryEndpoints": [{"method": "POST", "path": "/api/v1/routeplanservice/routePlan/cheapestRoute"}]}]}
                """);
        CoverageConfig config = new CoverageConfigReader().read(inTests);
        assertEquals("POST /api/v1/routeplanservice/routePlan/cheapestRoute",
                config.getTests().get(0).getEntryEndpoints().get(0).label());

        Path topLevel = dir.resolve("topLevel.json");
        Files.writeString(topLevel, """
                {"configName": "x", "entryEndpoints": [{"method": "POST", "path": "/a"}]}
                """);
        assertThrows(CoverageConfigException.class, () -> new CoverageConfigReader().read(topLevel));

        Path typo = dir.resolve("typo.json");
        Files.writeString(typo, """
                {"configName": "x", "tests": [{"path": "A.java", "entryEndpoint": []}]}
                """);
        assertThrows(CoverageConfigException.class, () -> new CoverageConfigReader().read(typo));
    }
}
