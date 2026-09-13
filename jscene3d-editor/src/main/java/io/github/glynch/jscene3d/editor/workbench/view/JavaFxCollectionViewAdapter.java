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
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Coordinates a logical collection view with focused JavaFX navigation, toolbar, and item-presentation modules. */
final class JavaFxCollectionViewAdapter<T> implements AutoCloseable {
    private final EditorCollectionDataProvider<T> provider;
    private final List<EditorCollectionCategory> categories;
    private final String allItemsLabel;
    private final Map<String, EditorCollectionCategory> categoryById;
    private final VBox root = new VBox();
    private final JavaFxCollectionItems<T> items;
    private final JavaFxCollectionNavigation<T> navigation;
    private final JavaFxCollectionToolbar toolbar;
    private final EditorRegistration dataRegistration;
    private final Optional<EditorRegistration> selectionRegistration;

    private EditorCollectionSnapshot<T> snapshot =
            new EditorCollectionSnapshot<>("Loading", List.of(), false, "Loading collection…");
    private Optional<String> selectedCategory = Optional.empty();
    private long generation;

    /** Creates and begins observing one logical collection view. */
    JavaFxCollectionViewAdapter(
            EditorCollectionView<T> view, BiConsumer<CommandId, Object> commandExecutor, JavaFxIconRenderer icons) {
        EditorCollectionView<T> logicalView = Objects.requireNonNull(view, "view");
        provider = Objects.requireNonNull(logicalView.dataProvider(), "view.dataProvider()");
        categories = List.copyOf(Objects.requireNonNull(logicalView.categories(), "view.categories()"));
        Optional<EditorCollectionSelectionModel<T>> selectionModel =
                Objects.requireNonNull(logicalView.selectionModel(), "view.selectionModel()");
        JavaFxIconRenderer iconRenderer = Objects.requireNonNull(icons, "icons");
        allItemsLabel = requireText(logicalView.allItemsLabel(), "view.allItemsLabel()");
        Optional<EditorIcon> rootIcon = Objects.requireNonNull(logicalView.rootIcon(), "view.rootIcon()");
        String searchPlaceholder = requireText(logicalView.searchPlaceholder(), "view.searchPlaceholder()");
        categoryById = indexCategories(categories);
        items = new JavaFxCollectionItems<>(provider, selectionModel, commandExecutor, iconRenderer);
        navigation = new JavaFxCollectionNavigation<>(
                logicalView.title(), categories, rootIcon, provider, iconRenderer, this::selectCategory);
        toolbar = new JavaFxCollectionToolbar(searchPlaceholder, iconRenderer, this::refreshItems, items::showGrid);
        configureView();
        dataRegistration = provider.observeChanges(this::reload);
        selectionRegistration = selectionModel.map(model -> model.observe(items::select));
        reload();
    }

    /** Returns the rendered JavaFX node owned by this adapter. */
    Node node() {
        return root;
    }

    /** Requests keyboard focus for the active collection presentation. */
    void requestFocus() {
        if (categories.isEmpty()) {
            items.requestFocus();
        } else {
            navigation.requestFocus();
        }
    }

    @Override
    public void close() {
        dataRegistration.close();
        selectionRegistration.ifPresent(EditorRegistration::close);
        generation++;
        items.clear();
        navigation.clear();
    }

    private void configureView() {
        VBox browserContent = new VBox(toolbar.node(), items.node());
        VBox.setVgrow(items.node(), Priority.ALWAYS);
        browserContent
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_CONTENT, EditorStyleClasses.EDITOR_PROJECT_CONTENT);
        Node browser = categories.isEmpty() ? browserContent : categorizedBrowser(browserContent);
        VBox.setVgrow(browser, Priority.ALWAYS);
        root.getChildren().setAll(browser);
        root.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_PANEL,
                        EditorStyleClasses.EDITOR_COLLECTION_PANEL,
                        EditorStyleClasses.EDITOR_PROJECT_PANEL);
        items.showGrid(true);
    }

    private Node categorizedBrowser(Node browserContent) {
        SplitPane browser = new SplitPane(navigation.node(), browserContent);
        browser.setOrientation(Orientation.HORIZONTAL);
        browser.setDividerPositions(0.22);
        browser.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_COLLECTION_BROWSER_SPLIT,
                        EditorStyleClasses.EDITOR_PROJECT_BROWSER_SPLIT);
        SplitPane.setResizableWithParent(navigation.node(), false);
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
            toolbar.clearSearch();
        }
        toolbar.setSearchable(snapshot.searchable());
        items.setEmptyMessage(snapshot.emptyMessage());
        navigation.show(snapshot, selectedCategory);
        refreshItems();
    }

    private void selectCategory(Optional<String> category) {
        selectedCategory = Objects.requireNonNull(category, "category");
        refreshItems();
    }

    private void refreshItems() {
        List<T> visible = snapshot.elements().stream().filter(this::visible).toList();
        items.show(visible);
        String location = selectedCategory
                .map(categoryById::get)
                .map(EditorCollectionCategory::label)
                .orElse(allItemsLabel);
        toolbar.showLocation(snapshot.rootLabel(), location);
    }

    private boolean visible(T element) {
        EditorCollectionItem item = provider.item(element);
        if (selectedCategory.isPresent() && !item.categoryId().equals(selectedCategory)) {
            return false;
        }
        String query = toolbar.query().strip().toLowerCase(Locale.ROOT);
        return query.isEmpty() || searchableText(item).contains(query);
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

    private void showFailure(Throwable failure) {
        snapshot = new EditorCollectionSnapshot<>(
                snapshot.rootLabel(), List.of(), false, "Unable to load view: " + failure.getMessage());
        navigation.show(snapshot, selectedCategory);
        refreshItems();
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
}
