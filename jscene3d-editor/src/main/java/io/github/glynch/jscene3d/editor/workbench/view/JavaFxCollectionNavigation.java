/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.view.EditorCollectionCategory;
import io.github.glynch.jscene3d.editor.view.EditorCollectionDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSnapshot;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** Owns collection category navigation and publishes semantic category identities. */
final class JavaFxCollectionNavigation<T> {
    private final List<EditorCollectionCategory> categories;
    private final Optional<EditorIcon> rootIcon;
    private final EditorCollectionDataProvider<T> provider;
    private final JavaFxIconRenderer icons;
    private final Consumer<Optional<String>> selectionListener;
    private final TreeView<CategoryLocation> tree = new TreeView<>();
    private final VBox root;
    private int selectionFeedbackSuppressionDepth;

    JavaFxCollectionNavigation(
            String title,
            List<EditorCollectionCategory> categories,
            Optional<EditorIcon> rootIcon,
            EditorCollectionDataProvider<T> provider,
            JavaFxIconRenderer icons,
            Consumer<Optional<String>> selectionListener) {
        this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
        this.rootIcon = Objects.requireNonNull(rootIcon, "rootIcon");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.icons = Objects.requireNonNull(icons, "icons");
        this.selectionListener = Objects.requireNonNull(selectionListener, "selectionListener");
        tree.setAccessibleText(Objects.requireNonNull(title, "title") + " categories");
        configureTree();
        Label heading = new Label("Categories");
        heading.getStyleClass()
                .addAll(
                        EditorStyleClasses.EDITOR_COLLECTION_CATEGORIES_HEADING,
                        EditorStyleClasses.EDITOR_PROJECT_CATEGORIES_HEADING);
        root = new VBox(heading, tree);
        root.setMinWidth(180.0);
        root.setPrefWidth(230.0);
        root.getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_NAVIGATION, EditorStyleClasses.EDITOR_PROJECT_NAVIGATION);
    }

    Node node() {
        return root;
    }

    void requestFocus() {
        tree.requestFocus();
    }

    void show(EditorCollectionSnapshot<T> snapshot, Optional<String> selectedCategory) {
        if (categories.isEmpty()) {
            return;
        }
        EditorCollectionSnapshot<T> current = Objects.requireNonNull(snapshot, "snapshot");
        Optional<String> selected = Objects.requireNonNull(selectedCategory, "selectedCategory");
        withoutSelectionFeedback(() -> {
            TreeItem<CategoryLocation> rootItem = new TreeItem<>(new CategoryLocation(
                    current.rootLabel(), Optional.empty(), current.elements().size(), rootIcon));
            for (EditorCollectionCategory category : categories) {
                long count = current.elements().stream()
                        .map(provider::item)
                        .filter(item -> item.categoryId().equals(Optional.of(category.id())))
                        .count();
                rootItem.getChildren()
                        .add(new TreeItem<>(new CategoryLocation(
                                category.label(), Optional.of(category.id()), count, category.icon())));
            }
            rootItem.setExpanded(true);
            tree.setRoot(rootItem);
            TreeItem<CategoryLocation> selectedItem = rootItem.getChildren().stream()
                    .filter(item -> item.getValue().categoryId().equals(selected))
                    .findFirst()
                    .orElse(rootItem);
            tree.getSelectionModel().select(selectedItem);
        });
    }

    void clear() {
        withoutSelectionFeedback(() -> tree.setRoot(null));
    }

    private void configureTree() {
        tree.setAccessibleHelp(
                "Use Up and Down Arrow keys to move between categories, Right Arrow to expand, and Left Arrow to collapse");
        tree.setCellFactory(ignored -> new CategoryCell());
        tree.setShowRoot(true);
        tree.setMinWidth(0.0);
        tree.getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_COLLECTION_NAVIGATION_TREE, EditorStyleClasses.EDITOR_PROJECT_TREE);
        tree.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selectionFeedbackSuppressionDepth == 0 && selected != null) {
                selectionListener.accept(selected.getValue().categoryId());
            }
        });
        VBox.setVgrow(tree, Priority.ALWAYS);
    }

    private void withoutSelectionFeedback(Runnable action) {
        selectionFeedbackSuppressionDepth++;
        try {
            action.run();
        } finally {
            selectionFeedbackSuppressionDepth--;
        }
    }

    private final class CategoryCell extends TreeCell<CategoryLocation> {
        private CategoryCell() {
            getStyleClass()
                    .addAll(
                            EditorStyleClasses.EDITOR_COLLECTION_NAVIGATION_CELL,
                            EditorStyleClasses.EDITOR_PROJECT_TREE_CELL);
        }

        @Override
        protected void updateItem(@Nullable CategoryLocation item, boolean cellEmpty) {
            super.updateItem(item, cellEmpty);
            if (cellEmpty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label name = categoryName(item);
            Label count = new Label(Long.toString(item.count()));
            count.getStyleClass().add(EditorStyleClasses.EDITOR_PROJECT_TREE_COUNT);
            HBox row = new HBox(7.0);
            item.icon()
                    .map(icon -> icons.create(
                            icon,
                            EditorStyleClasses.EDITOR_COLLECTION_NAVIGATION_ICON,
                            EditorStyleClasses.EDITOR_PROJECT_TREE_MARKER))
                    .ifPresent(row.getChildren()::add);
            row.getChildren().addAll(name, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getStyleClass().add(EditorStyleClasses.EDITOR_PROJECT_TREE_ROW);
            setText(null);
            setAccessibleText(item.label() + ", " + item.count() + " items");
            setGraphic(row);
        }

        private Label categoryName(CategoryLocation item) {
            Label name = new Label(item.label());
            name.setMinWidth(0.0);
            name.setMaxWidth(Double.MAX_VALUE);
            name.setPrefWidth(1.0);
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            name.setTooltip(new Tooltip(item.label()));
            HBox.setHgrow(name, Priority.ALWAYS);
            return name;
        }
    }

    private record CategoryLocation(String label, Optional<String> categoryId, long count, Optional<EditorIcon> icon) {}
}
