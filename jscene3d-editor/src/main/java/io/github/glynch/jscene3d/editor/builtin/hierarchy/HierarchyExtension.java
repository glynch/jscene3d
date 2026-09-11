/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.hierarchy;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.EditorSelection;
import io.github.glynch.jscene3d.editor.EditorSelectionModel;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorTreeDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorTreeItem;
import io.github.glynch.jscene3d.editor.view.EditorTreeItemCollapsibleState;
import io.github.glynch.jscene3d.editor.view.EditorTreeSelectionModel;
import io.github.glynch.jscene3d.editor.view.EditorTreeView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Built-in extension contributing the project entity Hierarchy. */
public final class HierarchyExtension implements EditorExtension {
    /** Stable identity of the built-in Hierarchy view. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.hierarchy");

    private final EditorProjectContext projects;
    private final EditorSelectionModel editorSelection;

    /**
     * Creates the built-in extension over editor-owned project and selection state.
     *
     * @param projects current-project lifecycle
     * @param editorSelection selection shared across editor views
     */
    public HierarchyExtension(EditorProjectContext projects, EditorSelectionModel editorSelection) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.editorSelection = Objects.requireNonNull(editorSelection, "editorSelection");
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.hierarchy";
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions().add(projects.observeHierarchy(this::selectInitialEntry));
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new HierarchyTreeView(), EditorViewContainers.PRIMARY_SIDEBAR, 10)));
    }

    private void selectInitialEntry(Optional<EditorHierarchyNode> hierarchy) {
        hierarchy.ifPresentOrElse(
                root -> editorSelection.select(
                        root.children().stream().findFirst().orElse(root).selection()),
                editorSelection::clear);
    }

    private final class HierarchyTreeView implements EditorTreeView<EditorHierarchyNode> {
        private final EditorTreeDataProvider<EditorHierarchyNode> dataProvider = new HierarchyDataProvider();
        private final EditorTreeSelectionModel<EditorHierarchyNode> selectionModel = new HierarchySelectionModel();

        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Hierarchy";
        }

        @Override
        public EditorTreeDataProvider<EditorHierarchyNode> dataProvider() {
            return dataProvider;
        }

        @Override
        public Optional<EditorTreeSelectionModel<EditorHierarchyNode>> selectionModel() {
            return Optional.of(selectionModel);
        }
    }

    private final class HierarchyDataProvider implements EditorTreeDataProvider<EditorHierarchyNode> {
        @Override
        public CompletionStage<List<EditorHierarchyNode>> roots() {
            return CompletableFuture.completedFuture(
                    projects.hierarchy().stream().toList());
        }

        @Override
        public CompletionStage<List<EditorHierarchyNode>> children(EditorHierarchyNode parent) {
            return CompletableFuture.completedFuture(parent.children());
        }

        @Override
        public EditorTreeItem item(EditorHierarchyNode element) {
            String kindLabel = kindLabel(element.kind());
            EditorTreeItemCollapsibleState collapsible = EditorTreeItemCollapsibleState.NONE;
            if (!element.children().isEmpty()) {
                collapsible = element.kind() == EditorHierarchyNode.Kind.WORLD
                        ? EditorTreeItemCollapsibleState.EXPANDED
                        : EditorTreeItemCollapsibleState.COLLAPSED;
            }
            return new EditorTreeItem(
                    element.label(),
                    Optional.empty(),
                    Optional.of(itemTooltip(element, kindLabel)),
                    Optional.of(new EditorIcon(icon(element.kind()), kindLabel)),
                    decorations(element),
                    Optional.empty(),
                    Optional.of(element.kind().name().toLowerCase(Locale.ROOT)),
                    collapsible);
        }

        @Override
        public EditorRegistration observeChanges(Consumer<Optional<EditorHierarchyNode>> listener) {
            Consumer<Optional<EditorHierarchyNode>> observer = Objects.requireNonNull(listener, "listener");
            return projects.observeHierarchy(ignored -> observer.accept(Optional.empty()));
        }
    }

    private final class HierarchySelectionModel implements EditorTreeSelectionModel<EditorHierarchyNode> {
        @Override
        public Optional<EditorHierarchyNode> selection() {
            return editorSelection.selection().flatMap(HierarchyExtension.this::findSelection);
        }

        @Override
        public void select(Optional<EditorHierarchyNode> selection) {
            Optional<EditorHierarchyNode> selected = Objects.requireNonNull(selection, "selection");
            if (selected.isPresent()) {
                editorSelection.select(selected.orElseThrow().selection());
            } else if (editorSelection
                    .selection()
                    .flatMap(HierarchyExtension.this::findSelection)
                    .isPresent()) {
                editorSelection.clear();
            }
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<EditorHierarchyNode>> listener) {
            Consumer<Optional<EditorHierarchyNode>> observer = Objects.requireNonNull(listener, "listener");
            Runnable removal = editorSelection.subscribe(
                    selection -> observer.accept(selection.flatMap(HierarchyExtension.this::findSelection)));
            return removal::run;
        }
    }

    private Optional<EditorHierarchyNode> findSelection(EditorSelection selection) {
        return projects.hierarchy().flatMap(root -> findSelection(root, selection));
    }

    private static Optional<EditorHierarchyNode> findSelection(EditorHierarchyNode node, EditorSelection selection) {
        if (node.selection().equals(selection)) {
            return Optional.of(node);
        }
        return node.children().stream()
                .map(child -> findSelection(child, selection))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static EditorIconId icon(EditorHierarchyNode.Kind kind) {
        return switch (kind) {
            case WORLD -> EditorIcons.WORLD;
            case LOCAL_ENTITY, GENERATED_ENTITY -> EditorIcons.ENTITY;
            case PLACEMENT -> EditorIcons.PLACEMENT;
        };
    }

    private static String kindLabel(EditorHierarchyNode.Kind kind) {
        return switch (kind) {
            case WORLD -> "World";
            case LOCAL_ENTITY -> "Local entity";
            case PLACEMENT -> "Entity definition placement";
            case GENERATED_ENTITY -> "Generated entity";
        };
    }

    private static List<EditorIcon> decorations(EditorHierarchyNode element) {
        ArrayList<EditorIcon> decorations = new ArrayList<>();
        if (element.kind() == EditorHierarchyNode.Kind.GENERATED_ENTITY) {
            decorations.add(new EditorIcon(EditorIcons.READ_ONLY, "Generated read-only entity"));
        }
        if (!element.isEnabled()) {
            decorations.add(new EditorIcon(EditorIcons.DISABLED, "Initially disabled"));
        }
        return List.copyOf(decorations);
    }

    private static String itemTooltip(EditorHierarchyNode element, String kindLabel) {
        StringBuilder tooltip = new StringBuilder(kindLabel);
        if (element.kind() == EditorHierarchyNode.Kind.GENERATED_ENTITY) {
            tooltip.append(" · read-only");
        }
        if (!element.isEnabled()) {
            tooltip.append(" · initially disabled");
        }
        return tooltip.toString();
    }
}
