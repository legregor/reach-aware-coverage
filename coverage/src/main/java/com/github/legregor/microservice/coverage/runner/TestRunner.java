package com.github.legregor.microservice.coverage.runner;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * HyperTestRunner is a utility class for executing tests in a Maven-based project.
 * It integrates with the Hypertrace agent for distributed tracing and generates detailed reports.
 */
public class TestRunner {

    private static final Dotenv dotenv = Dotenv.load();

    // HyperTrace agent configuration using OpenTelemetry VM options
    private static final List<String> VM_OPTIONS = List.of(
            "-javaagent:" + dotenv.get("HYPERTRACE_AGENT_PATH"),
            "-Dotel.propagators=tracecontext,baggage,b3",
            "-Dotel.javaagent.debug=true",
            "-Dotel.instrumentation.common.default-enabled=true",
            "-Dotel.instrumentation.spring-web.enabled=true");

    // Environment variables for Hypertrace to set the Hypertrace config file to
    // allow for request/response body tracing
    private static final Map<String, String> ENV_VARIABLES = Map.of(
            "HT_CONFIG_FILE",
            dotenv.get("HYPERTRACE_CONFIG_PATH"));

    /**
     * Runs the specified test files and generates JUnit test reports.
     * Returns the list of executed tests to retrieve the corresponding spans from Jaeger.
     *
     * @param testFiles A list of paths to test files (optionally with method names).
     * @param reportsDirectory The directory where test reports will be stored.
     * @return A list of service names corresponding to the executed tests.
     * @throws Exception If an error occurs during test execution.
     */
    public static List<String> runTests(List<Path> testFiles, String reportsDirectory) throws Exception {
        if (testFiles == null || testFiles.isEmpty()) {
            failureExit(
                    "Usage: java HyperTestRunner <absolute-path-to-Test.java>[#methodName] [<absolute-path-to-Test.java>[#methodName]...]");
        }

        List<String> testNames = new ArrayList<>();
        for (Path testFile : testFiles) {
            System.out.println("Running test: " + testFile);
            String testName = runTest(testFile, reportsDirectory);
            testNames.add(testName);
        }
        return testNames;
    }

    /**
     * Executes a single test file and generates a report.
     *
     * @param file The path to the test file.
     * @param reportsDirectory The directory where the test report will be stored.
     * @return The service name corresponding to the executed test.
     * @throws Exception If an error occurs during test execution.
     */
    private static String runTest(Path file, String reportsDirectory) throws Exception {

        Path module = locateModuleDir(file);

        compileTests(module);

        String fqn = pathToClassName(file);
        System.out.println("-> Running " + fqn);

        String cp = getClassPath(module);

        // build the serviceName for OpenTelemetry tracing
        String serviceName = fqn.replace('#', '/');
        Path microserviceDirectory = module.getFileName();
        if (microserviceDirectory != null) {
            serviceName = microserviceDirectory.toString() + "/" + serviceName;
        }

        // Set up and execute the test process with Hypertrace/OpenTelemetry tracing enabled
        List<String> cmd = createTestCommand(fqn, cp, serviceName, reportsDirectory + "/" + serviceName);

        // Log the Java binary being used
        System.out.println("[INFO] Test subprocess using java: " + cmd.get(0));
        System.out.println("[INFO] Service name for tracing: " + serviceName);

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        Map<String, String> environment = processBuilder.environment();
        environment.putAll(ENV_VARIABLES);
        processBuilder.inheritIO().start().waitFor();

        return serviceName;
    }

    /**
     * Locates the root directory of the module containing the specified test file.
     *
     * @param p The path to the test file.
     * @return The root directory of the module.
     */
    private static Path locateModuleDir(Path p) {
        String[] parts = extractFileAndMethod(p.toString());
        p = Paths.get(parts[0]);

        while (p != null) {
            if (Files.isDirectory(p.resolve("src/test/java")))
                return p;
            p = p.getParent();
        }
        failureExit("Could not find module root (src/test/java) for " + p);
        return null;
    }

