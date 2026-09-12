/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.hierarchy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKindId;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class HierarchyExtensionTest {
    @Test
    void contributesAProjectDrivenTreeAndSharesSelection() {
        EditorProjectContext projects = new EditorProjectContext();
        EditorSelectionContext editorSelection = new EditorSelectionContext();
        EditorExtensionHost host = new EditorExtensionHost(projects, editorSelection);
        List<List<EditorViewContribution>> snapshots = new ArrayList<>();
        List<List<EditorActivityContribution>> activitySnapshots = new ArrayList<>();
        host.observeViews(snapshots::add);
        host.observeActivities(activitySnapshots::add);

        host.activate(new HierarchyExtension(projects));
        assertThat(snapshots.getLast()).isEmpty();
        assertThat(activitySnapshots.getLast()).isEmpty();

        EditorHierarchyNode child = node("Player", EditorHierarchyNode.Kind.LOCAL_ENTITY, List.of(), true);
        EditorHierarchyNode generated = new EditorHierarchyNode(
                EditorHierarchyNode.Kind.GENERATED_ENTITY,
                "Generated Door",
                Optional.empty(),
                Optional.empty(),
                false,
                selection("Generated Door", EditorSelectionKinds.GENERATED_ENTITY),
                List.of());
        EditorHierarchyNode root = node("MAP01", EditorHierarchyNode.Kind.WORLD, List.of(child, generated));
        projects.showProject(
                new EditorProject("io.github.glynch.test", "Test", URI.create("file:///test/")), root, List.of());

        EditorViewContribution contribution = snapshots.getLast().getFirst();
        assertThat(activitySnapshots.getLast())
                .singleElement()
                .extracting(EditorActivityContribution::title)
                .isEqualTo("Scene");
        assertThat(contribution.container()).isEqualTo(EditorViewContainers.PRIMARY_SIDEBAR);
        assertThat(contribution.view().id()).isEqualTo(HierarchyExtension.VIEW_ID);
        assertThat(contribution.view()).isInstanceOf(EditorTreeView.class);
        @SuppressWarnings("unchecked")
        EditorTreeView<EditorHierarchyNode> view = (EditorTreeView<EditorHierarchyNode>) contribution.view();
        assertThat(view.dataProvider().roots().toCompletableFuture().join()).containsExactly(root);
        assertThat(view.dataProvider().children(root).toCompletableFuture().join())
                .containsExactly(child, generated);
        assertThat(view.dataProvider().item(root).collapsibleState())
                .isEqualTo(EditorTreeItemCollapsibleState.EXPANDED);
        assertThat(view.dataProvider().item(child).collapsibleState()).isEqualTo(EditorTreeItemCollapsibleState.NONE);
        assertThat(view.dataProvider().item(root).icon()).contains(new EditorIcon(EditorIcons.WORLD, "World"));
        assertThat(view.dataProvider().item(child).icon()).contains(new EditorIcon(EditorIcons.ENTITY, "Local entity"));
        assertThat(view.dataProvider().item(child).decorations())
                .containsExactly(new EditorIcon(EditorIcons.MODIFIED, "Modified"));
        assertThat(view.dataProvider().item(generated).decorations())
                .containsExactly(
                        new EditorIcon(EditorIcons.READ_ONLY, "Generated read-only entity"),
                        new EditorIcon(EditorIcons.DISABLED, "Initially disabled"));
        assertThat(view.dataProvider().item(generated).tooltip())
                .contains("Generated entity · read-only · initially disabled");
        assertThat(editorSelection.current()).contains(child.selection());

        view.selectionModel().orElseThrow().select(Optional.of(root));
        assertThat(editorSelection.current()).contains(root.selection());

        projects.clear();

        assertThat(view.dataProvider().roots().toCompletableFuture().join()).isEmpty();
        assertThat(editorSelection.current()).isEmpty();
        assertThat(snapshots.getLast()).isEmpty();
        assertThat(activitySnapshots.getLast()).isEmpty();
        host.close();
        assertThat(snapshots.getLast()).isEmpty();
    }

    private static EditorHierarchyNode node(
            String label, EditorHierarchyNode.Kind kind, List<EditorHierarchyNode> children) {
        return node(label, kind, children, false);
    }

    private static EditorHierarchyNode node(
            String label, EditorHierarchyNode.Kind kind, List<EditorHierarchyNode> children, boolean modified) {
        return new EditorHierarchyNode(
                kind,
                label,
                Optional.empty(),
                Optional.empty(),
                new EditorHierarchyNode.AuthoringState(true, modified),
                selection(label, selectionKind(kind)),
                children);
    }

    private static EditorSelection selection(String label, EditorSelectionKindId selectionKind) {
        EditorDetails details = new EditorDetails(label, "Test", "test", label, List.of(), List.of());
        return new EditorSelection(selectionKind, label, details);
    }

    private static EditorSelectionKindId selectionKind(EditorHierarchyNode.Kind kind) {
        return switch (kind) {
            case WORLD -> EditorSelectionKinds.WORLD;
            case LOCAL_ENTITY -> EditorSelectionKinds.LOCAL_ENTITY;
            case PLACEMENT -> EditorSelectionKinds.PLACEMENT;
            case GENERATED_ENTITY -> EditorSelectionKinds.GENERATED_ENTITY;
        };
    }
}
