package com.github.legregor.microservice.coverage.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPath;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageReport;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;

import io.jaegertracing.api_v2.Model.Span;

/**
 * Handles writing various types of data to the output directory.
 */
public class OutputWriter {

    private final Path resultDirectory;
    private final ObjectMapper mapper;
    private final TextCoverageReportWriter textWriter;
    private final MarkdownCoverageReportWriter markdownWriter;

    /**
     * Constructor for OutputWriter.
     *
     * @param resultDirectory The directory where output files will be written
     */
    public OutputWriter(Path resultDirectory) {
        this.resultDirectory = resultDirectory;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.textWriter = new TextCoverageReportWriter();
        this.markdownWriter = new MarkdownCoverageReportWriter();
    }

    /**
     * Writes the spans for each test to a JSON file in the result directory.
     *
     * @param allSpans Map of test names to their corresponding spans
     */
    public void writeSpansToJson(Map<String, List<CoverageSpan>> allSpans) {
        allSpans.forEach((testName, spans) -> {
            try {
                Path testDir = resultDirectory.resolve(testName);
                Files.createDirectories(testDir);
                Path jsonFile = testDir.resolve("processed-spans.json");
                mapper.writeValue(jsonFile.toFile(), spans);
                System.out.println("Wrote spans for " + testName + " to " + jsonFile);
            } catch (Exception e) {
                System.err.println("Failed to write spans for " + testName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Writes the raw spans for each test to a text file in the result directory.
     *
     * @param rawSpans Map of test names to their corresponding raw spans
     */
    public void writeRawSpansToFile(Map<String, List<Span>> rawSpans) {
        rawSpans.forEach((testName, rawSpanList) -> {
            try {
                Path testDir = resultDirectory.resolve(testName);
                Files.createDirectories(testDir);
                Path rawTextFile = testDir.resolve("raw-spans.txt");
                StringBuilder sb = new StringBuilder();
                for (Span span : rawSpanList) {
                    sb.append(span.toString()).append(System.lineSeparator());
                }
                Files.writeString(rawTextFile, sb.toString());
                System.out.println("Wrote raw spans for " + testName + " to " + rawTextFile);
            } catch (Exception e) {
                System.err.println("Failed to write raw spans for " + testName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Copies the coverage config used for the run into the result directory ({@code coverage-config.json}), so a
     * run can be understood later even if the config has changed since.
     */
    public void copyCoverageConfig(Path coverageConfigFile) {
        Path target = resultDirectory.resolve("coverage-config.json");
        try {
            java.nio.file.Files.copy(coverageConfigFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied coverage config to " + target);
        } catch (IOException e) {
            System.err.println("Failed to copy coverage config: " + e.getMessage());
        }
    }

    /**
     * Writes the coverage report summary to text files in the result directory.
     *
     * @param report The coverage report to write
     */
    public void writeCoverageReport(CoverageReport report) {
        // writeTextReport(report);
        writeMarkdownReport(report);
        writeJsonReport(report);
    }

    /**
     * Writes run metadata to a JSON file in the result directory.
     *
     * @param totalDuration    Duration for the entire run
     * @param testDuration     Duration for test execution
     * @param testServiceNames Service names where tests originated
     */
    public void writeRunMetadata(Duration totalDuration, Duration testDuration, List<String> testServiceNames) {
        writeRunMetadata(totalDuration, testDuration, testServiceNames, null);
    }

    /**
     * @param coverageConfigFile the coverage config file used for the run (expected status codes), null if none
     */
    public void writeRunMetadata(Duration totalDuration, Duration testDuration, List<String> testServiceNames,
                                 Path coverageConfigFile) {
        Path metadataFile = resultDirectory.resolve("run-metadata.json");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("totalDurationMillis", totalDuration.toMillis());
        metadata.put("testDurationMillis", testDuration.toMillis());
        metadata.put("testServiceNames", testServiceNames);
        metadata.put("coverageConfigFile", coverageConfigFile == null ? null : coverageConfigFile.toString());
        try {
            mapper.writeValue(metadataFile.toFile(), metadata);
            System.out.println("Wrote run metadata JSON to " + metadataFile);
        } catch (IOException e) {
            System.err.println("Failed to write run metadata JSON: " + e.getMessage());
        }
    }

    public void writeJsonReport(CoverageReport report) {
        Path jsonReportFile = resultDirectory.resolve("coverage-report.json");
        try {
            mapper.writeValue(jsonReportFile.toFile(), report);
            System.out.println("Wrote coverage report JSON to " + jsonReportFile);
        } catch (IOException e) {
            System.err.println("Failed to write coverage report JSON: " + e.getMessage());
        }
    }

    private void writeMarkdownReport(CoverageReport report) {
        Path markdownReportFile = resultDirectory.resolve("coverage-report.md");
        try {
            String markdownSummary = markdownWriter.generateDetailedCoverageReport(report);
            Files.writeString(markdownReportFile, markdownSummary);
            System.out.println("Wrote coverage report in markdown format to " + markdownReportFile);
        } catch (IOException e) {
            System.err.println("Failed to write markdown coverage report: " + e.getMessage());
        }
    }

    private void writeTextReport(CoverageReport report) {
        Path basicReportFile = resultDirectory.resolve("coverage-report.txt");
        try {
            String basicSummary = textWriter.generateDetailedCoverageReport(report);
            Files.writeString(basicReportFile, basicSummary);
            System.out.println("Wrote coverage report to " + basicReportFile);
        } catch (IOException e) {
            System.err.println("Failed to write coverage report: " + e.getMessage());
        }
    }

    /**
     * Writes the MicroserviceSystem to a JSON file in the result directory.
     *
     * @param system The MicroserviceSystem to write
     */
    public void writeMicroserviceSystem(MicroserviceSystem system) {
        if (system == null) {
            System.out.println("No MicroserviceSystem to write.");
            return;
        }
        Path outputFile = resultDirectory.resolve("microservice-system.json");
        try {
            mapper.writeValue(outputFile.toFile(), system);
            System.out.println("Wrote MicroserviceSystem to " + outputFile);
        } catch (IOException e) {
            System.err.println("Failed to write MicroserviceSystem: " + e.getMessage());
        }
    }

    /**
     * Writes static communication path analysis in a graph-friendly JSON format.
     *
     * @param pathResult The static analysis result to write
     */
    public void writeStaticAnalysis(CommunicationPathResult pathResult) {
        if (pathResult == null) {
            System.out.println("No CommunicationPathResult to write.");
            return;
        }

        Path outputFile = resultDirectory.resolve("static-analysis.json");
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            String originService = pathResult.getRootService();
            List<CommunicationPath> paths = pathResult.getPaths() == null ? List.of() : pathResult.getPaths();
            Set<String> unresolvedServices = pathResult.getUnresolvedServices() == null
                    ? Set.of()
                    : pathResult.getUnresolvedServices();

            Map<String, Integer> incomingCounts = new LinkedHashMap<>();
            Map<String, Integer> outgoingCounts = new LinkedHashMap<>();
            Set<String> discoveredServices = new LinkedHashSet<>();
            List<Map<String, Object>> pathways = new ArrayList<>();
            List<Map<String, Object>> connections = new ArrayList<>();

            if (originService != null && !originService.isBlank()) {
                discoveredServices.add(originService);
            }

            int pathCounter = 0;
            for (CommunicationPath path : paths) {
                String pathId = normalizePathId(path, pathCounter++);
                List<CommunicationPath.PathSegment> segments = path.getSegments() == null ? List.of() : path.getSegments();
                List<Map<String, Object>> pathwaySegments = new ArrayList<>();

                int segmentIndex = 0;
                for (CommunicationPath.PathSegment segment : segments) {
                    String source = safeValue(segment.getSourceService());
                    String target = safeValue(segment.getDestinationService());
                    String method = pickHttpMethod(segment);
                    String endpointPath = pickEndpointPath(segment);

                    if (!source.isBlank()) {
                        discoveredServices.add(source);
                        outgoingCounts.merge(source, 1, Integer::sum);
                    }
                    if (!target.isBlank()) {
                        discoveredServices.add(target);
                        incomingCounts.merge(target, 1, Integer::sum);
                    }

                    Map<String, Object> segmentPayload = new LinkedHashMap<>();
                    segmentPayload.put("sourceService", source);
                    segmentPayload.put("destinationService", target);
                    segmentPayload.put("method", method);
                    segmentPayload.put("path", endpointPath);
                    segmentPayload.put("destinationUrl", safeValue(segment.getDestinationUrl()));
                    segmentPayload.put("sourceEndpointMethodKey", safeValue(segment.getSourceEndpointMethodKey()));
                    segmentPayload.put("destinationEndpointArguments", segment.getDestinationEndpointArguments() == null
                            ? List.of()
                            : List.copyOf(segment.getDestinationEndpointArguments()));
                    segmentPayload.put("sentPathVariables", segment.getSentPathVariables() == null
                            ? List.of()
                            : List.copyOf(segment.getSentPathVariables()));
                    segmentPayload.put("sentQueryParameters", segment.getSentQueryParameters() == null
                            ? List.of()
                            : List.copyOf(segment.getSentQueryParameters()));
                    segmentPayload.put("requestBodySent", segment.isRequestBodySent());
                    segmentPayload.put("headersSent", segment.isHeadersSent());
                    segmentPayload.put("staticallyMissingArguments", segment.getStaticallyMissingArguments() == null
                            ? List.of()
                            : List.copyOf(segment.getStaticallyMissingArguments()));
                    segmentPayload.put("staticallyOptionalButUnsentArguments",
                            segment.getStaticallyOptionalButUnsentArguments() == null
                                    ? List.of()
                                    : List.copyOf(segment.getStaticallyOptionalButUnsentArguments()));
                    pathwaySegments.add(segmentPayload);

                    Map<String, Object> connectionPayload = new LinkedHashMap<>();
                    connectionPayload.put("id", source + "->" + target + ":" + method + ":" + endpointPath);
                    connectionPayload.put("pathId", pathId);
                    connectionPayload.put("segmentIndex", segmentIndex++);
                    connectionPayload.put("source", source);
                    connectionPayload.put("target", target);
                    connectionPayload.put("method", method);
                    connectionPayload.put("path", endpointPath);
                    connectionPayload.put("destinationUrl", safeValue(segment.getDestinationUrl()));
                    connectionPayload.put("destinationEndpointArguments", segment.getDestinationEndpointArguments() == null
                            ? List.of()
                            : List.copyOf(segment.getDestinationEndpointArguments()));
                    connectionPayload.put("sentPathVariables", segment.getSentPathVariables() == null
                            ? List.of()
                            : List.copyOf(segment.getSentPathVariables()));
                    connectionPayload.put("sentQueryParameters", segment.getSentQueryParameters() == null
                            ? List.of()
                            : List.copyOf(segment.getSentQueryParameters()));
                    connectionPayload.put("requestBodySent", segment.isRequestBodySent());
                    connectionPayload.put("headersSent", segment.isHeadersSent());
                    connectionPayload.put("staticallyMissingArguments", segment.getStaticallyMissingArguments() == null
                            ? List.of()
                            : List.copyOf(segment.getStaticallyMissingArguments()));
                    connectionPayload.put("staticallyOptionalButUnsentArguments",
                            segment.getStaticallyOptionalButUnsentArguments() == null
                                    ? List.of()
                                    : List.copyOf(segment.getStaticallyOptionalButUnsentArguments()));
                    connections.add(connectionPayload);
                }

                Map<String, Object> pathwayPayload = new LinkedHashMap<>();
                pathwayPayload.put("pathId", pathId);
                pathwayPayload.put("originService", safeValue(path.getOriginService()));
                pathwayPayload.put("finalDestinationService", safeValue(path.getFinalDestinationService()));
                pathwayPayload.put("segmentCount", segments.size());
                pathwayPayload.put("metadata", path.getMetadata() == null ? Map.of() : path.getMetadata());
                pathwayPayload.put("segments", pathwaySegments);
                pathways.add(pathwayPayload);
            }

            List<Map<String, Object>> services = new ArrayList<>();
            List<String> sortedServices = discoveredServices.stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            for (String service : sortedServices) {
                Map<String, Object> servicePayload = new LinkedHashMap<>();
                servicePayload.put("name", service);
                servicePayload.put("isOrigin", service.equals(originService));
                servicePayload.put("isUnresolved", unresolvedServices.contains(service));
                servicePayload.put("incomingConnections", incomingCounts.getOrDefault(service, 0));
                servicePayload.put("outgoingConnections", outgoingCounts.getOrDefault(service, 0));
                services.add(servicePayload);
            }

            payload.put("originService", safeValue(originService));
            payload.put("serviceCount", services.size());
            payload.put("connectionCount", connections.size());
            payload.put("analyzedServices", pathResult.getAnalyzedServices() == null
                    ? List.of()
                    : List.copyOf(pathResult.getAnalyzedServices()));
            payload.put("unresolvedServices", List.copyOf(unresolvedServices));
            payload.put("services", services);
            payload.put("pathways", pathways);
            payload.put("connections", connections);

            mapper.writeValue(outputFile.toFile(), payload);
            System.out.println("Wrote static analysis JSON to " + outputFile);
        } catch (IOException e) {
            System.err.println("Failed to write static analysis JSON: " + e.getMessage());
        }
    }

    private String normalizePathId(CommunicationPath path, int fallbackIndex) {
        String hash = path.getPathHash();
        if (hash != null && !hash.isBlank()) {
            return hash;
        }
        String computed = path.computeServiceChainHash();
        if (computed != null && !computed.isBlank()) {
            return computed;
        }
        return "path-" + fallbackIndex;
    }

    private String pickHttpMethod(CommunicationPath.PathSegment segment) {
        String destinationMethod = safeValue(segment.getDestinationHttpMethod());
        if (!destinationMethod.isBlank()) {
            return destinationMethod;
        }
        String sourceMethod = safeValue(segment.getSourceHttpMethod());
        if (!sourceMethod.isBlank()) {
            return sourceMethod;
        }
        return "CALL";
    }

    private String pickEndpointPath(CommunicationPath.PathSegment segment) {
        String destinationUrl = safeValue(segment.getDestinationUrl());
        if (!destinationUrl.isBlank()) {
            int schemeSep = destinationUrl.indexOf("://");
            int pathStart = destinationUrl.indexOf('/', schemeSep >= 0 ? schemeSep + 3 : 0);
            if (pathStart >= 0 && pathStart < destinationUrl.length()) {
                return destinationUrl.substring(pathStart);
            }
        }

        String sourcePath = safeValue(segment.getSourcePath());
        if (!sourcePath.isBlank()) {
            return sourcePath;
        }
        return "/";
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
