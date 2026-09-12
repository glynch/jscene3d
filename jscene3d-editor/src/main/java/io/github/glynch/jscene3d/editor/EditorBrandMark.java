/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.net.URL;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Displays the packaged raster derived from the approved JScene3D SVG brand mark. */
public final class EditorBrandMark extends ImageView {
    private static final String MARK_RESOURCE = "splash/jscene3d-mark.png";

    /** Creates an accessible, non-interactive mark at the requested square size. */
    public EditorBrandMark(double size) {
        super(loadMark());
        setFitWidth(size);
        setFitHeight(size);
        setPreserveRatio(true);
        setSmooth(true);
        setAccessibleText("JScene3D");
        setFocusTraversable(false);
    }

    /** Resolves the packaged mark without requiring the JavaFX graphics toolkit. */
    static URL resource() {
        URL resource = EditorBrandMark.class.getResource(MARK_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("JScene3D mark resource was not found: " + MARK_RESOURCE);
        }
        return resource;
    }

    private static Image loadMark() {
        return new Image(resource().toExternalForm(), false);
    }
}
