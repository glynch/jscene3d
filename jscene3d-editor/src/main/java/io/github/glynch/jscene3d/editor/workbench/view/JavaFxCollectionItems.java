/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.view.EditorCollectionDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorCollectionItem;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSelectionModel;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import org.jspecify.annotations.Nullable;

/** Owns the interchangeable list and grid presentations for collection elements. */
final class JavaFxCollectionItems<T> {
    private final EditorCollectionDataProvider<T> provider;
    private final Optional<EditorCollectionSelectionModel<T>> selectionModel;
    private final BiConsumer<CommandId, Object> commandExecutor;
    private final JavaFxCollectionItemPresentation presentation;
    private final ListView<T> list = new ListView<>();
    private final TilePane grid = new TilePane();
    private final ScrollPane gridScroll = new ScrollPane();
    private final StackPane content = new StackPane();
    private final ToggleGroup cardGroup = new ToggleGroup();
    private final Label empty = new Label();
    private Optional<T> desiredSelection = Optional.empty();
    private boolean showingGrid = true;
    private int selectionFeedbackSuppressionDepth;

    JavaFxCollectionItems(
            EditorCollectionDataProvider<T> provider,
            Optional<EditorCollectionSelectionModel<T>> selectionModel,
            BiConsumer<CommandId, Object> commandExecutor,
            JavaFxIconRenderer icons) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor");
        presentation = new JavaFxCollectionItemPresentation(Objects.requireNonNull(icons, "icons"));
        configure();
    }

    Node node() {
        return content;
    }

    void requestFocus() {
        if (showingGrid) {
            gridScroll.requestFocus();
        } else {
            list.requestFocus();
        }
    }

    void show(List<T> elements) {
        List<T> visible = List.copyOf(Objects.requireNonNull(elements, "elements"));
        withoutSelectionFeedback(() -> {
            list.getItems().setAll(visible);
            cardGroup.getToggles().clear();
            grid.getChildren().setAll(visible.stream().map(this::createCard).toList());
            synchronizeSelection();
        });
        refreshVisibility(visible.isEmpty());
    }

    void select(Optional<T> selection) {
        desiredSelection = Objects.requireNonNull(selection, "selection");
        runOnApplicationThread(() -> withoutSelectionFeedback(this::synchronizeSelection));
    }

    void showGrid(boolean showGrid) {
        showingGrid = showGrid;
        refreshVisibility(list.getItems().isEmpty());
        withoutSelectionFeedback(this::synchronizeSelection);
    }

    void setEmptyMessage(String message) {
        empty.setText(Objects.requireNonNull(message, "message"));
    }

    void clear() {
        withoutSelectionFeedback(() -> {
            list.getItems().clear();
            cardGroup.getToggles().clear();
            grid.getChildren().clear();
        });
    }

    private void configure() {
        list.setCellFactory(ignored -> new CollectionListCell());
        list.getStyleClass().addAll(EditorStyleClasses.EDITOR_COLLECTION_LIST, EditorStyleClasses.EDITOR_ASSET_LIST);
        list.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selectionFeedbackSuppressionDepth == 0 && selected != null) {
                selectionModel.ifPresent(model -> model.select(Optional.of(selected)));
            }
        });
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Optional.ofNullable(list.getSelectionModel().getSelectedItem()).ifPresent(this::executeItemCommand);
            }
        });

        grid.setHgap(8.0);
        grid.setVgap(8.0);
        grid.setPrefTileWidth(156.0);
        grid.setPrefTileHeight(112.0);
        grid.getStyleClass().addAll(EditorStyleClasses.EDITOR_COLLECTION_GRID, EditorStyleClasses.EDITOR_ASSET_GRID);
        gridScroll.setContent(grid);
        gridScroll.setFitToWidth(true);
        gridScroll.setPannable(true);
        gridScroll
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_GRID_SCROLL, EditorStyleClasses.EDITOR_ASSET_GRID_SCROLL);
        empty.setWrapText(true);
        empty.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_EMPTY_DETAIL,
                        EditorStyleClasses.EDITOR_COLLECTION_EMPTY,
                        EditorStyleClasses.EDITOR_PROJECT_EMPTY);
        content.getChildren().setAll(list, gridScroll, empty);
    }

    private void refreshVisibility(boolean noVisibleItems) {
        empty.setManaged(noVisibleItems);
        empty.setVisible(noVisibleItems);
        list.setManaged(!noVisibleItems && !showingGrid);
        list.setVisible(!noVisibleItems && !showingGrid);
        gridScroll.setManaged(!noVisibleItems && showingGrid);
        gridScroll.setVisible(!noVisibleItems && showingGrid);
    }

    private ToggleButton createCard(T element) {
        EditorCollectionItem item = provider.item(element);
        ToggleButton card = new ToggleButton();
        card.setGraphic(presentation.create(item));
        card.setUserData(element);
        card.setToggleGroup(cardGroup);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setTooltip(item.tooltip().map(Tooltip::new).orElse(null));
        card.setOnAction(ignored -> selectionModel.ifPresent(model -> model.select(Optional.of(element))));
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                executeItemCommand(element);
            }
        });
        card.getStyleClass().addAll(EditorStyleClasses.EDITOR_COLLECTION_CARD, EditorStyleClasses.EDITOR_ASSET_CARD);
        return card;
    }

    private void synchronizeSelection() {
        Optional<T> visible = desiredSelection.filter(list.getItems()::contains);
        if (visible.isPresent()) {
            list.getSelectionModel().select(visible.orElseThrow());
        } else {
            list.getSelectionModel().clearSelection();
        }
        cardGroup.selectToggle(null);
        visible.flatMap(selected -> cardGroup.getToggles().stream()
                        .filter(toggle -> Objects.equals(toggle.getUserData(), selected))
                        .findFirst())
                .ifPresent(cardGroup::selectToggle);
    }

    private void executeItemCommand(T element) {
        provider.item(element).command().ifPresent(command -> commandExecutor.accept(command, element));
    }

    private void withoutSelectionFeedback(Runnable action) {
        selectionFeedbackSuppressionDepth++;
        try {
            action.run();
        } finally {
            selectionFeedbackSuppressionDepth--;
        }
    }

    private static void runOnApplicationThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private final class CollectionListCell extends ListCell<T> {
        private CollectionListCell() {
            getStyleClass()
                    .addAll(EditorStyleClasses.EDITOR_COLLECTION_LIST_CELL, EditorStyleClasses.EDITOR_ASSET_LIST_CELL);
        }

        @Override
        protected void updateItem(@Nullable T element, boolean cellEmpty) {
            super.updateItem(element, cellEmpty);
            setText(null);
            setGraphic(cellEmpty || element == null ? null : presentation.create(provider.item(element)));
        }
    }
}
