package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.github.legregor.microservice.coverage.analysis.model.RequestElements;
import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.openapi.OpenApiRequestBodyResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSchema;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaFieldReader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the {@link RequestElementTree} of a connection by combining the OpenAPI spec of the called endpoint
 * (possible elements, read by {@link SchemaFieldReader}) with the static analysis of the caller (reachable elements,
 * stored in the {@link RestConnection}).
 * <ul>
 * <li>Path parameters: the variables of the endpoint's path template; required and always reachable.</li>
 * <li>Body with spec: the fields of the body schema. A top-level property is reachable if the caller can send it;
 * nested fields inherit their parent's reachability. Spec properties missing in the caller's DTO and DTO properties
 * missing in the spec are reported as interface mismatches.</li>
 * <li>readOnly properties of the spec are only in responses and are no request elements (they are not reported as
 * interface mismatches either).</li>
 * <li>Body without spec: the top-level properties of the caller's DTO.</li>
 * </ul>
 */
public class RequestElementTreeBuilder {

    private final OpenApiRequestBodyResolver resolver;

    /** @param openApiDir directory with the (slim) OpenAPI specs, may be null (then no spec is used) */
    public RequestElementTreeBuilder(Path openApiDir) {
        this(openApiDir == null ? null : new OpenApiRequestBodyResolver(new OpenApiSpecRegistry(openApiDir)));
    }

    public RequestElementTreeBuilder(OpenApiRequestBodyResolver resolver) {
        this.resolver = resolver;
    }

    public RequestElementTree build(RestConnection connection) {
        List<RequestElement> elements = new ArrayList<>();

        String endpointPath = connection.getEndpoint() != null ? connection.getEndpoint().getPath() : null;
        Map<String, SchemaField> pathParameters = resolvePathParameters(connection);
        for (String variable : RequestElements.pathVariables(endpointPath)) {
            // without a spec, a path parameter is a required string
            SchemaField field = pathParameters.getOrDefault(variable, new SchemaField(variable, "string", true, false));
            elements.add(new RequestElement(RequestElements.path(variable), RequestElement.Kind.PATH_PARAMETER,
                    field.getType(), true, true, field));
        }

        CallerBody caller = CallerBody.of(connection);
        Optional<OpenApiSchema> schema = resolveBodySchema(connection);
        List<String> onlyInSpec = new ArrayList<>();
        List<String> onlyInCaller = new ArrayList<>();

        SchemaField bodySchema = null;
        if (schema.isPresent()) {
            bodySchema = SchemaFieldReader.read(schema.get());
            addBodyFromSpec(bodySchema, caller, elements, onlyInSpec, onlyInCaller);
        } else {
            addBodyFromCaller(caller, elements);
        }
        boolean bodySpecMissing = schema.isEmpty() && caller.sendsBody();
        return new RequestElementTree(elements, bodySpecMissing, onlyInSpec, onlyInCaller, bodySchema);
    }

    // ---------------------------------------------------------------------------------------------
    // Body from the spec
    // ---------------------------------------------------------------------------------------------

    private void addBodyFromSpec(SchemaField body, CallerBody caller, List<RequestElement> elements,
                                 List<String> onlyInSpec, List<String> onlyInCaller) {
        if (!body.isObject()) {
            // the body is one value (List, String, number, ...); a list of objects gets the item properties
            RequestElement value = new RequestElement(RequestElements.WHOLE_BODY, RequestElement.Kind.BODY_VALUE,
                    body.getType(), true, caller.sendsBody(), body);
            addChildren(value, RequestElements.WHOLE_BODY, body, value.isReachable());
            elements.add(value);
            return;
        }

        Set<String> specProperties = new LinkedHashSet<>();
        for (SchemaField property : body.getChildren()) {
            specProperties.add(property.getName());
            if (property.isReadOnly()) {
                continue; // only in responses: never a request element (a value sent anyway is "unexpected")
            }
            boolean reachable = caller.reachable.contains(property.getName());
            String name = RequestElements.body(property.getName());
            RequestElement element = new RequestElement(name, RequestElement.Kind.BODY_PROPERTY, property.getType(),
                    property.isRequired(), reachable, property);
            element.setStaticValues(caller.constants.get(property.getName()), caller.neverNull.contains(property.getName()));
            addChildren(element, name, property, reachable);
            elements.add(element);

            if (caller.knowsProperties() && !caller.properties.contains(property.getName())) {
                onlyInSpec.add(property.getName());
            }
        }
        for (String callerProperty : caller.properties) {
            if (!specProperties.contains(callerProperty)) {
                onlyInCaller.add(callerProperty);
            }
        }
    }

