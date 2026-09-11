/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.project;

import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorCollectionCategory;
import io.github.glynch.jscene3d.editor.view.EditorCollectionDataProvider;
import io.github.glynch.jscene3d.editor.view.EditorCollectionItem;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSelectionModel;
import io.github.glynch.jscene3d.editor.view.EditorCollectionSnapshot;
import io.github.glynch.jscene3d.editor.view.EditorCollectionView;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Built-in extension contributing the categorized Project asset collection. */
public final class ProjectExtension implements EditorExtension {
    /** Stable identity of the built-in Project view. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.project");

    private static final List<EditorCollectionCategory> CATEGORIES = List.of(
            category(ProjectAsset.Kind.WORLD_DEFINITION, "Worlds"),
            category(ProjectAsset.Kind.ENTITY_DEFINITION, "Entity Definitions"),
            category(ProjectAsset.Kind.SOURCE_ASSET, "Source Assets"),
            category(ProjectAsset.Kind.IMPORT_DEFINITION, "Imports"));
    private static final Comparator<ProjectAsset> ASSET_ORDER = Comparator.comparingInt(
                    (ProjectAsset item) -> order(item.kind()))
            .thenComparing(ProjectAsset::label, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(item -> item.selection().identity());

    private final EditorProjectContext projects;

    /** Creates the built-in extension over editor-owned project state. */
    public ProjectExtension(EditorProjectContext projects) {
        this.projects = Objects.requireNonNull(projects, "projects");
    }

    @Override
    public String id() {
        return "io.github.glynch.jscene3d.editor.builtin.project";
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        EditorSelections selections = editor.selections();
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                new ProjectCollectionView(selections), EditorViewContainers.BOTTOM_PANEL, 10)));
    }

    private final class ProjectCollectionView implements EditorCollectionView<ProjectAsset> {
        private final EditorCollectionDataProvider<ProjectAsset> dataProvider = new ProjectDataProvider();
        private final EditorCollectionSelectionModel<ProjectAsset> selectionModel;

        private ProjectCollectionView(EditorSelections selections) {
            selectionModel = new ProjectSelectionModel(selections);
        }

        @Override
        public ViewId id() {
            return VIEW_ID;
        }

        @Override
        public String title() {
            return "Project";
        }

        @Override
        public EditorCollectionDataProvider<ProjectAsset> dataProvider() {
            return dataProvider;
        }

        @Override
        public List<EditorCollectionCategory> categories() {
            return CATEGORIES;
        }

        @Override
        public String allItemsLabel() {
            return "Project Assets";
        }

        @Override
        public Optional<EditorIcon> rootIcon() {
            return Optional.of(new EditorIcon(EditorIcons.PROJECT, "Project"));
        }

        @Override
        public String searchPlaceholder() {
            return "Search assets…";
        }

        @Override
        public Optional<EditorCollectionSelectionModel<ProjectAsset>> selectionModel() {
            return Optional.of(selectionModel);
        }
    }

    private final class ProjectDataProvider implements EditorCollectionDataProvider<ProjectAsset> {
        @Override
        public CompletionStage<EditorCollectionSnapshot<ProjectAsset>> snapshot() {
            String projectName =
                    projects.current().map(project -> project.name()).orElse("No project");
            List<ProjectAsset> items =
                    projects.assets().stream().sorted(ASSET_ORDER).toList();
            boolean projectOpen = projects.current().isPresent();
            String emptyMessage = projectOpen
                    ? "No assets match this Project location or search."
                    : "Open a project to browse Worlds, Entity Definitions, Source Assets, and Imports.";
            return CompletableFuture.completedFuture(
                    new EditorCollectionSnapshot<>(projectName, items, projectOpen, emptyMessage));
        }

        @Override
        public EditorCollectionItem item(ProjectAsset element) {
            ProjectAsset asset = Objects.requireNonNull(element, "element");
            return new EditorCollectionItem(
                    asset.label(),
                    Optional.of(asset.kind().label()),
                    asset.selection().details().map(details -> details.source()),
                    Optional.of(asset.source().toString()),
                    Optional.of(new EditorIcon(icon(asset.kind()), asset.kind().label())),
                    Optional.of(categoryId(asset.kind())),
                    List.of(new EditorIcon(EditorIcons.READ_ONLY, "Read-only")),
                    Optional.empty(),
                    Optional.of(asset.kind().name().toLowerCase(Locale.ROOT)));
        }

        @Override
        public EditorRegistration observeChanges(Runnable listener) {
            return projects.observeAssets(listener);
        }
    }

    private final class ProjectSelectionModel implements EditorCollectionSelectionModel<ProjectAsset> {
        private final EditorSelections selections;

        private ProjectSelectionModel(EditorSelections selections) {
            this.selections = Objects.requireNonNull(selections, "selections");
        }

        @Override
        public Optional<ProjectAsset> selection() {
            return selections.current().flatMap(ProjectExtension.this::findSelection);
        }

        @Override
        public void select(Optional<ProjectAsset> selection) {
            Optional<ProjectAsset> selected = Objects.requireNonNull(selection, "selection");
            if (selected.isPresent()) {
                selections.select(selected.orElseThrow().selection());
            } else if (selections
                    .current()
                    .flatMap(ProjectExtension.this::findSelection)
                    .isPresent()) {
                selections.clear();
            }
        }

        @Override
        public EditorRegistration observe(Consumer<Optional<ProjectAsset>> listener) {
            Consumer<Optional<ProjectAsset>> observer = Objects.requireNonNull(listener, "listener");
            return selections.observe(
                    selection -> observer.accept(selection.flatMap(ProjectExtension.this::findSelection)));
        }
    }

    private Optional<ProjectAsset> findSelection(EditorSelection selection) {
        if (!selection.kind().equals(EditorSelectionKinds.ASSET)) {
            return Optional.empty();
        }
        return projects.assets().stream()
                .filter(asset -> asset.selection().identity().equals(selection.identity()))
                .findFirst();
    }

    private static EditorCollectionCategory category(ProjectAsset.Kind kind, String label) {
        return new EditorCollectionCategory(categoryId(kind), label, Optional.of(new EditorIcon(icon(kind), label)));
    }

    private static String categoryId(ProjectAsset.Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static EditorIconId icon(ProjectAsset.Kind kind) {
        return switch (kind) {
            case WORLD_DEFINITION -> EditorIcons.WORLD;
            case ENTITY_DEFINITION -> EditorIcons.ENTITY_DEFINITION;
            case SOURCE_ASSET -> EditorIcons.SOURCE_ASSET;
            case IMPORT_DEFINITION -> EditorIcons.IMPORT;
        };
    }

    private static int order(ProjectAsset.Kind kind) {
        return switch (kind) {
            case WORLD_DEFINITION -> 0;
            case ENTITY_DEFINITION -> 1;
            case SOURCE_ASSET -> 2;
            case IMPORT_DEFINITION -> 3;
        };
    }
}
