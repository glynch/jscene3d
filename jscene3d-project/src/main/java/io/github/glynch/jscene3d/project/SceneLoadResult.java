/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of loading and validating one scene. */
public final class SceneLoadResult {
    private final Optional<SceneDefinition> scene;
    private final List<ProjectDiagnostic> diagnostics;

    /** Copies one loader result. */
    SceneLoadResult(Optional<SceneDefinition> scene, List<ProjectDiagnostic> diagnostics) {
        this.scene = Objects.requireNonNull(scene, "scene");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (scene.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a scene must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns the validated scene when loading succeeded.
     *
     * @return present scene for a valid result
     */
    public Optional<SceneDefinition> scene() {
        return scene;
    }

    /**
     * Returns ordered errors and warnings produced while loading.
     *
     * @return immutable diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether a validated scene is available.
     *
     * @return {@code true} when loading succeeded
     */
    public boolean isValid() {
        return scene.isPresent();
    }
}
