package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.legregor.microservice.coverage.analysis.CommunicationPathConverter;
import com.github.legregor.microservice.coverage.analysis.model.CommunicationPathResult;
import com.github.legregor.microservice.coverage.analysis.model.RestCall;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.analysis.model.RestEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link RequestElementTreeBuilder}: possible request elements from the OpenAPI spec, reachability from
 * the static analysis of the caller (stored in the RestConnection).
 */
class RequestElementTreeBuilderTest {

    private static final String SERVICE = "target-service";
    private static final Path TRAIN_TICKET_SPECS = Paths.get("src/test/resources/slim_open_api");
    private static final String PRESERVE_FIXTURE = "/converter/preserve-communication-paths.json";

    @TempDir
    Path specDir;

    // ---------------------------------------------------------------------------------------------
    // Path parameters and connections without elements
    // ---------------------------------------------------------------------------------------------

    @Test
    void pathParametersAreRequiredAndReachable() {
        RestConnection connection = connection("GET", "/orders/{orderId}/items/{itemNr}", List.of(), List.of());

        RequestElementTree tree = new RequestElementTreeBuilder((Path) null).build(connection);

        assertEquals(List.of("path:orderId", "path:itemNr"), names(tree.allElements()));
        assertTrue(tree.allElements().stream().allMatch(e -> e.isRequired() && e.isReachable()));
        assertFalse(tree.isBodySpecMissing());
    }

    @Test
    void pathParametersGetTypeAndConstraintsFromTheSpec() throws IOException {
        Files.writeString(specDir.resolve(SERVICE + "-slim.json"), """
                {"swagger": "2.0", "paths": {"/orders/{orderId}/pages/{page}": {"get": {"parameters": [
                  {"name": "orderId", "in": "path", "required": true, "type": "string", "format": "uuid"},
                  {"name": "page", "in": "path", "required": true, "type": "integer", "minimum": 0}]}}}}
                """);
        RestConnection connection = connection("GET", "/orders/{orderId}/pages/{page}", List.of(), List.of());

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        RequestElement page = tree.find("path:page").orElseThrow();
        assertEquals("integer", page.getType());
        assertEquals("0", page.getSchemaField().getConstraints().getMinimum().toPlainString());
        assertEquals("uuid", tree.find("path:orderId").orElseThrow().getSchemaField().getConstraints().getFormat());
    }

    @Test
    void staticConstantsAndNeverNullComeFromTheConnection() throws IOException {
        writeSpec("/seats", "post", """
                {"$ref": "#/definitions/Seat"}
                """, """
                "Seat": {"type": "object", "properties": {"seatClass": {"type": "string"}, "count": {"type": "integer"}}}
                """);
        RestConnection connection = connection("POST", "/seats", List.of("body:seatClass", "body:count"),
                List.of("body:seatClass", "body:count"));
        connection.setRequestElementConstants(new java.util.LinkedHashMap<>(java.util.Map.of(
                "body:seatClass", List.of("\"FIRST\""))));
        connection.setNeverNullRequestElements(new ArrayList<>(List.of("body:count")));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertEquals(List.of("\"FIRST\""), tree.find("body:seatClass").orElseThrow().getConstants());
        assertFalse(tree.find("body:seatClass").orElseThrow().isNeverNull());
        assertEquals(null, tree.find("body:count").orElseThrow().getConstants());
        assertTrue(tree.find("body:count").orElseThrow().isNeverNull());
    }

