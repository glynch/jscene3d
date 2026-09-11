/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorCollectionCategory;
import io.github.glynch.jscene3d.editor.view.EditorCollectionDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorCollectionItem;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSelectionModel;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSnapshot;
import io.github.glynch.jscene3d.editor.view.EditorCollectionView;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** Workbench-owned JavaFX adapter for any toolkit-independent collection view contribution. */
final class JavaFxCollectionViewAdapter<T> implements AutoCloseable {
    private final EditorCollectionDataProvider<T> provider;
    private final List<EditorCollectionCategory> categories;
    private final Optional<EditorCollectionSelectionModel<T>> selectionModel;
    private final Consumer<CommandId> commandExecutor;
    private final String allItemsLabel;
    private final Optional<EditorIcon> rootIcon;
    private final String searchPlaceholder;
    private final VBox root = new VBox();
    private final TreeView<CategoryLocation> navigation = new TreeView<>();
    private final ListView<T> list = new ListView<>();
    private final TilePane grid = new TilePane();
    private final ScrollPane gridScroll = new ScrollPane();
    private final StackPane content = new StackPane();
    private final ToggleGroup cardGroup = new ToggleGroup();
    private final ToggleButton gridView = new ToggleButton();
    private final ToggleButton listView = new ToggleButton();
    private final TextField search = new TextField();
    private final Label breadcrumb = new Label();
    private final Label empty = new Label();
    private final EditorRegistration dataRegistration;
    private final Optional<EditorRegistration> selectionRegistration;
    private final Map<String, EditorCollectionCategory> categoryById;

    private EditorCollectionSnapshot<T> snapshot =
            new EditorCollectionSnapshot<>("Loading", List.of(), false, "Loading collection…");
    private Optional<String> selectedCategory = Optional.empty();
    private Optional<T> desiredSelection = Optional.empty();
    private boolean showingGrid = true;
    private int selectionFeedbackSuppressionDepth;
    private long generation;

    /** Creates and begins observing one logical collection view. */
    JavaFxCollectionViewAdapter(EditorCollectionView<T> view, Consumer<CommandId> commandExecutor) {
        EditorCollectionView<T> logicalView = Objects.requireNonNull(view, "view");
        provider = Objects.requireNonNull(logicalView.dataProvider(), "view.dataProvider()");
        categories = List.copyOf(Objects.requireNonNull(logicalView.categories(), "view.categories()"));
        selectionModel = Objects.requireNonNull(logicalView.selectionModel(), "view.selectionModel()");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor");
        allItemsLabel = requireText(logicalView.allItemsLabel(), "view.allItemsLabel()");
        rootIcon = Objects.requireNonNull(logicalView.rootIcon(), "view.rootIcon()");
        searchPlaceholder = requireText(logicalView.searchPlaceholder(), "view.searchPlaceholder()");
        categoryById = indexCategories(categories);
        configureView();
        dataRegistration = provider.observeChanges(this::reload);
        selectionRegistration = selectionModel.map(model -> model.observe(this::applySelection));
        reload();
    }

    /** Returns the rendered JavaFX node owned by this adapter. */
    Node node() {
        return root;
    }

    /** Requests keyboard focus for the active collection presentation. */
    void requestFocus() {
        if (showingGrid) {
            gridScroll.requestFocus();
        } else {
            list.requestFocus();
        }
    }

    @Override
    public void close() {
        dataRegistration.close();
        selectionRegistration.ifPresent(EditorRegistration::close);
        generation++;
        withoutSelectionFeedback(() -> {
            list.getItems().clear();
            grid.getChildren().clear();
            navigation.setRoot(null);
        });
    }

