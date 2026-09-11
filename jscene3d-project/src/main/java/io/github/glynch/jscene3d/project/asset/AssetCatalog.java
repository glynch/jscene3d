/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable stable-ID index and typed loading interface for authored definition assets. */
public final class AssetCatalog implements DefinitionResolver {
    /** Current authored definition asset-format version. */
    public static final int FORMAT_VERSION = 1;

    private final Path root;
    private final Map<AssetId, AssetMetadata> assets;

    /** Stores a successfully scanned catalog in deterministic path order. */
    AssetCatalog(Path root, List<AssetMetadata> assets) {
        this.root = requireNormalizedAbsolute(root, "root");
        Map<AssetId, AssetMetadata> index = new LinkedHashMap<>();
        for (AssetMetadata asset : assets) {
            AssetMetadata validAsset = Objects.requireNonNull(asset, "asset");
            AssetMetadata previous = index.put(validAsset.id(), validAsset);
            if (previous != null) {
                throw new IllegalArgumentException("assets contains a duplicate id: " + validAsset.id());
            }
        }
        this.assets = Collections.unmodifiableMap(index);
    }

    /**
     * Scans supported authored definition files below one project root.
     *
     * <p>Discovery is recursive and deterministic. Only {@code .entity.json} and {@code .world.json} files are
     * considered. A nested directory containing its own {@code jscene3d.json} is a separate project and its subtree is
     * not scanned. Complete definitions remain unloaded until requested through the resulting catalog.
     *
     * @param projectRoot project directory
     * @return catalog or ordered structured diagnostics
     */
    public static AssetCatalogLoadResult scan(Path projectRoot) {
        return AssetCatalogScanner.scan(Objects.requireNonNull(projectRoot, "projectRoot"));
    }

    /**
     * Returns the normalized real project root.
     *
     * @return catalog root
     */
    public Path root() {
        return root;
    }

    /**
     * Returns catalog entries in deterministic project-relative path order.
     *
     * @return immutable asset metadata
     */
    public List<AssetMetadata> assets() {
        return List.copyOf(assets.values());
    }

    /**
     * Finds current metadata by authoritative asset identity.
     *
     * @param id asset identity
     * @return current metadata, when discovered
     */
    public Optional<AssetMetadata> find(AssetId id) {
        return Optional.ofNullable(assets.get(Objects.requireNonNull(id, "id")));
    }

    /**
     * Resolves, loads, and validates one entity definition and its transitive definition references.
     *
     * @param reference typed entity-definition reference
     * @return loaded definition or ordered structured diagnostics
     */
    public DefinitionLoadResult<EntityDefinition> loadEntity(AssetRef<EntityDefinition> reference) {
        return DefinitionGraphLoader.loadEntity(
                DefinitionAssetIndex.builder(this).build(), Objects.requireNonNull(reference, "reference"));
    }

    /**
     * Resolves and validates an entity definition through both its asset graph and component descriptors.
     *
     * @param reference typed entity-definition reference
     * @param types resolved safe extension metadata
     * @return loaded definition or ordered structural and component diagnostics
     */
    public DefinitionLoadResult<EntityDefinition> loadEntity(
            AssetRef<EntityDefinition> reference, RegisteredTypeCatalog types) {
        return DefinitionGraphLoader.loadEntity(
                DefinitionAssetIndex.builder(this).build(),
                Objects.requireNonNull(reference, "reference"),
                Objects.requireNonNull(types, "types"));
    }

    /**
     * Resolves, loads, and validates one world definition and its transitive definition references.
     *
     * @param reference typed world-definition reference
     * @return loaded definition or ordered structured diagnostics
     */
    public DefinitionLoadResult<WorldDefinition> loadWorld(AssetRef<WorldDefinition> reference) {
        return DefinitionGraphLoader.loadWorld(
                DefinitionAssetIndex.builder(this).build(), Objects.requireNonNull(reference, "reference"));
    }

    /**
     * Resolves and validates a world through both its asset graph and component descriptors.
     *
     * @param reference typed world-definition reference
     * @param types resolved safe extension metadata
     * @return loaded world or ordered structural and component diagnostics
     */
    public DefinitionLoadResult<WorldDefinition> loadWorld(
            AssetRef<WorldDefinition> reference, RegisteredTypeCatalog types) {
        return DefinitionGraphLoader.loadWorld(
                DefinitionAssetIndex.builder(this).build(),
                Objects.requireNonNull(reference, "reference"),
                Objects.requireNonNull(types, "types"));
    }
}
