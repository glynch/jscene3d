/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of composing one editor-only spatial world preview. */
record EditorWorldPreviewLoadResult(Optional<EditorWorldPreview> preview, List<ProjectDiagnostic> diagnostics) {
    /** Copies one preview result and its ordered diagnostics. */
    EditorWorldPreviewLoadResult {
        Objects.requireNonNull(preview, "preview");
        diagnostics = List.copyOf(diagnostics);
    }
}
