/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildDiagnostic;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Translates Maven compiler output into editor source diagnostics. */
final class MavenBuildDiagnostics {
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\x1B(?:[@-Z\\\\-_]|\\[[0-?]*[ -/]*[@-~])");
    private static final String ERROR_PREFIX = "[ERROR]";
    private static final String WARNING_PREFIX = "[WARNING]";

    private MavenBuildDiagnostics() {}

    static List<ProjectBuildDiagnostic> parse(Path projectRoot, String standardOutput, String standardError) {
        Path root = projectRoot.toAbsolutePath().normalize();
        List<ProjectBuildDiagnostic> diagnostics = new ArrayList<>();
        parse(root, standardOutput, diagnostics);
        parse(root, standardError, diagnostics);
        return List.copyOf(diagnostics);
    }

    private static void parse(Path projectRoot, String content, List<ProjectBuildDiagnostic> diagnostics) {
        ANSI_ESCAPE
                .matcher(content)
                .replaceAll("")
                .lines()
                .map(line -> parseLine(projectRoot, line))
                .flatMap(Optional::stream)
                .forEach(diagnostics::add);
    }

    private static Optional<ProjectBuildDiagnostic> parseLine(Path projectRoot, String line) {
        String compilerOutput = line.stripLeading();
        Optional<EditorDiagnosticSeverity> severity = severity(compilerOutput);
        if (severity.isEmpty()) {
            return Optional.empty();
        }
        String detail = compilerOutput.substring(prefixLength(compilerOutput)).stripLeading();
        int locationStart = detail.lastIndexOf(":[");
        int locationEnd = detail.indexOf(']', locationStart + 2);
        if (locationStart <= 0 || locationEnd < 0) {
            return Optional.empty();
        }
        String sourceText = detail.substring(0, locationStart);
        if (!sourceText.endsWith(".java")) {
            return Optional.empty();
        }
        Optional<Coordinates> coordinates = coordinates(detail.substring(locationStart + 2, locationEnd));
        if (coordinates.isEmpty()) {
            return Optional.empty();
        }
        Path source = Path.of(sourceText);
        if (!source.isAbsolute()) {
            source = projectRoot.resolve(source);
        }
        Coordinates location = coordinates.orElseThrow();
        EditorTextPosition start = new EditorTextPosition(location.line() - 1, location.column() - 1);
        EditorTextRange range = new EditorTextRange(start, new EditorTextPosition(start.line(), start.character() + 1));
        String message = detail.substring(locationEnd + 1).strip();
        return Optional.of(new ProjectBuildDiagnostic(
                source.normalize().toUri(),
                new EditorDiagnostic(
                        severity.orElseThrow(),
                        "maven.compiler",
                        "Maven",
                        message.isEmpty() ? "Maven compiler reported a source problem" : message,
                        location.line() + ":" + location.column(),
                        Optional.of(range),
                        Map.of())));
    }

    private static Optional<EditorDiagnosticSeverity> severity(String compilerOutput) {
        if (compilerOutput.startsWith(ERROR_PREFIX)) {
            return Optional.of(EditorDiagnosticSeverity.ERROR);
        }
        if (compilerOutput.startsWith(WARNING_PREFIX)) {
            return Optional.of(EditorDiagnosticSeverity.WARNING);
        }
        return Optional.empty();
    }

    private static int prefixLength(String compilerOutput) {
        return compilerOutput.startsWith(ERROR_PREFIX) ? ERROR_PREFIX.length() : WARNING_PREFIX.length();
    }

    private static Optional<Coordinates> coordinates(String text) {
        int separator = text.indexOf(',');
        if (separator <= 0 || separator == text.length() - 1) {
            return Optional.empty();
        }
        Optional<Integer> line = positiveInteger(text.substring(0, separator));
        Optional<Integer> column = positiveInteger(text.substring(separator + 1));
        if (line.isEmpty() || column.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Coordinates(line.orElseThrow(), column.orElseThrow()));
    }

    private static Optional<Integer> positiveInteger(String text) {
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(text);
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private record Coordinates(int line, int column) {}
}
