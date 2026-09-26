/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Exact wire result for reading one complete Inspector snapshot.
 *
 * @param read whether a current snapshot was produced
 * @param projectGeneration active generation for a successful read
 * @param snapshot complete Inspector snapshot for a successful read
 * @param diagnostics ordered structured diagnostics
 * @param failureCode stable operation-level rejection code
 */
public record InspectorReadResult(
        boolean read,
        @Nullable Long projectGeneration,
        @Nullable InspectorSnapshot snapshot,
        List<ProjectDiagnosticDto> diagnostics,
        @Nullable String failureCode) {
    /** Copies diagnostics and enforces the exact success/rejection shapes. */
    public InspectorReadResult {
        diagnostics = List.copyOf(diagnostics);
        if (read != (projectGeneration != null && snapshot != null && failureCode == null)) {
            throw new IllegalArgumentException("Inspector read result fields do not match read state");
        }
        if (!read && (projectGeneration != null || snapshot != null || failureCode == null)) {
            throw new IllegalArgumentException("Inspector rejection requires only a failureCode");
        }
        if (projectGeneration != null && projectGeneration < 1) {
            throw new IllegalArgumentException("projectGeneration must be positive");
        }
        Objects.requireNonNull(diagnostics, "diagnostics");
    }
}