    /**
     * Compiles the test resources and classes for the specified module.
     *
     * @param module The root directory of the module.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the compilation process is interrupted.
     */
    private static void compileTests(Path module) throws IOException, InterruptedException {
        // First, process test resources to ensure application.yml is copied to target/test-classes
        List<String> mvnResourcesCmd = List.of(
                "mvn", "-q", "-f", module.resolve("pom.xml").toString(), "process-test-resources");
        int resourcesStatus = new ProcessBuilder(mvnResourcesCmd).inheritIO().start().waitFor();
        if (resourcesStatus != 0) {
            System.err.println("Warning: Maven process-test-resources failed for module: " + module);
        }
        
        // Then compile tests
        List<String> mvnCompileCmd = List.of(
                "mvn", "-q", "-f", module.resolve("pom.xml").toString(), "test-compile");
        int status = new ProcessBuilder(mvnCompileCmd).inheritIO().start().waitFor();
        if (status != 0) {
            failureExit("Maven test-compile failed for module: " + module + "\nCommand: "
                    + String.join(" ", mvnCompileCmd));
        }
        System.out.println("Maven test-compile succeeded for module: " + module);
    }

    /**
     * Converts a file path to its fully qualified class name.
     *
     * @param path The path to the test file.
     * @return The fully qualified class name, optionally with a method name.
     */
    private static String pathToClassName(Path path) {
        String pathStr = path.toString();

        // Use the utility method to extract file path and method name
        String[] parts = extractFileAndMethod(pathStr);
        String filePath = parts[0];
        String methodName = parts[1];

        // If we extracted a method name, update the path
        if (methodName != null) {
            path = Paths.get(filePath);
        }

        String unix = path.toString().replace(File.separatorChar, '/');
        Matcher m = Pattern.compile("src/(test|main)/java/(.*?)(\\.java|\\.class)$").matcher(unix);
        if (!m.find())
            failureExit("Path must be under src/*/java: " + path);

        String className = m.group(2).replace('/', '.');
        // Return className#methodName if method was specified
        return methodName != null ? className + "#" + methodName : className;
    }

    /**
     * Constructs the classpath of the test JVM: the JUnit console launcher, then the module's test classes, main
     * classes and dependencies. The coverage tool's own jar is not on it, so its libraries cannot replace the
     * versions the system under test uses.
     *
     * @param module The root directory of the module.
     * @return The constructed classpath.
     * @throws URISyntaxException If the URI syntax is invalid.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the process is interrupted.
     */
    private static String getClassPath(Path module) throws URISyntaxException, IOException, InterruptedException {
        String launcherJar = findConsoleLauncherJar().toString();
        String deps = buildClasspath(module); // from Maven
        String mainCp = module.resolve("target/classes").toString();
        String testCp = module.resolve("target/test-classes").toString();

        // Put test-classes first so Spring Boot finds test application.yml before main application.yml
        return String.join(File.pathSeparator, launcherJar, testCp, mainCp, deps);
    }