    private void configureView() {
        VBox browserContent = new VBox(createToolbar(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
        browserContent.getStyleClass().addAll("editor-collection-content", "editor-project-content");

        list.setCellFactory(ignored -> new CollectionListCell());
        list.getStyleClass().addAll("editor-collection-list", "editor-asset-list");
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
        grid.getStyleClass().addAll("editor-collection-grid", "editor-asset-grid");
        gridScroll.setContent(grid);
        gridScroll.setFitToWidth(true);
        gridScroll.setPannable(true);
        gridScroll.getStyleClass().addAll("editor-collection-grid-scroll", "editor-asset-grid-scroll");
        empty.setWrapText(true);
        empty.getStyleClass().addAll("editor-empty-detail", "editor-collection-empty", "editor-project-empty");
        content.getChildren().setAll(list, gridScroll, empty);

        Node browser = categories.isEmpty() ? browserContent : createCategorizedBrowser(browserContent);
        VBox.setVgrow(browser, Priority.ALWAYS);
        root.getChildren().setAll(browser);
        root.getStyleClass().addAll("editor-panel", "editor-collection-panel", "editor-project-panel");
        showPresentation(true);
    }

    private HBox createToolbar() {
        breadcrumb.setMinWidth(80.0);
        breadcrumb.setMaxWidth(240.0);
        breadcrumb.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        breadcrumb.getStyleClass().addAll("editor-collection-breadcrumb", "editor-project-breadcrumb");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleGroup presentations = new ToggleGroup();
        gridView.setToggleGroup(presentations);
        listView.setToggleGroup(presentations);
        configurePresentationButton(gridView, "Grid view", true);
        configurePresentationButton(listView, "List view", false);
        gridView.setGraphic(JavaFxIconRenderer.create(new EditorIcon(EditorIcons.GRID, "Grid view")));
        listView.setGraphic(JavaFxIconRenderer.create(new EditorIcon(EditorIcons.LIST, "List view")));
        HBox viewButtons = new HBox(gridView, listView);
        viewButtons.getStyleClass().addAll("editor-collection-view-buttons", "editor-project-view-buttons");

        search.setPromptText(searchPlaceholder);
        search.setMinWidth(100.0);
        search.setPrefWidth(180.0);
        search.setDisable(true);
        search.getStyleClass().addAll("editor-collection-search", "editor-project-search");
        search.textProperty().addListener((ignored, previous, current) -> refreshItems());
        HBox toolbar = new HBox(8.0, breadcrumb, spacer, viewButtons, search);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().addAll("editor-collection-toolbar", "editor-project-toolbar");
        return toolbar;
    }

    private void configurePresentationButton(ToggleButton button, String name, boolean gridPresentation) {
        button.setAccessibleText(name);
        button.setTooltip(new Tooltip(name));
        button.setOnAction(ignored -> showPresentation(gridPresentation));
        button.getStyleClass().addAll("editor-collection-view-toggle", "editor-project-view-toggle");
    }

    private SplitPane createCategorizedBrowser(VBox browserContent) {
        navigation.setCellFactory(ignored -> new CategoryCell());
        navigation.setShowRoot(true);
        navigation.setMinWidth(0.0);
        navigation.getStyleClass().addAll("editor-collection-navigation-tree", "editor-project-tree");
        navigation.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selectionFeedbackSuppressionDepth == 0 && selected != null) {
                selectedCategory = selected.getValue().categoryId();
                refreshItems();
            }
        });
        VBox.setVgrow(navigation, Priority.ALWAYS);
        Label heading = new Label("Categories");
        heading.getStyleClass().addAll("editor-collection-categories-heading", "editor-project-categories-heading");
        VBox categoryPane = new VBox(heading, navigation);
        categoryPane.setMinWidth(180.0);
        categoryPane.setPrefWidth(230.0);
        categoryPane.getStyleClass().addAll("editor-collection-navigation", "editor-project-navigation");

