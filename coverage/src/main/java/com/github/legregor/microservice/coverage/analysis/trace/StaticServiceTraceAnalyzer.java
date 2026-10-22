package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.legregor.microservice.coverage.analysis.model.EndpointTrace;
import com.github.legregor.microservice.coverage.analysis.model.MethodTraceResult;
import com.github.legregor.microservice.coverage.analysis.model.StaticServiceTraceResult;
import com.github.legregor.microservice.coverage.analysis.model.TraceTarget;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StaticServiceTraceAnalyzer {
    public StaticServiceTraceResult analyze(List<Path> microserviceRoots) throws IOException {
        SourceIndex sourceIndex = SourceIndex.fromRoots(microserviceRoots);
        Map<MethodKey, MethodTraceResult> methodTraceCache = new LinkedHashMap<>();
        RequestBodyReachabilityAnalyzer bodyAnalyzer = new RequestBodyReachabilityAnalyzer(sourceIndex,
                this::resolveExpressionType,
                (classInfo, method, call) -> resolveInvokedMethod(classInfo, method, call, sourceIndex));
        List<EndpointTrace> endpointTraces = new ArrayList<>();

        for (ClassInfo classInfo : sourceIndex.getClasses()) {
            if (!classInfo.isRestController()) {
                continue;
            }

            String classLevelPath = pathFromAnnotation(
                    classInfo.getDeclaration().getAnnotationByName("RequestMapping").orElse(null));

            // for each method of controller, check in cfg which REST calls to other
            // services are reachable
            for (MethodDeclaration methodDeclaration : classInfo.getDeclaration().getMethods()) {
                Optional<String> httpMethod = resolveHttpMethod(methodDeclaration);
                if (httpMethod.isEmpty()) {
                    continue;
                }

                MethodKey methodKey = MethodKey.from(classInfo, methodDeclaration);
                MethodTraceResult methodTraceResult = traceMethod(classInfo, methodDeclaration, sourceIndex,
                        methodTraceCache, new ArrayDeque<>(), bodyAnalyzer);

                EndpointTrace endpointTrace = new EndpointTrace();
                endpointTrace.setSourceService(classInfo.getSourceServiceName());
                endpointTrace.setSourceRoot(classInfo.getSourceRoot().toString());
                endpointTrace.setEndpointMethodKey(methodKey.asString());
                endpointTrace.setHttpMethod(httpMethod.get());
                endpointTrace.setPath(SourceText.mergePaths(classLevelPath, methodPath(methodDeclaration)));
                endpointTrace.setReturnType(methodDeclaration.getTypeAsString());
                endpointTrace.setEndpointArguments(methodDeclaration.getParameters().stream()
                        .map(Parameter::toString)
                        .collect(Collectors.toList()));
                endpointTrace.setReachableMethodKeys(new LinkedHashSet<>(methodTraceResult.getCalledMethodKeys()));
                endpointTrace.setDownstreamTargets(new LinkedHashSet<>(methodTraceResult.getDownstreamTargets()));
                endpointTraces.add(endpointTrace);
            }
        }

        return new StaticServiceTraceResult(endpointTraces, methodTraceCacheToStringMap(methodTraceCache));
    }

    private Map<String, MethodTraceResult> methodTraceCacheToStringMap(Map<MethodKey, MethodTraceResult> cache) {
        Map<String, MethodTraceResult> converted = new LinkedHashMap<>();
        for (Map.Entry<MethodKey, MethodTraceResult> entry : cache.entrySet()) {
            converted.put(entry.getKey().asString(), entry.getValue());
        }
        return converted;
    }

    private MethodTraceResult traceMethod(ClassInfo classInfo, MethodDeclaration methodDeclaration,
            SourceIndex sourceIndex, Map<MethodKey, MethodTraceResult> cache, Deque<MethodKey> callStack,
            RequestBodyReachabilityAnalyzer bodyAnalyzer) {
        MethodKey methodKey = MethodKey.from(classInfo, methodDeclaration);
        MethodTraceResult cachedTrace = cache.get(methodKey);
        if (cachedTrace != null) {
            return cachedTrace;
        }

        if (callStack.contains(methodKey)) {
            MethodTraceResult cycleResult = new MethodTraceResult();
            cycleResult.setMethodKey(methodKey.asString());
            return cycleResult;
        }

        callStack.push(methodKey);
        MethodTraceResult traceResult = new MethodTraceResult();
        traceResult.setMethodKey(methodKey.asString());

        for (MethodCallExpr methodCallExpr : methodDeclaration.findAll(MethodCallExpr.class)) {
            List<TraceTarget> directTargets = resolveRestTemplateExchange(classInfo, methodDeclaration,
                    methodCallExpr, bodyAnalyzer);
            if (!directTargets.isEmpty()) {
                directTargets.forEach(target -> addOrMerge(traceResult.getDownstreamTargets(), target));
                continue;
            }

            Optional<MethodResolution> resolvedMethod = resolveInvokedMethod(classInfo, methodDeclaration,
                    methodCallExpr,
                    sourceIndex);
            if (resolvedMethod.isEmpty()) {
                continue;
            }

            MethodResolution resolution = resolvedMethod.get();
            traceResult.getCalledMethodKeys().add(resolution.methodKey.asString());

            MethodTraceResult downstreamTrace = traceMethod(resolution.classInfo, resolution.methodDeclaration,
                    sourceIndex,
                    cache, callStack, bodyAnalyzer);
            for (TraceTarget downstreamTarget : downstreamTrace.getDownstreamTargets()) {
                addOrMerge(traceResult.getDownstreamTargets(), downstreamTarget);
            }
            traceResult.getCalledMethodKeys().addAll(downstreamTrace.getCalledMethodKeys());
        }

        callStack.pop();
        cache.put(methodKey, traceResult);
        return traceResult;
    }

    /**
     * Adds a target to the set; if an equal target (same method, URL, destination, return type) is already there,
     * both are merged into a new object instead of dropping the second one. Targets from cached method results
     * are shared between methods, so they are never changed in place.
     */
    private static void addOrMerge(Set<TraceTarget> targets, TraceTarget incoming) {
        if (!targets.contains(incoming)) {
            targets.add(incoming);
            return;
        }
        List<TraceTarget> ordered = new ArrayList<>(targets);
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).equals(incoming) && ordered.get(i) != incoming) {
                ordered.set(i, TraceTarget.merge(ordered.get(i), incoming));
            }
        }
        targets.clear();
        targets.addAll(ordered);
    }

    private Optional<MethodResolution> resolveInvokedMethod(ClassInfo currentClass, MethodDeclaration currentMethod,
            MethodCallExpr methodCallExpr, SourceIndex sourceIndex) {
        String methodName = methodCallExpr.getNameAsString();

        if (isRestTemplateExchange(methodCallExpr, currentClass, currentMethod)) {
            return Optional.empty();
        }

        ClassInfo targetClass = resolveTargetClass(currentClass, currentMethod, methodCallExpr.getScope().orElse(null),
                sourceIndex);
        if (targetClass == null) {
            return Optional.empty();
        }

        targetClass = sourceIndex.resolveConcreteImplementation(targetClass).orElse(targetClass);

        MethodDeclaration targetMethod = resolveTargetMethod(targetClass, methodName,
                methodCallExpr.getArguments().size(),
                sourceIndex);
        if (targetMethod == null) {
            return Optional.empty();
        }

        return Optional.of(new MethodResolution(targetClass, MethodKey.from(targetClass, targetMethod), targetMethod));
    }

    private MethodDeclaration resolveTargetMethod(ClassInfo targetClass, String methodName, int argumentCount,
            SourceIndex sourceIndex) {
        List<MethodDeclaration> candidates = targetClass.getDeclaration().getMethodsByName(methodName).stream()
                .filter(methodDeclaration -> methodDeclaration.getParameters().size() == argumentCount)
                .toList();

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        if (candidates.isEmpty()) {
            for (ClassInfo implementor : sourceIndex.findImplementations(targetClass)) {
                List<MethodDeclaration> implementorCandidates = implementor.getDeclaration()
                        .getMethodsByName(methodName).stream()
                        .filter(methodDeclaration -> methodDeclaration.getParameters().size() == argumentCount)
                        .toList();
                if (implementorCandidates.size() == 1) {
                    return implementorCandidates.get(0);
                }
            }
        }

        return null;
    }

    private ClassInfo resolveTargetClass(ClassInfo currentClass, MethodDeclaration currentMethod, Expression scope,
            SourceIndex sourceIndex) {
        if (scope == null || scope.isThisExpr()) {
            return currentClass;
        }

        String resolvedType = resolveExpressionType(currentClass, currentMethod, scope);
        if (resolvedType == null) {
            return null;
        }

        return sourceIndex.resolveClass(resolvedType, currentClass.getPackageName());
    }

    /**
     * The downstream targets of a {@code RestTemplate.exchange} call: one per URL the call can use. A URL held in a
     * local variable that is assigned in several branches (e.g. {@code requestUrl = travel} / {@code travel2}
     * depending on the trip id) gives one target per assignment.
     */
    private List<TraceTarget> resolveRestTemplateExchange(ClassInfo currentClass, MethodDeclaration currentMethod,
            MethodCallExpr methodCallExpr, RequestBodyReachabilityAnalyzer bodyAnalyzer) {
        if (!"exchange".equals(methodCallExpr.getNameAsString())) {
            return List.of();
        }

        Expression scope = methodCallExpr.getScope().orElse(null);
        if (scope == null) {
            return List.of();
        }

        String scopeType = resolveExpressionType(currentClass, currentMethod, scope);
        if (scopeType == null || !scopeType.endsWith("RestTemplate")) {
            return List.of();
        }

        if (methodCallExpr.getArguments().isEmpty()) {
            return List.of();
        }

        List<String> urls = resolveStringAlternatives(currentClass, currentMethod,
                methodCallExpr.getArguments().get(0), 0).stream()
                .filter(url -> url != null && !url.isBlank() && !url.contains("{var}"))
                .toList();
        if (urls.isEmpty()) {
            return List.of();
        }

        String httpMethod = resolveHttpMethodForExchange(methodCallExpr.getArguments());
        String returnType = resolveReturnTypeForExchange(currentMethod, methodCallExpr);
        HttpEntityUsage usage = resolveHttpEntityUsage(currentClass, currentMethod, methodCallExpr);
        RequestBodyReachabilityAnalyzer.BodyReachability body =
                bodyAnalyzer.analyze(currentClass, currentMethod, methodCallExpr);
        Set<String> notReachable = new LinkedHashSet<>(body.properties());
        notReachable.removeAll(body.reachable());

        List<TraceTarget> targets = new ArrayList<>();
        for (String url : urls) {
            TraceTarget target = new TraceTarget();
            target.setHttpMethod(httpMethod);
            target.setUrl(url);
            target.setDestinationService(resolveDestinationService(url));
            target.setReturnType(returnType);
            target.setSentPathVariables(extractPathVariables(url));
            target.setSentQueryParameters(extractQueryParameters(url));
            target.setRequestBodySent(usage.requestBodySent);
            target.setHeadersSent(usage.headersSent);
            target.setBodyKind(body.kind());
            target.setBodyType(body.type());
            target.setBodyProperties(new LinkedHashSet<>(body.properties()));
            target.setReachableBodyProperties(new LinkedHashSet<>(body.reachable()));
            target.setNotReachableBodyProperties(new LinkedHashSet<>(notReachable));
            target.setBodyOrigins(new LinkedHashSet<>(body.origins()));
            target.setBodyConstantValues(new LinkedHashMap<>(body.constantValues()));
            target.setNeverNullBodyProperties(new LinkedHashSet<>(body.neverNull()));
            targets.add(target);
        }
        return targets;
    }

    /** Upper bound for the number of URL alternatives of one call (combinations of branch assignments). */
    private static final int MAX_URL_ALTERNATIVES = 16;

    /**
     * All values a string expression can have: like {@link #resolveStringExpression}, but a local variable that is
     * assigned several times (its initializer and every assignment, blank values skipped) contributes each of its
     * values, and concatenations combine the alternatives of both sides. A part only known at runtime after a known
     * prefix becomes a path placeholder. Empty if the value cannot be determined.
     */
    private List<String> resolveStringAlternatives(ClassInfo currentClass, MethodDeclaration currentMethod,
            Expression expression, int depth) {
        if (expression == null || depth > 8) {
            return List.of();
        }
        if (expression.isEnclosedExpr()) {
            return resolveStringAlternatives(currentClass, currentMethod, expression.asEnclosedExpr().getInner(),
                    depth + 1);
        }
        if (expression.isBinaryExpr() && expression.asBinaryExpr().getOperator() == BinaryExpr.Operator.PLUS) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            List<String> left = resolveStringAlternatives(currentClass, currentMethod, binaryExpr.getLeft(), depth + 1);
            if (left.isEmpty()) {
                return List.of();
            }
            List<String> right = resolveStringAlternatives(currentClass, currentMethod, binaryExpr.getRight(),
                    depth + 1);
            if (right.isEmpty()) {
                right = List.of("{" + placeholderName(binaryExpr.getRight()) + "}");
            }
            Set<String> combined = new LinkedHashSet<>();
            for (String l : left) {
                for (String r : right) {
                    if (combined.size() < MAX_URL_ALTERNATIVES) {
                        combined.add(l + r);
                    }
                }
            }
            return new ArrayList<>(combined);
        }
        if (expression.isNameExpr()) {
            String variableName = expression.asNameExpr().getNameAsString();
            List<Expression> values = localVariableValues(currentMethod, variableName);
            if (!values.isEmpty()) {
                Set<String> alternatives = new LinkedHashSet<>();
                for (Expression value : values) {
                    for (String alternative : resolveStringAlternatives(currentClass, currentMethod, value,
                            depth + 1)) {
                        if (!alternative.isBlank() && alternatives.size() < MAX_URL_ALTERNATIVES) {
                            alternatives.add(alternative);
                        }
                    }
                }
                if (!alternatives.isEmpty()) {
                    return new ArrayList<>(alternatives);
                }
            }
        }
        String single = resolveStringExpression(currentClass, currentMethod, expression);
        return single == null ? List.of() : List.of(single);
    }

    /** The initializer and every assigned value of a local variable of the method, in source order. */
    private static List<Expression> localVariableValues(MethodDeclaration currentMethod, String variableName) {
        List<Expression> values = new ArrayList<>();
        for (VariableDeclarationExpr declaration : currentMethod.findAll(VariableDeclarationExpr.class)) {
            for (VariableDeclarator declarator : declaration.getVariables()) {
                if (declarator.getNameAsString().equals(variableName)) {
                    declarator.getInitializer().ifPresent(values::add);
                }
            }
        }
        for (AssignExpr assignExpr : currentMethod.findAll(AssignExpr.class)) {
            if (assignExpr.getOperator() == AssignExpr.Operator.ASSIGN && assignExpr.getTarget().isNameExpr()
                    && assignExpr.getTarget().asNameExpr().getNameAsString().equals(variableName)) {
                values.add(assignExpr.getValue());
            }
        }
        return values;
    }

    private String resolveReturnTypeForExchange(MethodDeclaration currentMethod, MethodCallExpr exchangeCall) {
        if (exchangeCall.getArguments().size() < 4) {
            return null;
        }
        return resolveReturnTypeExpression(currentMethod, exchangeCall.getArguments().get(3), 0);
    }

    private String resolveReturnTypeExpression(MethodDeclaration currentMethod, Expression expression, int depth) {
        if (expression == null || depth > 3) {
            return null;
        }

        if (expression.isClassExpr()) {
            return expression.asClassExpr().getType().asString();
        }

        if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
            String createdType = objectCreationExpr.getType().asString();
            if ("ParameterizedTypeReference".equals(objectCreationExpr.getType().getNameAsString())
                    && objectCreationExpr.getType().getTypeArguments().isPresent()
                    && !objectCreationExpr.getType().getTypeArguments().get().isEmpty()) {
                return objectCreationExpr.getType().getTypeArguments().get().get(0).asString();
            }
            return createdType;
        }

        if (expression.isNameExpr()) {
            String variableName = expression.asNameExpr().getNameAsString();
            Expression initializer = findVariableInitializer(currentMethod, variableName);
            if (initializer != null) {
                return resolveReturnTypeExpression(currentMethod, initializer, depth + 1);
            }
            Expression assignment = findVariableAssignment(currentMethod, variableName);
            if (assignment != null) {
                return resolveReturnTypeExpression(currentMethod, assignment, depth + 1);
            }
        }

        if (expression.isCastExpr()) {
            return resolveReturnTypeExpression(currentMethod, expression.asCastExpr().getExpression(), depth + 1);
        }

        return null;
    }

    private Set<String> extractPathVariables(String url) {
        if (url == null || url.isBlank()) {
            return new LinkedHashSet<>();
        }
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\{([^/{}]+)\\}").matcher(url);
        while (matcher.find()) {
            String variable = matcher.group(1);
            if (variable != null && !variable.isBlank()) {
                variables.add(variable);
            }
        }
        return variables;
    }

    private Set<String> extractQueryParameters(String url) {
        if (url == null || url.isBlank()) {
            return new LinkedHashSet<>();
        }
        String query = null;
        try {
            query = new URI(url).getQuery();
        } catch (Exception ignored) {
            int queryStart = url.indexOf('?');
            if (queryStart >= 0 && queryStart < url.length() - 1) {
                query = url.substring(queryStart + 1);
            }
        }
        if (query == null || query.isBlank()) {
            return new LinkedHashSet<>();
        }
        Set<String> params = new LinkedHashSet<>();
        for (String part : query.split("&")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String key = part;
            int equals = part.indexOf('=');
            if (equals >= 0) {
                key = part.substring(0, equals);
            }
            String decoded = URLDecoder.decode(key, StandardCharsets.UTF_8);
            if (!decoded.isBlank()) {
                params.add(decoded);
            }
        }
        return params;
    }

    private HttpEntityUsage resolveHttpEntityUsage(ClassInfo currentClass, MethodDeclaration currentMethod,
            MethodCallExpr exchangeCall) {
        if (exchangeCall.getArguments().size() < 3) {
            return HttpEntityUsage.none();
        }
        return resolveHttpEntityUsage(currentClass, currentMethod, exchangeCall.getArguments().get(2), 0);
    }

    private HttpEntityUsage resolveHttpEntityUsage(ClassInfo currentClass, MethodDeclaration currentMethod,
            Expression requestEntityExpression, int depth) {
        if (requestEntityExpression == null || depth > 3) {
            return HttpEntityUsage.none();
        }
        if (requestEntityExpression.isNullLiteralExpr()) {
            return HttpEntityUsage.none();
        }
        if (requestEntityExpression.isObjectCreationExpr()) {
            ObjectCreationExpr objectCreationExpr = requestEntityExpression.asObjectCreationExpr();
            String typeName = objectCreationExpr.getType().getNameAsString();
            if (!typeName.endsWith("HttpEntity")) {
                return HttpEntityUsage.none();
            }
            int argumentCount = objectCreationExpr.getArguments().size();
            if (argumentCount == 0) {
                return HttpEntityUsage.none();
            }
            if (argumentCount == 1) {
                Expression single = objectCreationExpr.getArguments().get(0);
                String expressionType = resolveExpressionType(currentClass, currentMethod, single);
                boolean headersSent = expressionType != null
                        && (expressionType.endsWith("HttpHeaders") || expressionType.endsWith("MultiValueMap"));
                return headersSent ? HttpEntityUsage.headersOnly() : HttpEntityUsage.bodyOnly();
            }
            return HttpEntityUsage.bodyAndHeaders();
        }
        if (requestEntityExpression.isNameExpr()) {
            String variableName = requestEntityExpression.asNameExpr().getNameAsString();
            // a reused variable: take the value assigned last before this use
            Optional<Expression> reaching = ReachingDefinitions.lastDefinitionBefore(currentMethod, variableName,
                    requestEntityExpression);
            if (reaching.isPresent()) {
                return resolveHttpEntityUsage(currentClass, currentMethod, reaching.get(), depth + 1);
            }
            Expression initializer = findVariableInitializer(currentMethod, variableName);
            if (initializer != null) {
                return resolveHttpEntityUsage(currentClass, currentMethod, initializer, depth + 1);
            }
            Expression assignment = findVariableAssignment(currentMethod, variableName);
            if (assignment != null) {
                return resolveHttpEntityUsage(currentClass, currentMethod, assignment, depth + 1);
            }
        }
        return HttpEntityUsage.none();
    }

    private Expression findVariableInitializer(MethodDeclaration currentMethod, String variableName) {
        for (VariableDeclarationExpr variableDeclarationExpr : currentMethod.findAll(VariableDeclarationExpr.class)) {
            for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                if (!variableDeclarator.getNameAsString().equals(variableName)) {
                    continue;
                }
                if (variableDeclarator.getInitializer().isPresent()) {
                    return variableDeclarator.getInitializer().get();
                }
            }
        }
        return null;
    }

    private Expression findVariableAssignment(MethodDeclaration currentMethod, String variableName) {
        for (AssignExpr assignExpr : currentMethod.findAll(AssignExpr.class)) {
            if (assignExpr.getTarget().isNameExpr()
                    && assignExpr.getTarget().asNameExpr().getNameAsString().equals(variableName)) {
                return assignExpr.getValue();
            }
        }
        return null;
    }

    private String resolveHttpMethodForExchange(com.github.javaparser.ast.NodeList<Expression> arguments) {
        String joinedArguments = arguments.toString();
        if (joinedArguments.contains("HttpMethod.POST")) {
            return "POST";
        }
        if (joinedArguments.contains("HttpMethod.PUT")) {
            return "PUT";
        }
        if (joinedArguments.contains("HttpMethod.DELETE")) {
            return "DELETE";
        }
        if (joinedArguments.contains("HttpMethod.PATCH")) {
            return "PATCH";
        }
        return "GET";
    }

    private String resolveDestinationService(String url) {
        Matcher matcher = Pattern.compile("^https?://([^/:]+)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String resolveStringExpression(ClassInfo currentClass, MethodDeclaration currentMethod,
            Expression expression) {
        if (expression == null) {
            return null;
        }

        if (expression.isStringLiteralExpr()) {
            return SourceText.removeEnclosedQuotations(expression.toString());
        }

        if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            String left = resolveStringExpression(currentClass, currentMethod, binaryExpr.getLeft());
            if (left == null) {
                return null;
            }
            String right = resolveStringExpression(currentClass, currentMethod, binaryExpr.getRight());
            if (right == null) {
                // A value only known at runtime (e.g. "/routes/" + request.getTrainNumber(), or a local variable
                // assigned from a method call) becomes a path placeholder, like a method parameter. Dropping the
                // whole URL would hide the call and its connection.
                right = "{" + placeholderName(binaryExpr.getRight()) + "}";
            }
            return left + right;
        }

        if (expression.isNameExpr()) {
            String variableName = expression.asNameExpr().getNameAsString();
            String methodValue = findMethodStringValue(currentClass, currentMethod, variableName);
            if (methodValue != null) {
                return methodValue;
            }

            String parameterValue = findMethodParameterValue(currentMethod, variableName);
            if (parameterValue != null) {
                return parameterValue;
            }

            return resolveFieldStringValue(currentClass, variableName);
        }

        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            if (fieldAccessExpr.getScope().isThisExpr()) {
                return resolveFieldStringValue(currentClass, fieldAccessExpr.getNameAsString());
            }
        }

        return null;
    }

    /**
     * Name of the placeholder for a URL part that is only known at runtime: the variable name, or the property of a
     * getter ({@code seatRequest.getTrainNumber()} -> {@code trainNumber}); {@code value} otherwise. Never
     * {@code var}, which marks normalised path variables.
     */
    public static String placeholderName(Expression expression) {
        String name = null;
        if (expression.isNameExpr()) {
            name = expression.asNameExpr().getNameAsString();
        } else if (expression.isFieldAccessExpr()) {
            name = expression.asFieldAccessExpr().getNameAsString();
        } else if (expression.isMethodCallExpr()) {
            name = expression.asMethodCallExpr().getNameAsString();
            if (name.length() > 3 && name.startsWith("get") && Character.isUpperCase(name.charAt(3))) {
                name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2))) {
                name = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
        } else if (expression.isEnclosedExpr()) {
            return placeholderName(expression.asEnclosedExpr().getInner());
        }
        return name == null || name.isBlank() || name.equals("var") ? "value" : name;
    }

    private String findMethodStringValue(ClassInfo currentClass, MethodDeclaration currentMethod, String variableName) {
        for (VariableDeclarationExpr variableDeclarationExpr : currentMethod.findAll(VariableDeclarationExpr.class)) {
            for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                if (!variableDeclarator.getNameAsString().equals(variableName)) {
                    continue;
                }

                if (variableDeclarator.getInitializer().isPresent()) {
                    String resolved = resolveStringExpression(currentClass, currentMethod,
                            variableDeclarator.getInitializer().get());
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }

        for (AssignExpr assignExpr : currentMethod.findAll(AssignExpr.class)) {
            if (assignExpr.getTarget().isNameExpr()
                    && assignExpr.getTarget().asNameExpr().getNameAsString().equals(variableName)) {
                String resolved = resolveStringExpression(currentClass, currentMethod, assignExpr.getValue());
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return null;
    }

    private String findMethodParameterValue(MethodDeclaration currentMethod, String variableName) {
        for (Parameter parameter : currentMethod.getParameters()) {
            if (parameter.getNameAsString().equals(variableName)) {
                return "{" + variableName + "}";
            }
        }

        return null;
    }

    private String resolveFieldStringValue(ClassInfo currentClass, String fieldName) {
        for (FieldDeclaration fieldDeclaration : currentClass.getDeclaration().findAll(FieldDeclaration.class)) {
            for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                if (!variableDeclarator.getNameAsString().equals(fieldName)) {
                    continue;
                }

                if (variableDeclarator.getInitializer().isPresent()) {
                    return resolveLiteralString(variableDeclarator.getInitializer().get());
                }

                for (AnnotationExpr annotationExpr : fieldDeclaration.getAnnotations()) {
                    if ("Value".equals(annotationExpr.getNameAsString())) {
                        String text = annotationExpr.toString();
                        int colon = text.lastIndexOf(':');
                        int end = text.lastIndexOf('}');
                        if (colon != -1 && end != -1 && colon < end) {
                            return SourceText.removeEnclosedQuotations(text.substring(colon + 1, end).trim());
                        }
                    }
                }
            }
        }

        return null;
    }

    private String resolveLiteralString(Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return expression.asStringLiteralExpr().asString();
        }
        if (expression.isCharLiteralExpr()) {
            return String.valueOf(expression.asCharLiteralExpr().asChar());
        }
        return expression.toString();
    }

    String resolveExpressionType(ClassInfo currentClass, MethodDeclaration currentMethod,
            Expression expression) {
        if (expression == null) {
            return null;
        }

        if (expression.isThisExpr()) {
            return currentClass.getFqName();
        }

        if (expression.isNameExpr()) {
            String name = expression.asNameExpr().getNameAsString();

            for (Parameter parameter : currentMethod.getParameters()) {
                if (parameter.getNameAsString().equals(name)) {
                    return resolveTypeName(currentClass, parameter.getType().asString());
                }
            }

            for (VariableDeclarationExpr variableDeclarationExpr : currentMethod
                    .findAll(VariableDeclarationExpr.class)) {
                for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                    if (variableDeclarator.getNameAsString().equals(name)) {
                        String concreteType = resolveConcreteInitializerType(currentClass, variableDeclarator);
                        return concreteType != null ? concreteType
                                : resolveTypeName(currentClass, variableDeclarator.getType().asString());
                    }
                }
            }

            String fieldConcreteType = currentClass.getFieldConcreteTypes().get(name);
            if (fieldConcreteType != null) {
                return fieldConcreteType;
            }

            String fieldType = currentClass.getFieldTypes().get(name);
            if (fieldType != null) {
                return fieldType;
            }

            if (currentClass.getSimpleName().equals(name)) {
                return currentClass.getFqName();
            }
        }

        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = expression.asFieldAccessExpr();
            if (fieldAccessExpr.getScope().isThisExpr()) {
                String fieldName = fieldAccessExpr.getNameAsString();
                String fieldConcreteType = currentClass.getFieldConcreteTypes().get(fieldName);
                if (fieldConcreteType != null) {
                    return fieldConcreteType;
                }
                return currentClass.getFieldTypes().get(fieldName);
            }
        }

        if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr objectCreationExpr = expression.asObjectCreationExpr();
            return resolveTypeName(currentClass, objectCreationExpr.getType().asString());
        }

        return null;
    }

    private String resolveTypeName(ClassInfo currentClass, String typeName) {
        if (typeName == null) {
            return null;
        }

        String cleaned = typeName.replaceAll("<.*>", "").replaceAll("\\[\\]", "");
        if (cleaned.contains(".")) {
            return cleaned;
        }

        for (String importedType : currentClass.getImports()) {
            if (importedType.endsWith("." + cleaned)) {
                return importedType;
            }
        }

        if (currentClass.getPackageName() != null && !currentClass.getPackageName().isBlank()) {
            return currentClass.getPackageName() + "." + cleaned;
        }

        return cleaned;
    }

    private String resolveConcreteInitializerType(ClassInfo currentClass, VariableDeclarator variableDeclarator) {
        if (variableDeclarator.getInitializer().isEmpty()) {
            return null;
        }

        Expression initializer = variableDeclarator.getInitializer().get();
        if (initializer.isObjectCreationExpr()) {
            return resolveTypeName(currentClass, initializer.asObjectCreationExpr().getType().asString());
        }

        return null;
    }

    private boolean isRestTemplateExchange(MethodCallExpr methodCallExpr, ClassInfo currentClass,
            MethodDeclaration currentMethod) {
        if (!"exchange".equals(methodCallExpr.getNameAsString())) {
            return false;
        }

        Expression scope = methodCallExpr.getScope().orElse(null);
        if (scope == null) {
            return false;
        }

        String scopeType = resolveExpressionType(currentClass, currentMethod, scope);
        return scopeType != null && scopeType.endsWith("RestTemplate");
    }

    private Optional<String> resolveHttpMethod(MethodDeclaration methodDeclaration) {
        for (AnnotationExpr annotationExpr : methodDeclaration.getAnnotations()) {
            switch (annotationExpr.getNameAsString()) {
                case "GetMapping":
                    return Optional.of("GET");
                case "PostMapping":
                    return Optional.of("POST");
                case "DeleteMapping":
                    return Optional.of("DELETE");
                case "PutMapping":
                    return Optional.of("PUT");
                case "PatchMapping":
                    return Optional.of("PATCH");
                case "RequestMapping":
                    return Optional.of(resolveHttpMethodFromRequestMapping(annotationExpr));
                default:
                    break;
            }
        }

        return Optional.empty();
    }

    private String resolveHttpMethodFromRequestMapping(AnnotationExpr annotationExpr) {
        String annotationText = annotationExpr.toString();
        if (annotationText.contains("RequestMethod.POST")) {
            return "POST";
        }
        if (annotationText.contains("RequestMethod.PUT")) {
            return "PUT";
        }
        if (annotationText.contains("RequestMethod.DELETE")) {
            return "DELETE";
        }
        if (annotationText.contains("RequestMethod.PATCH")) {
            return "PATCH";
        }
        return "GET";
    }

    private String methodPath(MethodDeclaration methodDeclaration) {
        for (AnnotationExpr annotationExpr : methodDeclaration.getAnnotations()) {
            String annotationPath = pathFromAnnotation(annotationExpr);
            if (!annotationPath.isEmpty()) {
                return annotationPath;
            }
        }
        return "";
    }

    private String pathFromAnnotation(AnnotationExpr annotationExpr) {
        if (annotationExpr == null) {
            return "";
        }

        if (annotationExpr.isSingleMemberAnnotationExpr()) {
            return SourceText.removeEnclosedQuotations(
                    annotationExpr.asSingleMemberAnnotationExpr().getMemberValue().toString());
        }

        if (annotationExpr.isNormalAnnotationExpr() && !annotationExpr.asNormalAnnotationExpr().getPairs().isEmpty()) {
            for (MemberValuePair memberValuePair : annotationExpr.asNormalAnnotationExpr().getPairs()) {
                if ("path".equals(memberValuePair.getNameAsString())
                        || "value".equals(memberValuePair.getNameAsString())) {
                    return SourceText.removeEnclosedQuotations(memberValuePair.getValue().toString());
                }
            }
        }

        return "";
    }

    @Getter
    static final class SourceIndex {
        private final Map<String, ClassInfo> classesByFqName = new LinkedHashMap<>();
        private final Map<String, List<ClassInfo>> classesBySimpleName = new LinkedHashMap<>();
        private final List<ClassInfo> classes = new ArrayList<>();

        static SourceIndex fromRoots(List<Path> roots) throws IOException {
            SourceIndex sourceIndex = new SourceIndex();

            for (Path root : roots) {
                if (root == null || !Files.exists(root)) {
                    continue;
                }

                List<Path> javaFiles;
                try (var stream = Files.walk(root)) {
                    javaFiles = stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                            .filter(p -> !p.toString().endsWith("Test.java"))
                            .toList();
                }

                for (Path javaFile : javaFiles) {
                    CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
                    String packageName = SourceText.findPackage(compilationUnit);
                    for (ClassOrInterfaceDeclaration declaration : compilationUnit
                            .findAll(ClassOrInterfaceDeclaration.class)) {
                        sourceIndex.add(new ClassInfo(root, packageName, javaFile, compilationUnit, declaration));
                    }
                }
            }

            return sourceIndex;
        }

        private void add(ClassInfo classInfo) {
            classes.add(classInfo);
            classesByFqName.put(classInfo.getFqName(), classInfo);
            classesBySimpleName.computeIfAbsent(classInfo.getSimpleName(), ignored -> new ArrayList<>()).add(classInfo);
        }

        ClassInfo resolveClass(String typeName, String currentPackageName) {
            if (typeName == null || typeName.isBlank()) {
                return null;
            }

            String cleaned = typeName.replaceAll("<.*>", "").replaceAll("\\[\\]", "");
            ClassInfo directMatch = classesByFqName.get(cleaned);
            if (directMatch != null) {
                return directMatch;
            }

            List<ClassInfo> simpleMatches = classesBySimpleName.getOrDefault(cleaned, Collections.emptyList());
            if (simpleMatches.size() == 1) {
                return simpleMatches.get(0);
            }

            for (ClassInfo classInfo : simpleMatches) {
                if (Objects.equals(classInfo.getPackageName(), currentPackageName)) {
                    return classInfo;
                }
            }

            return null;
        }

        Optional<ClassInfo> resolveConcreteImplementation(ClassInfo targetClass) {
            if (targetClass == null) {
                return Optional.empty();
            }

            if (!targetClass.isInterfaceOrAbstract()) {
                return Optional.of(targetClass);
            }

            List<ClassInfo> implementations = findImplementations(targetClass);
            if (implementations.size() == 1) {
                return Optional.of(implementations.get(0));
            }

            return Optional.empty();
        }

        List<ClassInfo> findImplementations(ClassInfo interfaceOrAbstractClass) {
            List<ClassInfo> implementations = new ArrayList<>();
            String targetType = interfaceOrAbstractClass.getFqName();
            String targetSimpleName = interfaceOrAbstractClass.getSimpleName();

            for (ClassInfo candidate : classes) {
                if (candidate == interfaceOrAbstractClass) {
                    continue;
                }

                if (candidate.getParentTypes().contains(targetType)
                        || candidate.getParentTypes().contains(targetSimpleName)) {
                    implementations.add(candidate);
                }
            }

            return implementations;
        }
    }

    @Getter
    static final class ClassInfo {
        private final Path sourceRoot;
        private final String packageName;
        private final Path sourceFile;
        private final CompilationUnit compilationUnit;
        private final ClassOrInterfaceDeclaration declaration;
        private final Map<String, String> fieldTypes = new LinkedHashMap<>();
        private final Map<String, String> fieldConcreteTypes = new LinkedHashMap<>();
        private final List<String> imports;
        private final List<String> parentTypes;
        private final String simpleName;
        private final String fqName;

        private ClassInfo(Path sourceRoot, String packageName, Path sourceFile, CompilationUnit compilationUnit,
                ClassOrInterfaceDeclaration declaration) {
            this.sourceRoot = sourceRoot;
            this.packageName = packageName;
            this.sourceFile = sourceFile;
            this.compilationUnit = compilationUnit;
            this.declaration = declaration;
            this.simpleName = declaration.getNameAsString();
            this.fqName = packageName == null || packageName.isBlank() ? simpleName : packageName + "." + simpleName;
            this.imports = compilationUnit.getImports().stream()
                    .map(importDeclaration -> importDeclaration.getNameAsString())
                    .collect(Collectors.toList());
            this.parentTypes = new ArrayList<>();

            for (FieldDeclaration fieldDeclaration : declaration.getFields()) {
                for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                    String fieldName = variableDeclarator.getNameAsString();
                    fieldTypes.put(fieldName, resolveTypeName(variableDeclarator.getType().asString()));

                    if (variableDeclarator.getInitializer().isPresent()
                            && variableDeclarator.getInitializer().get().isObjectCreationExpr()) {
                        fieldConcreteTypes.put(fieldName,
                                resolveTypeName(variableDeclarator.getInitializer().get().asObjectCreationExpr()
                                        .getType().asString()));
                    }
                }
            }

            declaration.getExtendedTypes()
                    .forEach(extendedType -> parentTypes.add(resolveTypeName(extendedType.asString())));
            declaration.getImplementedTypes()
                    .forEach(implementedType -> parentTypes.add(resolveTypeName(implementedType.asString())));
        }

        String resolveTypeName(String typeName) {
            if (typeName == null) {
                return null;
            }

            String cleaned = typeName.replaceAll("<.*>", "").replaceAll("\\[\\]", "");
            if (cleaned.contains(".")) {
                return cleaned;
            }

            for (String importedType : imports) {
                if (importedType.endsWith("." + cleaned)) {
                    return importedType;
                }
            }

            if (packageName != null && !packageName.isBlank()) {
                return packageName + "." + cleaned;
            }

            return cleaned;
        }

        boolean isRestController() {
            return declaration.getAnnotationByName("RestController").isPresent();
        }

        boolean isInterfaceOrAbstract() {
            return declaration.isInterface() || declaration.isAbstract();
        }

        private String getSourceServiceName() {
            return sourceRoot.getFileName() == null ? "unknown" : sourceRoot.getFileName().toString();
        }
    }

    private record MethodKey(String className, String methodName, List<String> parameterTypes) {
        static MethodKey from(ClassInfo classInfo, MethodDeclaration methodDeclaration) {
            List<String> parameterTypes = methodDeclaration.getParameters().stream()
                    .map(parameter -> classInfo.resolveTypeName(parameter.getType().asString()))
                    .collect(Collectors.toList());
            return new MethodKey(classInfo.getFqName(), methodDeclaration.getNameAsString(), parameterTypes);
        }

        String asString() {
            return className + "#" + methodName + parameterTypes;
        }
    }

    record MethodResolution(ClassInfo classInfo, MethodKey methodKey, MethodDeclaration methodDeclaration) {
    }

    private static final class HttpEntityUsage {
        private final boolean requestBodySent;
        private final boolean headersSent;

        private HttpEntityUsage(boolean requestBodySent, boolean headersSent) {
            this.requestBodySent = requestBodySent;
            this.headersSent = headersSent;
        }

        private static HttpEntityUsage none() {
            return new HttpEntityUsage(false, false);
        }

        private static HttpEntityUsage bodyOnly() {
            return new HttpEntityUsage(true, false);
        }

        private static HttpEntityUsage headersOnly() {
            return new HttpEntityUsage(false, true);
        }

        private static HttpEntityUsage bodyAndHeaders() {
            return new HttpEntityUsage(true, true);
        }
    }
}