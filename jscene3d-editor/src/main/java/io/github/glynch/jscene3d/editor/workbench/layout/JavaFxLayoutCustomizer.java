/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Presents all session layout controls through one discoverable toolbar action. */
public final class JavaFxLayoutCustomizer implements AutoCloseable {
    private static final List<Destination> DESTINATIONS = List.of(
            new Destination("Primary Side Bar", EditorViewContainers.PRIMARY_SIDEBAR),
            new Destination("Secondary Side Bar", EditorViewContainers.SECONDARY_SIDEBAR),
            new Destination("Panel", EditorViewContainers.BOTTOM_PANEL));

    private final EditorWorkbenchLayout layout;
    private final Consumer<ViewId> viewRevealer;
    private final Button layoutButton = new Button();
    private final ContextMenu popup = new ContextMenu();

    /**
     * Creates a toolbar action backed by the current session layout.
     *
     * @param layout current workbench layout
     * @param icons icon renderer
     * @param viewRevealer reveals a view after it is moved
     */
    public JavaFxLayoutCustomizer(
            EditorWorkbenchLayout layout, JavaFxIconRenderer icons, Consumer<ViewId> viewRevealer) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.viewRevealer = Objects.requireNonNull(viewRevealer, "viewRevealer");
        JavaFxIconRenderer renderer = Objects.requireNonNull(icons, "icons");
        layoutButton.setGraphic(renderer.create(new EditorIcon(EditorIcons.LAYOUT, "Customize Layout")));
        layoutButton.setAccessibleText("Customize Layout");
        layoutButton.setTooltip(new Tooltip("Customize Layout"));
        layoutButton.setOnAction(ignored -> toggle());
        layoutButton.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_BUTTON);
        popup.setAutoHide(true);
        popup.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_MENU);
    }

    /**
     * Returns the workbench-owned toolbar button.
     *
     * @return Customize Layout button
     */
    public Button button() {
        return layoutButton;
    }

    @Override
    public void close() {
        popup.hide();
        popup.getItems().clear();
    }

    private void toggle() {
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        rebuild();
        popup.show(layoutButton, Side.BOTTOM, 0.0, 2.0);
    }

    private void rebuild() {
        EditorWorkbenchLayoutState state = layout.current();
        VBox content = new VBox(6.0);
        content.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_MENU_SECTION);
        content.getChildren().add(heading("Customize Layout"));
        addVisibility(content, state, "Activity Bar", EditorWorkbenchPart.ACTIVITY_BAR);
        addVisibility(content, state, "Primary Side Bar", EditorWorkbenchPart.PRIMARY_SIDEBAR);
        addVisibility(content, state, "Secondary Side Bar", EditorWorkbenchPart.SECONDARY_SIDEBAR);
        addVisibility(content, state, "Panel", EditorWorkbenchPart.PANEL);
        addVisibility(content, state, "Status Bar", EditorWorkbenchPart.STATUS_BAR);
        content.getChildren().add(new Separator());
        content.getChildren().add(heading("Primary Side Bar Position"));
        addSidebarPosition(content, state);
        List<EditorViewPlacement> movable =
                state.views().stream().filter(EditorViewPlacement::movable).toList();
        if (!movable.isEmpty()) {
            content.getChildren().add(new Separator());
            content.getChildren().add(heading("Views"));
            movable.forEach(view -> content.getChildren().add(viewDestination(view)));
        }
        content.getChildren().add(new Separator());
        Button restore = new Button("Restore Default Layout");
        restore.setMaxWidth(Double.MAX_VALUE);
        restore.setOnAction(ignored -> {
            layout.reset();
            popup.hide();
        });
        content.getChildren().add(restore);
        CustomMenuItem item = new CustomMenuItem(content, false);
        item.setHideOnClick(false);
        popup.getItems().setAll(item);
    }

    private void addVisibility(VBox content, EditorWorkbenchLayoutState state, String label, EditorWorkbenchPart part) {
        CheckBox toggle = new CheckBox(label);
        toggle.setSelected(state.isVisible(part));
        toggle.setMaxWidth(Double.MAX_VALUE);
        toggle.setOnAction(ignored -> layout.setVisible(part, toggle.isSelected()));
        content.getChildren().add(toggle);
    }

    private void addSidebarPosition(VBox content, EditorWorkbenchLayoutState state) {
        ToggleGroup positions = new ToggleGroup();
        RadioButton left = positionButton("Left", EditorPrimarySidebarPosition.LEFT, positions);
        RadioButton right = positionButton("Right", EditorPrimarySidebarPosition.RIGHT, positions);
        if (state.primarySidebarPosition() == EditorPrimarySidebarPosition.LEFT) {
            left.setSelected(true);
        } else {
            right.setSelected(true);
        }
        content.getChildren().addAll(left, right);
    }

    private RadioButton positionButton(String label, EditorPrimarySidebarPosition position, ToggleGroup positions) {
        RadioButton positionButton = new RadioButton(label);
        positionButton.setToggleGroup(positions);
        positionButton.setOnAction(ignored -> layout.setPrimarySidebarPosition(position));
        return positionButton;
    }

    private HBox viewDestination(EditorViewPlacement placement) {
        Label label = new Label(placement.view().title());
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        ComboBox<Destination> destination = new ComboBox<>();
        destination.getItems().setAll(DESTINATIONS);
        destination.setValue(DESTINATIONS.stream()
                .filter(candidate -> candidate.container().equals(placement.container()))
                .findFirst()
                .orElseThrow());
        destination.setOnAction(ignored -> {
            Destination selected = destination.getValue();
            if (selected == null
                    || layout.containerOf(placement.view().id())
                            .filter(selected.container()::equals)
                            .isPresent()) {
                return;
            }
            layout.move(placement.view().id(), selected.container());
            viewRevealer.accept(placement.view().id());
        });
        HBox row = new HBox(12.0, label, destination);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_MENU_ROW);
        return row;
    }

    private static Label heading(String text) {
        Label heading = new Label(text);
        heading.getStyleClass().add(EditorStyleClasses.EDITOR_LAYOUT_MENU_HEADING);
        return heading;
    }

    private record Destination(String label, ViewContainerId container) {
        private Destination {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(container, "container");
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
