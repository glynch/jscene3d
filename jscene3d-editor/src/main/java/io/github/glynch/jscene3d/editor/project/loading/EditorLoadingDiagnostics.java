/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import io.github.glynch.jscene3d.editor.diagnostics.EditorDiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Creates consistently structured diagnostics owned by editor project loading. */
final class EditorLoadingDiagnostics {
    private EditorLoadingDiagnostics() {}

    /** Creates one editor loading error. */
    static ProjectDiagnostic error(Path source, EditorDiagnosticCode code, String detail) {
        return diagnostic(ProjectDiagnostic.Severity.ERROR, source, code, detail);
    }

    /** Creates one non-terminal editor loading warning. */
    static ProjectDiagnostic warning(Path source, EditorDiagnosticCode code, String detail) {
        return diagnostic(ProjectDiagnostic.Severity.WARNING, source, code, detail);
    }

    /** Creates one structured editor-owned diagnostic. */
    private static ProjectDiagnostic diagnostic(
            ProjectDiagnostic.Severity severity, Path source, EditorDiagnosticCode code, String detail) {
        return new ProjectDiagnostic(
                severity,
                code,
                source.toAbsolutePath().normalize().toUri(),
                "",
                Map.of("technicalDetail", Objects.requireNonNullElse(detail, code.defaultMessage())));
    }
}
