/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.hierarchy;

import io.github.glynch.jscene3d.editor.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
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

    /** Stable identity of the built-in Scene Activity Bar container. */
    public static final ActivityId ACTIVITY_ID = new ActivityId("io.github.glynch.jscene3d.editor.scene-activity");

    private final EditorProjectContext projects;

    /**
     * Creates the built-in extension over editor-owned project state.
     *
     * @param projects current-project lifecycle
     */
    public HierarchyExtension(EditorProjectContext projects) {
        this.projects = Objects.requireNonNull(projects, "projects");
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.hierarchy";
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                "Scene Hierarchy",
                "Presents the entities and generated content in the open world.",
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        EditorSelections selections = editor.selections();
        editor.subscriptions().add(projects.observeHierarchy(hierarchy -> selectInitialEntry(selections, hierarchy)));
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new HierarchyTreeView(selections),
                                EditorViewContainers.PRIMARY_SIDEBAR,
                                10,
                                EditorContextCondition.isTrue(EditorContextKeys.PROJECT_OPEN))));
        editor.subscriptions()
                .add(editor.activities()
                        .register(new EditorActivityContribution(
                                ACTIVITY_ID,
                                "Scene",
                                new EditorIcon(EditorIcons.SCENE, "Scene"),
                                VIEW_ID,
                                10,
                                EditorContextCondition.isTrue(EditorContextKeys.PROJECT_OPEN))));
    }

    private static void selectInitialEntry(EditorSelections selections, Optional<EditorHierarchyNode> hierarchy) {
        hierarchy.ifPresentOrElse(
                root -> selections.select(selections
                        .current()
                        .flatMap(selection -> findSelection(root, selection))
                        .orElseGet(() -> root.children().stream().findFirst().orElse(root))
                        .selection()),
                selections::clear);
    }

    private final class HierarchyTreeView implements EditorTreeView<EditorHierarchyNode> {
        private final EditorTreeDataProvider<EditorHierarchyNode> dataProvider = new HierarchyDataProvider();
        private final EditorTreeSelectionModel<EditorHierarchyNode> selectionModel;

        private HierarchyTreeView(EditorSelections selections) {
            selectionModel = new HierarchySelectionModel(selections);
        }

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
        private final EditorSelections selections;

        private HierarchySelectionModel(EditorSelections selections) {
            this.selections = Objects.requireNonNull(selections, "selections");
        }

        @Override
        public Optional<EditorHierarchyNode> selection() {
            return selections.current().flatMap(HierarchyExtension.this::findSelection);
        }

        @Override
        public void select(Optional<EditorHierarchyNode> selection) {
            Optional<EditorHierarchyNode> selected = Objects.requireNonNull(selection, "selection");
            if (selected.isPresent()) {
                selections.select(selected.orElseThrow().selection());
            } else if (selections
                    .current()
                    .flatMap(HierarchyExtension.this::findSelection)
                    .isPresent()) {
                selections.clear();
            }
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<EditorHierarchyNode>> listener) {
            Consumer<Optional<EditorHierarchyNode>> observer = Objects.requireNonNull(listener, "listener");
            return selections.observe(
                    selection -> observer.accept(selection.flatMap(HierarchyExtension.this::findSelection)));
        }
    }

    private Optional<EditorHierarchyNode> findSelection(EditorSelection selection) {
        return projects.hierarchy().flatMap(root -> findSelection(root, selection));
    }

    private static Optional<EditorHierarchyNode> findSelection(EditorHierarchyNode node, EditorSelection selection) {
        if (sameIdentity(node.selection(), selection)) {
            return Optional.of(node);
        }
        return node.children().stream()
                .map(child -> findSelection(child, selection))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean sameIdentity(EditorSelection first, EditorSelection second) {
        return first.kind().equals(second.kind()) && first.identity().equals(second.identity());
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
        if (element.isModified()) {
            decorations.add(new EditorIcon(EditorIcons.MODIFIED, "Modified"));
        }
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
