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
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** JavaFX adapter for extension-contributed editor activities. */
public final class JavaFxActivityBar implements AutoCloseable {
    private final EditorExtensionHost extensions;
    private final EditorWorkbenchLayout layout;
    private final JavaFxIconRenderer icons;
    private final VBox root = new VBox();
    private final Map<ActivityId, Button> buttons = new LinkedHashMap<>();
    private final EditorRegistration registration;
    private @Nullable ActivityId selected;

    /**
     * Creates an Activity Bar which follows extension contributions.
     *
     * @param extensions active extension host
     * @param layout current session layout
     * @param icons icon renderer
     */
    public JavaFxActivityBar(EditorExtensionHost extensions, EditorWorkbenchLayout layout, JavaFxIconRenderer icons) {
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.icons = Objects.requireNonNull(icons, "icons");
        root.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BAR);
        registration = extensions.observeActivities(this::showActivities);
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
        registration.close();
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
            button.setOnAction(ignored -> select(activity));
            button.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON);
            buttons.put(activity.id(), button);
            root.getChildren().add(button);
        }
        EditorActivityContribution active = activities.stream()
                .filter(activity -> activity.id().equals(selected))
                .findFirst()
                .orElseGet(() -> activities.stream().findFirst().orElse(null));
        if (active == null) {
            selected = null;
        } else {
            selected = active.id();
            extensions.showView(active.view());
        }
        updateSelection();
    }

    private void select(EditorActivityContribution activity) {
        if (activity.id().equals(selected)) {
            layout.partOf(activity.view()).ifPresent(part -> {
                boolean reveal = !layout.isVisible(part);
                layout.setVisible(part, reveal);
                if (reveal) {
                    extensions.showView(activity.view());
                }
            });
            return;
        }
        selected = activity.id();
        extensions.showView(activity.view());
        updateSelection();
    }

    private void updateSelection() {
        buttons.values()
                .forEach(button -> button.getStyleClass().remove(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON_ACTIVE));
        Button active = selected == null ? null : buttons.get(selected);
        if (active != null) {
            active.getStyleClass().add(EditorStyleClasses.EDITOR_ACTIVITY_BUTTON_ACTIVE);
        }
    }
}
