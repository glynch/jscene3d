/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

/** One truthful, user-facing phase in editor startup or project opening. */
public enum EditorLoadingPhase {
    /** Starts the editor process and constructs its initial shell. */
    STARTING_EDITOR("Starting JScene3D Editor", 0.04),
    /** Creates the OpenGL-backed editor viewport. */
    PREPARING_VIEWPORT("Preparing the OpenGL viewport", 0.10),
    /** Reads and validates the project manifest. */
    READING_MANIFEST("Reading the project manifest", 0.16),
    /** Discovers the project's authored assets. */
    SCANNING_ASSETS("Scanning the asset catalog", 0.28),
    /** Loads extension metadata needed to understand project types. */
    LOADING_EXTENSIONS("Loading extension metadata", 0.40),
    /** Reads the project's import definitions. */
    READING_IMPORTS("Reading import definitions", 0.50),
    /** Loads content produced by the import pipeline. */
    LOADING_PUBLISHED_CONTENT("Loading published project content", 0.62),
    /** Validates the assets exposed to the editor. */
    VALIDATING_ASSETS("Validating project assets", 0.74),
    /** Loads the world configured as the project's startup world. */
    LOADING_STARTUP_WORLD("Loading the startup world", 0.84),
    /** Projects the startup world into the editor hierarchy. */
    BUILDING_HIERARCHY("Building the editor hierarchy", 0.90),
    /** Composes the scene preview and presents its first frame. */
    PREPARING_PREVIEW("Composing and presenting the first preview frame", 0.96),
    /** Marks project opening as complete. */
    READY("Ready", 1.00);

    private final String description;
    private final double progress;

    /** Creates one phase with its display description and cumulative progress. */
    EditorLoadingPhase(String description, double progress) {
        this.description = description;
        this.progress = progress;
    }

    /** Returns the concise activity description displayed by the splash screen. */
    String description() {
        return description;
    }

    /** Returns cumulative phase progress in the inclusive range {@code 0.0..1.0}. */
    double progress() {
        return progress;
    }
}
