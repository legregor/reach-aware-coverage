package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.legregor.microservice.coverage.analysis.model.BodyKind;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Static analysis of the request body sent by a {@code RestTemplate.exchange(...)} call: which top-level
 * properties of the sent object can get a non-null value (spec: "request body reachability").
 * <p>
 * Over-approximation: a property is only reported as not reachable when the code clearly never gives it a
 * non-null value. Whenever the origin of the object is unknown (method return value, REST response, class field,
 * object handed to other code), all properties count as reachable.
 * <p>
 * Java/Jackson/Lombok specifics (property names, setter names, constructors) are kept in {@link DtoModel}.
 */
final class RequestBodyReachabilityAnalyzer {

    /** How many times a method parameter is followed back to the callers of its method. */
    static final int MAX_PARAMETER_DEPTH = 3;

    private static final int MAX_ENTITY_DEPTH = 3;
    private static final int MAX_ORIGIN_TEXT = 60;
    /** How many steps (local variables, constants, conditionals) are followed to find a constant value. */
    private static final int MAX_CONSTANT_DEPTH = 5;

    /** Resolves the (fully qualified) type of an expression; may return null. */
    interface TypeResolver {
        String resolve(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method, Expression expression);
    }

    /** Resolves the method in the service source that a call invokes. */
    interface CallResolver {
        Optional<StaticServiceTraceAnalyzer.MethodResolution> resolve(StaticServiceTraceAnalyzer.ClassInfo classInfo,
                MethodDeclaration method, MethodCallExpr call);
    }

    /**
     * Result for one outgoing call. Property names are JSON names.
     *
     * @param constantValues reachable properties whose every assignment is a constant: the constants as JSON text
     *                       (e.g. {@code "FIRST"} with quotes, {@code 0}, {@code true}); a reachable property that
     *                       is missing here can get any value
     * @param neverNull      properties that can never be null (primitive fields: Jackson always writes them)
     */
    record BodyReachability(BodyKind kind, String type, Set<String> properties, Set<String> reachable,
                            Set<String> origins, Map<String, List<String>> constantValues, Set<String> neverNull) {
    }

    private record CallSite(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
                            MethodCallExpr call) {
    }

    private final StaticServiceTraceAnalyzer.SourceIndex sourceIndex;
    private final TypeResolver typeResolver;
    private final CallResolver callResolver;
    private Map<MethodDeclaration, List<CallSite>> callSitesByTarget;
    private final Map<StaticServiceTraceAnalyzer.ClassInfo, DtoModel> dtoModels = new IdentityHashMap<>();

    RequestBodyReachabilityAnalyzer(StaticServiceTraceAnalyzer.SourceIndex sourceIndex, TypeResolver typeResolver,
            CallResolver callResolver) {
        this.sourceIndex = sourceIndex;
        this.typeResolver = typeResolver;
        this.callResolver = callResolver;
    }

    // ---------------------------------------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------------------------------------

    BodyReachability analyze(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            MethodCallExpr exchangeCall) {
        if (exchangeCall.getArguments().size() < 3) {
            return none();
        }

        List<Expression> bodies = new ArrayList<>();
        collectBodyExpressions(classInfo, method, exchangeCall.getArgument(2), 0, bodies);
        if (bodies.isEmpty()) {
            return none();
        }

        String bodyType = null;
        for (Expression body : bodies) {
            bodyType = safeResolveType(classInfo, method, body);
            if (bodyType != null) {
                break;
            }
        }

        StaticServiceTraceAnalyzer.ClassInfo dtoClass = bodyType == null
                ? null
                : resolveSourceClass(sourceIndex, bodyType, classInfo);
        if (dtoClass == null || dtoClass.isInterfaceOrAbstract()) {
            Set<String> origins = new LinkedHashSet<>();
            origins.add("value of type " + (bodyType == null ? "unknown" : bodyType) + " in " + where(classInfo, method));
            return new BodyReachability(BodyKind.VALUE, bodyType, new LinkedHashSet<>(), new LinkedHashSet<>(), origins,
                    new LinkedHashMap<>(), new LinkedHashSet<>());
        }

        DtoModel dto = dtoModel(dtoClass);
        Reach reach = new Reach();
        Set<String> visited = new LinkedHashSet<>();
        for (Expression body : bodies) {
            reach.addAll(analyzeValue(classInfo, method, body, dto, 0, null, visited));
        }

        Set<String> properties = dto.jsonNames();
        Set<String> reachable = reach.all ? new LinkedHashSet<>(properties) : dto.jsonNamesOf(reach.fields);
        Map<String, List<String>> constants = new LinkedHashMap<>();
        if (!reach.all) {
            for (String field : reach.fields) {
                Set<String> values = reach.constants.get(field);
                if (values != null && !values.isEmpty() && !reach.unknownValues.contains(field)) {
                    constants.put(dto.jsonNameOf(field), new ArrayList<>(values));
                }
            }
        }
        return new BodyReachability(BodyKind.OBJECT, dtoClass.getFqName(), properties, reachable, reach.origins,
                constants, dto.primitiveJsonNames());
    }

    private static BodyReachability none() {
        return new BodyReachability(BodyKind.NONE, null, new LinkedHashSet<>(), new LinkedHashSet<>(),
                new LinkedHashSet<>(), new LinkedHashMap<>(), new LinkedHashSet<>());
    }

    // ---------------------------------------------------------------------------------------------
    // Step 1: find the body expression(s) inside the HttpEntity argument
    // ---------------------------------------------------------------------------------------------

