/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.net.URL;
import java.util.Objects;
import javafx.scene.Scene;

/** Installs the packaged visual theme on an editor scene. */
public final class EditorTheme {
    private static final String RESOURCE_NAME = "editor.css";

    /** Prevents construction of the stateless theme component. */
    private EditorTheme() {
        throw new AssertionError("EditorTheme cannot be instantiated");
    }

    /** Installs the packaged stylesheet at most once on the supplied scene. */
    static void install(Scene scene) {
        Scene validScene = Objects.requireNonNull(scene, "scene");
        String stylesheet = stylesheet();
        if (!validScene.getStylesheets().contains(stylesheet)) {
            validScene.getStylesheets().add(stylesheet);
        }
    }

    /** Returns the external URL of the packaged editor stylesheet. */
    public static String stylesheet() {
        URL resource = Objects.requireNonNull(EditorTheme.class.getResource(RESOURCE_NAME), RESOURCE_NAME);
        return resource.toExternalForm();
    }
}
