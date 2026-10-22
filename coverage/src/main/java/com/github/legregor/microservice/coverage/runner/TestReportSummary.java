package com.github.legregor.microservice.coverage.runner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Summary of the JUnit XML reports of one test run (one test class or method).
 * Used to explain why no spans were collected, e.g. because every test failed in its setup.
 */
public class TestReportSummary {

    /** Report files of the engines that run the tests (the suite engine only repeats them). */
    private static final List<String> ENGINE_REPORTS = List.of("TEST-junit-jupiter.xml", "TEST-junit-vintage.xml");

    private final Path reportDirectory;
    private int tests;
    private int failures;
    private int errors;
    private int skipped;
    private String firstProblem;

    private TestReportSummary(Path reportDirectory) {
        this.reportDirectory = reportDirectory;
    }

    /**
     * Reads the JUnit XML reports in the given directory. Missing or unreadable files are skipped.
     *
     * @param reportDirectory directory passed to the JUnit ConsoleLauncher as --reports-dir
     * @return the summary; all counts are 0 if no report was found
     */
    public static TestReportSummary read(Path reportDirectory) {
        TestReportSummary summary = new TestReportSummary(reportDirectory);
        for (Path report : reportFiles(reportDirectory)) {
            summary.add(report);
        }
        return summary;
    }

    private static List<Path> reportFiles(Path reportDirectory) {
        List<Path> engineReports = ENGINE_REPORTS.stream()
                .map(reportDirectory::resolve)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());
        if (!engineReports.isEmpty() || !Files.isDirectory(reportDirectory)) {
            return engineReports;
        }
        // Other report names (e.g. from a different launcher version): use all TEST-*.xml files
        try (Stream<Path> files = Files.list(reportDirectory)) {
            return files.filter(file -> file.getFileName().toString().matches("TEST-.*\\.xml"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private void add(Path report) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(report.toFile());
            NodeList suites = document.getElementsByTagName("testsuite");
            for (int i = 0; i < suites.getLength(); i++) {
                Element suite = (Element) suites.item(i);
                tests += intAttribute(suite, "tests");
                failures += intAttribute(suite, "failures");
                errors += intAttribute(suite, "errors");
                skipped += intAttribute(suite, "skipped");
            }
            if (firstProblem == null) {
                firstProblem = firstMessage(document, "error");
            }
            if (firstProblem == null) {
                firstProblem = firstMessage(document, "failure");
            }
        } catch (Exception e) {
            System.out.println("[WARN] Could not read JUnit report " + report + ": " + e.getMessage());
        }
    }

    private static int intAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String firstMessage(Document document, String tagName) {
        NodeList problems = document.getElementsByTagName(tagName);
        for (int i = 0; i < problems.getLength(); i++) {
            Element problem = (Element) problems.item(i);
            String message = problem.getAttribute("message");
            if (message != null && !message.isBlank()) {
                return message.lines().findFirst().orElse(message).trim();
            }
            String type = problem.getAttribute("type");
            if (type != null && !type.isBlank()) {
                return type;
            }
        }
        return null;
    }

    public boolean hasReport() {
        return tests > 0;
    }

    public int getTests() {
        return tests;
    }

    public int getFailures() {
        return failures;
    }

    public int getErrors() {
        return errors;
    }

    public int getSkipped() {
        return skipped;
    }

    /** Tests that neither failed, errored nor were skipped. */
    public int getSucceeded() {
        return Math.max(0, tests - failures - errors - skipped);
    }

    /** First error message (or failure message, if there is no error) of the reports, or null. */
    public String getFirstProblem() {
        return firstProblem;
    }

    public Path getReportDirectory() {
        return reportDirectory;
    }

    /**
     * One-line description of the result, e.g.
     * "14 tests: 0 succeeded, 0 failed, 14 errored, 0 skipped (first error: ...)".
     */
    public String describe() {
        if (!hasReport()) {
            return "no JUnit report found in " + reportDirectory;
        }
        String text = String.format("%d tests: %d succeeded, %d failed, %d errored, %d skipped",
                tests, getSucceeded(), failures, errors, skipped);
        if (firstProblem != null) {
            text += " (" + (errors > 0 ? "first error" : "first failure") + ": " + firstProblem + ")";
        }
        return text;
    }
}
