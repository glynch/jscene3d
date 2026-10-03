/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import org.jspecify.annotations.Nullable;

/**
 * Project-generation-scoped Java service result for one Scene View projection request.
 *
 * <p>This is an authoring-service boundary, not a wire DTO. A future protocol method can place it inside the existing
 * connection-generation response envelope without weakening the project, Scene, or definition-revision checks.
 *
 * @param accepted whether the request addressed the active project generation
 * @param projectGeneration active generation for an accepted request
 * @param projection Scene- and revision-scoped projection outcome for an accepted request
 * @param failureCode stable project ownership rejection for an unaccepted request
 */
public record SceneViewServiceResult(
        boolean accepted,
        @Nullable Long projectGeneration,
        @Nullable SceneViewProjectionResult projection,
        @Nullable String failureCode) {
    /** Validates the exact accepted and rejected result shapes. */
    public SceneViewServiceResult {
        if (accepted != (projectGeneration != null && projection != null && failureCode == null)) {
            throw new IllegalArgumentException("Scene View service result fields do not match accepted state");
        }
        if (!accepted && (projectGeneration != null || projection != null || failureCode == null)) {
            throw new IllegalArgumentException("Scene View rejection requires only a failureCode");
        }
        if (projectGeneration != null && projectGeneration < 1) {
            throw new IllegalArgumentException("projectGeneration must be positive");
        }
        if (failureCode != null && failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }
    }
}
