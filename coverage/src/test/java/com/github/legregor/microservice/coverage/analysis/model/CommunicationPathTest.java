package com.github.legregor.microservice.coverage.analysis.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunicationPathTest {
    
    @Test
    void computesServiceChainHash() {
        CommunicationPath path = new CommunicationPath();
        
        CommunicationPath.PathSegment segment1 = new CommunicationPath.PathSegment();
        segment1.setSourceService("serviceA");
        segment1.setSourcePath("/a");
        segment1.setDestinationService("serviceB");
        segment1.setDestinationHttpMethod("GET");
        segment1.setDestinationUrl("http://serviceB/api");
        
        CommunicationPath.PathSegment segment2 = new CommunicationPath.PathSegment();
        segment2.setSourceService("serviceB");
        segment2.setSourcePath("/b");
        segment2.setDestinationService("serviceC");
        segment2.setDestinationHttpMethod("POST");
        segment2.setDestinationUrl("http://serviceC/api");
        
        path.setSegments(List.of(segment1, segment2));
        
        String hash = path.computeServiceChainHash();
        assertEquals("serviceA|/a->serviceB|GET|http://serviceB/api->serviceC|POST|http://serviceC/api", hash);
    }
    
    @Test
    void getsOriginService() {
        CommunicationPath path = new CommunicationPath();
        
        CommunicationPath.PathSegment segment = new CommunicationPath.PathSegment();
        segment.setSourceService("serviceA");
        segment.setDestinationService("serviceB");
        
        path.setSegments(List.of(segment));
        
        assertEquals("serviceA", path.getOriginService());
    }
    
    @Test
    void getsFinalDestinationService() {
        CommunicationPath path = new CommunicationPath();
        
        CommunicationPath.PathSegment segment1 = new CommunicationPath.PathSegment();
        segment1.setSourceService("serviceA");
        segment1.setDestinationService("serviceB");
        
        CommunicationPath.PathSegment segment2 = new CommunicationPath.PathSegment();
        segment2.setSourceService("serviceB");
        segment2.setDestinationService("serviceC");
        
        path.setSegments(List.of(segment1, segment2));
        
        assertEquals("serviceC", path.getFinalDestinationService());
    }
    
    @Test
    void handlesEmptySegments() {
        CommunicationPath path = new CommunicationPath();
        path.setSegments(List.of());
        
        assertEquals("", path.getOriginService());
        assertEquals("", path.getFinalDestinationService());
        assertEquals("", path.computeServiceChainHash());
    }
}

class CommunicationPathResultTest {
    
    @Test
    void getsAllServiceNames() {
        CommunicationPathResult result = new CommunicationPathResult();
        
        CommunicationPath path = new CommunicationPath();
        CommunicationPath.PathSegment segment = new CommunicationPath.PathSegment();
        segment.setSourceService("serviceA");
        segment.setDestinationService("serviceB");
        path.setSegments(List.of(segment));
        
        result.setPaths(List.of(path));
        
        var services = result.getAllServiceNames();
        assertTrue(services.contains("serviceA"));
        assertTrue(services.contains("serviceB"));
    }
    
    @Test
    void filtersPathsByOriginService() {
        CommunicationPathResult result = new CommunicationPathResult();
        
        CommunicationPath path1 = new CommunicationPath();
        CommunicationPath.PathSegment segment1 = new CommunicationPath.PathSegment();
        segment1.setSourceService("serviceA");
        segment1.setDestinationService("serviceB");
        path1.setSegments(List.of(segment1));
        
        CommunicationPath path2 = new CommunicationPath();
        CommunicationPath.PathSegment segment2 = new CommunicationPath.PathSegment();
        segment2.setSourceService("serviceC");
        segment2.setDestinationService("serviceD");
        path2.setSegments(List.of(segment2));
        
        result.setPaths(List.of(path1, path2));
        
        var pathsFromA = result.getPathsFrom("serviceA");
        assertEquals(1, pathsFromA.size());
        assertEquals("serviceA", pathsFromA.get(0).getOriginService());
    }
    
    @Test
    void filtersPathsByDestinationService() {
        CommunicationPathResult result = new CommunicationPathResult();
        
        CommunicationPath path1 = new CommunicationPath();
        CommunicationPath.PathSegment segment1 = new CommunicationPath.PathSegment();
        segment1.setSourceService("serviceA");
        segment1.setDestinationService("serviceB");
        path1.setSegments(List.of(segment1));
        
        CommunicationPath path2 = new CommunicationPath();
        CommunicationPath.PathSegment segment2 = new CommunicationPath.PathSegment();
        segment2.setSourceService("serviceC");
        segment2.setDestinationService("serviceD");
        path2.setSegments(List.of(segment2));
        
        result.setPaths(List.of(path1, path2));
        
        var pathsToB = result.getPathsTo("serviceB");
        assertEquals(1, pathsToB.size());
        assertEquals("serviceB", pathsToB.get(0).getFinalDestinationService());
    }
    
    @Test
    void filtersPathsByLength() {
        CommunicationPathResult result = new CommunicationPathResult();
        
        CommunicationPath path1 = new CommunicationPath();
        CommunicationPath.PathSegment segment = new CommunicationPath.PathSegment();
        segment.setSourceService("serviceA");
        segment.setDestinationService("serviceB");
        path1.setSegments(List.of(segment));
        
        CommunicationPath path2 = new CommunicationPath();
        CommunicationPath.PathSegment segment1 = new CommunicationPath.PathSegment();
        segment1.setSourceService("serviceA");
        segment1.setDestinationService("serviceB");
        CommunicationPath.PathSegment segment2 = new CommunicationPath.PathSegment();
        segment2.setSourceService("serviceB");
        segment2.setDestinationService("serviceC");
        path2.setSegments(List.of(segment1, segment2));
        
        result.setPaths(List.of(path1, path2));
        
        var pathsLength1 = result.getPathsByLength(1);
        assertEquals(1, pathsLength1.size());
        
        var pathsLength2 = result.getPathsByLength(2);
        assertEquals(1, pathsLength2.size());
    }
}
