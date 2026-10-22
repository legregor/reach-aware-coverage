package com.github.legregor.microservice.coverage.evaluation.requestElement;

import com.github.legregor.microservice.coverage.analysis.model.RequestElements;
import com.github.legregor.microservice.coverage.openapi.SchemaField;

import java.util.ArrayList;
import java.util.List;

/** Test helper: element trees built directly from spec fields (all elements reachable), without a RestConnection. */
public final class RequestElementTestTrees {

    private RequestElementTestTrees() {
    }

    /**
     * @param pathParameters path parameter fields (element {@code path:<name>})
     * @param body           body schema with object properties, may be null
     */
    public static RequestElementTree tree(List<SchemaField> pathParameters, SchemaField body) {
        List<RequestElement> elements = new ArrayList<>();
        for (SchemaField parameter : pathParameters) {
            elements.add(new RequestElement(RequestElements.path(parameter.getName()),
                    RequestElement.Kind.PATH_PARAMETER, parameter.getType(), true, true, parameter));
        }
        if (body != null) {
            for (SchemaField property : body.getChildren()) {
                String name = RequestElements.body(property.getName());
                RequestElement element = new RequestElement(name, RequestElement.Kind.BODY_PROPERTY,
                        property.getType(), property.isRequired(), true, property);
                addChildren(element, name, property);
                elements.add(element);
            }
        }
        return new RequestElementTree(elements, false, List.of(), List.of(), body);
    }

    /** Sets the static analysis results (constants, never null) of an element of a test tree. */
    public static void staticValues(RequestElementTree tree, String element, List<String> constants,
                                    boolean neverNull) {
        tree.find(element).orElseThrow().setStaticValues(constants, neverNull);
    }

    private static void addChildren(RequestElement parent, String parentName, SchemaField field) {
        String prefix = field.isList() ? parentName + "[]" : parentName;
        for (SchemaField childField : field.getChildren()) {
            String name = prefix + "." + childField.getName();
            RequestElement child = new RequestElement(name, RequestElement.Kind.BODY_PROPERTY, childField.getType(),
                    childField.isRequired(), true, childField);
            addChildren(child, name, childField);
            parent.addChild(child);
        }
    }
}
