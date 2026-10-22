package com.github.legregor.microservice.coverage;

import com.github.legregor.microservice.coverage.config.CoverageConfigException;
import com.github.legregor.microservice.coverage.config.CoverageConfigReader;
import com.github.legregor.microservice.coverage.config.EntryEndpointResolver;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import com.github.legregor.microservice.coverage.config.model.EntryEndpoint;
import com.github.legregor.microservice.coverage.config.model.GraphSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.legregor.microservice.coverage.analysis.CommunicationPathConverter;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.trace.RecursiveStaticServiceTraceAnalyzer;
import com.github.legregor.microservice.coverage.analysis.MicroserviceSystemReader;
import com.github.legregor.microservice.coverage.args.ArgsParser;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageEvaluator;
import com.github.legregor.microservice.coverage.evaluation.overallCoverage.CoverageReport;
import com.github.legregor.microservice.coverage.output.OutputWriter;
import com.github.legregor.microservice.coverage.output.TextCoverageReportWriter;
import com.github.legregor.microservice.coverage.runner.TestReportSummary;
import com.github.legregor.microservice.coverage.runner.TestRunner;
import com.github.legregor.microservice.coverage.trace.JaegerTraceClient;
import com.github.legregor.microservice.coverage.trace.JaegerTraceProcessor;
import com.github.legregor.microservice.coverage.trace.model.CoverageSpan;

import io.jaegertracing.api_v2.Model.Span;

public class CoverageApp {

    // Limit for number of traces
    private static final int LIMIT = 100;

    private Path resultDirectory;
    private LocalDateTime startTime;
    private OutputWriter outputWriter;
    private long runStartNanos;
    private Duration testDuration = Duration.ZERO;
    private List<String> testServiceNames = List.of();
    private final RecursiveStaticServiceTraceAnalyzer recursiveAnalyzer = new RecursiveStaticServiceTraceAnalyzer();

    /**
     * Main method of the coverage application.
     *
     * @param args Command line arguments
     * @throws Exception if an error occurs during execution
     */
    public void run(String[] args) throws Exception {
        runStartNanos = System.nanoTime();
        startTime = LocalDateTime.now();
        ArgsParser argsParser = new ArgsParser(args);
        resultDirectory = createResultDirectory(argsParser);
        outputWriter = new OutputWriter(resultDirectory);
        if (!argsParser.hasValidArgs()) {
            System.out.println(ArgsParser.getUsage());
            System.exit(1);
        }
        if(argsParser.isForceReanalyze()){
            recursiveAnalyzer.setForceReanalysis(true);
        }
        // Read the coverage config before the analysis and the tests, so a broken config does not waste a test run;
        // its entry endpoints restrict the reachability analysis
        Path configFilePath = getConfigFilePath(argsParser);
        CoverageConfig coverageConfig = loadCoverageConfig(configFilePath);
        List<EntryEndpoint> entryEndpoints = resolveEntryEndpoints(coverageConfig, argsParser);

        if(argsParser.isStaticOnly()){
            CommunicationPathResult staticSystem = performStaticAnalysis(argsParser.getMicroserviceFolders(),
                    entryEndpoints);
            checkEntryEndpoints(staticSystem);
            outputWriter.writeStaticAnalysis(staticSystem);
            // also the full model (services with all endpoints and calls), as in a coverage run
            try {
                outputWriter.writeMicroserviceSystem(new CommunicationPathConverter().convertToSystem(staticSystem));
            } catch (Exception e) {
                System.out.println("Could not write the microservice system of the static analysis: " + e.getMessage());
            }
            System.out.println("[STORAGE PATH] static-analysis");
            System.exit(0);
        }
        MicroserviceSystem system = getMicroserviceSystem(argsParser.getMicroserviceFolders(), coverageConfig,
                entryEndpoints);
        checkEntryEndpoints(system.getCommunicationPathResult());

        List<String> testNames = runTests(argsParser);
        Map<String, List<CoverageSpan>> allSpans = getSpans(testNames);
        if (!testNames.isEmpty() && (!checkTestResults(testNames) || !checkCollectedSpans(testNames, allSpans))) {
            System.exit(1);
        }
        // Calculate coverage metrics
        evaluateCoverage(system, allSpans, getOpenApiDir(argsParser), configFilePath, coverageConfig);
    }



