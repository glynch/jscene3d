/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Owns the scene-preview editor tab and its observable presentation. */
public final class JavaFxPreviewEditor implements AutoCloseable {
    private final JavaFxEditorArea area;
    private final Tab tab = new Tab();
    private final Label title = new Label();
    private final Region dirtyIndicator = new Region();

    /** Creates a preview editor without revealing it. */
    public JavaFxPreviewEditor(
            JavaFxEditorArea area, ReadOnlyStringProperty title, ReadOnlyBooleanProperty dirty, Node content) {
        this.area = Objects.requireNonNull(area, "area");
        ReadOnlyStringProperty editorTitle = Objects.requireNonNull(title, "title");
        ReadOnlyBooleanProperty editorDirty = Objects.requireNonNull(dirty, "dirty");

        this.title.textProperty().bind(editorTitle);
        this.title
                .accessibleTextProperty()
                .bind(Bindings.createStringBinding(
                        () -> editorDirty.get() ? editorTitle.get() + ", modified" : editorTitle.get(),
                        editorTitle,
                        editorDirty));
        tab.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> editorDirty.get() ? editorTitle.get() + ", modified" : editorTitle.get(),
                        editorTitle,
                        editorDirty));
        tab.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_GRAPHIC_ONLY);
        this.title.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_TITLE);
        dirtyIndicator.visibleProperty().bind(editorDirty);
        dirtyIndicator.managedProperty().bind(editorDirty);
        dirtyIndicator.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_DIRTY);
        HBox graphic = new HBox(7.0, this.title, dirtyIndicator);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB);
        tab.setGraphic(graphic);
        tab.setContent(Objects.requireNonNull(content, "content"));
        tab.setClosable(false);
    }

    /** Reveals and selects the preview editor. */
    public void show() {
        if (!area.contains(tab)) {
            area.add(tab, () -> {});
        }
        area.select(tab);
    }

    /** Removes the project-owned preview editor. */
    public void hide() {
        area.remove(tab);
    }

    @Override
    public void close() {
        hide();
        title.textProperty().unbind();
        title.accessibleTextProperty().unbind();
        tab.textProperty().unbind();
        dirtyIndicator.visibleProperty().unbind();
        dirtyIndicator.managedProperty().unbind();
    }
}
