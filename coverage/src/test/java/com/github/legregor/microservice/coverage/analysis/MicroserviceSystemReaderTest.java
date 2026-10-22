package com.github.legregor.microservice.coverage.analysis;

import com.github.legregor.microservice.coverage.analysis.model.Microservice;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;


import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MicroserviceSystemReaderTest {

    @Test
    void readsMicroserviceSystemWithNestedRestConnections() throws Exception {
        String fileName = "microservice-system.json";
        //String fileName = "new-microservice-system.json";
        String filePath = "src/test/resources/" + fileName;

        //Test whether it also works to read from the storage folder
        //String fileName = "microservice-system.json";
        //String filePath = "../storage/runs/CancelConfig/20260306_110506/" + fileName;

        Path path = Paths.get(filePath);

        MicroserviceSystem system = new MicroserviceSystemReader().read(path);

        assertFalse(system.getMicroservices().isEmpty());
        assertEquals(5, system.getMicroservices().size());
        assertFalse(system.getRestConnections().isEmpty());
        List<String> microservices = Arrays.asList("ts-cancel-service",
                "ts-order-service",
                "ts-inside-payment-service",
                "ts-user-service",
                "ts-notification-service");


        for ( Microservice microservice : system.getMicroservices()
             ) {
            //check that names of the microservices are correct
            assertTrue(microservices.contains(microservice.getName()));

        }

        RestConnection connection = system.getRestConnections().get(0);
        assertNotNull(connection.getCall());
        assertNotNull(connection.getEndpoint());
        assertNotNull(connection.getEndpoint().getArguments());
        assertEquals("ts-cancel-service", connection.getSource());
        assertEquals("ts-order-service", connection.getDestination());
    }

    @Test
    void readsMicroserviceSystemWithSegments() throws Exception {
        String fileName = "new-microservice-system.json";
        String filePath = "src/test/resources/" + fileName;

        Path path = Paths.get(filePath);

        MicroserviceSystem system = new MicroserviceSystemReader().read(path);

        assertFalse(system.getMicroservices().isEmpty());
        assertEquals(7, system.getMicroservices().size());
        assertFalse(system.getRestConnections().isEmpty());
        List<String> microservices = Arrays.asList("ts-cancel-service",
                "ts-order-service",
                "ts-inside-payment-service",
                "ts-notification-service", "ts-payment-service", "ts-order-other-service",
                "ts-station-service");


        for ( Microservice microservice : system.getMicroservices()
        ) {
            //check that names of the microservices are correct
            System.out.println(microservice.getName());
            assertTrue(microservices.contains(microservice.getName()));

        }

        RestConnection connection = system.getRestConnections().get(0);
        assertNotNull(connection.getCall());
        assertNotNull(connection.getEndpoint());
        assertNotNull(connection.getEndpoint().getArguments());
        assertEquals("ts-cancel-service", connection.getSource());
        assertEquals("ts-order-service", connection.getDestination());
    }
}
