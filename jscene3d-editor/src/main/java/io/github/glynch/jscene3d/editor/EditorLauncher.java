/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import javafx.application.Application;

/** Named-module entry point for the JScene3D editor application. */
public final class EditorLauncher {

    private EditorLauncher() {}

    /**
     * Launches the editor.
     *
     * @param arguments command-line arguments passed to JavaFX
     */
    public static void main(String[] arguments) {
        Application.launch(EditorApplication.class, arguments);
    }
}
