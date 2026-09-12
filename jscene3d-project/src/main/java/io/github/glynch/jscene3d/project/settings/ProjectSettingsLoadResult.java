/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable outcome of loading portable project settings.
 *
 * @param settings loaded settings when validation succeeded
 * @param diagnostics settings diagnostics in deterministic order
 */
public record ProjectSettingsLoadResult(Optional<ProjectSettings> settings, List<ProjectDiagnostic> diagnostics) {
    /** Copies result values and enforces success/error consistency. */
    public ProjectSettingsLoadResult {
        Objects.requireNonNull(settings, "settings");
        diagnostics = List.copyOf(diagnostics);
        boolean hasErrors =
                diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (settings.isPresent() == hasErrors) {
            throw new IllegalArgumentException("settings must be present exactly when diagnostics contain no errors");
        }
    }
}