    private CommunicationPathResult performStaticAnalysis(List<Path> microserviceFolders,
                                                          List<EntryEndpoint> entryEndpoints) throws Exception {
        if (microserviceFolders.size() == 1) {
            return recursiveAnalyzer.analyzeRecursively(microserviceFolders.get(0), entryEndpoints);
        } else {
            System.out.println("Static Analysis requires exactly one service path, none or too many found. Skipping static analysis.");
            return new CommunicationPathResult();
        }
    }

    /**
     * Creates a directory for storing coverage results
     *
     * @return Path to the created directory
     * @throws IOException if the directory cannot be created
     */
    private Path createResultDirectory(ArgsParser argsParser) throws IOException {
        if(argsParser.hasConfig() && argsParser.isStaticOnly()){
            var staticDir = Paths.get("storage", "runs", argsParser.getConfigName());
            Files.createDirectories(staticDir);
            return staticDir.toAbsolutePath();
        }
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now());
        Path outputDir = argsParser.hasConfig() ? Paths.get("storage", "runs", argsParser.getConfigName(), timestamp)
                : Paths.get("coverage-results", timestamp);
        Files.createDirectories(outputDir);
        return outputDir.toAbsolutePath();
    }

    /**
     * Runs the tests specified in the command line arguments.
     *
     * @param argsParser Parsed command line arguments
     * @return List of test names that were executed
     * @throws Exception if an error occurs during test execution
     */
    private List<String> runTests(ArgsParser argsParser) throws Exception {
        List<Path> testFiles = argsParser.getTestFiles();
        if (!testFiles.isEmpty()) {
            testServiceNames = extractTestServiceNames(testFiles);
            long testStartNanos = System.nanoTime();
            List<String> testNames = TestRunner.runTests(testFiles, resultDirectory.toString());
            testDuration = Duration.ofNanos(System.nanoTime() - testStartNanos);
            return testNames;
        } else {
            testDuration = Duration.ZERO;
            testServiceNames = List.of();
            System.out.println("No test files provided. Skipping test execution.");
            return List.of();
        }
    }

    /**
     * Gets and processes spans for the given test names from Jaeger.
     *
     * @param testNames List of test names to fetch spans for
     * @return Map of test names to their corresponding spans
     */
    private Map<String, List<CoverageSpan>> getSpans(List<String> testNames) {
        Map<String, List<CoverageSpan>> allSpans = new HashMap<>();
        Map<String, List<Span>> allRawSpans = new HashMap<>();

        // Calculate lookback period from start time
        Duration lookback = calculateLookback();
        System.out.println("Using lookback period: " + lookback);

        for (String testName : testNames) {
            List<Span> rawSpans = JaegerTraceClient.findSpans(testName,
                    lookback, LIMIT);
            List<CoverageSpan> spans = JaegerTraceProcessor.processSpans(rawSpans);
            allSpans.put(testName, spans);
            allRawSpans.put(testName, rawSpans);
        }
        outputWriter.writeSpansToJson(allSpans);
        outputWriter.writeRawSpansToFile(allRawSpans);

        return allSpans;
    }

    /**
     * Checks that spans were collected and explains missing spans with the JUnit reports.
     * Prints a warning for each test run without spans.
     *
     * @param testNames Names of the executed test runs (also their report subdirectories)
     * @param allSpans  Collected spans per test run
     * @return false if no spans were collected at all, so the coverage cannot be evaluated
     */
    /**
     * Warns about failed or errored tests and stops if every test of every test class errored (none succeeded or
     * failed an assertion): the spans of such a run only come from the test setup (e.g. container health checks),
     * so the coverage would be meaningless. Failed assertions still count: their calls did happen.
     */
    private boolean checkTestResults(List<String> testNames) {
        boolean anySucceeded = false; // succeeded or failed an assertion, i.e. the test body ran
        boolean anyReport = false;
        for (String testName : testNames) {
            TestReportSummary summary = TestReportSummary.read(resultDirectory.resolve(testName));
            if (!summary.hasReport()) {
                continue;
            }
            anyReport = true;
            if (summary.getSucceeded() > 0 || summary.getFailures() > 0) {
                anySucceeded = true;
            }
            if (summary.getFailures() > 0 || summary.getErrors() > 0) {
                System.out.println("[WARN] " + testName + ": " + summary.describe()
                        + ". Failed and errored tests only contribute the calls made before the problem. JUnit report: "
                        + summary.getReportDirectory());
            }
        }
        if (anyReport && !anySucceeded) {
            System.out.println("[ERROR] All tests errored before or during their setup (see the first error above)."
                    + " Coverage evaluation skipped.");
            return false;
        }
        return true;
    }

    private boolean checkCollectedSpans(List<String> testNames, Map<String, List<CoverageSpan>> allSpans) {
        int totalSpans = 0;
        for (String testName : testNames) {
            List<CoverageSpan> spans = allSpans.getOrDefault(testName, List.of());
            totalSpans += spans.size();
            if (!spans.isEmpty()) {
                continue;
            }
            TestReportSummary summary = TestReportSummary.read(resultDirectory.resolve(testName));
            System.out.println("[WARN] 0 spans collected for " + testName + ": " + summary.describe());
            if (summary.hasReport() && summary.getSucceeded() == 0 && summary.getFailures() == 0) {
                System.out.println("[WARN] No test got past its setup (e.g. a container did not start). See the JUnit report: "
                        + summary.getReportDirectory());
            } else if (summary.hasReport()) {
                System.out.println("[WARN] Tests ran but no spans arrived. Check that Jaeger is running and that "
                        + "HYPERTRACE_AGENT_PATH / HYPERTRACE_CONFIG_PATH point to the agent and test-config.yaml. "
                        + "JUnit report: " + summary.getReportDirectory());
            }
        }
        if (totalSpans == 0) {
            System.out.println("[ERROR] 0 spans collected for all test runs. Coverage evaluation skipped.");
            return false;
        }
        return true;
    }

    /**
     * Calculates the lookback period based on the start time of the run method.
     * 
     * @return Duration representing the lookback period from start time to now
     */
    private Duration calculateLookback() {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(startTime, now);

        duration = duration.plusMinutes(1);

        return duration;
    }

    /**
     * Converts microservice folders to MicroserviceSystem using recursive static analysis
     * and saves the results.
     *
     * @param microserviceFolders List of directories containing microservice source
     *                            code
     * @return The converted MicroserviceSystem from recursive static analysis
     * @throws Exception if analysis or conversion fails
     */
    private MicroserviceSystem getMicroserviceSystem(List<Path> microserviceFolders, CoverageConfig config,
                                                     List<EntryEndpoint> entryEndpoints) throws Exception {
        /*if (microserviceFolders.size() != 1) {
            System.out.println("Microservice System analysis requires exactly one service path for recursive analysis.");
            return new MicroserviceSystem();
        } */

        try {
            // The cached system was written for the whole entry service, so it is only reused when no
            // entry endpoints restrict the analysis
            MicroserviceSystem system = entryEndpoints == null || entryEndpoints.isEmpty()
                    ? loadMicroserviceSystem(config) : null;
            if (system == null) {
                CommunicationPathResult pathResult = recursiveAnalyzer.analyzeRecursively(microserviceFolders.get(0),
                        entryEndpoints);
                system = new CommunicationPathConverter().convertToSystem(pathResult);
            }
            applySelectedConnections(system, config);
            outputWriter.writeMicroserviceSystem(system);
            return system;
        } catch (Exception e) {
            System.out.println("Error during recursive static analysis: " + e.getMessage());
            e.printStackTrace();
            return new MicroserviceSystem();
        }
    }

    private MicroserviceSystem loadMicroserviceSystem(CoverageConfig config) {
        Path cachePath = getMicroserviceSystemPath(config);
        if (cachePath == null || !Files.isRegularFile(cachePath)) {
            return null;
        }
        try {
            System.out.println("[INFO] Using cached microservice system: " + cachePath);
            return new MicroserviceSystemReader().read(cachePath);
        } catch (IOException e) {
            System.out.println("[WARN] Failed to read cached microservice system (" + cachePath + "): " + e.getMessage()
                    + ". Falling back to fresh static analysis.");
            return null;
        }
    }

    private Path getMicroserviceSystemPath(CoverageConfig config) {
        if (config == null || config.getConfigName() == null || config.getConfigName().isBlank()) {
            return null;
        }
        return Paths.get("storage", "runs", config.getConfigName(), "microservice-system.json")
                .toAbsolutePath()
                .normalize();
    }

    static void applySelectedConnections(MicroserviceSystem system, CoverageConfig config) {
        if (system == null || system.getRestConnections() == null || config == null) {
            return;
        }
        GraphSelection graphSelection = config.getGraphSelection();
        if (graphSelection == null || graphSelection.getSelectedConnections().isEmpty()) {
            return;
        }
        Set<String> selectedConnectionIds = new LinkedHashSet<>(graphSelection.getSelectedConnections());
        List<RestConnection> filtered = system.getRestConnections().stream()
                .filter(connection -> matchesSelectedConnection(connection, selectedConnectionIds))
                .toList();
        system.setRestConnections(new ArrayList<>(filtered));
    }

    private static boolean matchesSelectedConnection(RestConnection connection, Set<String> selectedConnectionIds) {
        if (connection == null) {
            return false;
        }
        String canonicalId = connectionId(connection);
        if (selectedConnectionIds.contains(canonicalId)) {
            return true;
        }
        for (String selectedId : selectedConnectionIds) {
            ParsedConnectionId parsed = parseConnectionId(selectedId);
            if (parsed != null && parsed.matches(connection)) {
                return true;
            }
        }
        return false;
    }

    private static String connectionId(RestConnection connection) {
        return safe(connection.getSource()) + "->" + safe(connection.getDestination()) + ":"
                + connectionMethod(connection) + ":" + endpointPathOrUrl(connection);
    }

    private static String connectionMethod(RestConnection connection) {
        if (connection.getEndpoint() != null && connection.getEndpoint().getMethod() != null
                && !connection.getEndpoint().getMethod().isBlank()) {
            return connection.getEndpoint().getMethod();
        }
        if (connection.getCall() != null && connection.getCall().getMethod() != null
                && !connection.getCall().getMethod().isBlank()) {
            return connection.getCall().getMethod();
        }
        return "CALL";
    }

    private static String endpointPathOrUrl(RestConnection connection) {
        if (connection.getEndpoint() != null && connection.getEndpoint().getPath() != null
                && !connection.getEndpoint().getPath().isBlank()) {
            return connection.getEndpoint().getPath();
        }
        if (connection.getCall() != null && connection.getCall().getUrl() != null && !connection.getCall().getUrl().isBlank()) {
            return connection.getCall().getUrl();
        }
        return "/";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static ParsedConnectionId parseConnectionId(String value) {
        if (value == null) {
            return null;
        }
        int sourceTargetSeparator = value.indexOf("->");
        if (sourceTargetSeparator < 1) {
            return null;
        }
        int targetMethodSeparator = value.indexOf(':', sourceTargetSeparator + 2);
        if (targetMethodSeparator < 0) {
            return null;
        }
        int methodPathSeparator = value.indexOf(':', targetMethodSeparator + 1);
        if (methodPathSeparator < 0) {
            return null;
        }
        String source = value.substring(0, sourceTargetSeparator).trim();
        String target = value.substring(sourceTargetSeparator + 2, targetMethodSeparator).trim();
        String method = value.substring(targetMethodSeparator + 1, methodPathSeparator).trim();
        String path = value.substring(methodPathSeparator + 1).trim();
        if (source.isEmpty() || target.isEmpty() || method.isEmpty()) {
            return null;
        }
        return new ParsedConnectionId(source, target, method, path.isEmpty() ? "/" : path);
    }

    private static final class ParsedConnectionId {
        private final String source;
        private final String target;
        private final String method;
        private final String path;

        private ParsedConnectionId(String source, String target, String method, String path) {
            this.source = source;
            this.target = target;
            this.method = method;
            this.path = path;
        }

        private boolean matches(RestConnection connection) {
            if (!source.equals(connection.getSource()) || !target.equals(connection.getDestination())) {
                return false;
            }
            if (!method.equalsIgnoreCase(connectionMethod(connection))) {
                return false;
            }
            String expectedPath = normalizePath(path);
            for (String candidatePath : candidatePaths(connection)) {
                if (normalizePath(candidatePath).equals(expectedPath)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static List<String> candidatePaths(RestConnection connection) {
        List<String> paths = new ArrayList<>();
        if (connection.getEndpoint() != null && connection.getEndpoint().getPath() != null) {
            paths.add(connection.getEndpoint().getPath());
        }
        if (connection.getCall() != null && connection.getCall().getUrl() != null) {
            paths.add(connection.getCall().getUrl());
        }
        if (connection.getCalls() != null) {
            for (RestCall call : connection.getCalls()) {
                if (call != null && call.getUrl() != null) {
                    paths.add(call.getUrl());
                }
            }
        }
        return paths;
    }

    private static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        String withoutQuery = rawPath.split("[?#]", 2)[0].trim();
        int schemeSeparator = withoutQuery.indexOf("://");
        String path = withoutQuery;
        if (schemeSeparator >= 0) {
            int pathStart = withoutQuery.indexOf('/', schemeSeparator + 3);
            path = pathStart < 0 ? "/" : withoutQuery.substring(pathStart);
        }
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * Evaluates the coverage of REST flows based on the captured spans and static
     * analysis results.
     *
     * @param system   The microservice system from static analysis
     * @param allSpans Map of test names to their corresponding spans
     */
    private void evaluateCoverage(MicroserviceSystem system, Map<String, List<CoverageSpan>> allSpans,
            Path openApiDir, Path configFilePath, CoverageConfig coverageConfig) {
        if (system == null) {
            System.out.println("No MicroserviceSystem analytics available for coverage evaluation.");
            return;
        }
        if (allSpans.isEmpty()) {
            System.out.println("No spans available for coverage evaluation.");
            return;

        }

        // Create evaluator and generate enhanced report with detailed usage statistics
        CoverageEvaluator evaluator = new CoverageEvaluator(openApiDir, coverageConfig);
        CoverageReport report = evaluator.evaluateCoverage(system, allSpans);

        // Print basic coverage report summary to console using the dedicated writer
        TextCoverageReportWriter textWriter = new TextCoverageReportWriter();
        System.out.println(textWriter.generateCoverageSummary(report));

        // Write both basic and detailed coverage reports to files
        outputWriter.writeCoverageReport(report);
        Duration totalDuration = Duration.ofNanos(System.nanoTime() - runStartNanos);
        outputWriter.writeRunMetadata(totalDuration, testDuration, testServiceNames, configFilePath);
        if (configFilePath != null) {
            outputWriter.copyCoverageConfig(configFilePath);
        }
        System.out.println("[STORAGE PATH] " + resultDirectory);
    }

    private List<String> extractTestServiceNames(List<Path> testFiles) {
        Set<String> serviceNames = new LinkedHashSet<>();
        for (Path testFile : testFiles) {
            Path normalizedTestFile = stripMethodSuffix(testFile);
            Path moduleDir = locateModuleDir(normalizedTestFile);
            if (moduleDir != null && moduleDir.getFileName() != null) {
                serviceNames.add(moduleDir.getFileName().toString());
            }
        }
        return List.copyOf(serviceNames);
    }

    private Path stripMethodSuffix(Path testFile) {
        String path = testFile.toString();
        int javaExtPos = path.lastIndexOf(".java");
        if (javaExtPos > 0) {
            int methodSeparatorPos = path.indexOf('#', javaExtPos);
            if (methodSeparatorPos > 0) {
                path = path.substring(0, methodSeparatorPos);
            }
        }
        return Paths.get(path);
    }

    private Path locateModuleDir(Path testFile) {
        Path current = testFile;
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/test/java"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private Path getOpenApiDir(ArgsParser argsParser) {
        Path configured = argsParser.getOpenApiDir();
        if (configured != null) {
            return configured;
        }

        Path defaultDir = Paths.get("slim_open_api").toAbsolutePath();
        if (Files.isDirectory(defaultDir)) {
            return defaultDir;
        }

        System.out.println("No OpenAPI spec folder (" + defaultDir + " does not exist, set one with --openapi-dir): "
                + "running without OpenAPI specs.");
        return null;
    }

    /** Reads the coverage config (null = defaults); stops the run with a readable message if it is invalid. */
    private CoverageConfig loadCoverageConfig(Path configFilePath) {
        if (configFilePath == null) {
            return null;
        }
        try {
            return new CoverageConfigReader().read(configFilePath);
        } catch (CoverageConfigException e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.err.println("[HINT] Fix the config file or run without it; the tests were not started.");
            System.exit(1);
            return null;
        }
    }

    /**
     * The entry endpoints of the run: the union of the {@code entryEndpoints} of the executed config tests (empty:
     * every endpoint of the entry service); stops the run with a readable message if the config is inconsistent.
     */
    private List<EntryEndpoint> resolveEntryEndpoints(CoverageConfig coverageConfig, ArgsParser argsParser) {
        List<Path> folders = argsParser.getMicroserviceFolders();
        String rootService = folders.isEmpty() || folders.get(0).getFileName() == null ? null
                : folders.get(0).getFileName().toString();
        try {
            EntryEndpointResolver.Resolution resolution = EntryEndpointResolver.resolve(coverageConfig,
                    argsParser.getTestFiles(), rootService);
            resolution.notes().forEach(note -> System.out.println("[INFO] Entry endpoints: " + note));
            return resolution.entryEndpoints();
        } catch (CoverageConfigException e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.err.println("[HINT] Fix the config file; the tests were not started.");
            System.exit(1);
            return List.of();
        }
    }

    /**
     * Stops the run if a configured entry endpoint matches no endpoint of the entry service (a typo would otherwise
     * silently remove its connections), listing the endpoints that exist; prints the entry endpoints used.
     */
    private void checkEntryEndpoints(CommunicationPathResult result) {
        if (result == null) {
            return;
        }
        if (!result.getUnmatchedEntryEndpoints().isEmpty()) {
            System.err.println("[ERROR] coverage config: entry endpoint(s) " + result.getUnmatchedEntryEndpoints()
                    + " match no endpoint of " + result.getRootService());
            if (result.getStaticTraceResult() != null) {
                System.err.println("[HINT] Endpoints of " + result.getRootService() + ": "
                        + result.getStaticTraceResult().getEndpointTraces().stream()
                        .map(endpoint -> endpoint.getHttpMethod() + " " + endpoint.getPath())
                        .collect(java.util.stream.Collectors.joining(", ")));
            }
            System.exit(1);
        }
        if (!result.getEntryEndpoints().isEmpty()) {
            System.out.println("Entry endpoints of " + result.getRootService() + ": "
                    + String.join(", ", result.getEntryEndpoints()));
        } else {
            System.out.println("Entry endpoints: all endpoints of " + result.getRootService());
        }
    }

    private Path getConfigFilePath(ArgsParser argsParser) {
        Path configured = argsParser.getConfigPath();
        if (configured != null) {
            System.out.println("Using coverage config " + configured);
            return configured;
        }
        System.out.println("No coverage config file" + (argsParser.hasConfig()
                ? " (storage/configs/" + argsParser.getConfigName() + ".json does not exist)" : "")
                + ": expected status codes default to 2xx and 4xx.");
        return null;
    }
}
