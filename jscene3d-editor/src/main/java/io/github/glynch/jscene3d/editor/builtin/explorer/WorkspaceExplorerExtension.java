/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorFileTypes;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorTreeDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorTreeItem;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeSelectionModel;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Built-in extension contributing a project-workspace filesystem Explorer. */
public final class WorkspaceExplorerExtension implements EditorExtension {
    /** Stable identity of the built-in Workspace Explorer view. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.workspace-explorer");

    /** Stable identity of the built-in Explorer Activity Bar container. */
    public static final ActivityId ACTIVITY_ID = new ActivityId("io.github.glynch.jscene3d.editor.explorer-activity");

    private static final CommandId OPEN_FILE =
            new CommandId("io.github.glynch.jscene3d.editor.workspace-explorer.open-file");

    private static final Comparator<WorkspaceExplorerEntry> ENTRY_ORDER = Comparator.comparing(
                    (WorkspaceExplorerEntry entry) -> entry.kind() == WorkspaceExplorerEntry.Kind.FILE)
            .thenComparing(WorkspaceExplorerEntry::label, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(entry -> entry.path().toString());

    private final EditorProjects projects;
    private final WorkspaceExplorerExclusionPolicy exclusions;

    /**
     * Creates the built-in Explorer over the editor's current-project lifecycle.
     *
     * @param projects current project workspace
     */
    public WorkspaceExplorerExtension(EditorProjects projects) {
        this(projects, WorkspaceExplorerExclusionPolicy.defaults());
    }

    /**
     * Creates the built-in Explorer with additional workspace exclusion policy sources already composed.
     *
     * @param projects current project workspace
     * @param exclusions exclusions applied to workspace entries
     */
    public WorkspaceExplorerExtension(EditorProjects projects, WorkspaceExplorerExclusionPolicy exclusions) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.exclusions = Objects.requireNonNull(exclusions, "exclusions");
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
        WorkspaceExplorerTreeView explorer = new WorkspaceExplorerTreeView(editor.fileTypes());
        editor.subscriptions()
                .add(editor.commands()
                        .register(
                                new EditorCommandContribution(OPEN_FILE, "Open File"),
                                ignored -> explorer.openSelection(editor.window())));
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                explorer, EditorViewContainers.PRIMARY_SIDEBAR, 5, projectOpen)));
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
        private final EditorTreeDataProvider<WorkspaceExplorerEntry> dataProvider;
        private final ExplorerSelectionModel selectionModel = new ExplorerSelectionModel();
        private final EditorFileTypes fileTypes;

        private WorkspaceExplorerTreeView(EditorFileTypes fileTypes) {
            this.fileTypes = Objects.requireNonNull(fileTypes, "fileTypes");
            dataProvider = new WorkspaceDataProvider(this.fileTypes);
        }

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

        @Override
        public Optional<EditorTreeSelectionModel<WorkspaceExplorerEntry>> selectionModel() {
            return Optional.of(selectionModel);
        }

        private void openSelection(EditorWindow window) {
            selectionModel
                    .selection()
                    .filter(entry -> entry.kind() == WorkspaceExplorerEntry.Kind.FILE)
                    .ifPresent(entry -> window.openFile(entry.path().toUri()));
        }
    }

    private final class WorkspaceDataProvider implements EditorTreeDataProvider<WorkspaceExplorerEntry> {
        private final EditorFileTypes fileTypes;

        private WorkspaceDataProvider(EditorFileTypes fileTypes) {
            this.fileTypes = Objects.requireNonNull(fileTypes, "fileTypes");
        }

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
            return CompletableFuture.supplyAsync(() -> childrenOf(entry));
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
                    Optional.of(icon(entry)),
                    List.of(),
                    file ? Optional.of(OPEN_FILE) : Optional.empty(),
                    Optional.of(file ? "workspace-file" : "workspace-folder"),
                    collapsibleState);
        }

        @Override
        public EditorRegistration observeChanges(Consumer<Optional<WorkspaceExplorerEntry>> listener) {
            Consumer<Optional<WorkspaceExplorerEntry>> observer = Objects.requireNonNull(listener, "listener");
            return projects.observe(ignored -> observer.accept(Optional.empty()));
        }

        private List<WorkspaceExplorerEntry> childrenOf(WorkspaceExplorerEntry parent) {
            try (Stream<Path> children = Files.list(parent.path())) {
                return children.filter(path -> exclusions.includes(parent.workspaceRoot(), path))
                        .map(path -> entry(parent.workspaceRoot(), path))
                        .sorted(ENTRY_ORDER)
                        .toList();
            } catch (IOException exception) {
                throw new UncheckedIOException("Unable to read workspace directory: " + parent.path(), exception);
            }
        }

        private String tooltip(WorkspaceExplorerEntry entry) {
            if (entry.kind() == WorkspaceExplorerEntry.Kind.WORKSPACE) {
                return entry.path().toString();
            }
            return entry.workspaceRoot().relativize(entry.path()).toString();
        }

        private EditorIcon icon(WorkspaceExplorerEntry entry) {
            return switch (entry.kind()) {
                case WORKSPACE -> new EditorIcon(EditorIcons.PROJECT, "Workspace");
                case DIRECTORY -> new EditorIcon(EditorIcons.FOLDER, "Folder");
                case FILE ->
                    fileTypes
                            .resolve(entry.path().toUri())
                            .map(EditorFileType::icon)
                            .orElseGet(() -> new EditorIcon(EditorIcons.TEXT_FILE, "File"));
            };
        }
    }

    private static final class ExplorerSelectionModel implements EditorTreeSelectionModel<WorkspaceExplorerEntry> {
        private final List<Consumer<Optional<WorkspaceExplorerEntry>>> observers = new ArrayList<>();
        private Optional<WorkspaceExplorerEntry> selection = Optional.empty();

        @Override
        public Optional<WorkspaceExplorerEntry> selection() {
            return selection;
        }

        @Override
        public void select(Optional<WorkspaceExplorerEntry> selected) {
            Optional<WorkspaceExplorerEntry> replacement = Objects.requireNonNull(selected, "selection");
            if (!selection.equals(replacement)) {
                selection = replacement;
                List.copyOf(observers).forEach(observer -> observer.accept(selection));
            }
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<WorkspaceExplorerEntry>> listener) {
            Consumer<Optional<WorkspaceExplorerEntry>> observer = Objects.requireNonNull(listener, "listener");
            observers.add(observer);
            observer.accept(selection);
            AtomicBoolean active = new AtomicBoolean(true);
            return () -> {
                if (active.compareAndSet(true, false)) {
                    observers.remove(observer);
                }
            };
        }
    }

    private static WorkspaceExplorerEntry workspaceEntry(EditorProject project) {
        EditorProject workspace = Objects.requireNonNull(project, "project");
        Path root = Path.of(workspace.root()).toAbsolutePath().normalize();
        return new WorkspaceExplorerEntry(root, root, workspace.name(), WorkspaceExplorerEntry.Kind.WORKSPACE);
    }

    private static WorkspaceExplorerEntry entry(Path workspaceRoot, Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path fileName = absolute.getFileName();
        WorkspaceExplorerEntry.Kind kind = Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)
                ? WorkspaceExplorerEntry.Kind.DIRECTORY
                : WorkspaceExplorerEntry.Kind.FILE;
        return new WorkspaceExplorerEntry(
                workspaceRoot, absolute, fileName == null ? absolute.toString() : fileName.toString(), kind);
    }
}
