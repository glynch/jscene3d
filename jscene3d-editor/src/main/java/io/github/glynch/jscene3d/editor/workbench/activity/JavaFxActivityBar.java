/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.activity;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/** JavaFX adapter for extension-contributed editor activities. */
public final class JavaFxActivityBar implements AutoCloseable {
    private final EditorActivitySelection selection;
    private final JavaFxIconRenderer icons;
    private final VBox root = new VBox();
    private final Map<ActivityId, Button> buttons = new LinkedHashMap<>();
    private final EditorRegistration activityRegistration;
    private final EditorRegistration selectionRegistration;

    /**
     * Creates an Activity Bar which follows extension contributions.
     *
     * @param extensions active extension host
     * @param selection selected activity container
     * @param icons icon renderer
     */
    public JavaFxActivityBar(
            EditorExtensionHost extensions, EditorActivitySelection selection, JavaFxIconRenderer icons) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        this.selection = Objects.requireNonNull(selection, "selection");
        this.icons = Objects.requireNonNull(icons, "icons");
        root.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BAR);
        activityRegistration = host.observeActivities(this::showActivities);
        selectionRegistration = selection.observe(ignored -> updateSelection());
    }

    /**
     * Returns the workbench-owned JavaFX node.
     *
     * @return Activity Bar node
     */
    public VBox node() {
        return root;
    }

    @Override
    public void close() {
        selectionRegistration.close();
        activityRegistration.close();
        buttons.clear();
        root.getChildren().clear();
    }

    private void showActivities(List<EditorActivityContribution> activities) {
        buttons.clear();
        root.getChildren().clear();
        for (EditorActivityContribution activity : activities) {
            Button button = new Button();
            button.setGraphic(icons.create(activity.icon(), EditorStyleClasses.EDITOR_ACTIVITY_ICON));
            button.setAccessibleText(activity.title());
            button.setTooltip(new Tooltip(activity.title()));
            button.setOnAction(ignored -> selection.select(activity.id()));
            button.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON);
            buttons.put(activity.id(), button);
            root.getChildren().add(button);
        }
        updateSelection();
    }

    private void updateSelection() {
        buttons.values()
                .forEach(button -> button.getStyleClass().remove(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON_ACTIVE));
        selection
                .current()
                .selected()
                .map(buttons::get)
                .ifPresent(active -> active.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON_ACTIVE));
    }
}
