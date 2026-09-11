/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.hierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.EditorInspectorView;
import io.github.glynch.jscene3d.editor.EditorSelection;
import io.github.glynch.jscene3d.editor.EditorSelectionModel;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class HierarchyExtensionTest {
    @Test
    void contributesAProjectDrivenTreeAndSharesSelection() {
        EditorProjectContext projects = new EditorProjectContext();
        EditorSelectionModel editorSelection = new EditorSelectionModel();
        EditorExtensionHost host = new EditorExtensionHost(projects);
        List<List<EditorViewContribution>> snapshots = new ArrayList<>();
        host.observeViews(snapshots::add);

        host.activate(new HierarchyExtension(projects, editorSelection));

        EditorViewContribution contribution = snapshots.getLast().getFirst();
        assertThat(contribution.container()).isEqualTo(EditorViewContainers.PRIMARY_SIDEBAR);
        assertThat(contribution.view().id()).isEqualTo(HierarchyExtension.VIEW_ID);
        assertThat(contribution.view()).isInstanceOf(EditorTreeView.class);
        @SuppressWarnings("unchecked")
        EditorTreeView<EditorHierarchyNode> view = (EditorTreeView<EditorHierarchyNode>) contribution.view();
        assertThat(view.dataProvider().roots().toCompletableFuture().join()).isEmpty();

        EditorHierarchyNode child = node("Player", EditorSelection.Kind.LOCAL_ENTITY, List.of());
        EditorHierarchyNode root = node("MAP01", EditorSelection.Kind.WORLD, List.of(child));
        projects.showProject(
                new EditorProject("io.github.glynch.test", "Test", URI.create("file:///test/")), root, List.of());

        assertThat(view.dataProvider().roots().toCompletableFuture().join()).containsExactly(root);
        assertThat(view.dataProvider().children(root).toCompletableFuture().join())
                .containsExactly(child);
        assertThat(view.dataProvider().item(root).collapsibleState())
                .isEqualTo(EditorTreeItemCollapsibleState.EXPANDED);
        assertThat(view.dataProvider().item(child).collapsibleState()).isEqualTo(EditorTreeItemCollapsibleState.NONE);
        assertThat(editorSelection.selection()).contains(child.selection());

        view.selectionModel().orElseThrow().select(Optional.of(root));
        assertThat(editorSelection.selection()).contains(root.selection());

        projects.clear();

        assertThat(view.dataProvider().roots().toCompletableFuture().join()).isEmpty();
        assertThat(editorSelection.selection()).isEmpty();
        host.close();
        assertThat(snapshots.getLast()).isEmpty();
    }

    private static EditorHierarchyNode node(
            String label, EditorSelection.Kind selectionKind, List<EditorHierarchyNode> children) {
        EditorInspectorView inspector = new EditorInspectorView(label, "Test", "test", label, false, List.of());
        EditorSelection selection = new EditorSelection(selectionKind, label, inspector);
        return new EditorHierarchyNode(
                kind(selectionKind), label, Optional.empty(), Optional.empty(), true, selection, children);
    }

    private static EditorHierarchyNode.Kind kind(EditorSelection.Kind selectionKind) {
        return switch (selectionKind) {
            case WORLD -> EditorHierarchyNode.Kind.WORLD;
            case LOCAL_ENTITY -> EditorHierarchyNode.Kind.LOCAL_ENTITY;
            case PLACEMENT -> EditorHierarchyNode.Kind.PLACEMENT;
            case GENERATED_ENTITY -> EditorHierarchyNode.Kind.GENERATED_ENTITY;
            case ASSET -> throw new IllegalArgumentException("Assets are not hierarchy entries");
        };
    }
}
