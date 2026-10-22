package com.github.legregor.microservice.coverage.evaluation.bodySchema;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.legregor.microservice.coverage.analysis.model.RestConnection;
import com.github.legregor.microservice.coverage.openapi.OpenApiRequestBodyResolver;
import com.github.legregor.microservice.coverage.openapi.OpenApiSchema;
import com.github.legregor.microservice.coverage.openapi.OpenApiSpecRegistry;
import com.github.legregor.microservice.coverage.openapi.RequestBodySchemaValidator;

/**
 * Evaluates request-body payloads of a REST connection against the resolved
 * OpenAPI body schema.
 */
public class BodySchemaCoverageEvaluator {
    private final OpenApiRequestBodyResolver resolver;
    private final RequestBodySchemaValidator validator;

    public BodySchemaCoverageEvaluator(Path openApiDir) {
        this.resolver = new OpenApiRequestBodyResolver(new OpenApiSpecRegistry(openApiDir));
        this.validator = new RequestBodySchemaValidator();
    }

    /**
     * Resolves the schema for a connection and validates all observed request
     * bodies.
     */
    public BodySchemaCoverageResult evaluate(RestConnection connection, List<String> requestBodies, int missingCount) {
        if (connection == null) {
            return BodySchemaCoverageResult.notApplicable();
        }

        String serviceName = connection.getDestination();
        String method = connection.getEndpoint() != null ? connection.getEndpoint().getMethod() : null;
        if (method == null || method.isBlank()) {
            method = connection.getCall() != null ? connection.getCall().getMethod() : null;
        }
        String path = connection.getEndpoint() != null ? connection.getEndpoint().getPath() : null;
        if (path == null || path.isBlank()) {
            path = connection.getCall() != null ? connection.getCall().getUrl() : null;
        }

        // Coverage is only applicable if we can map this connection to an OpenAPI body
        // schema.
        Optional<OpenApiSchema> schemaOptional = resolver.resolveSchema(serviceName, method, path);
        if (schemaOptional.isEmpty()) {
            return BodySchemaCoverageResult.notApplicable();
        }

        int checkedCount = requestBodies == null ? 0 : requestBodies.size();
        if (checkedCount == 0) {
            return BodySchemaCoverageResult.notApplicable();
        }

        OpenApiSchema schema = schemaOptional.get();
        int matchedCount = 0;
        String mismatchReason = null;
        List<RequestBodySchemaCheck> requestChecks = new java.util.ArrayList<>();

        for (String body : requestBodies) {
            RequestBodySchemaValidator.ValidationResult result = validator.validateWithFieldPresence(body, schema);
            if (result.matches()) {
                matchedCount++;
            } else if (mismatchReason == null) {
                mismatchReason = result.getMessage();
            }

            requestChecks.add(new RequestBodySchemaCheck(
                    body,
                    result.matches(),
                    result.matches() ? null : result.getMessage(),
                    result.getPresentFields(),
                    result.getMissingFields()));
        }

        return new BodySchemaCoverageResult(true, checkedCount, matchedCount, missingCount, mismatchReason,
                schema.getResolvedSchema(), requestChecks);
    }
}
