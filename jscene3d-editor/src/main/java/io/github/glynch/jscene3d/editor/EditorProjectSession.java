/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.List;
import java.util.Objects;

/** Editor-owned read-only state assembled from one project directory. */
final class EditorProjectSession {
    private final GameProject project;
    private final AssetCatalog authoredAssets;
    private final RegisteredTypeCatalog types;
    private final DefinitionResolver definitions;
    private final WorldDefinition startupWorld;
    private final EditorHierarchyNode hierarchy;
    private final List<EditorAssetItem> assets;

    /** Stores the validated project data needed by the first authoring views. */
    EditorProjectSession(
            GameProject project,
            AssetCatalog authoredAssets,
            RegisteredTypeCatalog types,
            DefinitionResolver definitions,
            WorldDefinition startupWorld,
            EditorHierarchyNode hierarchy,
            List<EditorAssetItem> assets) {
        this.project = Objects.requireNonNull(project, "project");
        this.authoredAssets = Objects.requireNonNull(authoredAssets, "authoredAssets");
        this.types = Objects.requireNonNull(types, "types");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.startupWorld = Objects.requireNonNull(startupWorld, "startupWorld");
        this.hierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        this.assets = List.copyOf(assets);
    }

    /** Returns the validated project descriptor. */
    GameProject project() {
        return project;
    }

    /** Returns authored definition metadata. */
    AssetCatalog authoredAssets() {
        return authoredAssets;
    }

    /** Returns safe component and resource metadata. */
    RegisteredTypeCatalog types() {
        return types;
    }

    /** Returns the combined authored and generated definition resolver. */
    DefinitionResolver definitions() {
        return definitions;
    }

    /** Returns the configured startup world. */
    WorldDefinition startupWorld() {
        return startupWorld;
    }

    /** Returns the hierarchy projection for the configured startup world. */
    EditorHierarchyNode hierarchy() {
        return hierarchy;
    }

    /** Returns asset-browser items in deterministic order. */
    List<EditorAssetItem> assets() {
        return assets;
    }
}