        SplitPane browser = new SplitPane(categoryPane, browserContent);
        browser.setOrientation(Orientation.HORIZONTAL);
        browser.setDividerPositions(0.22);
        browser.getStyleClass().addAll("editor-collection-browser-split", "editor-project-browser-split");
        SplitPane.setResizableWithParent(categoryPane, false);
        return browser;
    }

    private void reload() {
        long requestedGeneration = ++generation;
        CompletionStage<EditorCollectionSnapshot<T>> requested = provider.snapshot();
        requested.whenComplete((loaded, failure) -> runOnApplicationThread(() -> {
            if (requestedGeneration != generation) {
                return;
            }
            if (failure != null) {
                showFailure(failure);
                return;
            }
            showSnapshot(Objects.requireNonNull(loaded, "provider snapshot"));
        }));
    }

    private void showSnapshot(EditorCollectionSnapshot<T> loaded) {
        boolean changedRoot = !snapshot.rootLabel().equals(loaded.rootLabel());
        snapshot = loaded;
        if (changedRoot) {
            selectedCategory = Optional.empty();
            search.clear();
        }
        search.setDisable(!snapshot.searchable());
        empty.setText(snapshot.emptyMessage());
        rebuildNavigation();
        refreshItems();
    }

    private void rebuildNavigation() {
        if (categories.isEmpty()) {
            return;
        }
        withoutSelectionFeedback(() -> {
            TreeItem<CategoryLocation> rootItem = new TreeItem<>(new CategoryLocation(
                    snapshot.rootLabel(), Optional.empty(), snapshot.elements().size(), rootIcon));
            for (EditorCollectionCategory category : categories) {
                long count = snapshot.elements().stream()
                        .map(provider::item)
                        .filter(item -> item.categoryId().equals(Optional.of(category.id())))
                        .count();
                rootItem.getChildren()
                        .add(new TreeItem<>(new CategoryLocation(
                                category.label(), Optional.of(category.id()), count, category.icon())));
            }
            rootItem.setExpanded(true);
            navigation.setRoot(rootItem);
            TreeItem<CategoryLocation> selected = rootItem.getChildren().stream()
                    .filter(item -> item.getValue().categoryId().equals(selectedCategory))
                    .findFirst()
                    .orElse(rootItem);
            navigation.getSelectionModel().select(selected);
        });
    }

    private void refreshItems() {
        List<T> visible = snapshot.elements().stream().filter(this::visible).toList();
        withoutSelectionFeedback(() -> {
            list.getItems().setAll(visible);
            cardGroup.getToggles().clear();
            grid.getChildren().setAll(visible.stream().map(this::createCard).toList());
            synchronizeSelection();
        });
        String location = selectedCategory
                .map(categoryById::get)
                .map(EditorCollectionCategory::label)
                .orElse(allItemsLabel);
        breadcrumb.setText(snapshot.rootLabel() + "  ›  " + location);
        refreshVisibility(visible.isEmpty());
    }

    private boolean visible(T element) {
        EditorCollectionItem item = provider.item(element);
        if (selectedCategory.isPresent() && !item.categoryId().equals(selectedCategory)) {
            return false;
        }
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        return searchableText(item).contains(query);
    }

    private static String searchableText(EditorCollectionItem item) {
        return String.join(
                        " ",
                        item.label(),
                        item.description().orElse(""),
                        item.detail().orElse(""),
                        item.tooltip().orElse(""))
                .toLowerCase(Locale.ROOT);
    }

    private void showPresentation(boolean showGrid) {
        showingGrid = showGrid;
        gridView.setSelected(showGrid);
        listView.setSelected(!showGrid);
        refreshVisibility(list.getItems().isEmpty());
        withoutSelectionFeedback(this::synchronizeSelection);
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
        card.setGraphic(createPresentation(item));
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
        card.getStyleClass().addAll("editor-collection-card", "editor-asset-card");
        return card;
    }

    private VBox createPresentation(EditorCollectionItem item) {
        Label name = new Label(item.label());
        name.setMinWidth(0.0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setPrefWidth(1.0);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setTooltip(new Tooltip(item.label()));
        name.getStyleClass().addAll("editor-collection-name", "editor-asset-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox heading = new HBox(6.0);
        item.icon()
                .map(icon -> JavaFxIconRenderer.create(icon, "editor-collection-marker", "editor-asset-marker"))
                .ifPresent(heading.getChildren()::add);
        heading.getChildren().add(name);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setMaxWidth(Double.MAX_VALUE);

        Label description = new Label(item.description().orElse(""));
        description.getStyleClass().addAll("editor-collection-description", "editor-asset-kind");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox decorations = new HBox(3.0);
        decorations.setAlignment(Pos.CENTER_RIGHT);
        decorations.getStyleClass().add("editor-item-decorations");
        for (EditorIcon decoration : item.decorations()) {
            decorations
                    .getChildren()
                    .add(JavaFxIconRenderer.create(
                            decoration, "editor-item-decoration", "editor-collection-decoration"));
        }
        HBox metadata = new HBox(6.0, description, spacer, decorations);
        metadata.setAlignment(Pos.CENTER_LEFT);
        metadata.setMaxWidth(Double.MAX_VALUE);

        Label detail = new Label(item.detail().orElse(""));
        detail.setMinWidth(0.0);
        detail.setMaxWidth(Double.MAX_VALUE);
        detail.setPrefWidth(1.0);
        detail.setTextOverrun(OverrunStyle.ELLIPSIS);
        detail.setTooltip(item.tooltip().map(Tooltip::new).orElse(null));
        detail.getStyleClass().addAll("editor-collection-detail", "editor-asset-source");
        VBox presentation = new VBox(4.0, heading, metadata, detail);
        presentation.setMaxWidth(Double.MAX_VALUE);
        presentation.getStyleClass().addAll("editor-collection-presentation", "editor-asset-presentation");
        return presentation;
    }

    private void applySelection(Optional<T> selection) {
        desiredSelection = Objects.requireNonNull(selection, "selection");
        runOnApplicationThread(() -> withoutSelectionFeedback(this::synchronizeSelection));
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
        provider.item(element).command().ifPresent(commandExecutor);
    }

    private void showFailure(Throwable failure) {
        snapshot = new EditorCollectionSnapshot<>(
                snapshot.rootLabel(), List.of(), false, "Unable to load view: " + failure.getMessage());
        rebuildNavigation();
        refreshItems();
    }

    private void withoutSelectionFeedback(Runnable action) {
        selectionFeedbackSuppressionDepth++;
        try {
            action.run();
        } finally {
            selectionFeedbackSuppressionDepth--;
        }
    }

    private static Map<String, EditorCollectionCategory> indexCategories(List<EditorCollectionCategory> categories) {
        Map<String, EditorCollectionCategory> indexed = new HashMap<>();
        for (EditorCollectionCategory category : categories) {
            if (indexed.put(category.id(), category) != null) {
                throw new IllegalArgumentException("duplicate collection category: " + category.id());
            }
        }
        return Map.copyOf(indexed);
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name);
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
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
            getStyleClass().addAll("editor-collection-list-cell", "editor-asset-list-cell");
        }

        @Override
        protected void updateItem(@Nullable T element, boolean cellEmpty) {
            super.updateItem(element, cellEmpty);
            setText(null);
            setGraphic(cellEmpty || element == null ? null : createPresentation(provider.item(element)));
        }
    }

    private final class CategoryCell extends TreeCell<CategoryLocation> {
        private CategoryCell() {
            getStyleClass().addAll("editor-collection-navigation-cell", "editor-project-tree-cell");
        }

        @Override
        protected void updateItem(@Nullable CategoryLocation item, boolean cellEmpty) {
            super.updateItem(item, cellEmpty);
            if (cellEmpty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label name = new Label(item.label());
            name.setMinWidth(0.0);
            name.setMaxWidth(Double.MAX_VALUE);
            name.setPrefWidth(1.0);
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            name.setTooltip(new Tooltip(item.label()));
            HBox.setHgrow(name, Priority.ALWAYS);
            Label count = new Label(Long.toString(item.count()));
            count.getStyleClass().add("editor-project-tree-count");
            HBox row = new HBox(7.0);
            item.icon()
                    .map(icon -> JavaFxIconRenderer.create(
                            icon, "editor-collection-navigation-icon", "editor-project-tree-marker"))
                    .ifPresent(row.getChildren()::add);
            row.getChildren().addAll(name, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getStyleClass().add("editor-project-tree-row");
            setText(null);
            setAccessibleText(item.label() + ", " + item.count() + " items");
            setGraphic(row);
        }
    }

    /** One rendered category location; an empty identity denotes the collection root. */
    private record CategoryLocation(String label, Optional<String> categoryId, long count, Optional<EditorIcon> icon) {}
}