    private void collectBodyExpressions(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            Expression entity, int depth, List<Expression> bodies) {
        Expression expression = unwrap(entity);
        if (expression == null || expression.isNullLiteralExpr() || depth > MAX_ENTITY_DEPTH) {
            return;
        }

        if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr creation = expression.asObjectCreationExpr();
            if (!creation.getType().getNameAsString().endsWith("HttpEntity")) {
                return;
            }
            NodeList<Expression> arguments = creation.getArguments();
            if (arguments.isEmpty()) {
                return;
            }
            Expression first = unwrap(arguments.get(0));
            if (first == null || first.isNullLiteralExpr()) {
                return;
            }
            if (arguments.size() == 1) {
                String type = safeResolveType(classInfo, method, first);
                if (type != null && (type.endsWith("HttpHeaders") || type.endsWith("MultiValueMap"))) {
                    return;
                }
            }
            bodies.add(first);
            return;
        }

        if (expression.isNameExpr()) {
            String name = expression.asNameExpr().getNameAsString();
            // a reused variable: take the value assigned last before this use
            Optional<Expression> reaching = ReachingDefinitions.lastDefinitionBefore(method, name, expression);
            if (reaching.isPresent()) {
                collectBodyExpressions(classInfo, method, reaching.get(), depth + 1, bodies);
                return;
            }
            for (Expression definition : localDefinitions(method, name)) {
                collectBodyExpressions(classInfo, method, definition, depth + 1, bodies);
            }
        }
        // anything else (HttpEntity.EMPTY, parameters, method results): no body found
    }

    // ---------------------------------------------------------------------------------------------
    // Step 3: where does the sent object come from?
    // ---------------------------------------------------------------------------------------------

    private Reach analyzeValue(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            Expression value, DtoModel dto, int depth, MethodCallExpr excludedCall, Set<String> visited) {
        Expression expression = unwrap(value);
        String where = where(classInfo, method);
        if (expression == null) {
            return new Reach();
        }
        if (expression.isNullLiteralExpr()) {
            Reach reach = new Reach();
            reach.origins.add("null in " + where);
            return reach;
        }
        if (expression.isConditionalExpr()) {
            Reach reach = analyzeValue(classInfo, method, expression.asConditionalExpr().getThenExpr(), dto, depth,
                    excludedCall, visited);
            reach.addAll(analyzeValue(classInfo, method, expression.asConditionalExpr().getElseExpr(), dto, depth,
                    excludedCall, visited));
            return reach;
        }
        if (expression.isObjectCreationExpr()) {
            return analyzeCreation(classInfo, method, expression.asObjectCreationExpr(), dto);
        }
        if (expression.isMethodCallExpr()) {
            Optional<Reach> builder = analyzeBuilder(classInfo, method, expression.asMethodCallExpr(), dto);
            return builder.orElseGet(() -> Reach.all("return value of " + shorten(expression) + " in " + where));
        }
        if (expression.isNameExpr()) {
            String name = expression.asNameExpr().getNameAsString();
            int parameterIndex = parameterIndex(method, name);
            if (parameterIndex >= 0) {
                return analyzeParameter(classInfo, method, name, parameterIndex, dto, depth, excludedCall, visited);
            }
            List<Expression> definitions = localDefinitions(method, name);
            if (!definitions.isEmpty()) {
                return analyzeLocal(classInfo, method, name, definitions, dto, depth, excludedCall, visited);
            }
            return Reach.all("field " + name + " in " + where);
        }
        return Reach.all("expression " + shorten(expression) + " in " + where);
    }

    private Reach analyzeLocal(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method, String name,
            List<Expression> definitions, DtoModel dto, int depth, MethodCallExpr excludedCall, Set<String> visited) {
        String key = "local:" + System.identityHashCode(method) + ":" + name;
        if (!visited.add(key)) {
            return new Reach();
        }
        Reach reach = new Reach();
        for (Expression definition : definitions) {
            reach.addAll(analyzeValue(classInfo, method, definition, dto, depth, excludedCall, visited));
        }
        if (reach.all) {
            return reach;
        }
        if (escapes(classInfo, method, name, excludedCall)) {
            return Reach.all(name + " is handed to other code in " + where(classInfo, method));
        }
        reach.addAll(mutations(classInfo, method, name, dto));
        return reach;
    }

    private Reach analyzeParameter(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            String name, int index, DtoModel dto, int depth, MethodCallExpr excludedCall, Set<String> visited) {
        String where = where(classInfo, method);
        String key = "param:" + System.identityHashCode(method) + ":" + index;
        if (!visited.add(key)) {
            Reach reach = new Reach();
            reach.origins.add("recursive call of " + where);
            return reach;
        }
        if (escapes(classInfo, method, name, excludedCall)) {
            return Reach.all(name + " is handed to other code in " + where);
        }

        Reach reach = mutations(classInfo, method, name, dto);
        if (depth >= MAX_PARAMETER_DEPTH) {
            reach.addAll(Reach.all("parameter " + name + " of " + where + " (depth limit reached)"));
            return reach;
        }

        List<CallSite> callSites = callSitesOf(method);
        if (callSites.isEmpty()) {
            reach.addAll(Reach.all("parameter " + name + " of " + where + " (no call site found)"));
            return reach;
        }
        for (CallSite callSite : callSites) {
            if (index >= callSite.call().getArguments().size()) {
                continue;
            }
            reach.addAll(analyzeValue(callSite.classInfo(), callSite.method(), callSite.call().getArgument(index), dto,
                    depth + 1, callSite.call(), visited));
        }
        return reach;
    }

    private Reach analyzeCreation(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            ObjectCreationExpr creation, DtoModel dto) {
        String where = where(classInfo, method);
        String typeName = creation.getType().getNameAsString();
        StaticServiceTraceAnalyzer.ClassInfo created = resolveSourceClass(sourceIndex,
                classInfo.resolveTypeName(creation.getType().asString()), classInfo);
        if (created != dto.classInfo()) {
            return Reach.all("new " + typeName + " (other type than " + dto.classInfo().getSimpleName() + ") in " + where);
        }

        Reach reach = new Reach();
        addDefaults(reach, dto);
        NodeList<Expression> arguments = creation.getArguments();
        if (arguments.isEmpty()) {
            dto.fieldsSetByNoArgConstructor().forEach((field, value) ->
                    reach.set(field, constantsOf(dto.classInfo(), null, value, dto, field)));
            reach.origins.add("new " + typeName + "() in " + where);
            return reach;
        }
        Optional<Map<String, Expression>> byConstructor = dto.fieldsSetByConstructor(arguments);
        if (byConstructor.isEmpty()) {
            return Reach.all("new " + typeName + "(...) with unknown constructor in " + where);
        }
        byConstructor.get().forEach((field, value) ->
                reach.set(field, constantsOf(classInfo, method, value, dto, field)));
        if (dto.isAllArgsConstructor(arguments)) {
            // every field gets its argument: the defaults are overwritten
            Reach withoutDefaults = new Reach();
            withoutDefaults.origins.addAll(reach.origins);
            byConstructor.get().forEach((field, value) ->
                    withoutDefaults.set(field, constantsOf(classInfo, method, value, dto, field)));
            addDefaults(withoutDefaults, dto, byConstructor.get().keySet());
            reach.fields.clear();
            reach.constants.clear();
            reach.unknownValues.clear();
            reach.addAll(withoutDefaults);
        }
        reach.origins.add("new " + typeName + "(" + arguments.size() + " arguments) in " + where);
        return reach;
    }

    /** {@code T.builder().a(x).b(y).build()} in one expression. */
    private Optional<Reach> analyzeBuilder(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            MethodCallExpr call, DtoModel dto) {
        if (!"build".equals(call.getNameAsString()) || !call.getArguments().isEmpty()) {
            return Optional.empty();
        }
        List<MethodCallExpr> steps = new ArrayList<>();
        Expression current = call.getScope().orElse(null);
        while (current != null && current.isMethodCallExpr()) {
            MethodCallExpr step = current.asMethodCallExpr();
            if ("builder".equals(step.getNameAsString()) && step.getArguments().isEmpty()) {
                Reach reach = new Reach();
                Set<String> stepFields = new LinkedHashSet<>();
                steps.forEach(builderStep -> stepFields.add(builderStep.getNameAsString()));
                addDefaults(reach, dto, stepFields);
                for (MethodCallExpr setterStep : steps) {
                    if (setterStep.getArguments().size() == 1
                            && !isNull(setterStep.getArgument(0))
                            && dto.hasField(setterStep.getNameAsString())) {
                        reach.set(setterStep.getNameAsString(), constantsOf(classInfo, method,
                                setterStep.getArgument(0), dto, setterStep.getNameAsString()));
                    }
                }
                reach.origins.add(dto.classInfo().getSimpleName() + ".builder() in " + where(classInfo, method));
                return Optional.of(reach);
            }
            steps.add(step);
            current = step.getScope().orElse(null);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------------------------------------
    // Step 4: setter calls / field assignments on a variable, and "handed to other code"
    // ---------------------------------------------------------------------------------------------

    private Reach mutations(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method, String name,
            DtoModel dto) {
        Reach reach = new Reach();
        int count = 0;
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            Optional<Expression> scope = call.getScope();
            if (scope.isEmpty() || !isName(scope.get(), name)) {
                continue;
            }
            Map<String, Expression> fields = dto.fieldsSetByCall(call.getNameAsString(), call.getArguments());
            if (!fields.isEmpty()) {
                fields.forEach((field, value) -> reach.set(field, constantsOf(classInfo, method, value, dto, field)));
                count++;
            }
        }
        for (AssignExpr assign : method.findAll(AssignExpr.class)) {
            if (!assign.getTarget().isFieldAccessExpr()) {
                continue;
            }
            FieldAccessExpr target = assign.getTarget().asFieldAccessExpr();
            if (isName(target.getScope(), name) && dto.hasField(target.getNameAsString()) && !isNull(assign.getValue())) {
                Set<String> constants = assign.getOperator() == AssignExpr.Operator.ASSIGN
                        ? constantsOf(classInfo, method, assign.getValue(), dto, target.getNameAsString()) : null;
                reach.set(target.getNameAsString(), constants);
                count++;
            }
        }
        if (count > 0) {
            reach.origins.add(count + " setter call(s)/assignment(s) on " + name + " in " + where(classInfo, method));
        }
        return reach;
    }

    /**
     * True if the variable is passed to a method or constructor of the service's own source (other than the call
     * that leads to the sending call), or assigned to another variable. Calls on the object itself (getters,
     * toString), library calls (logging, collections) and string concatenation do not count.
     */
    private boolean escapes(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method, String name,
            MethodCallExpr excludedCall) {
        for (NameExpr usage : method.findAll(NameExpr.class)) {
            if (!usage.getNameAsString().equals(name)) {
                continue;
            }
            Optional<Node> parentNode = usage.getParentNode();
            if (parentNode.isEmpty()) {
                continue;
            }
            Node parent = parentNode.get();
            if (parent instanceof MethodCallExpr call) {
                if (call == excludedCall || !containsIdentical(call.getArguments(), usage)) {
                    continue;
                }
                if (safeResolveCall(classInfo, method, call).isPresent()) {
                    return true;
                }
            } else if (parent instanceof ObjectCreationExpr creation) {
                if (containsIdentical(creation.getArguments(), usage)
                        && resolveSourceClass(sourceIndex, classInfo.resolveTypeName(creation.getType().asString()),
                        classInfo) != null) {
                    return true;
                }
            } else if (parent instanceof AssignExpr assign) {
                if (assign.getValue() == usage) {
                    return true;
                }
            } else if (parent instanceof VariableDeclarator declarator) {
                if (declarator.getInitializer().isPresent() && declarator.getInitializer().get() == usage) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // Call sites (who calls a method) - built once per analysis run
    // ---------------------------------------------------------------------------------------------

    private List<CallSite> callSitesOf(MethodDeclaration target) {
        if (callSitesByTarget == null) {
            callSitesByTarget = new IdentityHashMap<>();
            for (StaticServiceTraceAnalyzer.ClassInfo classInfo : sourceIndex.getClasses()) {
                for (MethodDeclaration method : classInfo.getDeclaration().getMethods()) {
                    for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                        safeResolveCall(classInfo, method, call).ifPresent(resolution ->
                                callSitesByTarget.computeIfAbsent(resolution.methodDeclaration(), ignored -> new ArrayList<>())
                                        .add(new CallSite(classInfo, method, call)));
                    }
                }
            }
        }
        return callSitesByTarget.getOrDefault(target, List.of());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private DtoModel dtoModel(StaticServiceTraceAnalyzer.ClassInfo classInfo) {
        return dtoModels.computeIfAbsent(classInfo, info -> DtoModel.of(info, sourceIndex));
    }

    private String safeResolveType(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
            Expression expression) {
        try {
            return typeResolver.resolve(classInfo, method, expression);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Optional<StaticServiceTraceAnalyzer.MethodResolution> safeResolveCall(
            StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method, MethodCallExpr call) {
        try {
            return callResolver.resolve(classInfo, method, call);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Resolves a type name to a class of the service source. Falls back to the simple name, because type names
     * resolved via wildcard imports (e.g. {@code import cancel.entity.*;}) end up in the wrong package.
     */
    static StaticServiceTraceAnalyzer.ClassInfo resolveSourceClass(StaticServiceTraceAnalyzer.SourceIndex index,
            String typeName, StaticServiceTraceAnalyzer.ClassInfo context) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        String cleaned = typeName.replaceAll("<.*>", "").replaceAll("\\[\\]", "").trim();
        StaticServiceTraceAnalyzer.ClassInfo resolved = index.resolveClass(cleaned, context.getPackageName());
        if (resolved == null && cleaned.contains(".")) {
            resolved = index.resolveClass(cleaned.substring(cleaned.lastIndexOf('.') + 1), context.getPackageName());
        }
        return resolved;
    }

    /** Initializer and all assignments of a local variable of the method (scopes are not distinguished). */
    private static List<Expression> localDefinitions(MethodDeclaration method, String name) {
        List<Expression> definitions = new ArrayList<>();
        boolean declared = false;
        for (VariableDeclarator declarator : method.findAll(VariableDeclarator.class)) {
            if (declarator.getNameAsString().equals(name)) {
                declared = true;
                declarator.getInitializer().ifPresent(definitions::add);
            }
        }
        if (!declared) {
            return definitions;
        }
        for (AssignExpr assign : method.findAll(AssignExpr.class)) {
            if (assign.getOperator() == AssignExpr.Operator.ASSIGN && isName(assign.getTarget(), name)) {
                definitions.add(assign.getValue());
            }
        }
        return definitions;
    }

    private static int parameterIndex(MethodDeclaration method, String name) {
        NodeList<Parameter> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i).getNameAsString().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static Expression unwrap(Expression expression) {
        Expression current = expression;
        while (current != null) {
            if (current.isEnclosedExpr()) {
                current = current.asEnclosedExpr().getInner();
            } else if (current.isCastExpr()) {
                current = current.asCastExpr().getExpression();
            } else {
                return current;
            }
        }
        return null;
    }

    private static boolean isName(Expression expression, String name) {
        Expression unwrapped = unwrap(expression);
        return unwrapped != null && unwrapped.isNameExpr() && unwrapped.asNameExpr().getNameAsString().equals(name);
    }

    static boolean isNull(Expression expression) {
        Expression unwrapped = unwrap(expression);
        return unwrapped != null && unwrapped.isNullLiteralExpr();
    }

    private static boolean containsIdentical(NodeList<Expression> expressions, Node node) {
        for (Expression expression : expressions) {
            if (expression == node) {
                return true;
            }
        }
        return false;
    }

    private static String where(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method) {
        return classInfo.getSimpleName() + "." + method.getNameAsString();
    }

    private static String shorten(Expression expression) {
        String text = expression.toString().replaceAll("\\s+", " ");
        return text.length() <= MAX_ORIGIN_TEXT ? text : text.substring(0, MAX_ORIGIN_TEXT) + "...";
    }

    /**
     * Java field names that can get a non-null value; all = every property (with any value). Per field, the constant
     * values it is assigned (JSON text); a field with at least one non-constant assignment is in unknownValues.
     */
    private static final class Reach {
        private boolean all;
        private final Set<String> fields = new LinkedHashSet<>();
        private final Set<String> origins = new LinkedHashSet<>();
        private final Map<String, Set<String>> constants = new LinkedHashMap<>();
        private final Set<String> unknownValues = new LinkedHashSet<>();

        static Reach all(String origin) {
            Reach reach = new Reach();
            reach.all = true;
            reach.origins.add("all properties: " + origin);
            return reach;
        }

        /** The field gets a non-null value: one of the given constants, or any value if constants is null. */
        void set(String field, Set<String> values) {
            fields.add(field);
            if (values == null || values.isEmpty()) {
                unknownValues.add(field);
            } else {
                constants.computeIfAbsent(field, ignored -> new LinkedHashSet<>()).addAll(values);
            }
        }

        void addAll(Reach other) {
            all |= other.all;
            fields.addAll(other.fields);
            origins.addAll(other.origins);
            other.constants.forEach((field, values) ->
                    constants.computeIfAbsent(field, ignored -> new LinkedHashSet<>()).addAll(values));
            unknownValues.addAll(other.unknownValues);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Constant values (for value partition reachability)
    // ---------------------------------------------------------------------------------------------

    /** Primitive fields and fields with an initializer: always present, with their initial value. */
    private void addDefaults(Reach reach, DtoModel dto) {
        addDefaults(reach, dto, Set.of());
    }

    /** Same, except for fields that are certainly overwritten (by a builder step or constructor argument). */
    private void addDefaults(Reach reach, DtoModel dto, Set<String> overwritten) {
        for (String field : dto.alwaysReachableFields()) {
            if (overwritten.contains(field)) {
                continue;
            }
            Expression initializer = dto.initializerOf(field);
            reach.set(field, initializer != null
                    ? constantsOf(dto.classInfo(), null, initializer, dto, field)
                    : dto.primitiveDefault(field));
        }
    }

    /**
     * The constant values an expression can have, as JSON text, or null if it can have other values. Recognised:
     * literals (also negative numbers), enum constants of the field's type, static final constants with a constant
     * initializer, conditional expressions of constants, and local variables whose every assignment is a constant.
     */
    private Set<String> constantsOf(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
                                    Expression value, DtoModel dto, String field) {
        return constantsOf(classInfo, method, value, dto, field, 0);
    }

    private Set<String> constantsOf(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
                                    Expression value, DtoModel dto, String field, int depth) {
        Expression expression = unwrap(value);
        if (expression == null || depth > MAX_CONSTANT_DEPTH) {
            return null;
        }
        String literal = ConstantText.of(expression, dto.isTextField(field));
        if (literal != null) {
            return Set.of(literal);
        }
        if (expression.isConditionalExpr()) {
            Set<String> thenValues = constantsOf(classInfo, method, expression.asConditionalExpr().getThenExpr(), dto,
                    field, depth + 1);
            Set<String> elseValues = constantsOf(classInfo, method, expression.asConditionalExpr().getElseExpr(), dto,
                    field, depth + 1);
            if (thenValues == null || elseValues == null) {
                return null;
            }
            Set<String> values = new LinkedHashSet<>(thenValues);
            values.addAll(elseValues);
            return values;
        }
        if (expression.isNameExpr()) {
            String name = expression.asNameExpr().getNameAsString();
            if (method != null && parameterIndex(method, name) < 0) {
                List<Expression> definitions = localDefinitions(method, name);
                if (!definitions.isEmpty()) {
                    return isReassigned(method, name) ? null
                            : constantsOfAll(classInfo, method, definitions, dto, field, depth + 1);
                }
            }
            Expression constant = staticFinalInitializer(classInfo, name);
            return constant == null ? null : constantsOf(classInfo, null, constant, dto, field, depth + 1);
        }
        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr access = expression.asFieldAccessExpr();
            String scope = access.getScope().toString();
            String scopeSimpleName = scope.substring(scope.lastIndexOf('.') + 1);
            if (scopeSimpleName.equals(dto.simpleTypeOf(field))) {
                // an enum constant of the field's own type: Jackson writes its name
                return Set.of(ConstantText.text(access.getNameAsString()));
            }
            StaticServiceTraceAnalyzer.ClassInfo owner = resolveSourceClass(sourceIndex,
                    classInfo.resolveTypeName(scope), classInfo);
            Expression constant = owner == null ? null : staticFinalInitializer(owner, access.getNameAsString());
            return constant == null ? null : constantsOf(owner, null, constant, dto, field, depth + 1);
        }
        return null;
    }

    private Set<String> constantsOfAll(StaticServiceTraceAnalyzer.ClassInfo classInfo, MethodDeclaration method,
                                       List<Expression> definitions, DtoModel dto, String field, int depth) {
        Set<String> values = new LinkedHashSet<>();
        for (Expression definition : definitions) {
            Set<String> definitionValues = constantsOf(classInfo, method, definition, dto, field, depth);
            if (definitionValues == null) {
                return null;
            }
            values.addAll(definitionValues);
        }
        return values;
    }

    /** Compound assignments ({@code +=}) or increments change a variable in a way the definitions do not show. */
    private static boolean isReassigned(MethodDeclaration method, String name) {
        for (AssignExpr assign : method.findAll(AssignExpr.class)) {
            if (assign.getOperator() != AssignExpr.Operator.ASSIGN && isName(assign.getTarget(), name)) {
                return true;
            }
        }
        for (UnaryExpr unary : method.findAll(UnaryExpr.class)) {
            if (isName(unary.getExpression(), name) && unary.getOperator().isPostfix()
                    || isName(unary.getExpression(), name) && (unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT)) {
                return true;
            }
        }
        return false;
    }

    /** Initializer of a static final field of the class, null if there is none. */
    private static Expression staticFinalInitializer(StaticServiceTraceAnalyzer.ClassInfo classInfo, String name) {
        Optional<FieldDeclaration> field = classInfo.getDeclaration().getFieldByName(name);
        if (field.isEmpty() || !field.get().isStatic() || !field.get().isFinal()) {
            return null;
        }
        for (VariableDeclarator variable : field.get().getVariables()) {
            if (variable.getNameAsString().equals(name)) {
                return variable.getInitializer().orElse(null);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // DTO model: serializable properties and how they are set (Java / Jackson / Lombok specifics)
    // ---------------------------------------------------------------------------------------------

    /**
     * Properties of a DTO class and its superclasses in the source, as Jackson serializes them.
     * Keyed by Java field name.
     */
    static final class DtoModel {
        private record Property(String javaName, String jsonName, boolean alwaysReachable, String setterName,
                                StaticServiceTraceAnalyzer.ClassInfo declaringClass, String typeName,
                                boolean primitive, Expression initializer) {
        }

        private final StaticServiceTraceAnalyzer.ClassInfo classInfo;
        private final List<StaticServiceTraceAnalyzer.ClassInfo> hierarchy;
        private final Map<String, Property> properties = new LinkedHashMap<>();

        private DtoModel(StaticServiceTraceAnalyzer.ClassInfo classInfo,
                List<StaticServiceTraceAnalyzer.ClassInfo> hierarchy) {
            this.classInfo = classInfo;
            this.hierarchy = hierarchy;
        }

        static DtoModel of(StaticServiceTraceAnalyzer.ClassInfo classInfo, StaticServiceTraceAnalyzer.SourceIndex index) {
            List<StaticServiceTraceAnalyzer.ClassInfo> hierarchy = new ArrayList<>();
            StaticServiceTraceAnalyzer.ClassInfo current = classInfo;
            while (current != null && !hierarchy.contains(current)) {
                hierarchy.add(current);
                StaticServiceTraceAnalyzer.ClassInfo parent = null;
                for (ClassOrInterfaceType extended : current.getDeclaration().getExtendedTypes()) {
                    parent = resolveSourceClass(index, current.resolveTypeName(extended.asString()), current);
                    if (parent != null) {
                        break;
                    }
                }
                current = parent;
            }

            DtoModel model = new DtoModel(classInfo, hierarchy);
            for (StaticServiceTraceAnalyzer.ClassInfo owner : hierarchy) {
                for (FieldDeclaration field : owner.getDeclaration().getFields()) {
                    if (field.isStatic() || field.isTransient() || field.isAnnotationPresent("JsonIgnore")) {
                        continue;
                    }
                    String explicitJsonName = jsonPropertyName(field);
                    for (VariableDeclarator variable : field.getVariables()) {
                        String javaName = variable.getNameAsString();
                        if (model.properties.containsKey(javaName)) {
                            continue; // shadowed by a subclass field
                        }
                        boolean booleanIsPrefixed = "boolean".equals(variable.getType().asString())
                                && javaName.matches("is[A-Z].*");
                        String baseName = booleanIsPrefixed ? javaName.substring(2) : javaName;
                        String jsonName = explicitJsonName != null
                                ? explicitJsonName
                                : (booleanIsPrefixed ? decapitalize(baseName) : javaName);
                        boolean initialized = variable.getInitializer().isPresent()
                                && !variable.getInitializer().get().isNullLiteralExpr();
                        boolean alwaysReachable = variable.getType().isPrimitiveType() || initialized;
                        model.properties.put(javaName, new Property(javaName, jsonName, alwaysReachable,
                                "set" + capitalize(baseName), owner, variable.getType().asString(),
                                variable.getType().isPrimitiveType(),
                                initialized ? variable.getInitializer().get() : null));
                    }
                }
            }
            return model;
        }

        StaticServiceTraceAnalyzer.ClassInfo classInfo() {
            return classInfo;
        }

        boolean hasField(String javaName) {
            return properties.containsKey(javaName);
        }

        Set<String> jsonNames() {
            Set<String> names = new LinkedHashSet<>();
            properties.values().forEach(property -> names.add(property.jsonName()));
            return names;
        }

        String jsonNameOf(String javaName) {
            Property property = properties.get(javaName);
            return property == null ? javaName : property.jsonName();
        }

        /** JSON names of the primitive fields: never null in the JSON. */
        Set<String> primitiveJsonNames() {
            Set<String> names = new LinkedHashSet<>();
            properties.values().stream().filter(Property::primitive).forEach(property -> names.add(property.jsonName()));
            return names;
        }

        /** Non-null initializer of the field, null if it has none. */
        Expression initializerOf(String javaName) {
            Property property = properties.get(javaName);
            return property == null ? null : property.initializer();
        }

        /** Default value of a primitive field without initializer (JSON text), null for other fields. */
        Set<String> primitiveDefault(String javaName) {
            Property property = properties.get(javaName);
            if (property == null || !property.primitive()) {
                return null;
            }
            return switch (property.typeName()) {
                case "int", "long", "short", "byte" -> Set.of("0");
                case "float", "double" -> Set.of("0.0");
                case "boolean" -> Set.of("false");
                default -> null;
            };
        }

        /** Simple name of the field's declared type (without generics), e.g. {@code SeatClass}. */
        String simpleTypeOf(String javaName) {
            Property property = properties.get(javaName);
            if (property == null) {
                return null;
            }
            String type = property.typeName().replaceAll("<.*>", "");
            return type.substring(type.lastIndexOf('.') + 1);
        }

        /** String or char field: char literals are written as text. */
        boolean isTextField(String javaName) {
            String type = simpleTypeOf(javaName);
            return "String".equals(type) || "char".equals(type) || "Character".equals(type);
        }

        Set<String> jsonNamesOf(Set<String> javaNames) {
            Set<String> names = new LinkedHashSet<>();
            for (Property property : properties.values()) {
                if (javaNames.contains(property.javaName())) {
                    names.add(property.jsonName());
                }
            }
            return names;
        }

        /** Primitive fields (never null in JSON) and fields with a non-null initializer. */
        Set<String> alwaysReachableFields() {
            Set<String> fields = new LinkedHashSet<>();
            properties.values().stream().filter(Property::alwaysReachable).forEach(p -> fields.add(p.javaName()));
            return fields;
        }

        /** Fields assigned in the no-argument constructors of the class and its superclasses, with their values. */
        Map<String, Expression> fieldsSetByNoArgConstructor() {
            Map<String, Expression> fields = new LinkedHashMap<>();
            for (StaticServiceTraceAnalyzer.ClassInfo owner : hierarchy) {
                for (ConstructorDeclaration constructor : owner.getDeclaration().getConstructors()) {
                    if (constructor.getParameters().isEmpty()) {
                        fieldsAssignedIn(constructor, new NodeList<>()).forEach(fields::putIfAbsent);
                    }
                }
            }
            return fields;
        }

        /**
         * Fields set by a constructor call with arguments: a declared constructor (fields its body assigns) or a
         * Lombok {@code @AllArgsConstructor} (fields in declaration order). Empty if the constructor is unknown.
         */
        /** The constructor call uses a Lombok {@code @AllArgsConstructor} (no declared constructor of that arity). */
        boolean isAllArgsConstructor(NodeList<Expression> arguments) {
            ClassOrInterfaceDeclaration declaration = classInfo.getDeclaration();
            return declaration.isAnnotationPresent("AllArgsConstructor") && declaration.getConstructors().stream()
                    .noneMatch(constructor -> constructor.getParameters().size() == arguments.size());
        }

        Optional<Map<String, Expression>> fieldsSetByConstructor(NodeList<Expression> arguments) {
            ClassOrInterfaceDeclaration declaration = classInfo.getDeclaration();
            List<ConstructorDeclaration> candidates = new ArrayList<>();
            for (ConstructorDeclaration constructor : declaration.getConstructors()) {
                if (constructor.getParameters().size() == arguments.size()) {
                    candidates.add(constructor);
                }
            }

            if (candidates.size() == 1) {
                ConstructorDeclaration constructor = candidates.get(0);
                Map<String, Expression> fields = new LinkedHashMap<>(fieldsAssignedIn(constructor, arguments));
                for (ExplicitConstructorInvocationStmt invocation
                        : constructor.findAll(ExplicitConstructorInvocationStmt.class)) {
                    if (invocation.isThis()) {
                        return Optional.empty(); // delegation to another constructor: unknown
                    }
                    if (!invocation.getArguments().isEmpty()) {
                        // super(...) with arguments: superclass fields may be set
                        properties.values().stream()
                                .filter(property -> property.declaringClass() != classInfo)
                                .forEach(property -> fields.putIfAbsent(property.javaName(), null));
                    }
                }
                return Optional.of(fields);
            }

            if (candidates.isEmpty() && declaration.isAnnotationPresent("AllArgsConstructor")) {
                List<String> ownFields = new ArrayList<>();
                for (FieldDeclaration field : declaration.getFields()) {
                    if (!field.isStatic()) {
                        field.getVariables().forEach(variable -> ownFields.add(variable.getNameAsString()));
                    }
                }
                if (ownFields.size() != arguments.size()) {
                    return Optional.empty();
                }
                Map<String, Expression> fields = new LinkedHashMap<>();
                for (int i = 0; i < ownFields.size(); i++) {
                    if (!isNull(arguments.get(i)) && hasField(ownFields.get(i))) {
                        fields.put(ownFields.get(i), arguments.get(i));
                    }
                }
                return Optional.of(fields);
            }
            return Optional.empty();
        }

        /**
         * Fields set by calling {@code obj.<methodName>(arguments)}: a method declared in the source (fields its
         * body assigns), otherwise a bean/Lombok setter matched by name. Getters and unknown methods set nothing.
         */
        Map<String, Expression> fieldsSetByCall(String methodName, NodeList<Expression> arguments) {
            for (StaticServiceTraceAnalyzer.ClassInfo owner : hierarchy) {
                for (MethodDeclaration method : owner.getDeclaration().getMethodsByName(methodName)) {
                    if (!method.isStatic() && method.getParameters().size() == arguments.size()) {
                        return fieldsAssignedIn(method, arguments);
                    }
                }
            }
            String field = fieldOfSetter(methodName, arguments.size());
            if (field != null && !isNull(arguments.get(0))) {
                Map<String, Expression> fields = new LinkedHashMap<>();
                fields.put(field, arguments.get(0));
                return fields;
            }
            return new LinkedHashMap<>();
        }

        private String fieldOfSetter(String methodName, int argumentCount) {
            if (argumentCount != 1 || !methodName.startsWith("set")) {
                return null;
            }
            for (Property property : properties.values()) {
                if (property.setterName().equals(methodName)) {
                    return property.javaName();
                }
            }
            return null;
        }

        /**
         * Fields assigned with a non-null value in a method or constructor body: {@code this.f = x}, {@code f = x}
         * (f not a parameter or local variable) and setter calls on {@code this}. A value that is a parameter counts
         * as null when the call passes a null literal for it.
         *
         * @return field -> the assigned value: the call's argument for a parameter, a literal itself, null otherwise
         * (any value). A field assigned twice keeps null (any value) unless both values are known.
         */
        private Map<String, Expression> fieldsAssignedIn(CallableDeclaration<?> callable, NodeList<Expression> arguments) {
            List<String> parameterNames = new ArrayList<>();
            for (Parameter parameter : callable.getParameters()) {
                parameterNames.add(parameter.getNameAsString());
            }
            Set<String> localNames = new LinkedHashSet<>();
            for (VariableDeclarator local : callable.findAll(VariableDeclarator.class)) {
                localNames.add(local.getNameAsString());
            }

            Map<String, Expression> fields = new LinkedHashMap<>();
            for (AssignExpr assign : callable.findAll(AssignExpr.class)) {
                String field = null;
                Expression target = assign.getTarget();
                if (target.isFieldAccessExpr() && target.asFieldAccessExpr().getScope().isThisExpr()) {
                    field = target.asFieldAccessExpr().getNameAsString();
                } else if (target.isNameExpr()) {
                    String name = target.asNameExpr().getNameAsString();
                    if (!parameterNames.contains(name) && !localNames.contains(name)) {
                        field = name;
                    }
                }
                if (field != null && hasField(field)
                        && !isNullValue(assign.getValue(), parameterNames, arguments)) {
                    putValue(fields, field, assign.getOperator() == AssignExpr.Operator.ASSIGN
                            ? valueOf(assign.getValue(), parameterNames, arguments) : null);
                }
            }
            for (MethodCallExpr call : callable.findAll(MethodCallExpr.class)) {
                if (call.getScope().isPresent() && !call.getScope().get().isThisExpr()) {
                    continue;
                }
                String field = fieldOfSetter(call.getNameAsString(), call.getArguments().size());
                if (field != null && !isNullValue(call.getArgument(0), parameterNames, arguments)) {
                    putValue(fields, field, valueOf(call.getArgument(0), parameterNames, arguments));
                }
            }
            return fields;
        }

        /** A second assignment of the same field: keeps a value only if it is the same kind of constant text. */
        private static void putValue(Map<String, Expression> fields, String field, Expression value) {
            if (!fields.containsKey(field)) {
                fields.put(field, value);
            } else if (fields.get(field) == null || value == null
                    || !fields.get(field).toString().equals(value.toString())) {
                fields.put(field, null);
            }
        }

        /** The value an assignment in a callee gives: the call's argument for a parameter, a literal, else null. */
        private static Expression valueOf(Expression value, List<String> parameterNames, NodeList<Expression> arguments) {
            Expression unwrapped = unwrap(value);
            if (unwrapped == null) {
                return null;
            }
            if (unwrapped.isNameExpr()) {
                int index = parameterNames.indexOf(unwrapped.asNameExpr().getNameAsString());
                return index >= 0 && index < arguments.size() ? arguments.get(index) : null;
            }
            return unwrapped.isLiteralExpr() || unwrapped.isUnaryExpr() ? unwrapped : null;
        }

        private static boolean isNullValue(Expression value, List<String> parameterNames,
                NodeList<Expression> arguments) {
            if (isNull(value)) {
                return true;
            }
            Expression unwrapped = unwrap(value);
            if (unwrapped != null && unwrapped.isNameExpr()) {
                int index = parameterNames.indexOf(unwrapped.asNameExpr().getNameAsString());
                return index >= 0 && index < arguments.size() && isNull(arguments.get(index));
            }
            return false;
        }

        /** Name from {@code @JsonProperty("x")} or {@code @JsonProperty(value = "x")}, otherwise null. */
        private static String jsonPropertyName(FieldDeclaration field) {
            Optional<AnnotationExpr> annotation = field.getAnnotationByName("JsonProperty");
            if (annotation.isEmpty()) {
                return null;
            }
            AnnotationExpr expr = annotation.get();
            Expression value = null;
            if (expr.isSingleMemberAnnotationExpr()) {
                value = expr.asSingleMemberAnnotationExpr().getMemberValue();
            } else if (expr.isNormalAnnotationExpr()) {
                for (MemberValuePair pair : expr.asNormalAnnotationExpr().getPairs()) {
                    if ("value".equals(pair.getNameAsString())) {
                        value = pair.getValue();
                    }
                }
            }
            if (value != null && value.isStringLiteralExpr()) {
                String name = value.asStringLiteralExpr().asString();
                return name.isEmpty() ? null : name;
            }
            return null;
        }

        private static String capitalize(String name) {
            return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        private static String decapitalize(String name) {
            return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
    }
}
