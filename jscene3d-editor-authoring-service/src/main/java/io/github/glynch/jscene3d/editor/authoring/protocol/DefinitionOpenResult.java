/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Outcome of retaining a structural definition and projecting its complete hierarchy.
 *
 * @param opened whether the definition was retained
 * @param projectGeneration matching active project generation on success
 * @param definition complete retained definition snapshot on success
 * @param diagnostics ordered definition-loading diagnostics
 * @param failureCode stable operation-level rejection code, when applicable
 */
public record DefinitionOpenResult(
        boolean opened,
        @Nullable Long projectGeneration,
        @Nullable DefinitionSnapshot definition,
        List<ProjectDiagnosticDto> diagnostics,
        @Nullable String failureCode) {
    /** Copies diagnostics and validates the mutually exclusive result shapes. */
    public DefinitionOpenResult {
        diagnostics = List.copyOf(diagnostics);
        if ((opened && (projectGeneration == null || definition == null || failureCode != null))
                || (!opened && (projectGeneration != null || definition != null))) {
            throw new IllegalArgumentException("Successful definition open requires a generation and snapshot");
        }
    }
}
