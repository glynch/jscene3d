/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/** Presents in-session project-opening progress as concise status-bar text. */
final class EditorStatusProjectOpenProgress implements EditorProjectOpenProgress {
    private final Consumer<String> status;
    private String projectName = "Project";
    private EditorLoadingPhase phase = EditorLoadingPhase.READING_MANIFEST;

    /** Creates status progress over the workbench's project-status sink. */
    EditorStatusProjectOpenProgress(Consumer<String> status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    @Override
    public void opening(Path projectDirectory) {
        Path normalized = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath()
                .normalize();
        Path fileName = normalized.getFileName();
        projectName = fileName == null ? normalized.toString() : fileName.toString();
        show();
    }

    @Override
    public void projectIdentified(String authoredProjectName) {
        projectName = Objects.requireNonNull(authoredProjectName, "authoredProjectName");
        show();
    }

    @Override
    public void phaseStarted(EditorLoadingPhase loadingPhase) {
        phase = Objects.requireNonNull(loadingPhase, "loadingPhase");
        show();
    }

    /** Leaves final success or failure status to the project-opening workflow. */
    @Override
    public void finish() {
        phase = EditorLoadingPhase.READY;
    }

    private void show() {
        status.accept("Opening " + projectName + " · " + phase.description() + " · "
                + Math.round(phase.progress() * 100.0) + "%");
    }
}
