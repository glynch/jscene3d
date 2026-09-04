/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of loading and validating one project resource. */
public final class ResourceLoadResult {
    private final Optional<ResourceDefinition> resource;
    private final List<ProjectDiagnostic> diagnostics;

    /** Copies one internally validated loading result. */
    ResourceLoadResult(Optional<ResourceDefinition> resource, List<ProjectDiagnostic> diagnostics) {
        this.resource = Objects.requireNonNull(resource, "resource");
        this.diagnostics = List.copyOf(diagnostics);
        boolean hasErrors = this.diagnostics.stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (resource.isPresent() == hasErrors) {
            throw new IllegalArgumentException("a resource must be present exactly when diagnostics contain no errors");
        }
    }

    /**
     * Returns the validated resource when loading succeeded.
     *
     * @return present resource for a valid result
     */
    public Optional<ResourceDefinition> resource() {
        return resource;
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
     * Returns whether a validated resource is available.
     *
     * @return {@code true} when loading succeeded
     */
    public boolean isValid() {
        return resource.isPresent();
    }
}
