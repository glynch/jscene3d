/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the project-scoped Workspace Explorer contribution and filesystem projection. */
final class WorkspaceExplorerExtensionTest {
    @TempDir
    private Path workspace;

    /** Contributes one lazy filesystem tree while a project workspace is open. */
    @Test
    void contributesProjectWorkspaceTree() throws IOException {
        createWorkspace();
        EditorProjectContext projects = new EditorProjectContext();
        EditorExtensionHost host = new EditorExtensionHost(projects, new EditorSelectionContext());
        List<List<EditorViewContribution>> viewSnapshots = new ArrayList<>();
        List<List<EditorActivityContribution>> activitySnapshots = new ArrayList<>();
        host.observeViews(viewSnapshots::add);
        host.observeActivities(activitySnapshots::add);

        host.activate(new WorkspaceExplorerExtension(projects));
        assertThat(viewSnapshots.getLast()).isEmpty();
        assertThat(activitySnapshots.getLast()).isEmpty();

        projects.showProject(
                new EditorProject("io.github.glynch.test", "Test Workspace", workspace.toUri()),
                hierarchy(),
                List.of());

        assertThat(activitySnapshots.getLast()).singleElement().satisfies(activity -> {
            assertThat(activity.id()).isEqualTo(WorkspaceExplorerExtension.ACTIVITY_ID);
            assertThat(activity.title()).isEqualTo("Explorer");
            assertThat(activity.views()).containsExactly(WorkspaceExplorerExtension.VIEW_ID);
        });
        EditorViewContribution contribution = viewSnapshots.getLast().getFirst();
        assertThat(contribution.container()).isEqualTo(EditorViewContainers.PRIMARY_SIDEBAR);
        assertThat(contribution.view().id()).isEqualTo(WorkspaceExplorerExtension.VIEW_ID);
        assertThat(contribution.view()).isInstanceOf(EditorTreeView.class);

        @SuppressWarnings("unchecked")
        EditorTreeView<WorkspaceExplorerEntry> view = (EditorTreeView<WorkspaceExplorerEntry>) contribution.view();
        WorkspaceExplorerEntry root =
                view.dataProvider().roots().toCompletableFuture().join().getFirst();
        assertThat(root)
                .returns("Test Workspace", WorkspaceExplorerEntry::label)
                .returns(WorkspaceExplorerEntry.Kind.WORKSPACE, WorkspaceExplorerEntry::kind);
        assertThat(view.dataProvider().item(root).collapsibleState())
                .isEqualTo(EditorTreeItemCollapsibleState.EXPANDED);

        List<WorkspaceExplorerEntry> rootChildren = children(view, root);
        assertThat(rootChildren)
                .extracting(WorkspaceExplorerEntry::label)
                .containsExactly(".jscene3d", "src", "pom.xml");
        WorkspaceExplorerEntry source = named(rootChildren, "src");
        WorkspaceExplorerEntry main = named(children(view, source), "main");
        WorkspaceExplorerEntry java = named(children(view, main), "java");
        WorkspaceExplorerEntry example = named(children(view, java), "example");
        WorkspaceExplorerEntry javaFile = named(children(view, example), "Player.java");
        assertThat(view.dataProvider().item(javaFile))
                .returns(EditorTreeItemCollapsibleState.NONE, item -> item.collapsibleState())
                .returns(Optional.of("src/main/java/example/Player.java"), item -> item.tooltip())
                .returns(Optional.of(new EditorIcon(EditorIcons.SOURCE_ASSET, "File")), item -> item.icon())
                .returns(Optional.of("workspace-file"), item -> item.contextValue())
                .returns(Optional.empty(), item -> item.command());

        WorkspaceExplorerEntry editorState = named(rootChildren, ".jscene3d");
        assertThat(children(view, editorState))
                .extracting(WorkspaceExplorerEntry::label)
                .containsExactly("settings.json");

        projects.clear();
        assertThat(viewSnapshots.getLast()).isEmpty();
        assertThat(activitySnapshots.getLast()).isEmpty();
        host.close();
    }

    private void createWorkspace() throws IOException {
        Files.createDirectories(workspace.resolve("src/main/java/example"));
        Files.writeString(workspace.resolve("src/main/java/example/Player.java"), "final class Player {}\n");
        Files.writeString(workspace.resolve("pom.xml"), "<project/>\n");
        Files.createDirectories(workspace.resolve("target/classes"));
        Files.writeString(workspace.resolve("target/classes/Player.class"), "generated\n");
        Files.createDirectories(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git/config"), "private\n");
        Files.createDirectories(workspace.resolve(".jscene3d/cache"));
        Files.writeString(workspace.resolve(".jscene3d/cache/index"), "generated\n");
        Files.writeString(workspace.resolve(".jscene3d/settings.json"), "{}\n");
    }

    private static List<WorkspaceExplorerEntry> children(
            EditorTreeView<WorkspaceExplorerEntry> view, WorkspaceExplorerEntry parent) {
        return view.dataProvider().children(parent).toCompletableFuture().join();
    }

    private static WorkspaceExplorerEntry named(List<WorkspaceExplorerEntry> entries, String name) {
        return entries.stream()
                .filter(entry -> entry.label().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static EditorHierarchyNode hierarchy() {
        return new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                "Test World",
                Optional.empty(),
                Optional.empty(),
                true,
                new EditorSelection(EditorSelectionKinds.WORLD, "world:test", Optional.empty()),
                List.of());
    }
}
