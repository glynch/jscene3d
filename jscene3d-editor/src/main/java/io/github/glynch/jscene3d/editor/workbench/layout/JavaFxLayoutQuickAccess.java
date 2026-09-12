/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/** Presents frequently used workbench visibility controls in the editor header. */
public final class JavaFxLayoutQuickAccess implements AutoCloseable {
    private final EditorWorkbenchLayout layout;
    private final Map<EditorWorkbenchPart, ToggleButton> visibilityButtons = new EnumMap<>(EditorWorkbenchPart.class);
    private final HBox node = new HBox(2.0);
    private final EditorRegistration layoutRegistration;

    /**
     * Creates header controls backed by the current session layout.
     *
     * @param layout current workbench layout
     * @param icons icon renderer
     * @param customizeLayout button which opens the complete layout customizer
     */
    public JavaFxLayoutQuickAccess(EditorWorkbenchLayout layout, JavaFxIconRenderer icons, Button customizeLayout) {
        this.layout = Objects.requireNonNull(layout, "layout");
        JavaFxIconRenderer renderer = Objects.requireNonNull(icons, "icons");
        Button customizer = Objects.requireNonNull(customizeLayout, "customizeLayout");
        node.setAlignment(Pos.CENTER_LEFT);
        node.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_ACTIONS);
        node.getChildren()
                .addAll(
                        customizer,
                        visibilityButton(
                                renderer,
                                EditorWorkbenchPart.PRIMARY_SIDEBAR,
                                EditorIcons.PRIMARY_SIDEBAR,
                                "Toggle Primary Side Bar"),
                        visibilityButton(renderer, EditorWorkbenchPart.PANEL, EditorIcons.PANEL, "Toggle Panel"),
                        visibilityButton(
                                renderer,
                                EditorWorkbenchPart.SECONDARY_SIDEBAR,
                                EditorIcons.SECONDARY_SIDEBAR,
                                "Toggle Secondary Side Bar"));
        layoutRegistration = layout.observe(this::apply);
    }

    /**
     * Returns the workbench-owned header control group.
     *
     * @return quick-access controls
     */
    public HBox node() {
        return node;
    }

    @Override
    public void close() {
        layoutRegistration.close();
        visibilityButtons.clear();
        node.getChildren().clear();
    }

    private ToggleButton visibilityButton(
            JavaFxIconRenderer renderer, EditorWorkbenchPart part, EditorIconId icon, String label) {
        ToggleButton visibilityButton = new ToggleButton();
        visibilityButton.setGraphic(renderer.create(new EditorIcon(icon, label)));
        visibilityButton.setAccessibleText(label);
        visibilityButton.setTooltip(new Tooltip(label));
        visibilityButton.setOnAction(ignored -> layout.setVisible(part, visibilityButton.isSelected()));
        visibilityButton.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_TOGGLE);
        visibilityButtons.put(part, visibilityButton);
        return visibilityButton;
    }

    private void apply(EditorWorkbenchLayoutState state) {
        visibilityButtons.forEach((part, visibilityButton) -> visibilityButton.setSelected(state.isVisible(part)));
    }
}
