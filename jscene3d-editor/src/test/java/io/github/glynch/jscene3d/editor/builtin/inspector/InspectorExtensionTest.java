/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.inspector;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorDetailsView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class InspectorExtensionTest {
    @Test
    void contributesSelectionDrivenDetailsToTheSecondarySidebar() {
        EditorSelectionContext selections = new EditorSelectionContext();
        EditorProjectContext projects = new EditorProjectContext();
        EditorExtensionHost host = new EditorExtensionHost(projects, selections);
        List<List<EditorViewContribution>> snapshots = new ArrayList<>();
        host.observeViews(snapshots::add);

        host.activate(new InspectorExtension());
        assertThat(snapshots.getLast()).isEmpty();

        EditorDetails projectDetails = new EditorDetails("World", "World", "world", "world", List.of(), List.of());
        EditorSelection projectSelection = new EditorSelection(EditorSelectionKinds.WORLD, "world", projectDetails);
        EditorHierarchyNode hierarchy = new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                "World",
                Optional.empty(),
                Optional.empty(),
                true,
                projectSelection,
                List.of());
        projects.showProject(
                new EditorProject("io.github.glynch.test", "Test", URI.create("file:///test/")), hierarchy, List.of());

        EditorViewContribution contribution = snapshots.getLast().getFirst();
        assertThat(contribution.container()).isEqualTo(EditorViewContainers.SECONDARY_SIDEBAR);
        assertThat(contribution.view().id()).isEqualTo(InspectorExtension.VIEW_ID);
        assertThat(contribution.view()).isInstanceOf(EditorDetailsView.class);
        EditorDetailsView view = (EditorDetailsView) contribution.view();
        assertThat(view.dataProvider().details()).isEmpty();

        List<Optional<EditorDetails>> observed = new ArrayList<>();
        view.dataProvider().observe(observed::add);
        EditorDetails details = new EditorDetails("Player", "Local entity", "world", "player", List.of(), List.of());
        selections.select(new EditorSelection(EditorSelectionKinds.LOCAL_ENTITY, "world:player", details));
        selections.clear();

        assertThat(observed).containsExactly(Optional.empty(), Optional.of(details), Optional.empty());
        projects.clear();
        assertThat(snapshots.getLast()).isEmpty();
        host.close();
    }
}
