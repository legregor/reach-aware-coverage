package com.github.legregor.microservice.coverage;

import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import com.github.legregor.microservice.coverage.config.model.GraphSelection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoverageAppSelectionTest {

    @Test
    void applySelectedConnectionsKeepsAllWhenSelectionIsMissing() {
        MicroserviceSystem system = systemWithTwoConnections();
        CoverageConfig config = new CoverageConfig();

        CoverageApp.applySelectedConnections(system, config);

        assertEquals(2, system.getRestConnections().size());
    }

    @Test
    void applySelectedConnectionsFiltersByExactConnectionId() {
        MicroserviceSystem system = systemWithTwoConnections();
        CoverageConfig config = new CoverageConfig();
        GraphSelection graphSelection = new GraphSelection();
        graphSelection.setSelectedConnections(List.of(
                "ts-cancel-service->ts-order-service:GET:/api/v1/orderservice/order/{orderId}"
        ));
        config.setGraphSelection(graphSelection);

        CoverageApp.applySelectedConnections(system, config);

        assertEquals(1, system.getRestConnections().size());
        assertEquals("ts-order-service", system.getRestConnections().get(0).getDestination());
    }

    @Test
    void applySelectedConnectionsMatchesCalledUrlPathVariant() {
        MicroserviceSystem system = systemWithTwoConnections();
        CoverageConfig config = new CoverageConfig();
        GraphSelection graphSelection = new GraphSelection();
        graphSelection.setSelectedConnections(List.of(
                "ts-cancel-service->ts-order-service:GET:/api/v1/orderservice/order/42"
        ));
        config.setGraphSelection(graphSelection);

        CoverageApp.applySelectedConnections(system, config);

        assertEquals(1, system.getRestConnections().size());
        assertEquals("ts-order-service", system.getRestConnections().get(0).getDestination());
    }

    private static MicroserviceSystem systemWithTwoConnections() {
        RestConnection orderConnection = new RestConnection(
                "ts-cancel-service",
                "ts-order-service",
                new RestCall("GET", "http://ts-order-service:12031/api/v1/orderservice/order/42", null),
                new RestEndpoint("GET", "/api/v1/orderservice/order/{orderId}", List.of(), null)
        );
        RestConnection userConnection = new RestConnection(
                "ts-cancel-service",
                "ts-user-service",
                new RestCall("GET", "http://ts-user-service:12342/api/v1/userservice/users/42", null),
                new RestEndpoint("GET", "/api/v1/userservice/users/{userId}", List.of(), null)
        );

        return new MicroserviceSystem(
                new ArrayList<>(),
                new ArrayList<>(List.of(orderConnection, userConnection))
        );
    }
}
