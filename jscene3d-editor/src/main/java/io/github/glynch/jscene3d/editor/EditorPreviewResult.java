/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;

/** Result delivered after preview composition and, when successful, its first presentation. */
record EditorPreviewResult(List<ProjectDiagnostic> diagnostics, EditorProjectOpenDurations durations) {
    /** Preserves immutable diagnostics in the cross-thread completion value. */
    EditorPreviewResult {
        diagnostics = List.copyOf(diagnostics);
    }
}
