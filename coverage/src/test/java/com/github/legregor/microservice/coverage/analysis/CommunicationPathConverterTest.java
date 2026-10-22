package com.github.legregor.microservice.coverage.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.analysis.model.BodyKind;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPath;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.model.Microservice;
import com.github.legregor.microservice.coverage.analysis.model.MicroserviceSystem;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CommunicationPathConverter}.
 * <p>
 * Expected semantics:
 * - A connection k = (s_n, p) is identified by the calling service and the called endpoint
 *   (destination service + endpoint HTTP method + endpoint path).
 * - RestCall = the call as implemented in the source service (method, called URL, return type the caller expects).
 * - RestEndpoint = the endpoint of the destination service that receives the call.
 * - The source* fields of a segment (the caller's own endpoint) are not part of the connection.
 * - Multi-hop paths are split into one connection per hop; segments shared by several paths are merged.
 * - Different call implementations reaching the same endpoint are merged into one connection (calls list).
 */
class CommunicationPathConverterTest {

    private static final String FIXTURE = "/converter/communication-path-result.json";
    private static final String PRESERVE_FIXTURE = "/converter/preserve-communication-paths.json";

    private static final String INVENTORY_ITEM_PATH = "/api/inventory/items/{itemId}";

    // ---------------------------------------------------------------------------------------------
    // Mapping of a single segment
    // ---------------------------------------------------------------------------------------------

    @Test
    void callIsBuiltFromCallSiteAndEndpointFromCalledEndpoint() {
        CommunicationPath.PathSegment segment = segment("orders", "inventory",
                "GET", "http://inventory:8080/api/items/{itemId}", "ItemDto",
                "GET", "/api/items/{itemId}", "ResponseEntity<ItemDto>",
                List.of("@PathVariable String itemId"));
        // caller's own endpoint: must not end up anywhere in the connection
        segment.setSourceHttpMethod("POST");
        segment.setSourcePath("/api/orders");
        segment.setSourceReturnType("OrderResponse");

        RestConnection connection = singleConnection(convert(result(path(segment))));

        assertEquals("orders", connection.getSource());
        assertEquals("inventory", connection.getDestination());

        assertEquals(new RestCall("GET", "http://inventory:8080/api/items/{itemId}", "ItemDto"), connection.getCall());

        assertEquals("GET", connection.getEndpoint().getMethod());
        assertEquals("/api/items/{itemId}", connection.getEndpoint().getPath());
        assertEquals("ResponseEntity<ItemDto>", connection.getEndpoint().getReturnType());
        assertEquals(List.of("@PathVariable String itemId"), connection.getEndpoint().getArguments());

        assertEquals(List.of(connection.getCall()), connection.getCalls());
    }

    @Test
    void endpointArgumentsAreCopiedAndNullArgumentsBecomeEmptyList() {
        List<String> arguments = new ArrayList<>(List.of("@PathVariable String id"));
        CommunicationPath.PathSegment withArgs = segment("a", "b", "GET", "http://b/x/{id}", "X",
                "GET", "/x/{id}", "X", arguments);
        CommunicationPath.PathSegment withoutArgs = segment("a", "b", "GET", "http://b/y", "Y",
                "GET", "/y", "Y", null);

        List<RestConnection> connections = convert(result(path(withArgs), path(withoutArgs))).getRestConnections();

        RestConnection first = connections.get(0);
        first.getEndpoint().getArguments().add("modified");
        assertEquals(List.of("@PathVariable String id"), withArgs.getDestinationEndpointArguments(),
                "changing the connection must not change the analysis result");

        assertNotNull(connections.get(1).getEndpoint().getArguments());
        assertTrue(connections.get(1).getEndpoint().getArguments().isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // Robustness / edge cases with hand-built input
    // ---------------------------------------------------------------------------------------------

    @Test
    void nullResultGivesEmptySystem() {
        assertNotNull(new CommunicationPathConverter().convertToSystem(null));
    }

    @Test
    void invalidSegmentsAndEmptyPathsAreSkipped() {
        CommunicationPath.PathSegment valid = segment("a", "b", "GET", "http://b/x", "X", "GET", "/x", "X", List.of());
        CommunicationPath.PathSegment noSource = segment(null, "b", "GET", "http://b/y", "Y", "GET", "/y", "Y", List.of());
        CommunicationPath.PathSegment noDestination = segment("a", null, "GET", "http://c/z", "Z", "GET", "/z", "Z", List.of());

        CommunicationPath emptyPath = path();
        CommunicationPath pathWithoutSegmentList = new CommunicationPath();
        pathWithoutSegmentList.setSegments(null);

        MicroserviceSystem system = convert(result(
                emptyPath,
                pathWithoutSegmentList,
                path(noSource),
                path(null, noDestination, valid)));

        RestConnection connection = singleConnection(system);
        assertEquals("a", connection.getSource());
        assertEquals("b", connection.getDestination());
        assertEquals("/x", connection.getEndpoint().getPath());
    }

    @Test
    void httpMethodCaseDoesNotSplitAConnection() {
        CommunicationPath.PathSegment upper = segment("a", "b", "GET", "http://b/x", "X", "GET", "/x", "X", List.of());
        CommunicationPath.PathSegment lower = segment("a", "b", "get", "http://b/x", "X", "get", "/x", "X", List.of());

        RestConnection connection = singleConnection(convert(result(path(upper), path(lower))));

        assertEquals("GET", connection.getEndpoint().getMethod(), "first segment wins");
        assertEquals(2, connection.getCalls().size(), "the two calls differ (method spelling), so both are kept");
    }

    @Test
    void segmentsWithoutEndpointInformationAreGroupedByCall() {
        // e.g. segments written by an older analyzer version without the destinationEndpoint* fields
        CommunicationPath.PathSegment first = segment("a", "b", "GET", "http://b/x", "X", null, null, null, List.of());
        CommunicationPath.PathSegment sameCall = segment("a", "b", "GET", "http://b/x", "X", null, null, null, List.of());
        CommunicationPath.PathSegment otherCall = segment("a", "b", "GET", "http://b/other", "X", null, null, null, List.of());

        List<RestConnection> connections = convert(result(path(first), path(sameCall), path(otherCall))).getRestConnections();

        assertEquals(2, connections.size());
        assertEquals("http://b/x", connections.get(0).getCall().getUrl());
        assertEquals(1, connections.get(0).getCalls().size());
        assertEquals("http://b/other", connections.get(1).getCall().getUrl());
    }

    @Test
    void sameEndpointPathOnDifferentDestinationServicesIsNotMerged() {
        CommunicationPath.PathSegment toB = segment("a", "b", "GET", "http://b/health", "S", "GET", "/health", "S", List.of());
        CommunicationPath.PathSegment toC = segment("a", "c", "GET", "http://c/health", "S", "GET", "/health", "S", List.of());

        List<RestConnection> connections = convert(result(path(toB), path(toC))).getRestConnections();

        assertEquals(2, connections.size());
        assertEquals("b", connections.get(0).getDestination());
        assertEquals("c", connections.get(1).getDestination());
    }

    // ---------------------------------------------------------------------------------------------
    // Fixture: a small shop system with multi-hop paths, shared segments, merged calls,
    // a cycle back to an earlier service and an unresolved destination service.
    //
    //   gateway  --POST /api/orders-------------------> order
    //   gateway  --GET  /api/orders/{orderId}----------> order
    //   gateway  --GET  /api/inventory/items/{itemId}--> inventory      (same endpoint, other caller)
    //   order    --GET  /api/inventory/items/{itemId}--> inventory      (3 different call implementations)
    //   order    --PUT  /api/inventory/items/{itemId}--> inventory      (same path, other method)
    //   order    --POST /api/payments------------------> payment
    //   payment  --POST /api/orders/{orderId}/paid-----> order          (cycle)
    //   payment  --POST /billing/invoices--------------> legacy-billing (not analyzed)
    // ---------------------------------------------------------------------------------------------

    @Nested
    class ShopSystemFixture {

        @Test
        void fixtureCanBeReadIncludingDerivedJsonProperties() throws IOException {
            CommunicationPathResult result = loadFixture();
            assertEquals(11, result.getPaths().size());
            assertEquals("gateway-service", result.getRootService());
        }

        @Test
        void oneConnectionPerCallerAndCalledEndpointInFirstSeenOrder() throws IOException {
            List<String> keys = convertFixture().getRestConnections().stream()
                    .map(CommunicationPathConverterTest::key)
                    .toList();

            assertEquals(List.of(
                    "gateway-service -> order-service POST /api/orders",
                    "order-service -> inventory-service GET /api/inventory/items/{itemId}",
                    "order-service -> inventory-service PUT /api/inventory/items/{itemId}",
                    "order-service -> payment-service POST /api/payments",
                    "payment-service -> order-service POST /api/orders/{orderId}/paid",
                    "payment-service -> legacy-billing POST /billing/invoices",
                    "gateway-service -> order-service GET /api/orders/{orderId}",
                    "gateway-service -> inventory-service GET /api/inventory/items/{itemId}"
            ), keys);
        }

        @Test
        void segmentSharedByManyPathsBecomesOneConnectionWithOneCall() throws IOException {
            // gateway -> order POST is the first hop of 6 different paths
            RestConnection connection = connection(convertFixture(), "gateway-service", "order-service", "POST", "/api/orders");

            assertEquals(new RestCall("POST", "http://order-service:8080/api/orders", "OrderDto"), connection.getCall());
            assertEquals(1, connection.getCalls().size());
            assertEquals("ResponseEntity<OrderDto>", connection.getEndpoint().getReturnType());
            assertEquals(List.of("@RequestBody OrderRequest request", "@RequestHeader HttpHeaders headers"),
                    connection.getEndpoint().getArguments());
        }

        @Test
        void everyHopOfAMultiHopPathIsAConnection() throws IOException {
            // path gateway -> order -> payment -> order (3 hops)
            MicroserviceSystem system = convertFixture();
            connection(system, "gateway-service", "order-service", "POST", "/api/orders");
            connection(system, "order-service", "payment-service", "POST", "/api/payments");
            RestConnection back = connection(system, "payment-service", "order-service", "POST", "/api/orders/{orderId}/paid");

            assertEquals("http://order-service:8080/api/orders/{orderId}/paid", back.getCall().getUrl());
            assertEquals("ResponseEntity<Void>", back.getEndpoint().getReturnType());
        }

        @Test
        void differentCallImplementationsOfTheSameEndpointAreMerged() throws IOException {
            RestConnection connection = connection(convertFixture(),
                    "order-service", "inventory-service", "GET", INVENTORY_ITEM_PATH);

            RestCall templateItemId = new RestCall("GET", "http://inventory-service:8080/api/inventory/items/{itemId}", "ItemDto");
            RestCall templateId = new RestCall("GET", "http://inventory-service:8080/api/inventory/items/{id}", "ItemDto");
            RestCall otherReturnType = new RestCall("GET", "http://inventory-service:8080/api/inventory/items/{itemId}", "Map");

            assertEquals(templateItemId, connection.getCall(), "first call is the representative call");
            assertEquals(List.of(templateItemId, templateId, otherReturnType), connection.getCalls());
        }

        @Test
        void sameEndpointCalledByDifferentServicesGivesSeparateConnections() throws IOException {
            MicroserviceSystem system = convertFixture();

            RestConnection fromGateway = connection(system, "gateway-service", "inventory-service", "GET", INVENTORY_ITEM_PATH);
            RestConnection fromOrder = connection(system, "order-service", "inventory-service", "GET", INVENTORY_ITEM_PATH);

            assertEquals(fromGateway.getEndpoint(), fromOrder.getEndpoint());
            assertEquals(1, fromGateway.getCalls().size());
            assertEquals(3, fromOrder.getCalls().size());
        }

        @Test
        void samePathWithDifferentHttpMethodGivesSeparateConnections() throws IOException {
            MicroserviceSystem system = convertFixture();

            RestConnection get = connection(system, "order-service", "inventory-service", "GET", INVENTORY_ITEM_PATH);
            RestConnection put = connection(system, "order-service", "inventory-service", "PUT", INVENTORY_ITEM_PATH);

            assertEquals("ResponseEntity<ItemDto>", get.getEndpoint().getReturnType());
            assertEquals("ResponseEntity<ReservationDto>", put.getEndpoint().getReturnType());
            assertEquals("PUT", put.getCall().getMethod());
        }

        @Test
        void unresolvedDestinationUsesCallUrlFallbackFromAnalyzer() throws IOException {
            RestConnection connection = connection(convertFixture(), "payment-service", "legacy-billing", "POST", "/billing/invoices");

            assertEquals("http://legacy-billing:9000/billing/invoices?async=true", connection.getCall().getUrl());
            assertEquals("InvoiceDto", connection.getCall().getReturnType());
            assertNull(connection.getEndpoint().getReturnType(), "endpoint of an unanalyzed service is unknown");
            assertTrue(connection.getEndpoint().getArguments().isEmpty());
        }

        @Test
        void sourceEndpointInformationIsNotUsed() throws IOException {
            for (RestConnection connection : convertFixture().getRestConnections()) {
                assertTrue(!"GatewayResponse".equals(connection.getEndpoint().getReturnType())
                                && !connection.getEndpoint().getPath().startsWith("/gw/"),
                        () -> "endpoint built from the caller's own endpoint: " + key(connection));
                for (RestCall call : connection.getCalls()) {
                    assertTrue(call.getUrl().startsWith("http://"),
                            () -> "call built from the caller's own endpoint: " + key(connection));
                }
            }
        }

        @Test
        void microservicesAreBuiltFromAnalyzedServicesOnly() throws IOException {
            CommunicationPathResult result = loadFixture();
            MicroserviceSystem system = new CommunicationPathConverter().convertToSystem(result);

            // Order is not meaningful: analyzedServices is a Set (Jackson reads it as a HashSet, and the
            // analyzer fills it from a HashMap), so only the set of services is checked.
            List<String> names = system.getMicroservices().stream().map(Microservice::getName).toList();
            assertEquals(4, names.size(), () -> "duplicate or missing microservices: " + names);
            assertEquals(Set.of("gateway-service", "order-service", "inventory-service", "payment-service"),
                    Set.copyOf(names));

            Microservice gateway = microservice(system, "gateway-service");
            Microservice order = microservice(system, "order-service");
            assertSame(result, gateway.getCommunicationPathResult(), "root service carries the path result");
            assertNull(order.getCommunicationPathResult());
            assertSame(result, system.getCommunicationPathResult());

            assertEquals(3, order.getRestEndpoints().size());
            // GET {itemId} ItemDto, PUT, POST payments, GET {id} ItemDto, GET {itemId} Map
            assertEquals(5, order.getRestCalls().size());
        }

        @Test
        void convertedSystemCanBeWrittenAndReadBackWithTheReader(@TempDir Path tempDir) throws IOException {
            MicroserviceSystem system = convertFixture();
            Path file = tempDir.resolve("microservice-system.json");
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), system);

            MicroserviceSystem read = new MicroserviceSystemReader().read(file);

            assertEquals(system.getRestConnections(), read.getRestConnections());
            for (int i = 0; i < system.getRestConnections().size(); i++) {
                assertEquals(system.getRestConnections().get(i).getCalls(), read.getRestConnections().get(i).getCalls());
            }
            assertEquals(system.getMicroservices().size(), read.getMicroservices().size());
            assertEquals(system.getCommunicationPathResult().getPaths().size(),
                    read.getCommunicationPathResult().getPaths().size());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Request elements (path parameters + top-level body properties) per connection
    // ---------------------------------------------------------------------------------------------

    @Nested
    class RequestElementsOfAConnection {

        @Test
        void pathParametersComeFromTheEndpointTemplateAndAreReachable() {
            CommunicationPath.PathSegment segment = segment("a", "b", "GET", "http://b/orders/{id}/items/{nr}", "X",
                    "GET", "/orders/{orderId}/items/{itemNr:\\d+}", "X", List.of());

            RestConnection connection = singleConnection(convert(result(path(segment))));

            assertEquals(List.of("path:orderId", "path:itemNr"), connection.getRequestElements());
            assertEquals(List.of("path:orderId", "path:itemNr"), connection.getReachableRequestElements());
            assertTrue(connection.getNotReachableRequestElements().isEmpty());
            assertNull(connection.getRequestBodyType());
        }

        @Test
        void objectBodyPropertiesAreElementsAndNotReachableOnesAreListed() {
            CommunicationPath.PathSegment segment = segment("preserve", "consign", "POST", "http://consign/consigns", "R",
                    "POST", "/consigns/{orderId}", "R", List.of());
            withBody(segment, BodyKind.OBJECT, "preserve.entity.Consign",
                    List.of("id", "from", "within"), List.of("from", "within"));
            segment.setBodyOrigins(new LinkedHashSet<>(List.of("new Consign() in PreserveServiceImpl.preserve")));

            RestConnection connection = singleConnection(convert(result(path(segment))));

            assertEquals(List.of("path:orderId", "body:id", "body:from", "body:within"), connection.getRequestElements());
            assertEquals(Set.of("path:orderId", "body:from", "body:within"),
                    Set.copyOf(connection.getReachableRequestElements()));
            assertEquals(List.of("body:id"), connection.getNotReachableRequestElements());
            assertEquals("preserve.entity.Consign", connection.getRequestBodyType());
            assertEquals(List.of("new Consign() in PreserveServiceImpl.preserve"), connection.getRequestBodyOrigins());
        }

        @Test
        void nonObjectBodyIsTheSingleElementBody() {
            CommunicationPath.PathSegment segment = segment("order", "station", "POST", "http://station/namelist", "R",
                    "POST", "/namelist", "R", List.of());
            withBody(segment, BodyKind.VALUE, "java.util.List", List.of(), List.of());

            RestConnection connection = singleConnection(convert(result(path(segment))));

            assertEquals(List.of("body"), connection.getRequestElements());
            assertEquals(List.of("body"), connection.getReachableRequestElements());
        }

        @Test
        void segmentsWithoutBodyInformationGiveOnlyPathElements() {
            // e.g. a file written before the body analysis existed: bodyKind missing
            CommunicationPath.PathSegment segment = segment("a", "b", "GET", "http://b/x/{id}", "X",
                    "GET", "/x/{id}", "X", List.of());
            segment.setBodyKind(null);
            segment.setBodyProperties(null);

            RestConnection connection = singleConnection(convert(result(path(segment))));

            assertEquals(List.of("path:id"), connection.getRequestElements());
        }

        @Test
        void severalCallSitesUniteReachableElementsAndIntersectNeverSentArguments() {
            CommunicationPath.PathSegment first = segment("a", "b", "POST", "http://b/notify", "X",
                    "POST", "/notify", "X", List.of());
            withBody(first, BodyKind.OBJECT, "a.NotifyInfo", List.of("email", "name", "phone"), List.of("email"));
            first.setStaticallyOptionalButUnsentArguments(new ArrayList<>(List.of(
                    "@RequestParam(required = false) String debug", "@RequestHeader(required = false) String trace")));
            first.setStaticallyMissingArguments(new ArrayList<>(List.of("@RequestHeader String auth")));

            CommunicationPath.PathSegment second = segment("a", "b", "POST", "http://b/notify", "X",
                    "POST", "/notify", "X", List.of());
            withBody(second, BodyKind.OBJECT, "a.NotifyInfo", List.of("email", "name", "phone"), List.of("name"));
            second.setStaticallyOptionalButUnsentArguments(new ArrayList<>(List.of(
                    "@RequestParam(required = false) String debug")));
            second.setStaticallyMissingArguments(new ArrayList<>(List.of("@RequestParam String mode")));

            RestConnection connection = singleConnection(convert(result(path(first), path(second))));

            assertEquals(Set.of("body:email", "body:name"), Set.copyOf(connection.getReachableRequestElements()));
            assertEquals(List.of("body:phone"), connection.getNotReachableRequestElements());
            assertEquals(List.of("@RequestParam(required = false) String debug"), connection.getNeverSentArguments());
            assertEquals(List.of("@RequestHeader String auth", "@RequestParam String mode"),
                    connection.getStaticallyMissingArguments());
        }

        @Test
        void constantsAndNeverNullAreMergedOverCallSites() {
            CommunicationPath.PathSegment first = segment("a", "b", "POST", "http://b/seats", "X",
                    "POST", "/seats", "X", List.of());
            withBody(first, BodyKind.OBJECT, "a.Seat", List.of("type", "note", "count"), List.of("type", "note", "count"));
            first.setBodyConstantValues(new LinkedHashMap<>(Map.of("type", List.of("\"A\""), "count", List.of("0"))));
            first.setNeverNullBodyProperties(new LinkedHashSet<>(List.of("count")));

            CommunicationPath.PathSegment second = segment("a", "b", "POST", "http://b/seats", "X",
                    "POST", "/seats", "X", List.of());
            withBody(second, BodyKind.OBJECT, "a.Seat", List.of("type", "note", "count"), List.of("type", "count"));
            second.setBodyConstantValues(new LinkedHashMap<>(Map.of("type", List.of("\"B\""))));
            second.setNeverNullBodyProperties(new LinkedHashSet<>(List.of("count")));

            RestConnection connection = singleConnection(convert(result(path(first), path(second))));

            // type: constants in both call sites; count: any value in the second; note: any value in the first
            assertEquals(Map.of("body:type", List.of("\"A\"", "\"B\"")), connection.getRequestElementConstants());
            assertEquals(List.of("body:count"), connection.getNeverNullRequestElements());
        }

        @Test
        void sameSegmentInSeveralPathsDoesNotChangeTheResult() {
            CommunicationPath.PathSegment shared = segment("a", "b", "POST", "http://b/notify", "X",
                    "POST", "/notify", "X", List.of());
            withBody(shared, BodyKind.OBJECT, "a.NotifyInfo", List.of("email", "name"), List.of("email"));
            shared.setStaticallyOptionalButUnsentArguments(new ArrayList<>(List.of("@RequestParam(required = false) String debug")));
            CommunicationPath.PathSegment next = segment("b", "c", "GET", "http://c/x", "X", "GET", "/x", "X", List.of());

            List<RestConnection> connections = convert(result(path(shared), path(shared, next))).getRestConnections();

            RestConnection connection = connections.get(0);
            assertEquals(List.of("body:name"), connection.getNotReachableRequestElements());
            assertEquals(List.of("@RequestParam(required = false) String debug"), connection.getNeverSentArguments());
        }

        @Test
        void trainTicketPreserveScenario() throws IOException {
            CommunicationPathResult result;
            try (InputStream in = CommunicationPathConverterTest.class.getResourceAsStream(PRESERVE_FIXTURE)) {
                assertNotNull(in, "test fixture not found: " + PRESERVE_FIXTURE);
                result = new ObjectMapper().readValue(in, CommunicationPathResult.class);
            }
            MicroserviceSystem system = new CommunicationPathConverter().convertToSystem(result);

            RestConnection food = connection(system, "ts-preserve-service", "ts-food-service", "POST",
                    "/api/v1/foodservice/orders");
            assertEquals(List.of("body:id"), food.getNotReachableRequestElements());
            assertTrue(food.getReachableRequestElements().contains("body:stationName"));

            RestConnection consign = connection(system, "ts-preserve-service", "ts-consign-service", "POST",
                    "/api/v1/consignservice/consigns");
            assertEquals(List.of("body:id"), consign.getNotReachableRequestElements());
            assertTrue(consign.getReachableRequestElements().contains("body:within"));

            RestConnection contacts = connection(system, "ts-preserve-service", "ts-contacts-service", "GET",
                    "/api/v1/contactservice/contacts/{id}");
            assertEquals(List.of("path:id"), contacts.getRequestElements());

            RestConnection order = connection(system, "ts-preserve-service", "ts-order-service", "POST",
                    "/api/v1/orderservice/order");
            assertEquals(16, order.getReachableRequestElements().size());
            assertTrue(order.getNotReachableRequestElements().isEmpty());
        }

        @Test
        void requestElementsSurviveWritingAndReadingTheSystem(@TempDir Path tempDir) throws IOException {
            CommunicationPath.PathSegment segment = segment("preserve", "food", "POST", "http://food/orders", "R",
                    "POST", "/orders", "R", List.of());
            withBody(segment, BodyKind.OBJECT, "preserve.entity.FoodOrder", List.of("id", "foodName"), List.of("foodName"));
            MicroserviceSystem system = convert(result(path(segment)));

            Path file = tempDir.resolve("microservice-system.json");
            new ObjectMapper().writeValue(file.toFile(), system);
            RestConnection read = new MicroserviceSystemReader().read(file).getRestConnections().get(0);

            assertEquals(List.of("body:id"), read.getNotReachableRequestElements());
            assertEquals(List.of("body:foodName"), read.getReachableRequestElements());
            assertEquals("preserve.entity.FoodOrder", read.getRequestBodyType());
        }
    }

    private static void withBody(CommunicationPath.PathSegment segment, BodyKind kind, String type,
                                 List<String> properties, List<String> reachable) {
        segment.setBodyKind(kind);
        segment.setBodyType(type);
        segment.setBodyProperties(new LinkedHashSet<>(properties));
        segment.setReachableBodyProperties(new LinkedHashSet<>(reachable));
        Set<String> notReachable = new LinkedHashSet<>(properties);
        notReachable.removeAll(reachable);
        segment.setNotReachableBodyProperties(notReachable);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static CommunicationPathResult loadFixture() throws IOException {
        try (InputStream in = CommunicationPathConverterTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "test fixture not found: " + FIXTURE);
            return new ObjectMapper().readValue(in, CommunicationPathResult.class);
        }
    }

    private static MicroserviceSystem convertFixture() throws IOException {
        return new CommunicationPathConverter().convertToSystem(loadFixture());
    }

    private static MicroserviceSystem convert(CommunicationPathResult result) {
        return new CommunicationPathConverter().convertToSystem(result);
    }

    private static String key(RestConnection connection) {
        return connection.getSource() + " -> " + connection.getDestination() + " "
                + connection.getEndpoint().getMethod() + " " + connection.getEndpoint().getPath();
    }

    private static RestConnection singleConnection(MicroserviceSystem system) {
        assertEquals(1, system.getRestConnections().size(), () -> "connections: " + system.getRestConnections());
        return system.getRestConnections().get(0);
    }

    private static RestConnection connection(MicroserviceSystem system, String source, String destination,
                                             String method, String path) {
        List<RestConnection> matches = system.getRestConnections().stream()
                .filter(c -> source.equals(c.getSource())
                        && destination.equals(c.getDestination())
                        && method.equals(c.getEndpoint().getMethod())
                        && path.equals(c.getEndpoint().getPath()))
                .toList();
        assertEquals(1, matches.size(), () -> "expected exactly one connection "
                + source + " -> " + destination + " " + method + " " + path + ", found " + matches.size());
        return matches.get(0);
    }

    private static Microservice microservice(MicroserviceSystem system, String name) {
        return system.getMicroservices().stream()
                .filter(m -> name.equals(m.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("microservice not found: " + name));
    }

    private static CommunicationPath.PathSegment segment(String source, String destination,
                                                         String callMethod, String callUrl, String callReturnType,
                                                         String endpointMethod, String endpointPath,
                                                         String endpointReturnType, List<String> endpointArguments) {
        CommunicationPath.PathSegment segment = new CommunicationPath.PathSegment();
        segment.setSourceService(source);
        segment.setDestinationService(destination);
        segment.setDestinationHttpMethod(callMethod);
        segment.setDestinationUrl(callUrl);
        segment.setDestinationReturnType(callReturnType);
        segment.setDestinationEndpointHttpMethod(endpointMethod);
        segment.setDestinationEndpointPath(endpointPath);
        segment.setDestinationEndpointReturnType(endpointReturnType);
        segment.setDestinationEndpointArguments(endpointArguments);
        return segment;
    }

    private static CommunicationPath path(CommunicationPath.PathSegment... segments) {
        CommunicationPath path = new CommunicationPath();
        path.setSegments(new ArrayList<>(Arrays.asList(segments)));
        return path;
    }

    private static CommunicationPathResult result(CommunicationPath... paths) {
        CommunicationPathResult result = new CommunicationPathResult();
        result.setRootService("a");
        result.setPaths(new ArrayList<>(Arrays.asList(paths)));
        return result;
    }
}
