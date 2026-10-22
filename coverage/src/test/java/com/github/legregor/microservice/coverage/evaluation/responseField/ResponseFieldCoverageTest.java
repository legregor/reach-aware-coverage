package com.github.legregor.microservice.coverage.evaluation.responseField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.legregor.microservice.coverage.openapi.OpenApiResponseResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ResponseFieldCoverage}, with the response schemas of the sample-shop's inventory and pricing
 * services (extension R1-R6) and the responses of its tests T1-T6.
 */
class ResponseFieldCoverageTest {

    private static final List<String> DEFAULT_EXPECTED = List.of("2xx", "4xx");
    private static final String RESERVATIONS = "/api/v1/inventory/reservations";

    @TempDir
    Path specDir;

    private ResponseFieldTreeBuilder builder;

    @BeforeEach
    void writeSpecs() throws IOException {
        Files.writeString(specDir.resolve("inventory.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/api/v1/inventory/reservations": {"post": {"responses": {
                     "201": {"description": "created", "content": {"application/json": {
                       "schema": {"$ref": "#/components/schemas/Reservation"}}}},
                     "400": {"description": "invalid", "content": {"application/json": {
                       "schema": {"$ref": "#/components/schemas/ValidationErrorResponse"}}}},
                     "409": {"description": "out of stock", "content": {"application/json": {
                       "schema": {"$ref": "#/components/schemas/ErrorResponse"}}}},
                     "204": {"description": "no content"}}}}},
                 "components": {"schemas": {
                   "Reservation": {"type": "object", "required": ["reservationId", "orderId", "createdAt"],
                     "properties": {"reservationId": {"type": "string"}, "orderId": {"type": "string"},
                       "createdAt": {"type": "string"}, "estimatedDelivery": {"type": "string", "format": "date"},
                       "internalToken": {"type": "string", "writeOnly": true}}},
                   "ValidationErrorResponse": {"type": "object", "required": ["message"], "properties": {
                     "message": {"type": "string"},
                     "fieldErrors": {"type": "array", "items": {"$ref": "#/components/schemas/FieldError"}},
                     "hint": {"type": "string"}}},
                   "FieldError": {"type": "object", "required": ["field", "message"], "properties": {
                     "field": {"type": "string"}, "message": {"type": "string"}}},
                   "ErrorResponse": {"type": "object", "required": ["message"], "properties": {
                     "message": {"type": "string"}, "availableQuantity": {"type": "integer"}}}}}}
                """);
        Files.writeString(specDir.resolve("pricing.json"), """
                {"openapi": "3.0.3",
                 "paths": {
                   "/api/v1/pricing/quotes": {"post": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["quoteId", "currency", "total"], "properties": {
                         "quoteId": {"type": "string"}, "currency": {"type": "string"},
                         "total": {"type": "number"}, "discountApplied": {"type": "number"}}}}}},
                     "500": {"description": "error", "content": {"application/json": {"schema": {
                       "type": "object", "properties": {"error": {"type": "string"}}}}}}}}},
                   "/api/v1/pricing/prices/{sku}": {"get": {"responses": {
                     "200": {"description": "ok", "content": {"application/json": {"schema": {
                       "type": "object", "required": ["sku", "price"], "properties": {
                         "sku": {"type": "string"}, "price": {"type": "number"}}}}}}}}}}}
                """);
        builder = new ResponseFieldTreeBuilder(new OpenApiResponseResolver(new OpenApiSpecRegistry(specDir)));
    }

    /** K1 of the sample-shop: expected values from EXPECTED.md (10/11, 88.89 %, 3/4). */
    @Test
    void sampleShopReservationConnection() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(201, "{\"reservationId\": \"r1\", \"orderId\": \"o1\", \"createdAt\": \"t\", "
                        + "\"estimatedDelivery\": \"2026-10-11\"}"),                                    // T1
                new ObservedResponse(201, "{\"reservationId\": \"r2\", \"orderId\": \"o2\", \"createdAt\": \"t\", "
                        + "\"estimatedDelivery\": null}"),                                              // T2
                new ObservedResponse(409, "{\"message\": \"not enough stock\", \"availableQuantity\": 20}"),  // T3
                new ObservedResponse(400, "{\"message\": \"Unknown sku\", \"fieldErrors\": null, \"hint\": null}"), // T4
                new ObservedResponse(400, "{\"message\": \"Invalid\", \"fieldErrors\": [{\"field\": "
                        + "\"items[0].quantity\", \"message\": \"must be >= 1\"}], \"hint\": null}"),     // T5
                new ObservedResponse(201, "{\"reservationId\": \"r6\", \"orderId\": \"o6\", \"createdAt\": \"t\"}")), // T6
                DEFAULT_EXPECTED, builder, "inventory", "POST", RESERVATIONS);

        assertEquals(11, coverage.getFieldCount());
        assertEquals(10, coverage.getCoveredCount());
        assertEquals(10.0 / 11 * 100, coverage.getPooledCoveragePercentage(), 1e-9);
        assertEquals((1.0 + 2.0 / 3 + 1.0) / 3 * 100, coverage.getHierarchicalCoveragePercentage(), 1e-9);
        assertEquals(4, coverage.getOptionalFieldCount());
        assertEquals(75.0, coverage.getOptionalPooledCoveragePercentage(), 1e-9);
        assertEquals(List.of("400:hint"), coverage.getUncoveredFields());
        assertEquals(List.of(201, 400, 409), coverage.getStatusCodes().stream()
                .map(ResponseFieldCoverage.StatusFieldCoverage::statusCode).toList());
        assertEquals(List.of("reservationId", "orderId", "createdAt"),
                coverage.getStatusCodes().get(0).requiredFields());
        assertFalse(coverage.isNotApplicable());
    }

    /** Option (b): an expected status code that was never returned contributes no fields. */
    @Test
    void onlyReturnedExpectedStatusCodesCount() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(201, "{\"reservationId\": \"r1\", \"orderId\": \"o1\", \"createdAt\": \"t\"}")),
                DEFAULT_EXPECTED, builder, "inventory", "POST", RESERVATIONS);

        assertEquals(4, coverage.getFieldCount()); // 201 only (writeOnly internalToken skipped), no 400 / 409 fields
        assertEquals(List.of("201:estimatedDelivery"), coverage.getUncoveredFields());
    }

    /** Unexpected codes (here 500) are ignored; an optional field never returned is a gap (sample-shop K2). */
    @Test
    void unexpectedStatusCodesAreIgnored() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"quoteId\": \"q1\", \"currency\": \"EUR\", \"total\": 2.5, "
                        + "\"discountApplied\": null}"),
                new ObservedResponse(500, "{\"error\": \"Internal Server Error\"}")),
                DEFAULT_EXPECTED, builder, "pricing", "POST", "/api/v1/pricing/quotes");

        assertEquals(List.of(500), coverage.getIgnoredStatusCodes());
        assertEquals(75.0, coverage.getPooledCoveragePercentage(), 1e-9);
        assertEquals(75.0, coverage.getHierarchicalCoveragePercentage(), 1e-9);
        assertEquals(0.0, coverage.getOptionalPooledCoveragePercentage(), 1e-9);
    }

    /**
     * Sample-shop K2 in the real run: the 500 came from an untraced callee, so only the client span carries it. It is
     * still listed as ignored, while the values only use the traced responses.
     */
    @Test
    void ignoredStatusCodesIncludeClientSideCodes() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"quoteId\": \"q1\", \"currency\": \"EUR\", \"total\": 2.5}"),
                new ObservedResponse(200, "{\"quoteId\": \"q2\", \"currency\": \"EUR\", \"total\": 4.0}")),
                List.of(200, 200, 500), DEFAULT_EXPECTED, builder, "pricing", "POST", "/api/v1/pricing/quotes");

        assertEquals(List.of(500), coverage.getIgnoredStatusCodes());
        assertEquals(List.of(200), coverage.getStatusCodes().stream()
                .map(ResponseFieldCoverage.StatusFieldCoverage::statusCode).toList());
        assertEquals(75.0, coverage.getPooledCoveragePercentage(), 1e-9);
    }

    /** An expected code seen only on the client side is neither judged nor listed as ignored. */
    @Test
    void expectedClientSideOnlyCodeIsNotIgnored() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"quoteId\": \"q1\", \"currency\": \"EUR\", \"total\": 2.5}")),
                List.of(200, 404), DEFAULT_EXPECTED, builder, "pricing", "POST", "/api/v1/pricing/quotes");

        assertTrue(coverage.getIgnoredStatusCodes().isEmpty());
        assertEquals(4, coverage.getFieldCount());
    }

    /** A field returned but not documented is listed, not counted (sample-shop K3, R3). */
    @Test
    void returnedButNotInSpec() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(200, "{\"sku\": \"ABC-0001\", \"price\": 12.5, \"currency\": \"EUR\"}")),
                DEFAULT_EXPECTED, builder, "pricing", "GET", "/api/v1/pricing/prices/{sku}");

        assertEquals(100.0, coverage.getPooledCoveragePercentage(), 1e-9);
        assertEquals(List.of("currency"), coverage.getStatusCodes().get(0).returnedNotInSpec());
        assertEquals(1, coverage.getReturnedNotInSpecCount());
        assertNull(coverage.getOptionalPooledCoveragePercentage());
    }

    @Test
    void notApplicableReasons() {
        assertEquals(ResponseFieldNotApplicableReason.CALLEE_NOT_TRACED,
                reason(List.of(), RESERVATIONS));
        assertEquals(ResponseFieldNotApplicableReason.NO_EXPECTED_STATUS_RETURNED,
                reason(List.of(new ObservedResponse(503, "{}")), RESERVATIONS));
        assertEquals(ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA,
                reason(List.of(new ObservedResponse(418, "{}")), RESERVATIONS));
        assertEquals(ResponseFieldNotApplicableReason.NO_RESPONSE_SCHEMA,
                reason(List.of(new ObservedResponse(201, "{}")), "/api/v1/unknown"));
        assertEquals(ResponseFieldNotApplicableReason.NO_RESPONSE_FIELDS,
                reason(List.of(new ObservedResponse(204, null)), RESERVATIONS));
        assertEquals(ResponseFieldNotApplicableReason.BODY_NOT_CAPTURED,
                reason(List.of(new ObservedResponse(201, null)), RESERVATIONS));
    }

    @Test
    void statusCodeWithoutCapturedBodyIsNotJudged() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(201, "{\"reservationId\": \"r1\", \"orderId\": \"o1\", \"createdAt\": \"t\", "
                        + "\"estimatedDelivery\": \"2026-10-11\"}"),
                new ObservedResponse(409, null)),
                DEFAULT_EXPECTED, builder, "inventory", "POST", RESERVATIONS);

        assertEquals(4, coverage.getFieldCount());
        assertEquals(List.of(409), coverage.getBodyNotRecordedStatusCodes());
        assertTrue(coverage.getStatusCodes().get(1).uncoveredFields().isEmpty());
    }

    private static final String QUOTES = "/api/v1/pricing/quotes";
    private static final String EUR_200 = "{\"quoteId\": \"q1\", \"currency\": \"EUR\", \"total\": 1, "
            + "\"discountApplied\": null}";
    private static final String CHF_200 = "{\"quoteId\": \"q2\", \"currency\": \"CHF\", \"total\": 2, "
            + "\"discountApplied\": 5}";

    /** Body class only: the CHF response at HTTP 200 is not in a relevant class, so its discountApplied does not count. */
    @Test
    void onlyResponsesOfARelevantBodyClassCount() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(new ObservedResponse(200, EUR_200),
                new ObservedResponse(200, CHF_200)), List.of("currency=EUR"), builder, "pricing", "POST", QUOTES);

        assertEquals(4, coverage.getFieldCount());
        assertEquals(List.of("200:discountApplied"), coverage.getUncoveredFields());
        assertEquals(1, coverage.getSelectedResponses().size());
        assertTrue(coverage.getIgnoredStatusCodes().isEmpty());
    }

    /** Mixed classes: a response counts if it is in at least one of them (here 2xx covers the CHF response too). */
    @Test
    void aResponseCountsIfItIsInAnyRelevantClass() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(new ObservedResponse(200, EUR_200),
                new ObservedResponse(200, CHF_200)), List.of("2xx", "currency=EUR"), builder, "pricing", "POST",
                QUOTES);

        assertTrue(coverage.getUncoveredFields().isEmpty());
        assertEquals(2, coverage.getSelectedResponses().size());
    }

    /** A response outside 2xx/4xx counts if its body is in a relevant class; its variant is the status code 500. */
    @Test
    void anyStatusCodeCountsIfTheBodyIsInARelevantClass() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(
                new ObservedResponse(500, "{\"error\": \"boom\", \"currency\": \"EUR\"}")),
                List.of("currency=EUR"), builder, "pricing", "POST", QUOTES);

        assertEquals(List.of(500), coverage.getStatusCodes().stream()
                .map(ResponseFieldCoverage.StatusFieldCoverage::statusCode).toList());
        assertEquals(List.of("500:error"), coverage.getCoveredFields());
    }

    /** No response in a relevant class: the status code is ignored, the connection n/a. */
    @Test
    void statusCodeWithoutAResponseOfARelevantClassIsIgnored() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(new ObservedResponse(200, CHF_200)),
                List.of("currency=EUR"), builder, "pricing", "POST", QUOTES);

        assertEquals(List.of(200), coverage.getIgnoredStatusCodes());
        assertEquals(ResponseFieldNotApplicableReason.NO_EXPECTED_STATUS_RETURNED, coverage.getNotApplicableReason());
    }

    /** Body class, body not captured: the class is unknown, the status code is listed as "body not recorded". */
    @Test
    void bodyClassWithoutCapturedBodyIsBodyNotRecorded() {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(List.of(new ObservedResponse(200, null)),
                List.of("currency=EUR"), builder, "pricing", "POST", QUOTES);

        assertEquals(List.of(200), coverage.getBodyNotRecordedStatusCodes());
        assertEquals(ResponseFieldNotApplicableReason.BODY_NOT_CAPTURED, coverage.getNotApplicableReason());
    }

    /**
     * Codes only the caller saw (callee not traced): with HTTP classes, a code outside them is ignored; with a body
     * class, its class is unknown and it is not listed as ignored.
     */
    @Test
    void codesSeenOnlyByTheCaller() {
        assertEquals(List.of(500), ResponseFieldCoverage.of(List.of(), List.of(200, 500), DEFAULT_EXPECTED, builder,
                "pricing", "POST", QUOTES).getIgnoredStatusCodes());
        assertTrue(ResponseFieldCoverage.of(List.of(), List.of(200), List.of("currency=EUR"), builder,
                "pricing", "POST", QUOTES).getIgnoredStatusCodes().isEmpty());
    }

    private ResponseFieldNotApplicableReason reason(List<ObservedResponse> observed, String path) {
        ResponseFieldCoverage coverage = ResponseFieldCoverage.of(observed, DEFAULT_EXPECTED, builder,
                "inventory", "POST", path);
        assertTrue(coverage.isNotApplicable());
        return coverage.getNotApplicableReason();
    }
}
