package io.github.glynch.jscene3d.render;

import javafx.application.Application;

/** Classpath launcher which delegates to the JavaFX application. */
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
