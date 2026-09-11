/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.status;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/** Renders toolkit-independent status contributions inside the JavaFX workbench. */
public final class JavaFxStatusBarAdapter implements AutoCloseable {
    private final EditorExtensionHost extensions;
    private final JavaFxIconRenderer icons;
    private final HBox leftItems = createRegion();
    private final HBox rightItems = createRegion();
    private final EditorRegistration registration;
    private boolean closed;

    /** Creates an adapter which follows the host's complete status-item snapshot. */
    public JavaFxStatusBarAdapter(EditorExtensionHost extensions, JavaFxIconRenderer icons) {
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        this.icons = Objects.requireNonNull(icons, "icons");
        registration = extensions.observeStatusItems(this::accept);
    }

    /** Returns the node containing leading status contributions. */
    public HBox leftNode() {
        return leftItems;
    }

    /** Returns the node containing trailing status contributions. */
    public HBox rightNode() {
        return rightItems;
    }

    /** Stops observing contributions and clears rendered status nodes. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        registration.close();
        leftItems.getChildren().clear();
        rightItems.getChildren().clear();
    }

    private void accept(List<EditorStatusItemSnapshot> snapshot) {
        List<EditorStatusItemSnapshot> current = List.copyOf(snapshot);
        if (Platform.isFxApplicationThread()) {
            render(current);
        } else {
            Platform.runLater(() -> render(current));
        }
    }

    private void render(List<EditorStatusItemSnapshot> snapshot) {
        if (closed) {
            return;
        }
        leftItems.getChildren().setAll(nodes(snapshot, StatusBarAlignment.LEFT));
        rightItems.getChildren().setAll(nodes(snapshot, StatusBarAlignment.RIGHT));
    }

    private List<Node> nodes(List<EditorStatusItemSnapshot> snapshot, StatusBarAlignment alignment) {
        return snapshot.stream()
                .filter(item -> item.contribution().alignment() == alignment)
                .filter(item -> item.state().visible())
                .map(this::node)
                .toList();
    }

    private Node node(EditorStatusItemSnapshot snapshot) {
        EditorStatusItemState state = snapshot.state();
        Labeled item;
        if (state.command().isPresent()) {
            Button action = new Button(state.text());
            action.getStyleClass().add(EditorStyleClasses.EDITOR_STATUS_ACTION);
            action.setOnAction(ignored -> extensions.execute(state.command().orElseThrow()));
            item = action;
        } else {
            item = new Label(state.text());
        }
        item.getStyleClass().addAll(EditorStyleClasses.EDITOR_STATUS, EditorStyleClasses.EDITOR_STATUS_ITEM);
        state.icon().ifPresent(icon -> item.setGraphic(icons.create(icon)));
        state.tooltip().ifPresent(text -> item.setTooltip(new Tooltip(text)));
        item.setAccessibleText(state.tooltip().orElse(state.text()));
        return item;
    }

    private static HBox createRegion() {
        HBox region = new HBox();
        region.getStyleClass().add(EditorStyleClasses.EDITOR_STATUS_ITEMS);
        return region;
    }
}
