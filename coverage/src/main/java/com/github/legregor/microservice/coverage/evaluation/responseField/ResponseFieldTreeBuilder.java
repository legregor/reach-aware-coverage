package com.github.legregor.microservice.coverage.evaluation.responseField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.legregor.microservice.coverage.openapi.OpenApiResponseResolver;
import com.github.legregor.microservice.coverage.openapi.SchemaField;
import com.github.legregor.microservice.coverage.openapi.SchemaFieldReader;

/**
 * Builds the response fields of a connection: for each relevant status code the field tree of its documented
 * response schema. {@code writeOnly} fields are no response fields.
 */
public class ResponseFieldTreeBuilder {
    /** Name of the single top-level field of a response whose body is a list. */
    public static final String TOP_LEVEL_LIST = "[]";

    private final OpenApiResponseResolver resolver;

    /** @param resolver resolver for the callee's specs; null if no specs are available (every code undocumented) */
    public ResponseFieldTreeBuilder(OpenApiResponseResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * @param service     the called service
     * @param method      HTTP method of the called endpoint
     * @param path        path (template) of the called endpoint
     * @param statusCodes the relevant status codes (returned and expected), in report order
     */
    public List<StatusResponse> build(String service, String method, String path, List<Integer> statusCodes) {
        List<StatusResponse> responses = new ArrayList<>();
        for (int statusCode : statusCodes) {
            Optional<OpenApiResponseResolver.DocumentedResponse> documented = resolver == null ? Optional.empty()
                    : resolver.resolve(service, method, path, statusCode);
            if (documented.isEmpty()) {
                responses.add(new StatusResponse(statusCode, null, List.of()));
                continue;
            }
            SchemaField body = documented.get().hasBody() ? SchemaFieldReader.read(documented.get().schema()) : null;
            List<ResponseField> fields = body != null ? topLevelFields(body) : List.of();
            responses.add(new StatusResponse(statusCode, documented.get().key(), fields, body));
        }
        return responses;
    }

    /** Top-level fields of a body: the properties of an object, or one {@code []} field for a list. */
    static List<ResponseField> topLevelFields(SchemaField body) {
        List<ResponseField> fields = new ArrayList<>();
        if (body.isList()) {
            ResponseField list = new ResponseField(TOP_LEVEL_LIST, true, body);
            addChildren(list, body, TOP_LEVEL_LIST);
            fields.add(list);
            return fields;
        }
        for (SchemaField child : body.getChildren()) {
            if (!child.isWriteOnly()) {
                fields.add(field(child, child.getName()));
            }
        }
        return fields;
    }

    private static ResponseField field(SchemaField schemaField, String name) {
        ResponseField field = new ResponseField(name, schemaField.isRequired(), schemaField);
        addChildren(field, schemaField, schemaField.isList() ? name + "[]" : name);
        return field;
    }

    /** Children of an object or of the items of a list: {@code parent.child} or {@code parent[].child}. */
    private static void addChildren(ResponseField parent, SchemaField schemaField, String prefix) {
        String childPrefix = prefix.equals(TOP_LEVEL_LIST) ? TOP_LEVEL_LIST : prefix;
        for (SchemaField child : schemaField.getChildren()) {
            if (!child.isWriteOnly()) {
                parent.addChild(field(child, childPrefix + "." + child.getName()));
            }
        }
    }
}
