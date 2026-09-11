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

        private TestTreeView(EditorTreeDataProvider<String> provider) {
            this.provider = provider;
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
