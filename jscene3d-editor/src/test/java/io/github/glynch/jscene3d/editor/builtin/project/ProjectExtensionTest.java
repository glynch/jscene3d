/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.EditorInspectorView;
import io.github.glynch.jscene3d.editor.EditorSelection;
import io.github.glynch.jscene3d.editor.EditorSelectionModel;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSnapshot;
import io.github.glynch.jscene3d.editor.view.EditorCollectionView;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ProjectExtensionTest {
    @Test
    void contributesAProjectDrivenCollectionAndSharesSelection() {
        EditorProjectContext projects = new EditorProjectContext();
        EditorSelectionModel editorSelection = new EditorSelectionModel();
        EditorExtensionHost host = new EditorExtensionHost(projects);
        List<List<EditorViewContribution>> contributions = new ArrayList<>();
        host.observeViews(contributions::add);

        host.activate(new ProjectExtension(projects, editorSelection));

        EditorViewContribution contribution = contributions.getLast().getFirst();
        assertThat(contribution.container()).isEqualTo(EditorViewContainers.BOTTOM_PANEL);
        assertThat(contribution.view().id()).isEqualTo(ProjectExtension.VIEW_ID);
        assertThat(contribution.view()).isInstanceOf(EditorCollectionView.class);
        @SuppressWarnings("unchecked")
        EditorCollectionView<ProjectAsset> view = (EditorCollectionView<ProjectAsset>) contribution.view();
        assertThat(snapshot(view).elements()).isEmpty();
        assertThat(view.categories())
                .extracting(category -> category.label())
                .containsExactly("Worlds", "Entity Definitions", "Source Assets", "Imports");
        assertThat(view.allItemsLabel()).isEqualTo("Project Assets");
        assertThat(view.rootIcon()).contains(new EditorIcon(EditorIcons.PROJECT, "Project"));
        assertThat(view.searchPlaceholder()).isEqualTo("Search assets…");

        ProjectAsset source = item("actors", "source-z", ProjectAsset.Kind.SOURCE_ASSET);
        ProjectAsset world = item("MAP01", "world-a", ProjectAsset.Kind.WORLD_DEFINITION);
        projects.showProject(
                new EditorProject("io.github.glynch.test", "Test", URI.create("file:///test/")),
                hierarchy(),
                List.of(source, world));

        assertThat(snapshot(view).rootLabel()).isEqualTo("Test");
        assertThat(snapshot(view).elements()).containsExactly(world, source);
        assertThat(view.dataProvider().item(world).categoryId()).contains("world_definition");
        assertThat(view.dataProvider().item(world).icon())
                .contains(new EditorIcon(EditorIcons.WORLD, "World definition"));
        assertThat(view.dataProvider().item(world).decorations())
                .containsExactly(new EditorIcon(EditorIcons.READ_ONLY, "Read-only"));

        view.selectionModel().orElseThrow().select(Optional.of(world));

        assertThat(editorSelection.selection()).contains(world.selection());
        editorSelection.select(hierarchy().selection());
        assertThat(view.selectionModel().orElseThrow().selection()).isEmpty();
        host.close();
    }

    private static EditorCollectionSnapshot<ProjectAsset> snapshot(EditorCollectionView<ProjectAsset> view) {
        return view.dataProvider().snapshot().toCompletableFuture().join();
    }

    private static ProjectAsset item(String label, String identity, ProjectAsset.Kind kind) {
        Path source = Path.of("project", kind.name().toLowerCase(Locale.ROOT), identity);
        EditorInspectorView inspector =
                new EditorInspectorView(label, kind.label(), source.toString(), identity, false, List.of());
        EditorSelection selection = new EditorSelection(EditorSelection.Kind.ASSET, "selection:" + identity, inspector);
        return new ProjectAsset(label, identity, kind, source, selection);
    }

    private static EditorHierarchyNode hierarchy() {
        EditorInspectorView inspector = new EditorInspectorView("MAP01", "World", "world", "world", false, List.of());
        EditorSelection selection = new EditorSelection(EditorSelection.Kind.WORLD, "world", inspector);
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                "MAP01",
                Optional.empty(),
                Optional.empty(),
                true,
                selection,
                List.of());
    }
}
