/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorTreeDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorTreeItem;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeSelectionModel;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.jspecify.annotations.Nullable;

/** Workbench-owned JavaFX adapter for any toolkit-independent tree view contribution. */
final class JavaFxTreeViewAdapter<T> implements AutoCloseable {
    private final EditorTreeDataProvider<T> provider;
    private final Optional<EditorTreeSelectionModel<T>> selectionModel;
    private final TreeView<T> tree = new TreeView<>();
    private final Map<T, TreeItem<T>> renderedItems = new HashMap<>();
    private final EditorRegistration dataRegistration;
    private final Optional<EditorRegistration> selectionRegistration;
    private Optional<T> desiredSelection = Optional.empty();
    private long generation;

    private int selectionFeedbackSuppressionDepth;

    /** Creates and begins observing one logical tree view. */
    JavaFxTreeViewAdapter(EditorTreeView<T> view, Consumer<CommandId> commandExecutor) {
        EditorTreeView<T> logicalView = Objects.requireNonNull(view, "view");
        this.provider = Objects.requireNonNull(logicalView.dataProvider(), "view.dataProvider()");
        this.selectionModel = Objects.requireNonNull(logicalView.selectionModel(), "view.selectionModel()");
        Consumer<CommandId> commands = Objects.requireNonNull(commandExecutor, "commandExecutor");
        tree.setShowRoot(false);
        tree.getStyleClass().add(EditorStyleClasses.EDITOR_TREE_VIEW);
        tree.setCellFactory(ignored -> new LogicalTreeCell());
        tree.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selectionFeedbackSuppressionDepth == 0) {
                selectionModel.ifPresent(
                        model -> model.select(Optional.ofNullable(selected).map(TreeItem::getValue)));
            }
        });
        tree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Optional.ofNullable(tree.getSelectionModel().getSelectedItem())
                        .map(TreeItem::getValue)
                        .map(provider::item)
                        .flatMap(EditorTreeItem::command)
                        .ifPresent(commands);
            }
        });
        dataRegistration = provider.observeChanges(ignored -> reload());
        selectionRegistration = selectionModel.map(model -> model.observe(this::applySelection));
        reload();
    }

    /** Returns the rendered JavaFX node owned by this adapter. */
    Node node() {
        return tree;
    }

    /** Requests keyboard focus for this rendered tree. */
    void requestFocus() {
        tree.requestFocus();
    }

    @Override
    public void close() {
        dataRegistration.close();
        selectionRegistration.ifPresent(EditorRegistration::close);
        generation++;
        renderedItems.clear();
        withoutSelectionFeedback(() -> tree.setRoot(null));
    }

    private void reload() {
        long requestedGeneration = ++generation;
        CompletionStage<List<T>> roots = provider.roots();
        roots.whenComplete((loaded, failure) -> runOnApplicationThread(() -> {
            if (requestedGeneration != generation) {
                return;
            }
            if (failure != null) {
                showFailure(failure);
                return;
            }
            showRoots(List.copyOf(loaded));
        }));
    }

    private void showRoots(List<T> roots) {
        withoutSelectionFeedback(() -> {
            renderedItems.clear();
            tree.setShowRoot(false);
            TreeItem<T> syntheticRoot = new TreeItem<>();
            syntheticRoot.setExpanded(true);
            syntheticRoot
                    .getChildren()
                    .setAll(roots.stream().map(this::createTreeItem).toList());
            tree.setRoot(syntheticRoot);
            synchronizeRenderedSelection();
        });
    }

    private TreeItem<T> createTreeItem(T element) {
        EditorTreeItem presentation = Objects.requireNonNull(provider.item(element), "provider.item(element)");
        LazyTreeItem item = new LazyTreeItem(element, presentation);
        renderedItems.put(element, item);
        item.setExpanded(presentation.collapsibleState() == EditorTreeItemCollapsibleState.EXPANDED);
        return item;
    }

    private void applySelection(Optional<T> selection) {
        desiredSelection = Objects.requireNonNull(selection, "selection");
        runOnApplicationThread(() -> withoutSelectionFeedback(this::synchronizeRenderedSelection));
    }

    private void synchronizeRenderedSelection() {
        Optional<TreeItem<T>> rendered = desiredSelection.map(renderedItems::get);
        if (rendered.isPresent()) {
            tree.getSelectionModel().select(rendered.orElseThrow());
        } else {
            tree.getSelectionModel().clearSelection();
        }
    }

    private void withoutSelectionFeedback(Runnable action) {
        selectionFeedbackSuppressionDepth++;
        try {
            action.run();
        } finally {
            selectionFeedbackSuppressionDepth--;
        }
    }

    private void showFailure(Throwable failure) {
        withoutSelectionFeedback(() -> {
            renderedItems.clear();
            @SuppressWarnings("NullAway")
            TreeItem<T> failureRoot = new TreeItem<>(null, new Label("Unable to load view: " + failure.getMessage()));
            tree.setRoot(failureRoot);
            tree.setShowRoot(true);
        });
    }

    private static void runOnApplicationThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private final class LazyTreeItem extends TreeItem<T> {
        private final boolean collapsible;
        private boolean loaded;

        private LazyTreeItem(T element, EditorTreeItem presentation) {
            super(element);
            collapsible = presentation.collapsibleState() != EditorTreeItemCollapsibleState.NONE;
        }

        @Override
        public javafx.collections.ObservableList<TreeItem<T>> getChildren() {
            if (collapsible && !loaded) {
                loadChildren();
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            return !collapsible;
        }

        private void loadChildren() {
            loaded = true;
            long requestedGeneration = generation;
            provider.children(getValue())
                    .whenComplete((children, failure) -> runOnApplicationThread(() -> {
                        if (requestedGeneration != generation) {
                            return;
                        }
                        if (failure != null) {
                            getChildren().clear();
                            return;
                        }
                        withoutSelectionFeedback(() -> {
                            getChildren()
                                    .setAll(List.copyOf(children).stream()
                                            .map(JavaFxTreeViewAdapter.this::createTreeItem)
                                            .toList());
                            synchronizeRenderedSelection();
                        });
                    }));
        }
    }

    private final class LogicalTreeCell extends TreeCell<T> {
        @Override
        protected void updateItem(@Nullable T element, boolean empty) {
            super.updateItem(element, empty);
            if (empty || element == null) {
                setText(null);
                setGraphic(null);
                setTooltip(null);
                setAccessibleText(null);
                return;
            }
            EditorTreeItem presentation = provider.item(element);
            Label name = new Label(presentation.label());
            name.setMinWidth(0.0);
            HBox.setHgrow(name, Priority.ALWAYS);
            HBox row = new HBox(7.0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            presentation
                    .icon()
                    .map(icon -> JavaFxIconRenderer.create(icon, EditorStyleClasses.EDITOR_TREE_ITEM_ICON))
                    .ifPresent(row.getChildren()::add);
            row.getChildren().add(name);
            presentation.description().ifPresent(description -> {
                Label detail = new Label(description);
                detail.getStyleClass().add(EditorStyleClasses.EDITOR_TREE_ITEM_DESCRIPTION);
                row.getChildren().add(detail);
            });
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().add(spacer);
            for (EditorIcon decoration : presentation.decorations()) {
                row.getChildren()
                        .add(JavaFxIconRenderer.create(
                                decoration,
                                EditorStyleClasses.EDITOR_ITEM_DECORATION,
                                EditorStyleClasses.EDITOR_TREE_ITEM_DECORATION));
            }
            row.getStyleClass().add(EditorStyleClasses.EDITOR_TREE_ITEM_ROW);
            setText(null);
            setGraphic(row);
            setTooltip(presentation.tooltip().map(Tooltip::new).orElse(null));
            setAccessibleText(accessibleText(presentation));
        }

        private String accessibleText(EditorTreeItem presentation) {
            List<String> parts = new ArrayList<>();
            parts.add(presentation.label());
            presentation.description().ifPresent(parts::add);
            parts.addAll(
                    presentation.decorations().stream().map(EditorIcon::tooltip).toList());
            return String.join(", ", parts);
        }
    }
}
