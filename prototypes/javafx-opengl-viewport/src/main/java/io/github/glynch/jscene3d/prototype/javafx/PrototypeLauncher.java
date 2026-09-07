/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.prototype.javafx;

import javafx.application.Application;

/** Named-module launcher which delegates to the JavaFX application. */
public final class PrototypeLauncher {

    private PrototypeLauncher() {}

    /**
     * Launches the prototype.
     *
     * @param arguments command-line arguments
     */
    public static void main(String[] arguments) {
        Application.launch(JavaFxOpenGlViewportPrototype.class, arguments);
    }
}
