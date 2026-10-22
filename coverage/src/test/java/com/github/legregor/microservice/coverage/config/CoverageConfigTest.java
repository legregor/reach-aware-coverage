package com.github.legregor.microservice.coverage.config;

import com.github.legregor.microservice.coverage.config.model.CoverageConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link CoverageConfig}: which expected status codes apply to a connection. */
class CoverageConfigTest {

    private static final List<String> ORDER_PATHS = List.of("/api/v1/orderservice/order/{orderId}",
            "http://ts-order-service:12031/api/v1/orderservice/order/{orderId}");

    @Test
    void overrideMatchesTheEndpointPathOrTheCalledUrl() throws IOException {
        CoverageConfig byTemplate = config("""
                {"connectionOverrides": [{"sourceService": "ts-cancel-service", "destinationService": "ts-order-service",
                  "method": "get", "path": "/api/v1/orderservice/order/{orderId}", "expectedStatusCodes": ["200", "404"]}]}
                """);
        CoverageConfig byUrl = config("""
                {"connectionOverrides": [{"path": "http://ts-order-service:12031/api/v1/orderservice/order/{orderId}",
                  "expectedStatusCodes": ["2xx"]}]}
                """);

        assertEquals(List.of("200", "404"),
                byTemplate.getExpectedStatusCodesFor("ts-cancel-service", "ts-order-service", "GET", ORDER_PATHS));
        assertEquals(List.of("2xx"),
                byUrl.getExpectedStatusCodesFor("ts-cancel-service", "ts-order-service", "GET", ORDER_PATHS));
    }

    @Test
    void missingFieldsOfAnOverrideMatchAnything() throws IOException {
        CoverageConfig config = config("""
                {"globalExpectedStatusCodes": ["2xx"],
                 "connectionOverrides": [{"destinationService": "ts-order-service", "expectedStatusCodes": ["2xx", "5xx"]}]}
                """);

        assertEquals(List.of("2xx", "5xx"),
                config.getExpectedStatusCodesFor("ts-cancel-service", "ts-order-service", "PUT", ORDER_PATHS));
        // another destination: the global list
        assertEquals(List.of("2xx"),
                config.getExpectedStatusCodesFor("ts-cancel-service", "ts-user-service", "GET", List.of("/users")));
    }

    @Test
    void firstMatchingOverrideWinsAndEmptyOverridesAreSkipped() throws IOException {
        CoverageConfig config = config("""
                {"connectionOverrides": [
                  {"destinationService": "ts-order-service", "expectedStatusCodes": []},
                  {"destinationService": "ts-order-service", "method": "GET", "expectedStatusCodes": ["200"]},
                  {"destinationService": "ts-order-service", "expectedStatusCodes": ["4xx"]}]}
                """);

        assertEquals(List.of("200"), config.getExpectedStatusCodesFor("a", "ts-order-service", "GET", ORDER_PATHS));
        assertEquals(List.of("4xx"), config.getExpectedStatusCodesFor("a", "ts-order-service", "PUT", ORDER_PATHS));
    }

    @Test
    void webUiConfigWithoutStatusCodesUsesTheDefaults() throws IOException {
        // the file the web UI writes: only name, services and tests
        CoverageConfig config = config("""
                {"configName": "CancelConfig", "services": [{"serviceName": "ts-cancel-service"}],
                 "tests": [{"path": "cancel/integration/get/CancelOrderTest.java"}]}
                """);
        CoverageConfig explicitNull = config("""
                {"globalExpectedStatusCodes": null, "connectionOverrides": null}
                """);

        assertEquals(List.of("2xx", "4xx"), config.getExpectedStatusCodesFor("a", "b", "GET", ORDER_PATHS));
        assertEquals(List.of("2xx", "4xx"), explicitNull.getExpectedStatusCodesFor("a", "b", "GET", ORDER_PATHS));
        assertTrue(explicitNull.getConnectionOverrides().isEmpty());
    }

    @Test
    void graphSelectionIsParsedAndNormalized() throws IOException {
        CoverageConfig config = config("""
                {"graphSelection": {
                   "selectedServices": [" ts-cancel-service ", "ts-order-service", "ts-order-service", ""],
                   "selectedConnections": [" a->b:GET:/orders ", "a->b:GET:/orders", " ", null]
                }}
                """);

        assertEquals(List.of("ts-cancel-service", "ts-order-service"),
                config.getGraphSelection().getSelectedServices());
        assertEquals(List.of("a->b:GET:/orders"),
                config.getGraphSelection().getSelectedConnections());
    }

    @Test
    void entriesThatCanNeverMatchAreReported() throws IOException {
        CoverageConfig config = config("""
                {"globalExpectedStatusCodes": ["2xx", "4XX", "404", "status=0", "result.code=OK", "success", "4xxx"],
                 "connectionOverrides": [{"expectedStatusCodes": ["600", "=1"]}]}
                """);

        String hint = " is neither a status code (e.g. 404), a class (e.g. 4xx) nor a body outcome (e.g. status=0)";
        assertEquals(List.of(
                "globalExpectedStatusCodes: 'success'" + hint,
                "globalExpectedStatusCodes: '4xxx'" + hint,
                "connectionOverrides[0].expectedStatusCodes: '600'" + hint,
                "connectionOverrides[0].expectedStatusCodes: '=1'" + hint),
                CoverageConfigReader.validate(config));
    }

    @Test
    void shortFieldNamesAreRejectedWithASuggestion() throws IOException {
        Path file = Files.createTempFile("coverage-config", ".json");
        Files.writeString(file, """
                {"connectionOverrides": [{"source": "shop-order-service", "expectedStatusCodes": ["200"]}]}
                """);

        CoverageConfigException e = assertThrows(CoverageConfigException.class,
                () -> new CoverageConfigReader().read(file));

        assertTrue(e.getMessage().contains("unknown field 'source' in connectionOverrides[0]"), e.getMessage());
        assertTrue(e.getMessage().contains("did you mean 'sourceService'?"), e.getMessage());
    }

    @Test
    void unknownFieldGivesAReadableMessage() throws IOException {
        Path file = Files.createTempFile("coverage-config", ".json");
        Files.writeString(file, """
                {"connectionOverrides": [{"sourceService": "a"}, {"targetService": "b", "expectedStatusCodes": ["200"]}]}
                """);

        CoverageConfigException e = assertThrows(CoverageConfigException.class,
                () -> new CoverageConfigReader().read(file));

        assertTrue(e.getMessage().contains("unknown field 'targetService' in connectionOverrides[1]"), e.getMessage());
        assertTrue(e.getMessage().contains("destinationService"), e.getMessage());
    }

    @Test
    void invalidJsonGivesAReadableMessage() throws IOException {
        Path file = Files.createTempFile("coverage-config", ".json");
        Files.writeString(file, "{\"globalExpectedStatusCodes\": [\"2xx\",]");

        CoverageConfigException e = assertThrows(CoverageConfigException.class,
                () -> new CoverageConfigReader().read(file));

        assertTrue(e.getMessage().contains("is not valid (line 1"), e.getMessage());
    }

    private static CoverageConfig config(String json) throws IOException {
        Path file = Files.createTempFile("coverage-config", ".json");
        Files.writeString(file, json);
        return new CoverageConfigReader().read(file);
    }
}
