/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of loading and validating one project input map. */
public final class InputMapLoadResult {
    private final Optional<InputMapDefinition> definition;
    private final List<ProjectDiagnostic> diagnostics;

    /** Copies one internally validated loading result. */
    InputMapLoadResult(Optional<InputMapDefinition> definition, List<ProjectDiagnostic> diagnostics) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (definition.isPresent() == hasErrors) {
            throw new IllegalArgumentException(
                    "an input-map definition must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns the validated definition when loading succeeded.
     *
     * @return present definition for a valid result
     */
    public Optional<InputMapDefinition> definition() {
        return definition;
    }

    /**
     * Returns ordered diagnostics produced while loading.
     *
     * @return immutable errors and warnings
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether a validated definition is available.
     *
     * @return {@code true} when loading succeeded
     */
    public boolean isValid() {
        return definition.isPresent();
    }
}
