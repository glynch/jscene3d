/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

/** One truthful, user-facing phase in editor startup or project opening. */
enum EditorLoadingPhase {
    STARTING_EDITOR("Starting JScene3D Editor", 0.04),
    PREPARING_VIEWPORT("Preparing the OpenGL viewport", 0.10),
    READING_MANIFEST("Reading the project manifest", 0.16),
    SCANNING_ASSETS("Scanning the asset catalog", 0.28),
    LOADING_EXTENSIONS("Loading extension metadata", 0.40),
    READING_IMPORTS("Reading import definitions", 0.50),
    LOADING_PUBLISHED_CONTENT("Loading published project content", 0.62),
    VALIDATING_ASSETS("Validating project assets", 0.74),
    LOADING_STARTUP_WORLD("Loading the startup world", 0.84),
    BUILDING_HIERARCHY("Building the editor hierarchy", 0.90),
    PREPARING_PREVIEW("Composing and presenting the first preview frame", 0.96),
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
