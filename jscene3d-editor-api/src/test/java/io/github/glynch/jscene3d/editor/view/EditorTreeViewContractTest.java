/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

final class EditorTreeViewContractTest {
    @Test
    void suppliesSemanticTreeDataThroughTheStandardTreeKind() {
        TestTreeProvider provider = new TestTreeProvider();
        EditorTreeView<String> view = new TestTreeView(provider);

        assertThat(view.kind()).isEqualTo(EditorTreeView.TREE_VIEW_KIND);
        assertThat(view.selectionModel()).isEmpty();
        assertThat(view.dataProvider()).isSameAs(provider);
        assertThat(view.dataProvider().roots().toCompletableFuture().join()).containsExactly("root");
        assertThat(view.dataProvider().children("root").toCompletableFuture().join())
                .containsExactly("child");
        assertThat(view.dataProvider().item("child")).isEqualTo(EditorTreeItem.leaf("child"));
    }

    @Test
    void publishesTreeInvalidationWithoutToolkitEvents() {
        TestTreeProvider provider = new TestTreeProvider();
        List<Optional<String>> invalidations = new ArrayList<>();
        EditorRegistration registration = provider.observeChanges(invalidations::add);

        provider.invalidate("root");
        registration.close();
        provider.invalidate("ignored");

        assertThat(invalidations).containsExactly(Optional.of("root"));
    }

    @Test
    void sharesTreeSelectionWithoutToolkitEvents() {
        TestTreeSelectionModel selection = new TestTreeSelectionModel();
        TestTreeView view = new TestTreeView(new TestTreeProvider(), selection);
        List<Optional<String>> selections = new ArrayList<>();
        EditorRegistration registration = view.selectionModel().orElseThrow().observe(selections::add);

        view.selectionModel().orElseThrow().select(Optional.of("child"));
        registration.close();
        view.selectionModel().orElseThrow().select(Optional.empty());

        assertThat(selections).containsExactly(Optional.empty(), Optional.of("child"));
        assertThat(selection.selection()).isEmpty();
    }

    @Test
    void retainsOptionalTreePresentation() {
        CommandId command = new CommandId("io.github.glynch.test.open-child");
        EditorTreeItem item = new EditorTreeItem(
                "Child",
                Optional.of("Source asset"),
                Optional.of("Open Child"),
                Optional.of("asset"),
                Optional.of(command),
                Optional.of("source-asset"),
                EditorTreeItemCollapsibleState.COLLAPSED);

        assertThat(item.description()).contains("Source asset");
        assertThat(item.tooltip()).contains("Open Child");
        assertThat(item.icon()).contains("asset");
        assertThat(item.command()).contains(command);
        assertThat(item.contextValue()).contains("source-asset");
        assertThat(item.collapsibleState()).isEqualTo(EditorTreeItemCollapsibleState.COLLAPSED);
        assertThat(EditorTreeItemCollapsibleState.values())
                .containsExactly(
                        EditorTreeItemCollapsibleState.NONE,
                        EditorTreeItemCollapsibleState.COLLAPSED,
                        EditorTreeItemCollapsibleState.EXPANDED);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesTreePresentation() {
        Optional<String> emptyText = Optional.empty();
        Optional<CommandId> emptyCommand = Optional.empty();

        assertThatThrownBy(() -> EditorTreeItem.leaf(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorTreeItem(
                        "Item",
                        null,
                        emptyText,
                        emptyText,
                        emptyCommand,
                        emptyText,
                        EditorTreeItemCollapsibleState.NONE))
                .withMessage("description");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorTreeItem(
                        "Item",
                        emptyText,
                        null,
                        emptyText,
                        emptyCommand,
                        emptyText,
                        EditorTreeItemCollapsibleState.NONE))
                .withMessage("tooltip");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorTreeItem(
                        "Item",
                        emptyText,
                        emptyText,
                        null,
                        emptyCommand,
                        emptyText,
                        EditorTreeItemCollapsibleState.NONE))
                .withMessage("icon");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorTreeItem(
                        "Item", emptyText, emptyText, emptyText, null, emptyText, EditorTreeItemCollapsibleState.NONE))
                .withMessage("command");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorTreeItem(
                        "Item",
                        emptyText,
                        emptyText,
                        emptyText,
                        emptyCommand,
                        null,
                        EditorTreeItemCollapsibleState.NONE))
                .withMessage("contextValue");
        assertThatNullPointerException()
                .isThrownBy(() ->
                        new EditorTreeItem("Item", emptyText, emptyText, emptyText, emptyCommand, emptyText, null))
                .withMessage("collapsibleState");
    }

    private static final class TestTreeView implements EditorTreeView<String> {
        private final EditorTreeDataProvider<String> provider;
        private final Optional<EditorTreeSelectionModel<String>> selection;

        private TestTreeView(EditorTreeDataProvider<String> provider) {
            this.provider = provider;
            this.selection = Optional.empty();
        }

        private TestTreeView(EditorTreeDataProvider<String> provider, EditorTreeSelectionModel<String> selection) {
            this.provider = provider;
            this.selection = Optional.of(selection);
        }

        @Override
        public ViewId id() {
            return new ViewId("io.github.glynch.test.tree");
        }

        @Override
        public String title() {
            return "Test Tree";
        }

        @Override
        public EditorTreeDataProvider<String> dataProvider() {
            return provider;
        }

        @Override
        public Optional<EditorTreeSelectionModel<String>> selectionModel() {
            return selection;
        }
    }

    private static final class TestTreeSelectionModel implements EditorTreeSelectionModel<String> {
        private Optional<String> selection = Optional.empty();
        private Consumer<Optional<String>> listener = ignored -> {};

        @Override
        public Optional<String> selection() {
            return selection;
        }

        @Override
        public void select(Optional<String> updated) {
            selection = updated;
            listener.accept(updated);
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<String>> newListener) {
            listener = newListener;
            listener.accept(selection);
            return () -> listener = ignored -> {};
        }
    }

    private static final class TestTreeProvider implements EditorTreeDataProvider<String> {
        private Consumer<Optional<String>> listener = ignored -> {};

        @Override
        public CompletionStage<List<String>> roots() {
            return CompletableFuture.completedFuture(List.of("root"));
        }

        @Override
        public CompletionStage<List<String>> children(String parent) {
            return CompletableFuture.completedFuture("root".equals(parent) ? List.of("child") : List.of());
        }

        @Override
        public EditorTreeItem item(String element) {
            return EditorTreeItem.leaf(element);
        }

        @Override
        public EditorRegistration observeChanges(Consumer<Optional<String>> newListener) {
            listener = newListener;
            return () -> listener = ignored -> {};
        }

        private void invalidate(String element) {
            listener.accept(Optional.of(element));
        }
    }
}
