package com.github.legregor.microservice.coverage.analysis.trace;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import org.apache.commons.io.FilenameUtils;

/**
 * Small text helpers for the static analysis. Taken over one-to-one from rad-source's {@code Helper} (0.0.5) when
 * the rad-source dependency was removed, so the analysis results stay identical.
 */
final class SourceText {

    private SourceText() {
    }

    /** Package name of a compilation unit, null if it has none. */
    static String findPackage(CompilationUnit compilationUnit) {
        for (PackageDeclaration packageDeclaration : compilationUnit.findAll(PackageDeclaration.class)) {
            return packageDeclaration.getNameAsString();
        }
        return null;
    }

    /** Joins a class-level and a method-level request path, e.g. {@code /orders} + {@code /{id}} -> {@code /orders/{id}}. */
    static String mergePaths(String classPath, String methodPath) {
        if (classPath.startsWith("/")) {
            classPath = classPath.substring(1);
        }
        if (methodPath.startsWith("/")) {
            methodPath = methodPath.substring(1);
        }
        String path = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.concat(classPath, methodPath), true);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    /** Removes enclosing double quotes ({@code "x"} -> {@code x}); other strings are returned unchanged. */
    static String removeEnclosedQuotations(String s) {
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
