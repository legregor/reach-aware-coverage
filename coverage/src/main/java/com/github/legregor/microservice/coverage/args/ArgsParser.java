package com.github.legregor.microservice.coverage.args;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line argument parser for microservice integration test coverage
 * analysis.
 * This class parses command-line arguments and categorizes them into two types:
 * <ul>
 * <li>Microservice folders - directories containing microservice code for
 * static analysis</li>
 * <li>Test files - Java test files (with optional method names) to execute for
 * coverage analysis</li>
 * <li>Config Flag - if set, save evaluation run under given config name</li>
 * <li>Config Name</li>
 * <li>Force Reanalyze Flag - if set, ignore cache and re-analyze services</li>
 * </ul>
 */
public class ArgsParser {

    private final List<Path> microserviceFolders;
    private final List<Path> testFiles;
    private String configName;
    private Path openApiDir;
    private boolean staticOnly;
    private boolean forceReanalyze;

    private Path configFilePath;

    public ArgsParser(String[] args) {
        microserviceFolders = new ArrayList<>();
        testFiles = new ArrayList<>();
        configName = null;
        openApiDir = null;
        configFilePath = null;
        staticOnly = false;
        forceReanalyze = false;
        parseArgs(args);
    }

    /**
     * Parses the arguments and categorizes them as either microservice folders or
     * test files
     * 
     * @param args Command-line arguments to process
     */
    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // Handle --config-file flag (before --config, which shares the prefix)
            if (arg.equals("--config-file") || arg.startsWith("--config-file=")) {
                String file = null;
                if (arg.equals("--config-file")) {
                    if (i + 1 < args.length) {
                        file = args[++i];
                    } else {
                        System.err.println("Warning: --config-file flag requires a file path");
                    }
                } else {
                    file = arg.substring("--config-file=".length());
                }
                if (file != null && !file.isEmpty()) {
                    configFilePath = Paths.get(file).toAbsolutePath().normalize();
                    if (!Files.isRegularFile(configFilePath)) {
                        System.err.println("Warning: coverage config file not found: " + configFilePath);
                    }
                }
                continue;
            }

            // Handle --config flag (allow both "--config name" and "--config=name")
            if (arg.startsWith("--config")) {
                if (arg.equals("--config")) {
                    if (i + 1 < args.length) {
                        configName = args[++i];
                    } else {
                        System.err.println("Warning: --config flag requires a config name");
                    }
                } else if (arg.startsWith("--config=")) {
                    configName = arg.substring("--config=".length());
                    if (configName.isEmpty()) {
                        System.err.println("Warning: --config= requires a non-empty config name");
                    }
                } else {
                    // Unknown variation, warn and continue
                    System.err.println("Warning: Unrecognized config flag format: " + arg);
                }
                continue;
            }

            if (arg.startsWith("--openapi-dir")) {
                if (arg.equals("--openapi-dir")) {
                    if (i + 1 < args.length) {
                        openApiDir = Paths.get(args[++i]).toAbsolutePath().normalize();
                        if (!Files.isDirectory(openApiDir)) {
                            System.err.println("Warning: OpenAPI directory not found: " + openApiDir);
                        }
                    } else {
                        System.err.println("Warning: --openapi-dir flag requires a directory path");
                    }
                } else if (arg.startsWith("--openapi-dir=")) {
                    String dir = arg.substring("--openapi-dir=".length());
                    if (dir.isEmpty()) {
                        System.err.println("Warning: --openapi-dir= requires a non-empty directory path");
                    } else {
                        openApiDir = Paths.get(dir).toAbsolutePath().normalize();
                        if (!Files.isDirectory(openApiDir)) {
                            System.err.println("Warning: OpenAPI directory not found: " + openApiDir);
                        }
                    }
                } else {
                    System.err.println("Warning: Unrecognized openapi-dir flag format: " + arg);
                }
                continue;
            }

            if (arg.equals("--force-reanalyze")) {
                forceReanalyze = true;
                continue;
            }

            if (arg.equals("--static")) {
                staticOnly = true;
                continue;
            }

            // Check if this might be a test file with a method name
            if (arg.contains("#")) {
                String filePart = arg.substring(0, arg.indexOf('#'));
                if (filePart.endsWith(".java")) {
                    Path fileOnlyPath = Paths.get(filePart).toAbsolutePath().normalize();
                    if (Files.isRegularFile(fileOnlyPath)) {
                        testFiles.add(Paths.get(arg).toAbsolutePath().normalize());
                        continue;
                    }
                }
            }

            Path path = Paths.get(arg).toAbsolutePath().normalize();

            if (Files.isDirectory(path)) {
                microserviceFolders.add(path);
            } else if (Files.isRegularFile(path) && arg.endsWith(".java")) {
                testFiles.add(path);
            } else if (!Files.exists(path)) {
                System.err.println("Warning: Path does not exist: " + path);
            } else {
                System.err.println("Warning: Ignoring unknown argument: " + arg);
            }
        }
    }

    public List<Path> getMicroserviceFolders() {
        return microserviceFolders;
    }

    public List<Path> getTestFiles() {
        return testFiles;
    }

    /**
     * Checks if any arguments were parsed successfully
     * 
     * @return true if either microservice folders or test files were found
     */
    public boolean hasValidArgs() {
        return !microserviceFolders.isEmpty() || !testFiles.isEmpty();
    }

    /**
     * Whether a config name was provided via --config
     *
     * @return true if a config name was set
     */
    public boolean hasConfig() {
        return configName != null && !configName.isEmpty();
    }

    /**
     * Returns the provided config name, or null if none was provided
     *
     * @return config name or null
     */
    public String getConfigName() {
        return configName;
    }

    public Path getOpenApiDir() {
        return openApiDir;
    }

    /**
     * The coverage config file (expected status codes): the file given with {@code --config-file}, otherwise the
     * config of the web UI for the {@code --config} name, {@code storage/configs/<name>.json} (relative to the
     * working directory, the repository root), if it exists. Null if there is none (defaults apply).
     */
    public Path getConfigPath() {
        if (configFilePath != null) {
            return configFilePath;
        }
        if (hasConfig()) {
            Path byName = Paths.get("storage", "configs", configName + ".json").toAbsolutePath().normalize();
            if (Files.isRegularFile(byName)) {
                return byName;
            }
        }
        return null;
    }
    public boolean isStaticOnly() {
        return staticOnly;
    }

    public boolean isForceReanalyze() {
        return forceReanalyze;
    }

    /**
     * Provides usage information for the command
     * 
     * @return String with usage information
     */
    public static String getUsage() {
        return "Usage: java -jar target/coverage-1.0.0.jar [<microservice-folder>...] [<test-file.java>[#methodName]...] [--config <name>] [--config-file <path>] [--openapi-dir <path>] [--static] [--force-reanalyze]\n"
                +
                "  - <microservice-folder>: Path to a microservice root folder to analyze statically\n" +
                "  - <test-file.java>: Path to a Java test file to run\n" +
                "  - <test-file.java#methodName>: Path to a Java test file with a specific method to run\n" +
                "  - --force-reanalyze: force re-analysis of services, ignoring cache\n" +
                "  - --config <name> or --config=<name>: save evaluation run under given config name\n" +
                "    (expected status codes are read from storage/configs/<name>.json if it exists)\n" +
                "  - --config-file <path> or --config-file=<path>: coverage config file to use instead\n" +
                "  - --openapi-dir <path> or --openapi-dir=<path>: directory with slim OpenAPI specs\n" +
                "  - --static: enable static-only mode";
    }
}
