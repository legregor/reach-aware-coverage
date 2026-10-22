package com.github.legregor.microservice.coverage.evaluation.overallCoverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.legregor.microservice.coverage.evaluation.responseField.ObservedResponse;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldCoverage;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldNotApplicableReason;
import com.github.legregor.microservice.coverage.evaluation.responseField.ResponseFieldTreeBuilder;
import com.github.legregor.microservice.coverage.openapi.OpenApiResponseResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import com.github.legregor.microservice.coverage.evaluation.connection.ConnectionReport;

/**
 * Totals of response field coverage in {@link CoverageReport}, with the called connections K1-K4 of the sample-shop
 * (extension R1-R6) and the responses of its tests T1-T6; expected values from the sample-shop's EXPECTED.md.
 */
class CoverageReportResponseFieldTest {
    private static final double DELTA = 1e-9;
    private static final List<String> DEFAULT_EXPECTED = List.of("2xx", "4xx");

    @TempDir
    Path specDir;

    private ResponseFieldTreeBuilder builder;

    @BeforeEach
    void writeSpecs() throws IOException {
        Files.writeString(specDir.resolve("inventory.json"), """
                {"openapi": "3.0.3",
                 "paths": {"/api/v1/inventory/reservations": {"post": {"responses": {
                   "201": {"description": "created", "content": {"application/json": {"schema": {
                     "type": "object", "required": ["reservationId", "orderId", "createdAt"], "properties": {
                       "reservationId": {"type": "string"}, "orderId": {"type": "string"},
                       "createdAt": {"type": "string"}, "estimatedDelivery": {"type": "string"}}}}}},
                   "400": {"description": "invalid", "content": {"application/json": {"schema": {
                     "type": "object", "required": ["message"], "properties": {
                       "message": {"type": "string"},
                       "fieldErrors": {"type": "array", "items": {"type": "object", "required": ["field", "message"],
                         "properties": {"field": {"type": "string"}, "message": {"type": "string"}}}},
                       "hint": {"type": "string"}}}}}},
                   "409": {"description": "conflict", "content": {"application/json": {"schema": {
                     "type": "object", "required": ["message"], "properties": {
                       "message": {"type": "string"}, "availableQuantity": {"type": "integer"}}}}}}}}}}}
                """);
        Files.writeString(specDir.resolve("pricing.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/api/v1/pricing/quotes": {"post": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["quoteId", "currency", "total"], "properties": {
                         "quoteId": {"type": "string"}, "currency": {"type": "string"},
                         "total": {"type": "number"}, "discountApplied": {"type": "number"}}}}}}}}},
                   "/api/v1/pricing/prices/{sku}": {"get": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["sku", "price"], "properties": {
                         "sku": {"type": "string"}, "price": {"type": "number"}}}}}},
                     "404": {"$ref": "#/components/responses/NotFound"}}}},
                   "/api/v1/pricing/discounts/{code}": {"get": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["code", "percent"], "properties": {
                         "code": {"type": "string"}, "percent": {"type": "integer"}}}}}},
                     "400": {"$ref": "#/components/responses/NotFound"},
                     "404": {"$ref": "#/components/responses/NotFound"}}}}},
                 "components": {"responses": {"NotFound": {"description": "error", "content": {
                   "application/json": {"schema": {"type": "object", "required": ["message"], "properties": {
                     "message": {"type": "string"}}}}}}}}}
                """);
        builder = new ResponseFieldTreeBuilder(new OpenApiResponseResolver(new OpenApiSpecRegistry(specDir)));
    }

    @Test
    void sampleShopTotals() {
        CoverageReport coverageReport = sampleShopReport();

        assertEquals(19.0 / 21 * 100, coverageReport.getResponseFieldPooledCoveragePercentage(), DELTA);
        assertEquals((10.0 / 11 * 100 + 75 + 100 + 100) / 4,
                coverageReport.getResponseFieldMeanPooledCoveragePercentage(), DELTA);
        assertEquals(((1 + 2.0 / 3 + 1) / 3 * 100 + 75 + 100 + 100) / 4,
                coverageReport.getResponseFieldMeanHierarchicalCoveragePercentage(), DELTA);
        assertEquals(60.0, coverageReport.getResponseFieldOptionalPooledCoveragePercentage(), DELTA);
        assertEquals(75.0, coverageReport.getResponseFieldMinPooledCoveragePercentage(), DELTA);
        assertEquals((10.0 / 11 * 100 + 100) / 2, coverageReport.getResponseFieldMedianPooledCoveragePercentage(),
                DELTA);
        assertEquals(4, coverageReport.getConnectionsWithResponseFields());
        assertEquals(0, coverageReport.getConnectionsWithResponseFieldCoverageNotApplicable());
        assertEquals(1, coverageReport.getResponseFieldReturnedNotInSpecCount());
    }

    @Test
    void notApplicableConnectionsAreCountedByReason() {
        ConnectionReport mocked = report("mock", ResponseFieldCoverage.of(List.of(), DEFAULT_EXPECTED, builder,
                "notification", "POST", "/notify"));
        CoverageReport coverageReport = new CoverageReport(List.of(mocked.getConnection()), List.of(),
                List.of(mocked));

        assertEquals(1, coverageReport.getConnectionsWithResponseFieldCoverageNotApplicable());
        assertEquals(Map.of(ResponseFieldNotApplicableReason.CALLEE_NOT_TRACED, 1L),
                coverageReport.getResponseFieldNotApplicableReasons());
    }

    private static void assertInOrder(String report, String... sections) {
        int position = -1;
        for (String section : sections) {
            int next = report.indexOf(section, position + 1);
            assertTrue(next > position, "missing or out of order: " + section.strip() + "\n" + report);
            position = next;
        }
    }

    /** K1-K4 with the responses of T1-T6 (K5 is never called). */
    private CoverageReport sampleShopReport() {
        ConnectionReport k1 = report("inventory", ResponseFieldCoverage.of(List.of(
                new ObservedResponse(201, "{\"reservationId\": \"r1\", \"orderId\": \"o1\", \"createdAt\": \"t\", "
                        + "\"estimatedDelivery\": \"2026-10-11\"}"),
                new ObservedResponse(201, "{\"reservationId\": \"r2\", \"orderId\": \"o2\", \"createdAt\": \"t\", "
                        + "\"estimatedDelivery\": null}"),
                new ObservedResponse(409, "{\"message\": \"not enough stock\", \"availableQuantity\": 20}"),
                new ObservedResponse(400, "{\"message\": \"Unknown sku\", \"fieldErrors\": null, \"hint\": null}"),
                new ObservedResponse(400, "{\"message\": \"Invalid\", \"fieldErrors\": [{\"field\": "
                        + "\"items[0].quantity\", \"message\": \"must be >= 1\"}], \"hint\": null}"),
                new ObservedResponse(201, "{\"reservationId\": \"r6\", \"orderId\": \"o6\", \"createdAt\": \"t\", "
                        + "\"estimatedDelivery\": null}")),
                DEFAULT_EXPECTED, builder, "inventory", "POST", "/api/v1/inventory/reservations"));
        ConnectionReport k2 = report("quotes", ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"quoteId\": \"q1\", \"currency\": \"EUR\", \"total\": 2.5, "
                        + "\"discountApplied\": null}"),
                new ObservedResponse(200, "{\"quoteId\": \"q2\", \"currency\": \"EUR\", \"total\": 25.0, "
                        + "\"discountApplied\": null}"),
                new ObservedResponse(500, "{\"status\": 500, \"error\": \"Internal Server Error\"}")),
                DEFAULT_EXPECTED, builder, "pricing", "POST", "/api/v1/pricing/quotes"));
        ConnectionReport k3 = report("prices", ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"sku\": \"ABC-0001\", \"price\": 12.5, \"currency\": \"EUR\"}"),
                new ObservedResponse(404, "{\"message\": \"Unknown sku: ZZZ-9999\"}")),
                DEFAULT_EXPECTED, builder, "pricing", "GET", "/api/v1/pricing/prices/{sku}"));
        ConnectionReport k4 = report("discounts", ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"code\": \"SAVE10\", \"percent\": 10}"),
                new ObservedResponse(404, "{\"message\": \"Unknown discount code: NOPE\"}")),
                List.of("200", "400", "404"), builder, "pricing", "GET", "/api/v1/pricing/discounts/{code}"));
        List<ConnectionReport> reports = List.of(k1, k2, k3, k4);
        return new CoverageReport(reports.stream().map(ConnectionReport::getConnection).toList(),
                List.of(connection("uncalled")), reports);
    }

    private static RestConnection connection(String name) {
        return new RestConnection("caller", name, new RestCall("POST", "http://" + name + "/x", "X"),
                new RestEndpoint("POST", "/x", new ArrayList<>(), "X"));
    }

    private static ConnectionReport report(String name, ResponseFieldCoverage coverage) {
        ConnectionReport report = new ConnectionReport();
        report.setConnection(connection(name));
        report.setCount(1);
        report.setStatusCodes(new ArrayList<>(List.of(200)));
        report.setResponseFieldCoverage(coverage);
        return report;
    }
}
