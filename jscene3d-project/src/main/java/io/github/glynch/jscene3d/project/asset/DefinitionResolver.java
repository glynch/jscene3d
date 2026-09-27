/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.world.WorldDefinition;

/** Resolves and validates authored and generated definitions by stable asset identity. */
public interface DefinitionResolver {
    /**
     * Resolves and validates an entity definition and its complete transitive placement graph.
     *
     * @param reference stable entity-definition reference
     * @param types resolved safe component metadata
     * @return loaded definition or ordered diagnostics
     */
    DefinitionLoadResult<EntityDefinition> loadEntity(
            AssetRef<EntityDefinition> reference, RegisteredTypeCatalog types);

    /**
     * Resolves and validates a world definition and its complete transitive placement graph.
     *
     * @param reference stable world-definition reference
     * @param types resolved safe component metadata
     * @return loaded definition or ordered diagnostics
     */
    DefinitionLoadResult<WorldDefinition> loadWorld(AssetRef<WorldDefinition> reference, RegisteredTypeCatalog types);

    /**
     * Resolves an authored entity graph while replacing its root source with caller-owned candidate bytes.
     *
     * <p>The override must identify an authored source already present in this resolver. It is copied before use and
     * affects only this load. This is the authoritative reparse seam for source-preserving authored documents.
     *
     * @param metadata trusted authored catalog metadata
     * @param content complete candidate JSON source
     * @param types resolved safe component metadata
     * @return loaded candidate definition or ordered diagnostics
     */
    DefinitionLoadResult<EntityDefinition> loadAuthoredEntity(
            AssetMetadata metadata, byte[] content, RegisteredTypeCatalog types);

    /**
     * Resolves an authored world graph while replacing its root source with caller-owned candidate bytes.
     *
     * <p>The override must identify an authored source already present in this resolver. It is copied before use and
     * affects only this load. This is the authoritative reparse seam for source-preserving authored documents.
     *
     * @param metadata trusted authored catalog metadata
     * @param content complete candidate JSON source
     * @param types resolved safe component metadata
     * @return loaded candidate world or ordered diagnostics
     */
    DefinitionLoadResult<WorldDefinition> loadAuthoredWorld(
            AssetMetadata metadata, byte[] content, RegisteredTypeCatalog types);
}
