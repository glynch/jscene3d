/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.splash;

import io.github.glynch.jscene3d.editor.workbench.appearance.EditorBrandMark;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.net.URL;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Builds the static artwork and product identity used by the startup splash. */
final class EditorSplashBranding {
    private static final String ARTWORK_RESOURCE =
            "/io/github/glynch/jscene3d/editor/splash/viewport-emergence-background.png";

    private EditorSplashBranding() {}

    /** Creates the full-frame splash artwork. */
    static Region createArtwork() {
        Region artwork = new Region();
        artwork.setBackground(coveringBackground(ARTWORK_RESOURCE));
        artwork.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_ARTWORK);
        return artwork;
    }

    /** Creates the original product mark and live JScene3D Editor wordmark. */
    static HBox createBrand() {
        EditorBrandMark mark = new EditorBrandMark(72.0);
        mark.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_MARK);

        HBox wordmark = new HBox(
                12.0,
                styledLabel("JScene3D", EditorStyleClasses.EDITOR_SPLASH_BRAND),
                styledLabel("Editor", EditorStyleClasses.EDITOR_SPLASH_BRAND_ACCENT));
        wordmark.setAlignment(Pos.CENTER_LEFT);
        wordmark.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_WORDMARK);

        HBox brand = new HBox(22.0, mark, wordmark);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        brand.getStyleClass().add(EditorStyleClasses.EDITOR_SPLASH_IDENTITY);
        BorderPane.setMargin(brand, new Insets(46.0, 48.0, 0.0, 48.0));
        return brand;
    }

    /** Creates one label and assigns its splash-specific style class. */
    private static Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** Creates a background that fills the frame while retaining the artwork's aspect ratio. */
    private static Background coveringBackground(String resourceName) {
        BackgroundSize cover = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true);
        BackgroundImage image = new BackgroundImage(
                loadImage(resourceName),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                cover);
        return new Background(image);
    }

    /** Loads one required packaged image and fails startup clearly if packaging is incomplete. */
    private static Image loadImage(String resourceName) {
        URL resource = EditorSplashBranding.class.getResource(resourceName);
        if (resource == null) {
            throw new IllegalStateException("splash image resource was not found: " + resourceName);
        }
        return new Image(resource.toExternalForm(), false);
    }
}
