/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import java.util.Objects;

/** Resolves complete definition graphs from one immutable mixed-source index. */
final class IndexedDefinitionResolver implements DefinitionResolver {
    private final DefinitionAssetIndex definitions;

    /** Stores the immutable source index. */
    IndexedDefinitionResolver(DefinitionAssetIndex definitions) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
    }

    @Override
    public DefinitionLoadResult<EntityDefinition> loadEntity(
            AssetRef<EntityDefinition> reference, RegisteredTypeCatalog types) {
        return DefinitionGraphLoader.loadEntity(
                definitions, Objects.requireNonNull(reference, "reference"), Objects.requireNonNull(types, "types"));
    }

    @Override
    public DefinitionLoadResult<SceneDefinition> loadScene(
            AssetRef<SceneDefinition> reference, RegisteredTypeCatalog types) {
        return DefinitionGraphLoader.loadScene(
                definitions, Objects.requireNonNull(reference, "reference"), Objects.requireNonNull(types, "types"));
    }

    @Override
    public DefinitionLoadResult<EntityDefinition> loadAuthoredEntity(
            AssetMetadata metadata, byte[] content, RegisteredTypeCatalog types) {
        AssetMetadata validMetadata = Objects.requireNonNull(metadata, "metadata");
        DefinitionAssetIndex candidate =
                definitions.withAuthoredOverride(validMetadata, Objects.requireNonNull(content, "content"));
        return DefinitionGraphLoader.loadEntity(
                candidate, AssetRef.to(validMetadata.id()), Objects.requireNonNull(types, "types"));
    }

    @Override
    public DefinitionLoadResult<SceneDefinition> loadAuthoredScene(
            AssetMetadata metadata, byte[] content, RegisteredTypeCatalog types) {
        AssetMetadata validMetadata = Objects.requireNonNull(metadata, "metadata");
        DefinitionAssetIndex candidate =
                definitions.withAuthoredOverride(validMetadata, Objects.requireNonNull(content, "content"));
        return DefinitionGraphLoader.loadScene(
                candidate, AssetRef.to(validMetadata.id()), Objects.requireNonNull(types, "types"));
    }
}
