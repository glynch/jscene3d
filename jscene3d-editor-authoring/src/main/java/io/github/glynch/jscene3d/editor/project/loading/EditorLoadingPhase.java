/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;

/** One truthful, user-facing phase in editor startup or project opening. */
public enum EditorLoadingPhase {
    /** Starts the editor process and constructs its initial shell. */
    STARTING_EDITOR("starting-editor", "Starting JScene3D Editor", 0.04),
    /** Creates the OpenGL-backed editor viewport. */
    PREPARING_VIEWPORT("preparing-viewport", "Preparing the OpenGL viewport", 0.10),
    /** Reads and validates the project manifest. */
    READING_MANIFEST("reading-manifest", "Reading the project manifest", 0.16),
    /** Discovers the project's authored assets. */
    SCANNING_ASSETS("scanning-assets", "Scanning the asset catalog", 0.28),
    /** Loads extension metadata needed to understand project types. */
    LOADING_EXTENSIONS("loading-extensions", "Loading extension metadata", 0.40),
    /** Reads the project's import definitions. */
    READING_IMPORTS("reading-imports", "Reading import definitions", 0.50),
    /** Loads content produced by the import pipeline. */
    LOADING_PUBLISHED_CONTENT("loading-published-content", "Loading published project content", 0.62),
    /** Validates the assets exposed to the editor. */
    VALIDATING_ASSETS("validating-assets", "Validating project assets", 0.74),
    /** Composes the scene preview and presents its first frame. */
    PREPARING_PREVIEW("preparing-preview", "Composing and presenting the first preview frame", 0.96),
    /** Marks project opening as complete. */
    READY("ready", "Ready", 1.00);

    private final AuthoringText description;
    private final double progress;

    /** Creates one phase with its display description and cumulative progress. */
    EditorLoadingPhase(String codeSuffix, String defaultDescription, double progress) {
        description = AuthoringText.message("editor.loading." + codeSuffix, defaultDescription);
        this.progress = progress;
    }

    /**
     * Returns the concise activity description displayed by loading-progress presentations.
     *
     * @return user-facing phase description
     */
    public AuthoringText description() {
        return description;
    }

    /**
     * Returns cumulative phase progress in the inclusive range {@code 0.0..1.0}.
     *
     * @return cumulative phase progress
     */
    public double progress() {
        return progress;
    }
}
