/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.nio.file.Path;
import java.util.Objects;
import javafx.application.Platform;
import javafx.concurrent.Task;

/** Loads one project away from the JavaFX Application Thread while forwarding visible progress. */
public final class EditorProjectLoadTask extends Task<EditorProjectLoadResult> {
    private final EditorProjectLoader loader;
    private final EditorProjectOpenTrace trace;
    private final Path projectDirectory;
    private final EditorProjectLoadProgress progress;

    /**
     * Creates one background task for a normalized project directory.
     *
     * @param loader project-session loader
     * @param trace project-opening trace
     * @param projectDirectory normalized project directory
     * @param progress receiver for user-facing load progress
     */
    public EditorProjectLoadTask(
            EditorProjectLoader loader,
            EditorProjectOpenTrace trace,
            Path projectDirectory,
            EditorProjectLoadProgress progress) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.trace = Objects.requireNonNull(trace, "trace");
        this.projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    /** Performs descriptor loading on the editor's dedicated project-loading thread. */
    @Override
    protected EditorProjectLoadResult call() {
        return trace.load(operation -> loader.load(projectDirectory, operation, new FxProgress()));
    }

    /** Marshals background loading notifications onto the JavaFX Application Thread. */
    private final class FxProgress implements EditorProjectLoadProgress {
        @Override
        public void projectIdentified(String projectName) {
            Platform.runLater(() -> progress.projectIdentified(projectName));
        }

        @Override
        public void phaseStarted(EditorLoadingPhase phase) {
            Platform.runLater(() -> progress.phaseStarted(phase));
        }
    }
}
