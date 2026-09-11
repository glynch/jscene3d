/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.util.Objects;

/** Receives project identity and phase changes while editor-safe project data is loaded. */
public interface EditorProjectLoadProgress {
    /** Progress receiver that intentionally ignores every notification. */
    EditorProjectLoadProgress NONE = new EditorProjectLoadProgress() {
        @Override
        public void projectIdentified(String projectName) {
            Objects.requireNonNull(projectName, "projectName");
        }

        @Override
        public void phaseStarted(EditorLoadingPhase phase) {
            Objects.requireNonNull(phase, "phase");
        }
    };

    /**
     * Reports the authored project name as soon as its manifest has been accepted.
     *
     * @param projectName authored project name
     */
    void projectIdentified(String projectName);

    /**
     * Reports that one material loading phase is about to begin.
     *
     * @param phase phase about to begin
     */
    void phaseStarted(EditorLoadingPhase phase);
}
