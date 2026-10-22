package com.github.legregor.microservice.coverage.analysis.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceLocatorTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void locatesExistingService() throws Exception {
        Path serviceDir = tempDir.resolve("my-service");
        Files.createDirectory(serviceDir);
        
        ServiceLocator locator = new ServiceLocator(tempDir);
        var result = locator.locateService("my-service");
        
        assertTrue(result.isPresent());
        assertEquals(serviceDir, result.get());
    }
    
    @Test
    void returnsEmptyForNonExistentService() {
        ServiceLocator locator = new ServiceLocator(tempDir);
        var result = locator.locateService("non-existent-service");
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void returnsEmptyForNullOrEmptyServiceName() {
        ServiceLocator locator = new ServiceLocator(tempDir);
        
        assertFalse(locator.locateService(null).isPresent());
        assertFalse(locator.locateService("").isPresent());
    }
    
    @Test
    void checksServiceExistence() throws Exception {
        Path serviceDir = tempDir.resolve("my-service");
        Files.createDirectory(serviceDir);
        
        ServiceLocator locator = new ServiceLocator(tempDir);
        
        assertTrue(locator.serviceExists("my-service"));
        assertFalse(locator.serviceExists("non-existent"));
    }
    
    @Test
    void throwsExceptionWhenServiceNotFound() {
        ServiceLocator locator = new ServiceLocator(tempDir);
        
        assertThrows(IllegalArgumentException.class, () -> {
            locator.locateServiceOrThrow("non-existent");
        });
    }
    
    @Test
    void locatesServiceOrThrowWhenExists() throws Exception {
        Path serviceDir = tempDir.resolve("my-service");
        Files.createDirectory(serviceDir);
        
        ServiceLocator locator = new ServiceLocator(tempDir);
        Path result = locator.locateServiceOrThrow("my-service");
        
        assertEquals(serviceDir, result);
    }
    
    @Test
    void throwsExceptionForInvalidRootPath() {
        Path invalidPath = tempDir.resolve("non-existent-root");
        
        assertThrows(IllegalStateException.class, () -> {
            new ServiceLocator(invalidPath);
        });
    }
}
