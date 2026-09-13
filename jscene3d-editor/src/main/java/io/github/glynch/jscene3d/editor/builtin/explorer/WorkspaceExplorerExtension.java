/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorTreeDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorTreeItem;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Built-in extension contributing a project-workspace filesystem Explorer. */
public final class WorkspaceExplorerExtension implements EditorExtension {
    /** Stable identity of the built-in Workspace Explorer view. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.workspace-explorer");

    /** Stable identity of the built-in Explorer Activity Bar container. */
    public static final ActivityId ACTIVITY_ID = new ActivityId("io.github.glynch.jscene3d.editor.explorer-activity");

    private static final Comparator<WorkspaceExplorerEntry> ENTRY_ORDER = Comparator.comparing(
                    (WorkspaceExplorerEntry entry) -> entry.kind() == WorkspaceExplorerEntry.Kind.FILE)
            .thenComparing(WorkspaceExplorerEntry::label, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.path().toString());

    private final EditorProjects projects;

    /**
     * Creates the built-in Explorer over the editor's current-project lifecycle.
     *
     * @param projects current project workspace
     */
    public WorkspaceExplorerExtension(EditorProjects projects) {
        this.projects = Objects.requireNonNull(projects, "projects");
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.workspace-explorer";
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Workspace Explorer",
                "Browses editable files in the open project workspace.",
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        EditorContextCondition<Boolean> projectOpen = EditorContextCondition.isTrue(EditorContextKeys.PROJECT_OPEN);
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new WorkspaceExplorerTreeView(),
                                EditorViewContainers.PRIMARY_SIDEBAR,
                                5,
                                projectOpen)));
        editor.subscriptions()
                .add(editor.activities()
                        .register(new EditorActivityContribution(
                                ACTIVITY_ID,
                                "Explorer",
                                new EditorIcon(EditorIcons.PROJECT, "Explorer"),
                                VIEW_ID,
                                5,
                                projectOpen)));
    }

    private final class WorkspaceExplorerTreeView implements EditorTreeView<WorkspaceExplorerEntry> {
        private final EditorTreeDataProvider<WorkspaceExplorerEntry> dataProvider = new WorkspaceDataProvider();

        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Explorer";
        }

        @Override
        public EditorTreeDataProvider<WorkspaceExplorerEntry> dataProvider() {
            return dataProvider;
        }
    }

    private final class WorkspaceDataProvider implements EditorTreeDataProvider<WorkspaceExplorerEntry> {
        @Override
        public CompletionStage<List<WorkspaceExplorerEntry>> roots() {
            return CompletableFuture.completedFuture(projects.current().stream()
                    .map(WorkspaceExplorerExtension::workspaceEntry)
                    .toList());
        }

        @Override
        public CompletionStage<List<WorkspaceExplorerEntry>> children(WorkspaceExplorerEntry parent) {
            WorkspaceExplorerEntry entry = Objects.requireNonNull(parent, "parent");
            if (entry.kind() == WorkspaceExplorerEntry.Kind.FILE) {
                return CompletableFuture.completedFuture(List.of());
            }
            return CompletableFuture.supplyAsync(() -> childrenOf(entry.path()));
        }

        @Override
        public EditorTreeItem item(WorkspaceExplorerEntry element) {
            WorkspaceExplorerEntry entry = Objects.requireNonNull(element, "element");
            boolean file = entry.kind() == WorkspaceExplorerEntry.Kind.FILE;
            EditorTreeItemCollapsibleState collapsibleState =
                    switch (entry.kind()) {
                        case WORKSPACE -> EditorTreeItemCollapsibleState.EXPANDED;
                        case DIRECTORY -> EditorTreeItemCollapsibleState.COLLAPSED;
                        case FILE -> EditorTreeItemCollapsibleState.NONE;
                    };
            return new EditorTreeItem(
                    entry.label(),
                    Optional.empty(),
                    Optional.of(tooltip(entry)),
                    Optional.of(new EditorIcon(
                            file ? EditorIcons.SOURCE_ASSET : EditorIcons.PROJECT, file ? "File" : "Folder")),
                    List.of(),
                    Optional.empty(),
                    Optional.of(file ? "workspace-file" : "workspace-folder"),
                    collapsibleState);
        }

        @Override
        public EditorRegistration observeChanges(Consumer<Optional<WorkspaceExplorerEntry>> listener) {
            Consumer<Optional<WorkspaceExplorerEntry>> observer = Objects.requireNonNull(listener, "listener");
            return projects.observe(ignored -> observer.accept(Optional.empty()));
        }

        private String tooltip(WorkspaceExplorerEntry entry) {
            if (entry.kind() == WorkspaceExplorerEntry.Kind.WORKSPACE) {
                return entry.path().toString();
            }
            return projects.current()
                    .map(EditorProject::root)
                    .map(Path::of)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .filter(entry.path()::startsWith)
                    .map(root -> root.relativize(entry.path()).toString())
                    .orElseGet(() -> entry.path().toString());
        }
    }

    private static WorkspaceExplorerEntry workspaceEntry(EditorProject project) {
        EditorProject workspace = Objects.requireNonNull(project, "project");
        return new WorkspaceExplorerEntry(
                Path.of(workspace.root()), workspace.name(), WorkspaceExplorerEntry.Kind.WORKSPACE);
    }

    private static List<WorkspaceExplorerEntry> childrenOf(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.filter(WorkspaceExplorerExtension::isVisible)
                    .map(WorkspaceExplorerExtension::entry)
                    .sorted(ENTRY_ORDER)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read workspace directory: " + directory, exception);
        }
    }

    private static WorkspaceExplorerEntry entry(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path fileName = absolute.getFileName();
        WorkspaceExplorerEntry.Kind kind = Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)
                ? WorkspaceExplorerEntry.Kind.DIRECTORY
                : WorkspaceExplorerEntry.Kind.FILE;
        return new WorkspaceExplorerEntry(absolute, fileName == null ? absolute.toString() : fileName.toString(), kind);
    }

    private static boolean isVisible(Path path) {
        if (Files.isSymbolicLink(path)) {
            return false;
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return true;
        }
        String name = fileName.toString();
        if (name.equals(".git") || name.equals("target") || name.equals(".DS_Store")) {
            return false;
        }
        Path parent = path.getParent();
        return !name.equals("cache")
                || parent == null
                || parent.getFileName() == null
                || !parent.getFileName().toString().equals(".jscene3d");
    }
}
