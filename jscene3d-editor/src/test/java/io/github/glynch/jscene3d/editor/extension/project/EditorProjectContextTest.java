/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.EditorInspectorView;
import io.github.glynch.jscene3d.editor.EditorSelection;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorProjectContextTest {
    @Test
    void publishesCurrentProjectAndInternalProjectionsUntilObserversClose() {
        EditorProjectContext context = new EditorProjectContext();
        List<Optional<EditorProject>> projects = new ArrayList<>();
        List<Optional<EditorHierarchyNode>> hierarchies = new ArrayList<>();
        List<List<ProjectAsset>> assets = new ArrayList<>();
        EditorRegistration projectRegistration = context.observe(projects::add);
        EditorRegistration hierarchyRegistration = context.observeHierarchy(hierarchies::add);
        EditorRegistration assetRegistration = context.observeAssets(() -> assets.add(context.assets()));
        EditorProject project = new EditorProject("io.github.glynch.test", "Test", URI.create("file:///test/"));
        EditorHierarchyNode hierarchy = hierarchy();

        context.showProject(project, hierarchy, List.of());
        projectRegistration.close();
        hierarchyRegistration.close();
        assetRegistration.close();
        context.clear();
        context.clear();
        projectRegistration.close();

        assertThat(projects).containsExactly(Optional.empty(), Optional.of(project));
        assertThat(hierarchies).containsExactly(Optional.empty(), Optional.of(hierarchy));
        assertThat(assets).containsExactly(List.of(), List.of());
        assertThat(context.current()).isEmpty();
        assertThat(context.hierarchy()).isEmpty();
        assertThat(context.assets()).isEmpty();
    }

    private static EditorHierarchyNode hierarchy() {
        EditorInspectorView inspector = new EditorInspectorView("World", "World", "test", "world", false, List.of());
        EditorSelection selection = new EditorSelection(EditorSelection.Kind.WORLD, "world", inspector);
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                "World",
                Optional.empty(),
                Optional.empty(),
                true,
                selection,
                List.of());
    }
}
