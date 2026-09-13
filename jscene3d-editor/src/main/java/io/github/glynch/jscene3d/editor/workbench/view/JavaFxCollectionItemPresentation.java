/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.view.EditorCollectionItem;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Renders collection metadata consistently in list rows and grid cards. */
final class JavaFxCollectionItemPresentation {
    private final JavaFxIconRenderer icons;

    JavaFxCollectionItemPresentation(JavaFxIconRenderer icons) {
        this.icons = Objects.requireNonNull(icons, "icons");
    }

    VBox create(EditorCollectionItem item) {
        EditorCollectionItem presentation = Objects.requireNonNull(item, "item");
        VBox rendered = new VBox(4.0, heading(presentation), metadata(presentation), detail(presentation));
        rendered.setMaxWidth(Double.MAX_VALUE);
        rendered.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_COLLECTION_PRESENTATION,
                        EditorStyleClasses.EDITOR_ASSET_PRESENTATION);
        return rendered;
    }

    private HBox heading(EditorCollectionItem item) {
        Label name = new Label(item.label());
        name.setMinWidth(0.0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setPrefWidth(1.0);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setTooltip(new Tooltip(item.label()));
        name.getStyleClass().addAll(EditorStyleClasses.EDITOR_COLLECTION_NAME, EditorStyleClasses.EDITOR_ASSET_NAME);
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox heading = new HBox(6.0);
        item.icon()
                .map(icon -> icons.create(
                        icon, EditorStyleClasses.EDITOR_COLLECTION_MARKER, EditorStyleClasses.EDITOR_ASSET_MARKER))
                .ifPresent(heading.getChildren()::add);
        heading.getChildren().add(name);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setMaxWidth(Double.MAX_VALUE);
        return heading;
    }

    private HBox metadata(EditorCollectionItem item) {
        Label description = new Label(item.description().orElse(""));
        description
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_DESCRIPTION, EditorStyleClasses.EDITOR_ASSET_KIND);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox decorations = new HBox(3.0);
        decorations.setAlignment(Pos.CENTER_RIGHT);
        decorations.getStyleClass().add(EditorStyleClasses.EDITOR_ITEM_DECORATIONS);
        for (EditorIcon decoration : item.decorations()) {
            decorations
                    .getChildren()
                    .add(icons.create(
                            decoration,
                            EditorStyleClasses.EDITOR_ITEM_DECORATION,
                            EditorStyleClasses.EDITOR_COLLECTION_DECORATION));
        }
        HBox metadata = new HBox(6.0, description, spacer, decorations);
        metadata.setAlignment(Pos.CENTER_LEFT);
        metadata.setMaxWidth(Double.MAX_VALUE);
        return metadata;
    }

    private static Label detail(EditorCollectionItem item) {
        Label detail = new Label(item.detail().orElse(""));
        detail.setMinWidth(0.0);
        detail.setMaxWidth(Double.MAX_VALUE);
        detail.setPrefWidth(1.0);
        detail.setTextOverrun(OverrunStyle.ELLIPSIS);
        detail.setTooltip(item.tooltip().map(Tooltip::new).orElse(null));
        detail.getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_DETAIL, EditorStyleClasses.EDITOR_ASSET_SOURCE);
        return detail;
    }
}
