package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.legregor.microservice.coverage.analysis.model.BodyKind;
import com.github.legregor.microservice.coverage.analysis.model.EndpointTrace;
import com.github.legregor.microservice.coverage.analysis.model.StaticServiceTraceResult;
import com.github.legregor.microservice.coverage.analysis.model.TraceTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the request body reachability analysis (spec "request body reachability"): which top-level
 * properties of a sent request body can get a non-null value.
 * <p>
 * Each test writes a tiny service (controller, service class, DTOs) and runs the {@link StaticServiceTraceAnalyzer}.
 * Property names in the assertions are JSON names.
 */
class RequestBodyReachabilityAnalyzerTest {

    private static final String URL = "http://target-service/api/things";

    @TempDir
    Path tempDir;

    // ---------------------------------------------------------------------------------------------
    // Locally created objects
    // ---------------------------------------------------------------------------------------------

    @Test
    void localObjectWithSettersExplicitNullPrimitivesAndInitializers() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Notification", """
                        @Data
                        public class Notification {
                            private String email;
                            private String name;
                            private String phone;
                            private int priority;
                            private String channel = "MAIL";
                            private String note = null;
                        }
                        """),
                """
                        Notification notification = new Notification();
                        notification.setEmail("a@b.c");
                        notification.setName(null);
                        HttpEntity<Notification> entity = new HttpEntity<>(notification, headers);
                        restTemplate.exchange("%s", HttpMethod.POST, entity, String.class);
                        """.formatted(URL));

        assertEquals(BodyKind.OBJECT, target.getBodyKind());
        assertEquals("com.example.shop.Notification", target.getBodyType());
        assertEquals(Set.of("email", "name", "phone", "priority", "channel", "note"), target.getBodyProperties());
        assertEquals(Set.of("email", "priority", "channel"), target.getReachableBodyProperties());
        assertEquals(Set.of("name", "phone", "note"), target.getNotReachableBodyProperties());
    }

    @Test
    void directFieldAssignmentsCount() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Item", """
                        public class Item {
                            public String sku;
                            public String label;
                            public String color;
                        }
                        """),
                """
                        Item item = new Item();
                        item.sku = "A-1";
                        item.label = null;
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(item, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("sku"), target.getReachableBodyProperties());
        assertEquals(Set.of("label", "color"), target.getNotReachableBodyProperties());
    }

    @Test
    void lombokAllArgsConstructorWithNullArgument() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Consign", """
                        @Data
                        @AllArgsConstructor
                        @NoArgsConstructor
                        public class Consign {
                            private String id;
                            private String from;
                            private String to;
                        }
                        """),
                """
                        Consign consign = new Consign(null, "Berlin", "Munich");
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(consign, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("from", "to"), target.getReachableBodyProperties());
        assertEquals(Set.of("id"), target.getNotReachableBodyProperties());
    }

    @Test
    void declaredConstructorsAreAnalysed() throws IOException {
        Map<String, String> dto = Map.of(
                "Order", """
                        public class Order {
                            private String createdBy;
                            private String item;
                            private String comment;

                            public Order() {
                                createdBy = "system";
                            }

                            public Order(String item) {
                                this.item = item;
                            }
                        }
                        """);

        TraceTarget noArgs = analyzeSingleTarget(dto, """
                Order order = new Order();
                restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                """.formatted(URL));
        assertEquals(Set.of("createdBy"), noArgs.getReachableBodyProperties());

        TraceTarget withValue = analyzeSingleTarget(dto, """
                Order order = new Order("book");
                restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                """.formatted(URL));
        assertEquals(Set.of("item"), withValue.getReachableBodyProperties());

        TraceTarget withNull = analyzeSingleTarget(dto, """
                Order order = new Order(null);
                restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                """.formatted(URL));
        assertTrue(withNull.getReachableBodyProperties().isEmpty());
    }

    @Test
    void builderChain() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Builder
                        public class Order {
                            private String item;
                            private String comment;
                            private String voucher;
                            private int amount;
                        }
                        """),
                """
                        Order order = Order.builder().item("book").comment(null).build();
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("item", "amount"), target.getReachableBodyProperties());
        assertEquals(Set.of("comment", "voucher"), target.getNotReachableBodyProperties());
    }

    @Test
    void customSetterMethodUsesTheFieldsItAssigns() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private java.util.Date travelDate;
                            private String item;

                            public void setTravelDate(int year, int month, int day) {
                                java.util.Date date = new java.util.Date(year, month, day);
                                this.travelDate = date;
                            }
                        }
                        """),
                """
                        Order order = new Order();
                        order.setTravelDate(2026, 10, 6);
                        order.getItem();
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("travelDate"), target.getReachableBodyProperties());
        assertEquals(Set.of("item"), target.getNotReachableBodyProperties());
    }

    // ---------------------------------------------------------------------------------------------
    // Property names (Jackson / Lombok) and class hierarchy
    // ---------------------------------------------------------------------------------------------

    @Test
    void jacksonAndLombokPropertyNames() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Booking", """
                        @Data
                        public class Booking {
                            public static final String TYPE = "booking";
                            @JsonProperty("order_id")
                            private String orderId;
                            @JsonIgnore
                            private String secret;
                            private transient String cache;
                            private boolean isWithin;
                            private Boolean isPaid;
                        }
                        """),
                """
                        Booking booking = new Booking();
                        booking.setOrderId("o-1");
                        booking.setIsPaid(Boolean.TRUE);
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(booking, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("order_id", "within", "isPaid"), target.getBodyProperties());
        assertEquals(Set.of("order_id", "within", "isPaid"), target.getReachableBodyProperties());
    }

    @Test
    void lombokBooleanSetterWithoutIsPrefix() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Consign", """
                        @Data
                        public class Consign {
                            private Boolean express;
                            private boolean isWithin;
                            private String note;
                        }
                        """),
                """
                        Consign consign = new Consign();
                        consign.setWithin(true);
                        consign.setExpress(Boolean.TRUE);
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(consign, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("express", "within"), target.getReachableBodyProperties());
        assertEquals(Set.of("note"), target.getNotReachableBodyProperties());
    }

    @Test
    void superclassFieldsAreProperties() throws IOException {
        Map<String, String> classes = new LinkedHashMap<>();
        classes.put("BaseEntity", """
                @Data
                public class BaseEntity {
                    private String createdBy;
                }
                """);
        classes.put("Order", """
                @Data
                public class Order extends BaseEntity {
                    private String item;
                }
                """);
        TraceTarget target = analyzeSingleTarget(classes, """
                Order order = new Order();
                order.setCreatedBy("me");
                restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                """.formatted(URL));

        assertEquals(Set.of("item", "createdBy"), target.getBodyProperties());
        assertEquals(Set.of("createdBy"), target.getReachableBodyProperties());
    }

    // ---------------------------------------------------------------------------------------------
    // Origins: parameters, method results, fields, handed-on objects
    // ---------------------------------------------------------------------------------------------

    @Test
    void parameterIsFollowedBackToTheCallerThatBuildsTheObject() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "NotifyInfo", """
                        @Data
                        public class NotifyInfo {
                            private String email;
                            private String username;
                            private String orderNumber;
                        }
                        """),
                """
                        NotifyInfo info = new NotifyInfo();
                        info.setEmail("a@b.c");
                        info.setUsername("lena");
                        LOGGER.info("sending " + info.toString());
                        System.out.println(info);
                        sendEmail(info, headers);
                        """,
                """
                        public boolean sendEmail(NotifyInfo notifyInfo, HttpHeaders headers) {
                            HttpEntity requestEntity = new HttpEntity(notifyInfo, headers);
                            restTemplate.exchange("%s", HttpMethod.POST, requestEntity, Boolean.class);
                            return true;
                        }
                        """.formatted(URL));

        assertEquals(Set.of("email", "username"), target.getReachableBodyProperties());
        assertEquals(Set.of("orderNumber"), target.getNotReachableBodyProperties());
    }

    @Test
    void parameterWithSeveralCallSitesGivesTheUnion() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "NotifyInfo", """
                        @Data
                        public class NotifyInfo {
                            private String email;
                            private String username;
                            private String orderNumber;
                        }
                        """),
                """
                        NotifyInfo first = new NotifyInfo();
                        first.setEmail("a@b.c");
                        sendEmail(first, headers);
                        NotifyInfo second = new NotifyInfo();
                        second.setUsername("lena");
                        sendEmail(second, headers);
                        """,
                """
                        public boolean sendEmail(NotifyInfo notifyInfo, HttpHeaders headers) {
                            restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(notifyInfo, headers), Boolean.class);
                            return true;
                        }
                        """.formatted(URL));

        assertEquals(Set.of("email", "username"), target.getReachableBodyProperties());
        assertEquals(Set.of("orderNumber"), target.getNotReachableBodyProperties());
    }

    @Test
    void objectFromMethodReturnValueIsAllReachable() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private String id;
                            private int status;
                            private String comment;
                        }
                        """),
                """
                        Order order = loadOrder();
                        order.setStatus(4);
                        restTemplate.exchange("%s", HttpMethod.PUT, new HttpEntity<>(order, headers), String.class);
                        """.formatted(URL),
                """
                        private Order loadOrder() {
                            return new Order();
                        }
                        """);

        assertEquals(Set.of("id", "status", "comment"), target.getReachableBodyProperties());
        assertTrue(target.getNotReachableBodyProperties().isEmpty());
        assertTrue(target.getBodyOrigins().stream().anyMatch(origin -> origin.contains("return value")));
    }

    @Test
    void forwardedEndpointParameterIsAllReachable() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private String id;
                            private String comment;
                        }
                        """),
                null,
                """
                        public String forward(Order order, HttpHeaders headers) {
                            restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                            return "ok";
                        }
                        """.formatted(URL),
                "@PostMapping(\"/forward\") public String forward(@RequestBody Order order) { return service.forward(order, new HttpHeaders()); }");

        assertEquals(Set.of("id", "comment"), target.getReachableBodyProperties());
    }

    @Test
    void objectHandedToAnotherServiceMethodIsAllReachable() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private String id;
                            private String comment;
                        }
                        """),
                """
                        Order order = new Order();
                        order.setId("1");
                        enrich(order);
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                        """.formatted(URL),
                """
                        private void enrich(Order order) {
                            order.setComment("enriched");
                        }
                        """);

        assertEquals(Set.of("id", "comment"), target.getReachableBodyProperties());
    }

    @Test
    void aliasedObjectIsAllReachable() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private String id;
                            private String comment;
                        }
                        """),
                """
                        Order order = new Order();
                        Order alias = order;
                        alias.setComment("x");
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                        """.formatted(URL));

        assertEquals(Set.of("id", "comment"), target.getReachableBodyProperties());
    }

    @Test
    void classFieldIsAllReachable() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private String id;
                            private String comment;
                        }
                        """),
                """
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(template, headers), String.class);
                        """.formatted(URL),
                "private Order template = new Order();");

        assertEquals(Set.of("id", "comment"), target.getReachableBodyProperties());
    }

    // ---------------------------------------------------------------------------------------------
    // Body kinds and merging
    // ---------------------------------------------------------------------------------------------

    @Test
    void nonObjectBodyIsOneValue() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(), """
                List<String> ids = new ArrayList<>();
                ids.add("a");
                restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity(ids, headers), String.class);
                """.formatted(URL));

        assertEquals(BodyKind.VALUE, target.getBodyKind());
        assertTrue(target.getBodyProperties().isEmpty());
    }

    @Test
    void headersOnlyAndNullEntityMeanNoBody() throws IOException {
        TraceTarget headersOnly = analyzeSingleTarget(Map.of(), """
                HttpEntity requestEntity = new HttpEntity(headers);
                restTemplate.exchange("%s", HttpMethod.GET, requestEntity, String.class);
                """.formatted(URL));
        assertEquals(BodyKind.NONE, headersOnly.getBodyKind());

        TraceTarget nullEntity = analyzeSingleTarget(Map.of(), """
                restTemplate.exchange("%s", HttpMethod.GET, null, String.class);
                """.formatted(URL));
        assertEquals(BodyKind.NONE, nullEntity.getBodyKind());
    }

    @Test
    void sameCallTwiceInOneEndpointIsMergedNotDropped() throws IOException {
        StaticServiceTraceResult result = analyze(Map.of(
                "NotifyInfo", """
                        @Data
                        public class NotifyInfo {
                            private String email;
                            private String username;
                            private String orderNumber;
                        }
                        """),
                """
                        NotifyInfo first = new NotifyInfo();
                        first.setEmail("a@b.c");
                        restTemplate.exchange("%1$s", HttpMethod.POST, new HttpEntity<>(first, headers), String.class);
                        NotifyInfo second = new NotifyInfo();
                        second.setUsername("lena");
                        restTemplate.exchange("%1$s", HttpMethod.POST, new HttpEntity<>(second, headers), String.class);
                        """.formatted(URL),
                null, null);

        List<TraceTarget> targets = targets(result);
        assertEquals(1, targets.size());
        assertEquals(Set.of("email", "username"), targets.get(0).getReachableBodyProperties());
        assertEquals(Set.of("orderNumber"), targets.get(0).getNotReachableBodyProperties());
    }

    @Test
    void reusedEntityVariableUsesTheValueAssignedBeforeEachCall() throws IOException {
        StaticServiceTraceResult result = analyze(Map.of(
                "Seat", """
                        @Data
                        public class Seat {
                            private String trainNumber;
                            private int seatType;
                        }
                        """),
                """
                        Seat seat = new Seat();
                        seat.setTrainNumber("G1234");
                        HttpEntity requestEntity = new HttpEntity(headers);
                        restTemplate.exchange("http://travel-service/routes", HttpMethod.GET, requestEntity, String.class);
                        requestEntity = new HttpEntity(seat, headers);
                        restTemplate.exchange("http://order-service/tickets", HttpMethod.POST, requestEntity, String.class);
                        requestEntity = new HttpEntity(headers);
                        restTemplate.exchange("http://travel-service/train_types", HttpMethod.GET, requestEntity, String.class);
                        """,
                null, null);

        TraceTarget routes = targetWithUrl(result, "http://travel-service/routes");
        TraceTarget tickets = targetWithUrl(result, "http://order-service/tickets");
        TraceTarget trainTypes = targetWithUrl(result, "http://travel-service/train_types");

        assertEquals(BodyKind.NONE, routes.getBodyKind());
        assertEquals(false, routes.isRequestBodySent());
        assertEquals(BodyKind.OBJECT, tickets.getBodyKind());
        assertEquals(true, tickets.isRequestBodySent());
        assertEquals(Set.of("trainNumber", "seatType"), tickets.getReachableBodyProperties());
        assertEquals(BodyKind.NONE, trainTypes.getBodyKind());
        assertEquals(false, trainTypes.isRequestBodySent());
    }

    // ---------------------------------------------------------------------------------------------
    // Constant values and never-null properties (for value partition reachability)
    // ---------------------------------------------------------------------------------------------

    @Test
    void constantsFromLiteralsEnumsStaticFinalsAndPrimitiveDefaults() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "SeatClass", """
                        public enum SeatClass { ECONOMY, BUSINESS, FIRST }
                        """,
                "Seat", """
                        @Data
                        public class Seat {
                            private SeatClass seatClass;
                            private String type;
                            private boolean express;
                            private int count;
                            private Integer level;
                            private String note;
                            private String channel = "MAIL";
                        }
                        """),
                """
                        Seat seat = new Seat();
                        seat.setSeatClass(SeatClass.FIRST);
                        seat.setType(TYPE);
                        seat.setExpress(true);
                        seat.setLevel(-1);
                        seat.setNote(computeNote());
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(seat, headers), String.class);
                        """.formatted(URL),
                """
                        private static final String TYPE = "EXPRESS";

                        private String computeNote() {
                            return "note";
                        }
                        """);

        Map<String, List<String>> constants = target.getBodyConstantValues();
        assertEquals(List.of("\"FIRST\""), constants.get("seatClass"));
        assertEquals(List.of("\"EXPRESS\""), constants.get("type"));
        // the default false stays possible (a setter call does not have to run before the object is sent)
        assertEquals(List.of("false", "true"), constants.get("express"));
        assertEquals(List.of("0"), constants.get("count"));
        assertEquals(List.of("-1"), constants.get("level"));
        assertEquals(List.of("\"MAIL\""), constants.get("channel"));
        // any value: the result of a method call
        assertFalse(constants.containsKey("note"));
        assertEquals(Set.of("express", "count"), target.getNeverNullBodyProperties());
    }

    @Test
    void localVariablesConditionalsAndReassignments() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Message", """
                        @Data
                        public class Message {
                            private String channel;
                            private String mode;
                            private String ref;
                        }
                        """),
                """
                        Message message = new Message();
                        String channel = "MAIL";
                        if (urgent()) {
                            channel = "SMS";
                        }
                        message.setChannel(channel);
                        message.setMode(urgent() ? "FAST" : "SLOW");
                        String ref = "R";
                        ref += "1";
                        message.setRef(ref);
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(message, headers), String.class);
                        """.formatted(URL),
                """
                        private boolean urgent() {
                            return true;
                        }
                        """);

        Map<String, List<String>> constants = target.getBodyConstantValues();
        assertEquals(List.of("\"MAIL\"", "\"SMS\""), constants.get("channel"));
        assertEquals(List.of("\"FAST\"", "\"SLOW\""), constants.get("mode"));
        assertFalse(constants.containsKey("ref"));
    }

    @Test
    void builderAndAllArgsConstructorOverwriteDefaults() throws IOException {
        StaticServiceTraceResult result = analyze(Map.of(
                "Item", """
                        @Data
                        @Builder
                        @AllArgsConstructor
                        @NoArgsConstructor
                        public class Item {
                            private String sku;
                            private int quantity;
                        }
                        """),
                """
                        Item built = Item.builder().sku("A-1").quantity(2).build();
                        restTemplate.exchange("http://target-service/built", HttpMethod.POST, new HttpEntity<>(built, headers), String.class);
                        Item created = new Item("B-2", 5);
                        restTemplate.exchange("http://target-service/created", HttpMethod.POST, new HttpEntity<>(created, headers), String.class);
                        """, null, null);

        Map<String, List<String>> built = targetWithUrl(result, "http://target-service/built").getBodyConstantValues();
        assertEquals(List.of("\"A-1\""), built.get("sku"));
        assertEquals(List.of("2"), built.get("quantity"));
        Map<String, List<String>> created = targetWithUrl(result, "http://target-service/created").getBodyConstantValues();
        assertEquals(List.of("\"B-2\""), created.get("sku"));
        assertEquals(List.of("5"), created.get("quantity"));
    }

    @Test
    void constantsOfSeveralCallSitesAreUnited() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Seat", """
                        @Data
                        public class Seat {
                            private String type;
                        }
                        """),
                """
                        Seat first = new Seat();
                        first.setType("A");
                        post(first, headers);
                        Seat second = new Seat();
                        second.setType("B");
                        post(second, headers);
                        """,
                """
                        private void post(Seat seat, HttpHeaders headers) {
                            restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(seat, headers), String.class);
                        }
                        """.formatted(URL));

        assertEquals(List.of("\"A\"", "\"B\""), target.getBodyConstantValues().get("type"));
    }

    @Test
    void objectFromElsewhereHasNoConstants() throws IOException {
        TraceTarget target = analyzeSingleTarget(Map.of(
                "Order", """
                        @Data
                        public class Order {
                            private String id;
                            private int status;
                        }
                        """),
                """
                        Order order = load();
                        restTemplate.exchange("%s", HttpMethod.POST, new HttpEntity<>(order, headers), String.class);
                        """.formatted(URL),
                """
                        private Order load() {
                            return new Order();
                        }
                        """);

        assertTrue(target.getBodyConstantValues().isEmpty());
        // a primitive field is never null, wherever the object comes from
        assertEquals(Set.of("status"), target.getNeverNullBodyProperties());
    }

    private static TraceTarget targetWithUrl(StaticServiceTraceResult result, String url) {
        return targets(result).stream()
                .filter(target -> url.equals(target.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no call to " + url + " in " + targets(result)));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private TraceTarget analyzeSingleTarget(Map<String, String> dtos, String endpointBody) throws IOException {
        return analyzeSingleTarget(dtos, endpointBody, null);
    }

    private TraceTarget analyzeSingleTarget(Map<String, String> dtos, String endpointBody, String serviceMembers)
            throws IOException {
        return analyzeSingleTarget(dtos, endpointBody, serviceMembers, null);
    }

    private TraceTarget analyzeSingleTarget(Map<String, String> dtos, String endpointBody, String serviceMembers,
            String controllerEndpoint) throws IOException {
        List<TraceTarget> targets = targets(analyze(dtos, endpointBody, serviceMembers, controllerEndpoint));
        assertEquals(1, targets.size(), () -> "expected exactly one outgoing call, found " + targets);
        return targets.get(0);
    }

    /**
     * Writes a service "shop" with a controller (one endpoint calling {@code ShopService.run()} or the given
     * endpoint), {@code ShopService} (method {@code run()} with the given body + further members) and the DTOs.
     */
    private StaticServiceTraceResult analyze(Map<String, String> dtos, String endpointBody, String serviceMembers,
            String controllerEndpoint) throws IOException {
        Path root = Files.createTempDirectory(tempDir, "shop");
        Path sources = root.resolve("src/main/java/com/example/shop");
        Files.createDirectories(sources);

        String endpoint = controllerEndpoint != null
                ? controllerEndpoint
                : "@GetMapping(\"/run\") public String run() { service.run(); return \"ok\"; }";
        Files.writeString(sources.resolve("ShopController.java"), header() + """
                @RestController
                @RequestMapping("/shop")
                public class ShopController {
                    private final ShopService service = new ShopService();

                    %s
                }
                """.formatted(endpoint));

        String run = endpointBody == null ? "" : """
                public void run() {
                    HttpHeaders headers = new HttpHeaders();
                    %s
                }
                """.formatted(endpointBody);
        Files.writeString(sources.resolve("ShopService.java"), header() + """
                public class ShopService {
                    private static final Logger LOGGER = LoggerFactory.getLogger(ShopService.class);
                    private final RestTemplate restTemplate = new RestTemplate();

                    %s

                    %s
                }
                """.formatted(run, serviceMembers == null ? "" : serviceMembers));

        for (Map.Entry<String, String> dto : dtos.entrySet()) {
            Files.writeString(sources.resolve(dto.getKey() + ".java"), header() + dto.getValue());
        }
        return new StaticServiceTraceAnalyzer().analyze(List.of(root));
    }

    private static String header() {
        return """
                package com.example.shop;

                import com.fasterxml.jackson.annotation.JsonIgnore;
                import com.fasterxml.jackson.annotation.JsonProperty;
                import lombok.AllArgsConstructor;
                import lombok.Builder;
                import lombok.Data;
                import lombok.NoArgsConstructor;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                import org.springframework.http.HttpEntity;
                import org.springframework.http.HttpHeaders;
                import org.springframework.http.HttpMethod;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.client.RestTemplate;

                import java.util.ArrayList;
                import java.util.List;

                """;
    }

    private static List<TraceTarget> targets(StaticServiceTraceResult result) {
        List<TraceTarget> targets = new ArrayList<>();
        for (EndpointTrace endpointTrace : result.getEndpointTraces()) {
            targets.addAll(endpointTrace.getDownstreamTargets());
        }
        return targets;
    }
}
