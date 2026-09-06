/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of validating and transactionally composing one inactive world. */
public final class WorldCompositionResult {
    private final Optional<World> world;
    private final List<ProjectDiagnostic> diagnostics;

    /** Stores an internally validated composition outcome. */
    private WorldCompositionResult(Optional<World> world, List<ProjectDiagnostic> diagnostics) {
        this.world = Objects.requireNonNull(world, "world");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (world.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a world must be present exactly when diagnostics contain no errors");
        }
    }

    /** Creates one successful internal result. */
    static WorldCompositionResult success(World world, List<ProjectDiagnostic> diagnostics) {
        return new WorldCompositionResult(Optional.of(world), diagnostics);
    }

    /** Creates one failed internal result. */
    static WorldCompositionResult failure(List<ProjectDiagnostic> diagnostics) {
        return new WorldCompositionResult(Optional.empty(), diagnostics);
    }

    /**
     * Returns the complete inactive world when composition succeeded.
     *
     * @return composed world, or empty after a terminal diagnostic
     */
    public Optional<World> world() {
        return world;
    }

    /**
     * Returns ordered loading, validation, registration, and composition diagnostics.
     *
     * @return immutable diagnostics
     */
    public List<ProjectDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Returns whether an inactive world is available.
     *
     * @return {@code true} after successful composition
     */
    public boolean isComposed() {
        return world.isPresent();
    }
}
