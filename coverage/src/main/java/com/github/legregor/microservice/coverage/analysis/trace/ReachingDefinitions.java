package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;

import java.util.Optional;

/**
 * Finds the value a local variable holds at a given use: the last definition (initializer or assignment) that
 * appears before the use in the source text.
 * <p>
 * Needed for code that reuses one variable for several calls, e.g. TrainTicket's SeatServiceImpl:
 * {@code requestEntity = new HttpEntity(headers)} for a GET, then {@code requestEntity = new HttpEntity(seat, headers)}
 * for a POST. This is a textual approximation (branches are not distinguished), but it picks the right value for
 * straight-line code.
 */
final class ReachingDefinitions {

    private ReachingDefinitions() {
    }

    /** The last definition of the variable before the use, or empty if there is none before it. */
    static Optional<Expression> lastDefinitionBefore(MethodDeclaration method, String variableName, Node use) {
        Optional<Position> usePosition = use.getBegin();
        if (usePosition.isEmpty()) {
            return Optional.empty();
        }
        Expression best = null;
        Position bestPosition = null;

        for (VariableDeclarator declarator : method.findAll(VariableDeclarator.class)) {
            if (!declarator.getNameAsString().equals(variableName) || declarator.getInitializer().isEmpty()) {
                continue;
            }
            Optional<Position> position = declarator.getBegin();
            if (position.isPresent() && position.get().isBefore(usePosition.get())
                    && (bestPosition == null || bestPosition.isBefore(position.get()))) {
                best = declarator.getInitializer().get();
                bestPosition = position.get();
            }
        }
        for (AssignExpr assign : method.findAll(AssignExpr.class)) {
            if (assign.getOperator() != AssignExpr.Operator.ASSIGN || !assign.getTarget().isNameExpr()
                    || !assign.getTarget().asNameExpr().getNameAsString().equals(variableName)) {
                continue;
            }
            Optional<Position> position = assign.getBegin();
            if (position.isPresent() && position.get().isBefore(usePosition.get())
                    && (bestPosition == null || bestPosition.isBefore(position.get()))) {
                best = assign.getValue();
                bestPosition = position.get();
            }
        }
        return Optional.ofNullable(best);
    }
}
