package com.github.legregor.microservice.coverage.evaluation.requestValuePartition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.legregor.microservice.coverage.analysis.model.RequestElements;
import com.github.legregor.microservice.coverage.evaluation.requestElement.RequestElementTree;
import com.github.legregor.microservice.coverage.evaluation.valuePartition.ElementValueCollector;
import org.springframework.util.AntPathMatcher;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the values sent over a connection per request element name:
 * <ul>
 * <li>path parameters: from the URLs of the recorded calls, matched against the endpoint's path template and
 * URL-decoded (text, converted to the declared type by the classifier)</li>
 * <li>body elements: from the captured request bodies, along the body schema of the spec (absent fields are
 * recorded as "no value")</li>
 * </ul>
 * Element names follow {@link RequestElements}.
 */
public class ExecutedRequestValuesCollector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ElementValueCollector valueCollector = new ElementValueCollector();

    /**
     * @param endpointPath  path template of the called endpoint
     * @param callUrls      URLs of the recorded calls (client spans)
     * @param requestBodies captured request bodies of these calls
     */
    public Map<String, List<JsonNode>> collect(RequestElementTree tree, String endpointPath, List<String> callUrls,
                                               List<String> requestBodies) {
        Map<String, List<JsonNode>> values = new LinkedHashMap<>();
        if (endpointPath != null && callUrls != null) {
            for (String url : callUrls) {
                pathVariables(endpointPath, url).forEach((name, value) -> values
                        .computeIfAbsent(RequestElements.path(name), ignored -> new ArrayList<>())
                        .add(TextNode.valueOf(value)));
            }
        }
        if (tree.getBodySchema() != null && requestBodies != null) {
            for (String body : requestBodies) {
                JsonNode json = parse(body);
                if (json == null) {
                    continue;
                }
                valueCollector.collect(tree.getBodySchema(), json).forEach((path, list) -> values
                        .computeIfAbsent(elementName(path), ignored -> new ArrayList<>()).addAll(list));
            }
        }
        return values;
    }

    /** Field path of {@link ElementValueCollector} -> request element name. */
    static String elementName(String fieldPath) {
        if (fieldPath.isEmpty()) {
            return RequestElements.WHOLE_BODY;
        }
        return fieldPath.startsWith("[]") ? RequestElements.WHOLE_BODY + fieldPath : RequestElements.body(fieldPath);
    }

    /** Values of the path variables of a call URL, empty if the URL does not match the template. */
    Map<String, String> pathVariables(String endpointPath, String url) {
        String path = pathOf(url);
        if (path == null || !pathMatcher.match(endpointPath, path)) {
            return Map.of();
        }
        Map<String, String> decoded = new LinkedHashMap<>();
        pathMatcher.extractUriTemplateVariables(endpointPath, path).forEach((name, value) ->
                decoded.put(name, URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8)));
        return decoded;
    }

    private static String pathOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String withoutQuery = url.split("[?#]", 2)[0];
        int scheme = withoutQuery.indexOf("://");
        if (scheme < 0) {
            return withoutQuery;
        }
        try {
            return new URI(withoutQuery).getRawPath();
        } catch (Exception e) {
            int pathStart = withoutQuery.indexOf('/', scheme + 3);
            return pathStart < 0 ? "/" : withoutQuery.substring(pathStart);
        }
    }

    private static JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(body);
        } catch (Exception e) {
            return TextNode.valueOf(body); // not JSON: the body as a whole is a text value
        }
    }
}
