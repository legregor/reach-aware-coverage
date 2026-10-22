package com.github.legregor.microservice.coverage.runner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link TestReportSummary}: reading the JUnit XML reports of a test run. */
class TestReportSummaryTest {

    @TempDir
    Path reportDirectory;

    @Test
    void allTestsErroredInSetup() throws IOException {
        write("TEST-junit-jupiter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="JUnit Jupiter" tests="2" skipped="0" failures="0" errors="2" time="381.8">
                  <testcase name="a()" classname="preserve.PreserveTest" time="0">
                    <error message="Container startup failed for image local/ts-price-service:0.1" type="org.testcontainers.containers.ContainerLaunchException">stack</error>
                  </testcase>
                  <testcase name="b()" classname="preserve.PreserveTest" time="0">
                    <error message="Container startup failed for image local/ts-price-service:0.1" type="org.testcontainers.containers.ContainerLaunchException">stack</error>
                  </testcase>
                </testsuite>
                """);
        // The suite engine report repeats the same tests and must not be counted twice
        write("TEST-junit-platform-suite.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="JUnit Platform Suite" tests="2" skipped="0" failures="0" errors="2"/>
                """);

        TestReportSummary summary = TestReportSummary.read(reportDirectory);

        assertTrue(summary.hasReport());
        assertEquals(2, summary.getTests());
        assertEquals(2, summary.getErrors());
        assertEquals(0, summary.getSucceeded());
        assertEquals("Container startup failed for image local/ts-price-service:0.1", summary.getFirstProblem());
        assertEquals("2 tests: 0 succeeded, 0 failed, 2 errored, 0 skipped "
                + "(first error: Container startup failed for image local/ts-price-service:0.1)", summary.describe());
    }

    @Test
    void jupiterAndVintageReportsAreAdded() throws IOException {
        write("TEST-junit-jupiter.xml", """
                <testsuite name="JUnit Jupiter" tests="3" skipped="1" failures="1" errors="0">
                  <testcase name="a()"><failure message="expected: &lt;200&gt; but was: &lt;500&gt;"/></testcase>
                </testsuite>
                """);
        write("TEST-junit-vintage.xml", """
                <testsuite name="JUnit Vintage" tests="2" skipped="0" failures="0" errors="0"/>
                """);

        TestReportSummary summary = TestReportSummary.read(reportDirectory);

        assertEquals(5, summary.getTests());
        assertEquals(3, summary.getSucceeded());
        assertEquals("expected: <200> but was: <500>", summary.getFirstProblem());
        assertTrue(summary.describe().contains("(first failure: expected: <200> but was: <500>)"));
    }

    @Test
    void otherReportNamesAreUsedWhenNoEngineReportExists() throws IOException {
        write("TEST-cancel.CancelOrderTest.xml", """
                <testsuite tests="4" skipped="0" failures="0" errors="0"/>
                """);

        TestReportSummary summary = TestReportSummary.read(reportDirectory);

        assertEquals(4, summary.getTests());
        assertEquals(4, summary.getSucceeded());
        assertNull(summary.getFirstProblem());
    }

    @Test
    void missingDirectoryGivesEmptySummary() {
        TestReportSummary summary = TestReportSummary.read(reportDirectory.resolve("does-not-exist"));

        assertFalse(summary.hasReport());
        assertEquals(0, summary.getTests());
        assertTrue(summary.describe().startsWith("no JUnit report found in "));
    }

    @Test
    void unreadableReportIsSkipped() throws IOException {
        write("TEST-junit-jupiter.xml", "not xml");

        TestReportSummary summary = TestReportSummary.read(reportDirectory);

        assertFalse(summary.hasReport());
    }

    private void write(String fileName, String content) throws IOException {
        Files.writeString(reportDirectory.resolve(fileName), content);
    }
}
