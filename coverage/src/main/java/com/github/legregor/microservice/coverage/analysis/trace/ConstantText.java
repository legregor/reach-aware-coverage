package com.github.legregor.microservice.coverage.analysis.trace;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;

/**
 * Java literals as the JSON text Jackson writes for them ({@code "FIRST"} with quotes, {@code 5}, {@code -1.5},
 * {@code true}). Used for the constant values of request body properties (Java specific, replaceable).
 */
final class ConstantText {

    private ConstantText() {
    }

    /**
     * JSON text of a literal expression, null if the expression is not a literal.
     *
     * @param textField the target field is a String / char: char literals become text
     */
    static String of(Expression expression, boolean textField) {
        if (expression.isStringLiteralExpr()) {
            return text(expression.asStringLiteralExpr().asString());
        }
        if (expression.isCharLiteralExpr()) {
            return textField ? text(String.valueOf(expression.asCharLiteralExpr().asChar())) : null;
        }
        if (expression.isBooleanLiteralExpr()) {
            return String.valueOf(expression.asBooleanLiteralExpr().getValue());
        }
        if (expression.isIntegerLiteralExpr()) {
            return String.valueOf(expression.asIntegerLiteralExpr().asInt());
        }
        if (expression.isLongLiteralExpr()) {
            return String.valueOf(expression.asLongLiteralExpr().asLong());
        }
        if (expression.isDoubleLiteralExpr()) {
            return String.valueOf(expression.asDoubleLiteralExpr().asDouble());
        }
        if (expression.isUnaryExpr()) {
            UnaryExpr unary = expression.asUnaryExpr();
            String inner = of(unary.getExpression(), false);
            if (inner == null || inner.startsWith("\"") || inner.equals("true") || inner.equals("false")) {
                return null;
            }
            if (unary.getOperator() == UnaryExpr.Operator.MINUS) {
                return inner.startsWith("-") ? inner.substring(1) : "-" + inner;
            }
            return unary.getOperator() == UnaryExpr.Operator.PLUS ? inner : null;
        }
        return null;
    }

    /** A string as JSON text (quoted and escaped). */
    static String text(String value) {
        return TextNode.valueOf(value).toString();
    }
}
