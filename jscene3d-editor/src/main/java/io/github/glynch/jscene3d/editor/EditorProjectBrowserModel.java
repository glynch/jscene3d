/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Maintains deterministic Project-browser navigation independently of JavaFX controls. */
final class EditorProjectBrowserModel {
    private static final Comparator<EditorAssetItem> DISPLAY_ORDER = Comparator.comparing(
                    (EditorAssetItem item) -> Category.of(item.kind()))
            .thenComparing(EditorAssetItem::label, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(item -> item.selection().identity());

    private List<EditorAssetItem> assets = List.of();
    private Category category = Category.ALL;
    private String query = "";
    private Optional<String> selectedIdentity = Optional.empty();

    /** Replaces the projected assets while retaining a still-valid stable selection. */
    void showProject(List<EditorAssetItem> projectAssets) {
        assets = Objects.requireNonNull(projectAssets, "projectAssets").stream()
                .sorted(DISPLAY_ORDER)
                .toList();
        selectedIdentity = selectedIdentity.filter(identity ->
                assets.stream().anyMatch(item -> item.selection().identity().equals(identity)));
        category = Category.ALL;
        query = "";
    }

    /** Clears project-owned browser state. */
    void clear() {
        assets = List.of();
        category = Category.ALL;
        query = "";
        selectedIdentity = Optional.empty();
    }

    /** Selects the root view or one JScene3D asset category. */
    void showCategory(Category selectedCategory) {
        category = Objects.requireNonNull(selectedCategory, "selectedCategory");
    }

    /** Applies a case-insensitive search across author-facing asset information. */
    void search(String searchText) {
        query = Objects.requireNonNull(searchText, "searchText").strip().toLowerCase(Locale.ROOT);
    }

    /** Remembers one projected asset by its source-scoped stable selection identity. */
    void select(EditorAssetItem item) {
        Objects.requireNonNull(item, "item");
        selectedIdentity = Optional.of(item.selection().identity());
    }

    /** Clears the Project selection without changing navigation or filtering. */
    void clearSelection() {
        selectedIdentity = Optional.empty();
    }

    /** Returns items visible for the current category and query. */
    List<EditorAssetItem> visibleItems() {
        return assets.stream()
                .filter(item -> category.matches(item.kind()))
                .filter(this::matchesQuery)
                .toList();
    }

    /** Returns the selected item when it still belongs to the current project. */
    Optional<EditorAssetItem> selectedItem() {
        return selectedIdentity.flatMap(identity -> assets.stream()
                .filter(item -> item.selection().identity().equals(identity))
                .findFirst());
    }

    /** Returns the active browser category. */
    Category category() {
        return category;
    }

    /** Counts projected assets in one category before search filtering. */
    long count(Category countedCategory) {
        Objects.requireNonNull(countedCategory, "countedCategory");
        return assets.stream()
                .filter(item -> countedCategory.matches(item.kind()))
                .count();
    }

    /** Tests one item against the normalized search text. */
    private boolean matchesQuery(EditorAssetItem item) {
        if (query.isEmpty()) {
            return true;
        }
        String searchable = String.join(
                        " ", item.label(), item.kind().label(), item.source().toString())
                .toLowerCase(Locale.ROOT);
        return searchable.contains(query);
    }

    /** JScene3D domain categories exposed by the Project browser. */
    enum Category {
        /** All project content. */
        ALL("Project Assets"),
        /** Authored world definitions. */
        WORLDS("Worlds", EditorAssetItem.Kind.WORLD_DEFINITION),
        /** Reusable entity definitions. */
        ENTITY_DEFINITIONS("Entity Definitions", EditorAssetItem.Kind.ENTITY_DEFINITION),
        /** Authoritative source and resource assets. */
        SOURCE_ASSETS("Source Assets", EditorAssetItem.Kind.SOURCE_ASSET),
        /** Deterministic source-import definitions. */
        IMPORTS("Imports", EditorAssetItem.Kind.IMPORT_DEFINITION);

        private final String label;
        private final Optional<EditorAssetItem.Kind> assetKind;

        /** Stores the author-facing label for the unfiltered project root. */
        Category(String label) {
            this.label = label;
            assetKind = Optional.empty();
        }

        /** Stores the author-facing label and optional represented asset kind. */
        Category(String label, EditorAssetItem.Kind assetKind) {
            this.label = label;
            this.assetKind = Optional.of(assetKind);
        }

        /** Returns the category label used in navigation and breadcrumbs. */
        String label() {
            return label;
        }

        /** Returns whether the category contains the supplied asset kind. */
        boolean matches(EditorAssetItem.Kind kind) {
            return assetKind.map(candidate -> candidate == kind).orElse(true);
        }

        /** Resolves the one visible category owned by an asset kind. */
        static Category of(EditorAssetItem.Kind kind) {
            return switch (Objects.requireNonNull(kind, "kind")) {
                case WORLD_DEFINITION -> WORLDS;
                case ENTITY_DEFINITION -> ENTITY_DEFINITIONS;
                case SOURCE_ASSET -> SOURCE_ASSETS;
                case IMPORT_DEFINITION -> IMPORTS;
            };
        }
    }
}
