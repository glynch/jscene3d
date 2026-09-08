/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of assembling one read-only editor project session. */
record EditorProjectLoadResult(Optional<EditorProjectSession> session, List<ProjectDiagnostic> diagnostics) {
    /** Copies one editor loading outcome. */
    EditorProjectLoadResult {
        Objects.requireNonNull(session, "session");
        diagnostics = List.copyOf(diagnostics);
    }
}
