package com.github.legregor.microservice.coverage.evaluation;

import com.github.legregor.microservice.coverage.analysis.MicroserviceSystemReader;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageEvaluator;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageReport;
import com.github.legregor.microservice.coverage.output.OutputWriter;
import com.github.legregor.microservice.coverage.trace.CoverageSpanReader;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CoverageEvaluatorTests {

    private static MicroserviceSystem system;
    private static Map<String, List<CoverageSpan>> allSpans;

    private static Path openApiDir = Paths.get("src/test/resources/slim_open_api");

    private static Path configFilePath = Paths.get("src/test/resources/CancelConfig.json");

    private static Path outputReportPath = Paths.get("src/test/output");

    private static OutputWriter reportWriter;

    @BeforeAll
    static void setUp() throws IOException, URISyntaxException {
        URL serviceResource = CoverageEvaluatorTests.class
                .getClassLoader()
                .getResource("microservice-system.json");

        assertNotNull(serviceResource, "Microservice system resource file not found on classpath");

        Path path = Paths.get(serviceResource.toURI());
        system = new MicroserviceSystemReader().read(path);

        URL spansResource = CoverageEvaluatorTests.class
                .getClassLoader()
                .getResource("processed-spans.json");

        assertNotNull(spansResource, "Processed spans resource file not found on classpath");

        Path spansPath = Paths.get(spansResource.toURI());
        List<CoverageSpan> spans = new CoverageSpanReader().read(spansPath);

        allSpans = Map.of("CancelOrderTest", spans);

        reportWriter = new OutputWriter(outputReportPath);


    }

    @Test
    void basicTest() {
        // Create evaluator and generate enhanced report with detailed usage statistics
        CoverageEvaluator evaluator = new CoverageEvaluator(openApiDir, configFilePath);
        CoverageReport report = evaluator.evaluateCoverage(system, allSpans);

        assertNotNull(report, "Coverage report should not be null");

        reportWriter.writeJsonReport(report);

        System.out.println("[Report stored at:] " + outputReportPath);


    }

}
