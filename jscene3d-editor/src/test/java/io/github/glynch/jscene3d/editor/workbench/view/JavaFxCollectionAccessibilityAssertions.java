/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorCollectionCategory;
import io.github.glynch.jscene3d.editor.view.EditorCollectionDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorCollectionItem;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSnapshot;
import io.github.glynch.jscene3d.editor.view.EditorCollectionView;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeView;

/** Assertions for collection accessibility that must run on the JavaFX application thread. */
public final class JavaFxCollectionAccessibilityAssertions {
    private JavaFxCollectionAccessibilityAssertions() {}

    /** Verifies that a categorized collection exposes and initializes its keyboard navigation tree. */
    public static void assertAccessibleProjectCategories() {
        try (JavaFxCollectionViewAdapter<String> adapter = new JavaFxCollectionViewAdapter<>(
                new TestCollectionView(), ignored -> {}, JavaFxIconRenderer.builtIn())) {
            TreeView<?> navigation = findTree(adapter.node());

            assertThat(navigation.getAccessibleText()).isEqualTo("Project categories");
            assertThat(navigation.getAccessibleHelp()).contains("Up and Down Arrow", "Right Arrow", "Left Arrow");
            assertThat(navigation.isFocusTraversable()).isTrue();
            assertThat(navigation.getSelectionModel().getSelectedIndex()).isZero();
        }
    }

    private static TreeView<?> findTree(Node node) {
        if (node instanceof TreeView<?> tree) {
            return tree;
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream()
                    .map(JavaFxCollectionAccessibilityAssertions::findTreeOrNull)
                    .flatMap(Optional::stream)
                    .findFirst()
                    .orElseThrow();
        }
        throw new IllegalStateException("collection view does not contain a category tree");
    }

    private static Optional<TreeView<?>> findTreeOrNull(Node node) {
        if (node instanceof TreeView<?> tree) {
            return Optional.of(tree);
        }
        if (node instanceof SplitPane split) {
            return split.getItems().stream()
                    .map(JavaFxCollectionAccessibilityAssertions::findTreeOrNull)
                    .flatMap(Optional::stream)
                    .findFirst();
        }
        if (node instanceof Parent parent) {
            return parent.getChildrenUnmodifiable().stream()
                    .map(JavaFxCollectionAccessibilityAssertions::findTreeOrNull)
                    .flatMap(Optional::stream)
                    .findFirst();
        }
        return Optional.empty();
    }

    private static final class TestCollectionView implements EditorCollectionView<String> {
        private final EditorCollectionDataProvider<String> provider = new TestCollectionProvider();

        @Override
        public ViewId id() {
            return new ViewId("io.github.glynch.test.collection");
        }

        @Override
        public String title() {
            return "Project";
        }

        @Override
        public EditorCollectionDataProvider<String> dataProvider() {
            return provider;
        }

        @Override
        public List<EditorCollectionCategory> categories() {
            return List.of(new EditorCollectionCategory("worlds", "Worlds", Optional.empty()));
        }
    }

    private static final class TestCollectionProvider implements EditorCollectionDataProvider<String> {
        @Override
        public CompletionStage<EditorCollectionSnapshot<String>> snapshot() {
            return CompletableFuture.completedFuture(
                    new EditorCollectionSnapshot<>("Test Project", List.of("world"), true, "No assets"));
        }

        @Override
        public EditorCollectionItem item(String element) {
            return new EditorCollectionItem(
                    "World",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("worlds"),
                    List.of(),
                    Optional.empty(),
                    Optional.empty());
        }

        @Override
        public EditorRegistration observeChanges(Runnable listener) {
            return () -> {};
        }
    }
}
