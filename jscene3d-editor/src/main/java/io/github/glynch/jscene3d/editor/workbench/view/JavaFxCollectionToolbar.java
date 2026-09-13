/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Owns collection search, location, and presentation controls. */
final class JavaFxCollectionToolbar {
    private final Label breadcrumb = new Label();
    private final ToggleButton gridView = new ToggleButton();
    private final ToggleButton listView = new ToggleButton();
    private final TextField search = new TextField();
    private final HBox root;

    JavaFxCollectionToolbar(
            String searchPlaceholder,
            JavaFxIconRenderer icons,
            Runnable queryChanged,
            Consumer<Boolean> presentationChanged) {
        JavaFxIconRenderer iconRenderer = Objects.requireNonNull(icons, "icons");
        Runnable searchListener = Objects.requireNonNull(queryChanged, "queryChanged");
        Consumer<Boolean> presentationListener = Objects.requireNonNull(presentationChanged, "presentationChanged");
        configureBreadcrumb();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox viewButtons = createViewButtons(iconRenderer, presentationListener);
        configureSearch(searchPlaceholder, searchListener);
        root = new HBox(8.0, breadcrumb, spacer, viewButtons, search);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_TOOLBAR, EditorStyleClasses.EDITOR_PROJECT_TOOLBAR);
    }

    Node node() {
        return root;
    }

    String query() {
        return search.getText();
    }

    void clearSearch() {
        search.clear();
    }

    void setSearchable(boolean searchable) {
        search.setDisable(!searchable);
    }

    void showLocation(String rootLabel, String location) {
        breadcrumb.setText(Objects.requireNonNull(rootLabel, "rootLabel") + "  ›  "
                + Objects.requireNonNull(location, "location"));
    }

    private void configureBreadcrumb() {
        breadcrumb.setMinWidth(80.0);
        breadcrumb.setMaxWidth(240.0);
        breadcrumb.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        breadcrumb
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_BREADCRUMB, EditorStyleClasses.EDITOR_PROJECT_BREADCRUMB);
    }

    private HBox createViewButtons(JavaFxIconRenderer icons, Consumer<Boolean> presentationChanged) {
        ToggleGroup presentations = new ToggleGroup();
        gridView.setToggleGroup(presentations);
        listView.setToggleGroup(presentations);
        configurePresentationButton(gridView, "Grid view", true, presentationChanged);
        configurePresentationButton(listView, "List view", false, presentationChanged);
        gridView.setGraphic(icons.create(new EditorIcon(EditorIcons.GRID, "Grid view")));
        listView.setGraphic(icons.create(new EditorIcon(EditorIcons.LIST, "List view")));
        gridView.setSelected(true);
        HBox buttons = new HBox(gridView, listView);
        buttons.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_COLLECTION_VIEW_BUTTONS,
                        EditorStyleClasses.EDITOR_PROJECT_VIEW_BUTTONS);
        return buttons;
    }

    private void configurePresentationButton(
            ToggleButton button, String name, boolean gridPresentation, Consumer<Boolean> presentationChanged) {
        button.setAccessibleText(name);
        button.setTooltip(new Tooltip(name));
        button.setOnAction(ignored -> {
            gridView.setSelected(gridPresentation);
            listView.setSelected(!gridPresentation);
            presentationChanged.accept(gridPresentation);
        });
        button.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_COLLECTION_VIEW_TOGGLE,
                        EditorStyleClasses.EDITOR_PROJECT_VIEW_TOGGLE);
    }

    private void configureSearch(String placeholder, Runnable queryChanged) {
        search.setPromptText(Objects.requireNonNull(placeholder, "searchPlaceholder"));
        search.setMinWidth(100.0);
        search.setPrefWidth(180.0);
        search.setDisable(true);
        search.getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_SEARCH, EditorStyleClasses.EDITOR_PROJECT_SEARCH);
        search.textProperty().addListener((ignored, previous, current) -> queryChanged.run());
    }
}