    @Test
    void connectionWithoutElementsIsEmpty() {
        RestConnection connection = connection("GET", "/health", List.of(), List.of());

        RequestElementTree tree = new RequestElementTreeBuilder((Path) null).build(connection);

        assertTrue(tree.isEmpty());
        assertTrue(tree.allElements().isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // Body from the spec
    // ---------------------------------------------------------------------------------------------

    @Test
    void specPropertiesAreElementsAndCallerDecidesReachability() throws IOException {
        writeSpec("/consigns", "post", """
                {"$ref": "#/definitions/Consign"}
                """, """
                "Consign": {"type": "object", "required": ["from"],
                  "properties": {"id": {"type": "string"}, "from": {"type": "string"}, "within": {"type": "boolean"}}}
                """);
        RestConnection connection = connection("POST", "/consigns",
                List.of("body:id", "body:from", "body:within"), List.of("body:from", "body:within"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertEquals(List.of("body:id", "body:from", "body:within"), names(tree.allElements()));
        assertEquals(List.of("body:id"), names(tree.notReachableElements()));
        assertTrue(tree.find("body:from").orElseThrow().isRequired());
        assertFalse(tree.find("body:id").orElseThrow().isRequired());
        assertEquals("boolean", tree.find("body:within").orElseThrow().getType());
        assertFalse(tree.isBodySpecMissing());
        assertTrue(tree.getOnlyInSpec().isEmpty());
        assertTrue(tree.getOnlyInCaller().isEmpty());
    }

    @Test
    void interfaceMismatchesBetweenCallerDtoAndSpec() throws IOException {
        writeSpec("/notify", "post", """
                {"$ref": "#/definitions/NotifyInfo"}
                """, """
                "NotifyInfo": {"type": "object",
                  "properties": {"email": {"type": "string"}, "startingPlace": {"type": "string"}}}
                """);
        // the caller's DTO calls the field startPlace instead of startingPlace
        RestConnection connection = connection("POST", "/notify",
                List.of("body:email", "body:startPlace"), List.of("body:email", "body:startPlace"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertEquals(List.of("body:email", "body:startingPlace"), names(tree.allElements()));
        assertEquals(List.of("body:startingPlace"), names(tree.notReachableElements()));
        assertEquals(List.of("startingPlace"), tree.getOnlyInSpec());
        assertEquals(List.of("startPlace"), tree.getOnlyInCaller());
    }

    @Test
    void readOnlyPropertiesAreNoRequestElements() throws IOException {
        writeSpec("/orders", "post", """
                {"$ref": "#/definitions/Order"}
                """, """
                "Order": {"type": "object",
                  "properties": {"id": {"type": "string", "readOnly": true},
                                 "customerId": {"type": "string"},
                                 "address": {"type": "object", "properties": {
                                   "zip": {"type": "string"}, "geoHash": {"type": "string", "readOnly": true}}}}}
                """);
        // a shared DTO class: the caller also has the server-generated id
        RestConnection connection = connection("POST", "/orders",
                List.of("body:id", "body:customerId", "body:address"),
                List.of("body:id", "body:customerId", "body:address"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertEquals(List.of("body:customerId", "body:address", "body:address.zip"), names(tree.allElements()));
        // id is in the spec (only in responses), so it is no interface mismatch
        assertTrue(tree.getOnlyInCaller().isEmpty());
        assertTrue(tree.getOnlyInSpec().isEmpty());
    }

    @Test
    void nestedObjectsAndListItemsAreChildrenThatInheritReachability() throws IOException {
        writeSpec("/orders", "post", """
                {"$ref": "#/definitions/Order"}
                """, """
                "Order": {"type": "object", "properties": {
                    "address": {"$ref": "#/definitions/Address"},
                    "items": {"type": "array", "items": {"$ref": "#/definitions/Item"}},
                    "tags": {"type": "array", "items": {"type": "string"}},
                    "voucher": {"$ref": "#/definitions/Address"}}},
                "Address": {"type": "object", "required": ["zip"],
                    "properties": {"zip": {"type": "string"}, "city": {"type": "string"}}},
                "Item": {"type": "object", "properties": {"name": {"type": "string"}, "amount": {"type": "integer"}}}
                """);
        RestConnection connection = connection("POST", "/orders",
                List.of("body:address", "body:items", "body:tags", "body:voucher"),
                List.of("body:address", "body:items", "body:tags"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertEquals(List.of("body:address", "body:address.zip", "body:address.city",
                "body:items", "body:items[].name", "body:items[].amount",
                "body:tags",
                "body:voucher", "body:voucher.zip", "body:voucher.city"), names(tree.allElements()));
        assertEquals(List.of("body:voucher", "body:voucher.zip", "body:voucher.city"),
                names(tree.notReachableElements()));
        assertTrue(tree.find("body:address.zip").orElseThrow().isRequired());
        assertEquals(4, tree.getTopLevelElements().size());
        assertEquals(2, tree.find("body:items").orElseThrow().getChildren().size());
    }

    @Test
    void referenceCyclesAndDepthAreLimited() throws IOException {
        writeSpec("/nodes", "post", """
                {"$ref": "#/definitions/Node"}
                """, """
                "Node": {"type": "object", "properties": {"value": {"type": "string"},
                    "next": {"$ref": "#/definitions/Node"}}}
                """);
        RestConnection connection = connection("POST", "/nodes", List.of("body:value", "body:next"),
                List.of("body:value", "body:next"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        // the cycle Node -> next -> Node stops at the reference that is already being expanded
        assertEquals(List.of("body:value", "body:next"), names(tree.allElements()));
    }

    @Test
    void allOfPropertiesAreMerged() throws IOException {
        writeSpec("/users", "post", """
                {"allOf": [{"$ref": "#/definitions/Base"},
                           {"type": "object", "required": ["name"], "properties": {"name": {"type": "string"}}}]}
                """, """
                "Base": {"type": "object", "properties": {"id": {"type": "string"}}}
                """);
        RestConnection connection = connection("POST", "/users", List.of("body:id", "body:name"),
                List.of("body:name"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertEquals(List.of("body:id", "body:name"), names(tree.allElements()));
        assertTrue(tree.find("body:name").orElseThrow().isRequired());
        assertEquals(List.of("body:id"), names(tree.notReachableElements()));
    }

    @Test
    void nonObjectBodyIsOneElementAndListItemsAreItsChildren() throws IOException {
        writeSpec("/namelist", "post", """
                {"type": "array", "items": {"type": "string"}}
                """, "");
        writeSpec("/batch", "post", """
                {"type": "array", "items": {"$ref": "#/definitions/Item"}}
                """, """
                "Item": {"type": "object", "properties": {"name": {"type": "string"}}}
                """);

        RequestElementTree names = new RequestElementTreeBuilder(specDir)
                .build(connection("POST", "/namelist", List.of("body"), List.of("body")));
        assertEquals(List.of("body"), names(names.allElements()));
        assertTrue(names.allElements().get(0).isReachable());
        assertEquals(RequestElement.Kind.BODY_VALUE, names.allElements().get(0).getKind());

        RequestElementTree batch = new RequestElementTreeBuilder(specDir)
                .build(connection("POST", "/batch", List.of("body"), List.of("body")));
        assertEquals(List.of("body", "body[].name"), names(batch.allElements()));
    }

    @Test
    void bodyInSpecButCallerSendsNothingIsNotReachable() throws IOException {
        writeSpec("/orders", "put", """
                {"$ref": "#/definitions/Order"}
                """, """
                "Order": {"type": "object", "properties": {"id": {"type": "string"}}}
                """);

        RequestElementTree tree = new RequestElementTreeBuilder(specDir)
                .build(connection("PUT", "/orders", List.of(), List.of()));

        assertEquals(List.of("body:id"), names(tree.notReachableElements()));
        assertTrue(tree.getOnlyInSpec().isEmpty(), "no mismatch without knowledge of the caller's DTO");
    }

    // ---------------------------------------------------------------------------------------------
    // No spec: the caller's DTO
    // ---------------------------------------------------------------------------------------------

    @Test
    void withoutSpecTheCallerDtoPropertiesAreUsed() {
        RestConnection connection = connection("POST", "/orders/{id}", List.of("path:id", "body:id", "body:comment"),
                List.of("path:id", "body:comment"));

        RequestElementTree tree = new RequestElementTreeBuilder(specDir).build(connection);

        assertTrue(tree.isBodySpecMissing());
        assertEquals(List.of("path:id", "body:id", "body:comment"), names(tree.allElements()));
        assertEquals(List.of("body:id"), names(tree.notReachableElements()));
    }

    // ---------------------------------------------------------------------------------------------
    // TrainTicket: preserve scenario with the real (slim) specs
    // ---------------------------------------------------------------------------------------------

    @Test
    void trainTicketPreserveScenarioWithRealSpecs() throws IOException {
        List<RestConnection> connections = preserveConnections();
        RequestElementTreeBuilder builder = new RequestElementTreeBuilder(TRAIN_TICKET_SPECS);

        RequestElementTree consign = builder.build(find(connections, "ts-consign-service", "/api/v1/consignservice/consigns"));
        assertEquals(11, consign.allElements().size());
        assertEquals(List.of("body:id"), names(consign.notReachableElements()));
        assertTrue(consign.getOnlyInSpec().isEmpty());
        assertTrue(consign.getOnlyInCaller().isEmpty());

        RequestElementTree food = builder.build(find(connections, "ts-food-service", "/api/v1/foodservice/orders"));
        assertEquals(List.of("body:id"), names(food.notReachableElements()));

        // Travel.trip is a nested object (Trip), Trip.tripId another one (TripId)
        RequestElementTree ticketinfo = builder.build(find(connections, "ts-ticketinfo-service",
                "/api/v1/ticketinfoservice/ticketinfo"));
        assertTrue(ticketinfo.find("body:trip.startingStationId").isPresent());
        assertTrue(ticketinfo.allElements().stream().anyMatch(e -> e.getName().startsWith("body:trip.tripId.")));
        assertTrue(ticketinfo.notReachableElements().isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static RestConnection connection(String method, String path, List<String> elements,
                                             List<String> reachable) {
        RestConnection connection = new RestConnection("caller-service", SERVICE,
                new RestCall(method, "http://" + SERVICE + path, "X"),
                new RestEndpoint(method, path, new ArrayList<>(), "X"));
        connection.setRequestElements(new ArrayList<>(elements));
        connection.setReachableRequestElements(new ArrayList<>(reachable));
        List<String> notReachable = new ArrayList<>(elements);
        notReachable.removeAll(reachable);
        connection.setNotReachableRequestElements(notReachable);
        return connection;
    }

    /** Writes (or extends) the slim spec of {@link #SERVICE} with one operation that has the given body schema. */
    private void writeSpec(String path, String method, String bodySchema, String definitions) throws IOException {
        Path file = specDir.resolve(SERVICE + "-slim.json");
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode spec = Files.exists(file)
                ? (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(file.toFile())
                : mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ObjectNode paths = spec.has("paths")
                ? (com.fasterxml.jackson.databind.node.ObjectNode) spec.get("paths") : spec.putObject("paths");
        paths.set(path, mapper.readTree("""
                {"%s": {"parameters": [{"in": "body", "name": "body", "required": true, "schema": %s}]}}
                """.formatted(method, bodySchema)));
        com.fasterxml.jackson.databind.node.ObjectNode defs = spec.has("definitions")
                ? (com.fasterxml.jackson.databind.node.ObjectNode) spec.get("definitions") : spec.putObject("definitions");
        if (!definitions.isBlank()) {
            defs.setAll((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("{" + definitions + "}"));
        }
        mapper.writeValue(file.toFile(), spec);
    }

    private static List<RestConnection> preserveConnections() throws IOException {
        try (InputStream in = RequestElementTreeBuilderTest.class.getResourceAsStream(PRESERVE_FIXTURE)) {
            assertNotNull(in, "test fixture not found: " + PRESERVE_FIXTURE);
            CommunicationPathResult result = new ObjectMapper().readValue(in, CommunicationPathResult.class);
            return new CommunicationPathConverter().convertToSystem(result).getRestConnections();
        }
    }

    private static RestConnection find(List<RestConnection> connections, String destination, String path) {
        return connections.stream()
                .filter(c -> "ts-preserve-service".equals(c.getSource()) && destination.equals(c.getDestination())
                        && path.equals(c.getEndpoint().getPath()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no connection preserve -> " + destination + " " + path));
    }

    private static List<String> names(List<RequestElement> elements) {
        return elements.stream().map(RequestElement::getName).collect(Collectors.toList());
    }
}
