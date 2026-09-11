/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

/** Renders semantic editor icons as original, CSS-coloured JavaFX vector paths. */
final class JavaFxIconRenderer {
    private static final String GENERIC_DOCUMENT = "M3 1.5 H10 L13 4.5 V14.5 H3 Z M10 1.5 V4.5 H13";

    private JavaFxIconRenderer() {}

    /** Creates one tooltip-accessible icon node without exposing JavaFX through the extension interface. */
    static Node create(EditorIcon presentation, String... styleClasses) {
        EditorIcon icon = Objects.requireNonNull(presentation, "presentation");
        SVGPath path = new SVGPath();
        path.setContent(path(icon.id()));
        path.getStyleClass().add(EditorStyleClasses.EDITOR_ICON_SHAPE);

        StackPane container = new StackPane(path);
        container.setAccessibleRole(AccessibleRole.TEXT);
        container.setAccessibleText(icon.tooltip());
        container.setFocusTraversable(false);
        container.getStyleClass().addAll(EditorStyleClasses.EDITOR_ICON, styleClass(icon.id()));
        container.getStyleClass().addAll(styleClasses);
        Tooltip.install(container, new Tooltip(icon.tooltip()));
        return container;
    }

    private static String path(EditorIconId id) {
        if (id.equals(EditorIcons.PROJECT)) {
            return "M1.5 4.5 H6 L7.5 6 H14.5 V13.5 H1.5 Z M1.5 4.5 V3 H6.5 L8 4.5";
        }
        if (id.equals(EditorIcons.WORLD)) {
            return "M8 1.5 A6.5 6.5 0 1 0 8 14.5 A6.5 6.5 0 1 0 8 1.5 M1.5 8 H14.5 "
                    + "M8 1.5 C5.5 4 5.5 12 8 14.5 M8 1.5 C10.5 4 10.5 12 8 14.5";
        }
        if (id.equals(EditorIcons.ENTITY)) {
            return "M8 1.5 L14 4.8 V11.2 L8 14.5 L2 11.2 V4.8 Z M2 4.8 L8 8.2 L14 4.8 M8 8.2 V14.5";
        }
        if (id.equals(EditorIcons.ENTITY_DEFINITION)) {
            return "M3 1.5 H10 L13 4.5 V14.5 H3 Z M10 1.5 V4.5 H13 M5 8 H11 M5 11 H10";
        }
        if (id.equals(EditorIcons.PLACEMENT)) {
            return "M2 2 H6 V6 H2 Z M10 10 H14 V14 H10 Z M6 5 L11 10 M9 4 H12 V7";
        }
        if (id.equals(EditorIcons.SOURCE_ASSET)) {
            return "M3 1.5 H10 L13 4.5 V14.5 H3 Z M10 1.5 V4.5 H13 M5 8 H11 M5 11 H11";
        }
        if (id.equals(EditorIcons.IMPORT)) {
            return "M8 1.5 V10 M4.5 6.5 L8 10 L11.5 6.5 M2 11.5 V14 H14 V11.5";
        }
        if (id.equals(EditorIcons.READ_ONLY)) {
            return "M4 7 H12 V14 H4 Z M5.5 7 V5 A2.5 2.5 0 0 1 10.5 5 V7 M8 10 V11.5";
        }
        if (id.equals(EditorIcons.DISABLED)) {
            return "M1.5 8 C3.2 5 5.4 3.5 8 3.5 C10.6 3.5 12.8 5 14.5 8 "
                    + "C13.5 9.8 12.2 11.1 10.6 11.8 M5.4 11.8 C3.7 11.1 2.4 9.8 1.5 8 M2 2 L14 14";
        }
        if (id.equals(EditorIcons.GRID)) {
            return "M2 2 H6.5 V6.5 H2 Z M9.5 2 H14 V6.5 H9.5 Z M2 9.5 H6.5 V14 H2 Z " + "M9.5 9.5 H14 V14 H9.5 Z";
        }
        if (id.equals(EditorIcons.LIST)) {
            return "M2 3 H4 V5 H2 Z M6 4 H14 M2 7 H4 V9 H2 Z M6 8 H14 M2 11 H4 V13 H2 Z M6 12 H14";
        }
        return GENERIC_DOCUMENT;
    }

    private static String styleClass(EditorIconId id) {
        if (id.equals(EditorIcons.READ_ONLY)) {
            return EditorStyleClasses.EDITOR_ICON_READ_ONLY;
        }
        if (id.equals(EditorIcons.DISABLED)) {
            return EditorStyleClasses.EDITOR_ICON_DISABLED;
        }
        return EditorStyleClasses.EDITOR_ICON_PRIMARY;
    }
}
