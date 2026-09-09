/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Performs a headless editor load and spatial-preview composition for one project. */
final class EditorProjectCheck {
    private static final Logger LOGGER = Logger.getLogger(EditorProjectCheck.class.getName());
    private static final String ARGUMENT = "--check-project=";

    private EditorProjectCheck() {}

    /** Returns the project requested by the optional headless-check command-line argument. */
    static Optional<Path> requestedProject(String[] arguments) {
        return Arrays.stream(arguments)
                .filter(argument -> argument.startsWith(ARGUMENT))
                .findFirst()
                .map(argument -> argument.substring(ARGUMENT.length()))
                .map(EditorProjectCheck::projectPath);
    }

    /** Loads and composes one project through the same path used by the graphical editor. */
    static void check(Path projectDirectory) {
        EditorProjectLoader loader = new EditorProjectLoader(
                EditorBuildInfo.engineVersion(),
                EditorProjectCheck.class.getClassLoader(),
                EditorExtensionPath.configured());
        check(loader, projectDirectory);
    }

    /** Loads and composes one project with an injected loader for deterministic testing. */
    static void check(EditorProjectLoader loader, Path projectDirectory) {
        EditorProjectLoadResult load = loader.load(projectDirectory);
        requireNoErrors("project load", load.diagnostics());
        EditorProjectSession session = load.session()
                .orElseThrow(() -> new IllegalStateException("project load did not create an editor session"));
        EditorWorldPreviewLoadResult composition = EditorWorldPreview.compose(session);
        requireNoErrors("preview composition", composition.diagnostics());
        try (EditorWorldPreview preview = composition
                .preview()
                .orElseThrow(() -> new IllegalStateException("preview composition did not create a preview"))) {
            if (!preview.isReady()) {
                throw new IllegalStateException("preview composition is not ready to render");
            }
        }
        LOGGER.info(() -> "Checked " + session.project().identity().name() + ": "
                + session.hierarchy().children().size() + " root entities, "
                + session.assets().size() + " assets");
    }

    /** Converts one non-empty command-line value into an absolute normalized project path. */
    private static Path projectPath(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(ARGUMENT + " requires a project directory");
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    /** Rejects one phase when it produced any structured error diagnostics. */
    private static void requireNoErrors(String phase, List<ProjectDiagnostic> diagnostics) {
        List<ProjectDiagnostic> errors = diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR)
                .toList();
        if (!errors.isEmpty()) {
            throw new IllegalStateException(phase + " failed: " + errors);
        }
    }
}
