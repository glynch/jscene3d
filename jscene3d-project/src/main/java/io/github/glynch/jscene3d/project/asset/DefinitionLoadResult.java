/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of resolving and loading one authored definition.
 *
 * @param <T> loaded definition kind
 */
public final class DefinitionLoadResult<T> {
    private final Optional<T> definition;
    private final List<ProjectDiagnostic> diagnostics;

    /** Stores a definition exactly when no error diagnostics were produced. */
    DefinitionLoadResult(Optional<T> definition, List<ProjectDiagnostic> diagnostics) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (definition.isPresent() == hasErrors) {
            throw new IllegalArgumentException(
                    "a definition must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns the definition when loading and its transitive references succeeded.
     *
     * @return loaded definition
     */
    public Optional<T> definition() {
        return definition;
    }

    /**
     * Returns ordered loading diagnostics.
     *
     * @return immutable diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether a definition is available.
     *
     * @return {@code true} when loading succeeded
     */
    public boolean isValid() {
        return definition.isPresent();
    }
}