    /** Nested fields: {@code parent.child}, list items {@code parent[].child}; reachability is inherited. */
    private void addChildren(RequestElement parent, String parentName, SchemaField field, boolean reachable) {
        String prefix = field.isList() ? parentName + "[]" : parentName;
        for (SchemaField childField : field.getChildren()) {
            if (childField.isReadOnly()) {
                continue;
            }
            String name = prefix + "." + childField.getName();
            RequestElement child = new RequestElement(name, RequestElement.Kind.BODY_PROPERTY, childField.getType(),
                    childField.isRequired(), reachable, childField);
            addChildren(child, name, childField, reachable);
            parent.addChild(child);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Body without spec: the caller's DTO
    // ---------------------------------------------------------------------------------------------

    private void addBodyFromCaller(CallerBody caller, List<RequestElement> elements) {
        if (caller.sendsValue) {
            elements.add(new RequestElement(RequestElements.WHOLE_BODY, RequestElement.Kind.BODY_VALUE, null, false,
                    true));
            return;
        }
        for (String property : caller.properties) {
            elements.add(new RequestElement(RequestElements.body(property), RequestElement.Kind.BODY_PROPERTY, null,
                    false, caller.reachable.contains(property)));
        }
    }

    /** Path parameters of the called endpoint from its spec, by name; empty without spec. */
    private Map<String, SchemaField> resolvePathParameters(RestConnection connection) {
        if (resolver == null || connection.getEndpoint() == null) {
            return Map.of();
        }
        return resolver.resolvePathParameters(connection.getDestination(), connection.getEndpoint().getMethod(),
                connection.getEndpoint().getPath());
    }

    private Optional<OpenApiSchema> resolveBodySchema(RestConnection connection) {
        if (resolver == null) {
            return Optional.empty();
        }
        String method = connection.getEndpoint() != null ? connection.getEndpoint().getMethod() : null;
        String path = connection.getEndpoint() != null ? connection.getEndpoint().getPath() : null;
        if ((method == null || path == null) && connection.getCall() != null) {
            method = method != null ? method : connection.getCall().getMethod();
            path = path != null ? path : connection.getCall().getUrl();
        }
        return resolver.resolveSchema(connection.getDestination(), method, path);
    }

    // ---------------------------------------------------------------------------------------------
    // What the caller sends (from the static analysis, stored in the RestConnection)
    // ---------------------------------------------------------------------------------------------

    private static final class CallerBody {
        private final Set<String> properties = new LinkedHashSet<>();
        private final Set<String> reachable = new LinkedHashSet<>();
        private final Map<String, List<String>> constants = new java.util.LinkedHashMap<>();
        private final Set<String> neverNull = new LinkedHashSet<>();
        private boolean sendsValue;

        static CallerBody of(RestConnection connection) {
            CallerBody caller = new CallerBody();
            for (String element : nullSafe(connection.getRequestElements())) {
                if (element.equals(RequestElements.WHOLE_BODY)) {
                    caller.sendsValue = true;
                } else if (element.startsWith(RequestElements.BODY_PREFIX)) {
                    caller.properties.add(element.substring(RequestElements.BODY_PREFIX.length()));
                }
            }
            for (String element : nullSafe(connection.getReachableRequestElements())) {
                if (element.startsWith(RequestElements.BODY_PREFIX)) {
                    caller.reachable.add(element.substring(RequestElements.BODY_PREFIX.length()));
                }
            }
            if (connection.getRequestElementConstants() != null) {
                connection.getRequestElementConstants().forEach((element, values) -> {
                    if (element.startsWith(RequestElements.BODY_PREFIX)) {
                        caller.constants.put(element.substring(RequestElements.BODY_PREFIX.length()), values);
                    }
                });
            }
            for (String element : nullSafe(connection.getNeverNullRequestElements())) {
                if (element.startsWith(RequestElements.BODY_PREFIX)) {
                    caller.neverNull.add(element.substring(RequestElements.BODY_PREFIX.length()));
                }
            }
            return caller;
        }

        /** The static analysis knows the properties of the caller's DTO. */
        boolean knowsProperties() {
            return !properties.isEmpty();
        }

        boolean sendsBody() {
            return sendsValue || !reachable.isEmpty();
        }

        private static List<String> nullSafe(List<String> values) {
            return values == null ? List.of() : values;
        }
    }
}
