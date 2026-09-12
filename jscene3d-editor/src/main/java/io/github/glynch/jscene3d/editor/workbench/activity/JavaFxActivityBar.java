/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.activity;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.accessibility.JavaFxDirectionalNavigation;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

/** JavaFX adapter for extension-contributed editor activities. */
public final class JavaFxActivityBar implements AutoCloseable {
    private final EditorActivitySelection selection;
    private final JavaFxIconRenderer icons;
    private final VBox root = new VBox();
    private final Map<ActivityId, ToggleButton> buttons = new LinkedHashMap<>();
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
        root.setAccessibleRole(AccessibleRole.TOOL_BAR);
        root.setAccessibleText("Activity Bar");
        root.setAccessibleHelp("Use Up and Down Arrow keys to move between activities");
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
        Optional<ActivityId> focused = buttons.entrySet().stream()
                .filter(entry -> entry.getValue().isFocused())
                .map(Map.Entry::getKey)
                .findFirst();
        buttons.clear();
        root.getChildren().clear();
        for (EditorActivityContribution activity : activities) {
            ToggleButton button = new ToggleButton();
            button.setGraphic(icons.create(activity.icon(), EditorStyleClasses.EDITOR_ACTIVITY_ICON));
            button.setAccessibleText(activity.title());
            button.setAccessibleHelp("Activates the " + activity.title()
                    + " view. Use Up and Down Arrow keys to move between activities");
            button.setTooltip(new Tooltip(activity.title()));
            button.setOnAction(ignored -> {
                selection.select(activity.id());
                updateSelection();
            });
            button.setOnKeyPressed(event -> navigate(activity.id(), event));
            button.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON);
            buttons.put(activity.id(), button);
            root.getChildren().add(button);
        }
        updateSelection();
        if (focused.isPresent()) {
            ToggleButton focusTarget = focused.map(buttons::get)
                    .or(() -> selection.current().selected().map(buttons::get))
                    .orElse(null);
            if (focusTarget != null) {
                Platform.runLater(focusTarget::requestFocus);
            }
        }
    }

    private void updateSelection() {
        Optional<ActivityId> selected = selection.current().selected();
        buttons.forEach((activity, button) -> {
            boolean active = selected.filter(activity::equals).isPresent();
            button.setSelected(active);
            button.getStyleClass().remove(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON_ACTIVE);
            if (active) {
                button.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON_ACTIVE);
            }
            String title = button.getTooltip().getText();
            button.setAccessibleText(active ? title + ", selected" : title);
        });
    }

    private void navigate(ActivityId current, KeyEvent event) {
        JavaFxDirectionalNavigation.target(
                        List.copyOf(buttons.keySet()), current, event.getCode(), Orientation.VERTICAL)
                .map(buttons::get)
                .ifPresent(target -> {
                    target.requestFocus();
                    event.consume();
                });
    }
}
