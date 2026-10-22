package com.github.legregor.microservice.coverage.analysis.util;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Utility for dynamically discovering microservices in the file system.
 * 
 * Services are expected to be located at:
 * $MICROSERVICES_ROOT/{service-name}
 * 
 * This locator validates that discovered services exist and are accessible.
 */
@Slf4j
public class ServiceLocator {
    
    @Getter
    private final Path microservicesRoot;
    
    private static Dotenv dotenv;
    
    /**
     * Constructs a ServiceLocator using the MICROSERVICES_ROOT environment variable.
     * 
     * @throws IllegalStateException if MICROSERVICES_ROOT is not set or invalid
     */
    public ServiceLocator() {
        String rootPath = getEnvironmentVariable("MICROSERVICES_ROOT");
        if (rootPath == null || rootPath.isEmpty()) {
            throw new IllegalStateException("MICROSERVICES_ROOT environment variable is not set");
        }
        
        this.microservicesRoot = Paths.get(rootPath).toAbsolutePath();
        
        if (!Files.isDirectory(this.microservicesRoot)) {
            throw new IllegalStateException(
                    "MICROSERVICES_ROOT does not point to a valid directory: " + rootPath);
        }
        
        log.info("ServiceLocator initialized with MICROSERVICES_ROOT: {}", this.microservicesRoot);
    }
    
    /**
     * Constructs a ServiceLocator with an explicit root path (useful for testing).
     * 
     * @param rootPath The root directory containing services
     * @throws IllegalStateException if rootPath is not a valid directory
     */
    public ServiceLocator(Path rootPath) {
        this.microservicesRoot = rootPath.toAbsolutePath();
        
        if (!Files.isDirectory(this.microservicesRoot)) {
            throw new IllegalStateException(
                    "Provided microservices root does not point to a valid directory: " + rootPath);
        }
        
        log.info("ServiceLocator initialized with root path: {}", this.microservicesRoot);
    }
    
    /**
     * Gets an environment variable, first checking system properties, then .env file.
     */
    private static String getEnvironmentVariable(String key) {
        // First try system environment
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Then try .env file
        try {
            if (dotenv == null) {
                dotenv = Dotenv.load();
            }
            return dotenv.get(key);
        } catch (Exception e) {
            log.debug("Could not load .env file: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Locates a service by name.
     * 
     * @param serviceName The name of the service to locate
     * @return Optional containing the Path to the service root if found
     */
    public Optional<Path> locateService(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            log.warn("Cannot locate service with null or empty name");
            return Optional.empty();
        }
        
        Path servicePath = microservicesRoot.resolve(serviceName);
        
        if (!Files.isDirectory(servicePath)) {
            log.debug("Service not found at expected location: {}", servicePath);
            return Optional.empty();
        }
        
        log.debug("Located service {} at {}", serviceName, servicePath);
        return Optional.of(servicePath);
    }
    
    /**
     * Checks if a service exists and is accessible.
     * 
     * @param serviceName The name of the service to check
     * @return true if the service exists and is a directory, false otherwise
     */
    public boolean serviceExists(String serviceName) {
        return locateService(serviceName).isPresent();
    }
    
    /**
     * Attempts to locate a service, throwing an exception if not found.
     * 
     * @param serviceName The name of the service to locate
     * @return The Path to the service root
     * @throws IllegalArgumentException if the service cannot be found
     */
    public Path locateServiceOrThrow(String serviceName) {
        return locateService(serviceName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Service '" + serviceName + "' not found in MICROSERVICES_ROOT: " 
                        + microservicesRoot));
    }
}
