/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Outcome of resolving and retaining one structural definition.
 *
 * @param definition retained definition when loading succeeded
 * @param diagnostics ordered definition-loading diagnostics
 */
public record DefinitionRetentionResult(
        Optional<EditorRetainedDefinition> definition, List<ProjectDiagnostic> diagnostics) {
    /** Copies and validates the retention result. */
    public DefinitionRetentionResult {
        Objects.requireNonNull(definition, "definition");
        diagnostics = List.copyOf(diagnostics);
    }
}
