/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.icon;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/** Renders semantic editor icons using glyphs resolved by the active icon registry. */
public final class JavaFxIconRenderer {
    private final JavaFxIconRegistry icons;

    private JavaFxIconRenderer(JavaFxIconRegistry icons) {
        this.icons = Objects.requireNonNull(icons, "icons");
    }

    /**
     * Creates a renderer backed by the editor's built-in icon theme.
     *
     * @return built-in icon renderer
     */
    public static JavaFxIconRenderer builtIn() {
        return new JavaFxIconRenderer(JavaFxIconRegistry.builtIn());
    }

    /**
     * Creates one tooltip-accessible icon node without exposing JavaFX through the extension interface.
     *
     * @param presentation semantic icon presentation
     * @param styleClasses additional CSS classes applied to the icon container
     * @return tooltip-accessible JavaFX icon node
     */
    public Node create(EditorIcon presentation, String... styleClasses) {
        EditorIcon icon = Objects.requireNonNull(presentation, "presentation");
        JavaFxIconGlyph glyph = icons.resolve(icon.id());
        SVGPath path = new SVGPath();
        path.setContent(glyph.path());
        path.setFillRule(FillRule.EVEN_ODD);
        path.getStyleClass().add(EditorStyleClasses.EDITOR_ICON_SHAPE);

        StackPane container = new StackPane(path);
        container.setAccessibleRole(AccessibleRole.TEXT);
        container.setAccessibleText(icon.tooltip());
        container.setFocusTraversable(false);
        container.getStyleClass().addAll(EditorStyleClasses.EDITOR_ICON, toneStyleClass(glyph.tone()));
        container.getStyleClass().addAll(styleClasses);
        Tooltip.install(container, new Tooltip(icon.tooltip()));
        return container;
    }

    private static String toneStyleClass(JavaFxIconGlyph.Tone tone) {
        return switch (tone) {
            case PRIMARY -> EditorStyleClasses.EDITOR_ICON_PRIMARY;
            case READ_ONLY -> EditorStyleClasses.EDITOR_ICON_READ_ONLY;
            case DISABLED -> EditorStyleClasses.EDITOR_ICON_DISABLED;
            case ERROR -> EditorStyleClasses.EDITOR_ICON_ERROR;
            case WARNING -> EditorStyleClasses.EDITOR_ICON_WARNING;
            case INFORMATION -> EditorStyleClasses.EDITOR_ICON_INFORMATION;
        };
    }
}