    /**
     * The junit-platform-console-standalone jar that {@code mvn package} copies to {@code target/lib}, next to the
     * tool's jar (or next to {@code target/classes} when run from an IDE).
     */
    static Path findConsoleLauncherJar() throws URISyntaxException, IOException {
        Path self = Paths.get(TestRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path libDirectory = self.toAbsolutePath().getParent().resolve("lib");
        if (Files.isDirectory(libDirectory)) {
            try (DirectoryStream<Path> jars = Files.newDirectoryStream(libDirectory,
                    "junit-platform-console-standalone-*.jar")) {
                for (Path jar : jars) {
                    return jar;
                }
            }
        }
        failureExit("JUnit console launcher not found in " + libDirectory
                + "; build the coverage tool with 'mvn package' in coverage/ (run-coverage.sh does this).");
        return null;
    }

    /**
     * Creates the command to execute a test using the JUnit ConsoleLauncher.
     *
     * @param fqn The fully qualified name of the test class or method.
     * @param cp The classpath for the test execution.
     * @param serviceName The service name for distributed tracing.
     * @param reportsDirectory The directory where test reports will be stored.
     * @return A list of command arguments.
     */
    private static List<String> createTestCommand(String fqn, String cp, String serviceName, String reportsDirectory) {
        List<String> cmd = new ArrayList<>();
        // Use the same java binary as the coverage runner to ensure consistent JVM version
        String javaHome = System.getProperty("java.home");
        String javaBinary = Paths.get(javaHome, "bin", "java").toString();
        cmd.add(javaBinary);

        cmd.add("--add-opens");
        cmd.add("java.base/java.nio=ALL-UNNAMED");
        cmd.add("--add-opens");
        cmd.add("java.base/jdk.internal.misc=ALL-UNNAMED");
        cmd.addAll(VM_OPTIONS);

        boolean isMethodTest = fqn.contains("#");

        // Ensure Spring uses test profile and loads test resources
        cmd.add("-Dspring.profiles.active=test");
        // Explicitly tell Spring where to find configuration files
        cmd.add("-Dspring.config.location=classpath:/");
        cmd.add("-Dspring.config.name=application");
        // Additional logging to debug configuration loading
        cmd.add("-Dlogging.level.org.springframework.boot.context.config=DEBUG");
        cmd.add("-Dlogging.level.org.springframework.boot.autoconfigure.mongo=DEBUG");
        cmd.add("-Dotel.service.name=" + serviceName);
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add("org.junit.platform.console.ConsoleLauncher");
        cmd.add("execute");

        // Use select-method or select-class based on whether a method is specified
        if (isMethodTest) {
            cmd.add("--select-method");
        } else {
            cmd.add("--select-class");
        }
        cmd.add(fqn);
        // Add reports-dir for JUnit XML output
        cmd.add("--reports-dir");
        cmd.add(reportsDirectory);
        return cmd;
    }

    /**
     * Builds the classpath for the specified module using Maven.
     *
     * @param module The root directory of the module.
     * @return The constructed classpath.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the process is interrupted.
     */
    private static String buildClasspath(Path module) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(
                "mvn",
                "-f", module.resolve("pom.xml").toString(),
                "dependency:build-classpath",
                "-Dmdep.includeScope=test",
                "-Dmdep.path" // prints CP
        )
                .directory(module.toFile())
                .redirectErrorStream(true)
                .start();

        String cp = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            for (String line; (line = br.readLine()) != null;) {
                // Maven 3.9+: path is on the very next line after the marker
                if (line.startsWith("[INFO] Dependencies classpath:")) {
                    cp = br.readLine(); // <-- the path itself
                    break;
                }
                // Maven 4.0-alpha: path is on the same line after the colon
                if (line.contains("Dependencies classpath:")) {
                    cp = line.substring(line.indexOf(':') + 1).trim();
                    break;
                }
            }
        }
        if (cp == null || cp.isBlank())
            throw new IllegalStateException("Could not read class-path from Maven output");
        p.waitFor();
        return cp;
    }

    /**
     * Exits the program with an error message.
     *
     * @param msg The error message to display.
     */
    private static void failureExit(String msg) {
        System.err.println(msg);
        if (!msg.contains("Usage:")) {
            System.err.println(
                    "\nUsage: java HyperTestRunner <absolute-path-to-Test.java>[#methodName] [<absolute-path-to-Test.java>[#methodName]...]");
            System.err.println("Example to run a class: java HyperTestRunner /path/to/MyTest.java");
            System.err.println("Example to run a method: java HyperTestRunner /path/to/MyTest.java#testMethodName");
        }
        System.exit(1);
    }

    /**
     * Extracts the file path and method name from a combined path.
     *
     * @param path The combined path that may include a method name (e.g., "Test.java#methodName").
     * @return A string array where [0] is the file path and [1] is the method name (or null if none).
     */
    private static String[] extractFileAndMethod(String path) {
        String[] result = new String[2];

        // Initialize with defaults
        result[0] = path;
        result[1] = null;

        // Check if we have a method specifier after .java
        int javaExtPos = path.lastIndexOf(".java");
        if (javaExtPos > 0) {
            int methodSeparatorPos = path.indexOf('#', javaExtPos);
            if (methodSeparatorPos > 0) {
                result[0] = path.substring(0, methodSeparatorPos);
                result[1] = path.substring(methodSeparatorPos + 1);
            }
        }

        return result;
    }

}
