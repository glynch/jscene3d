/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Verifies deterministic Project-browser navigation without requiring JavaFX. */
final class EditorProjectBrowserModelTest {
    /** Groups and orders items by JScene3D category, label, and stable identity. */
    @Test
    void ordersAndCategorizesProjectAssets() {
        EditorProjectBrowserModel model = new EditorProjectBrowserModel();
        model.showProject(List.of(
                item("textures", "source-z", EditorAssetItem.Kind.SOURCE_ASSET),
                item("MAP02", "world-z", EditorAssetItem.Kind.WORLD_DEFINITION),
                item("Barrel", "entity-b", EditorAssetItem.Kind.ENTITY_DEFINITION),
                item("actors", "import-a", EditorAssetItem.Kind.IMPORT_DEFINITION),
                item("MAP01", "world-a", EditorAssetItem.Kind.WORLD_DEFINITION)));

        assertThat(model.visibleItems())
                .extracting(EditorAssetItem::label)
                .containsExactly("MAP01", "MAP02", "Barrel", "textures", "actors");
        assertThat(model.count(EditorProjectBrowserModel.Category.WORLDS)).isEqualTo(2L);

        model.showCategory(EditorProjectBrowserModel.Category.ENTITY_DEFINITIONS);

        assertThat(model.visibleItems()).singleElement().returns("Barrel", EditorAssetItem::label);
    }

    /** Searches labels, kinds, and source paths without changing the selected category. */
    @Test
    void filtersRealProjectedInformationCaseInsensitively() {
        EditorProjectBrowserModel model = new EditorProjectBrowserModel();
        model.showProject(List.of(
                item("MAP01", "world-a", EditorAssetItem.Kind.WORLD_DEFINITION),
                item("player-capsule", "source-a", EditorAssetItem.Kind.SOURCE_ASSET),
                item("actors", "import-a", EditorAssetItem.Kind.IMPORT_DEFINITION)));
        model.showCategory(EditorProjectBrowserModel.Category.SOURCE_ASSETS);

        model.search("CAPSULE");

        assertThat(model.visibleItems()).singleElement().returns("player-capsule", EditorAssetItem::label);
        assertThat(model.category()).isEqualTo(EditorProjectBrowserModel.Category.SOURCE_ASSETS);

        model.search("world definition");

        assertThat(model.visibleItems()).isEmpty();
    }

    /** Retains selection across filtering and refreshed projections while clearing removed assets. */
    @Test
    void retainsOnlyAStillValidStableSelection() {
        EditorProjectBrowserModel model = new EditorProjectBrowserModel();
        EditorAssetItem original = item("MAP01", "world-a", EditorAssetItem.Kind.WORLD_DEFINITION);
        model.showProject(List.of(original));
        model.select(original);

        model.showCategory(EditorProjectBrowserModel.Category.IMPORTS);
        model.search("missing");

        assertThat(model.selectedItem()).contains(original);

        EditorAssetItem refreshed = item("Renamed MAP01", "world-a", EditorAssetItem.Kind.WORLD_DEFINITION);
        model.showProject(List.of(refreshed));

        assertThat(model.selectedItem()).contains(refreshed);

        model.showProject(List.of(item("MAP02", "world-b", EditorAssetItem.Kind.WORLD_DEFINITION)));

        assertThat(model.selectedItem()).isEmpty();
    }

    /** Creates the minimum truthful asset projection needed by browser-model tests. */
    private static EditorAssetItem item(String label, String identity, EditorAssetItem.Kind kind) {
        Path source = Path.of("project", kind.name().toLowerCase(Locale.ROOT), identity);
        EditorInspectorView inspector =
                new EditorInspectorView(label, kind.label(), source.toString(), identity, false, List.of());
        EditorSelection selection = new EditorSelection(EditorSelection.Kind.ASSET, "selection:" + identity, inspector);
        return new EditorAssetItem(label, identity, kind, source, selection);
    }
}
