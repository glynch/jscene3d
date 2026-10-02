/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete runtime-free interpretation of one validated authored composition.
 *
 * @param kind root composition kind
 * @param rootDefinition root asset identity
 * @param rootSource root source URI
 * @param entities expanded entities in deterministic owner-first order
 * @param connections scoped authored connections in realization order
 * @param endpointExports scoped placement endpoint exports in dependency order
 * @param entityDefinitions loaded reusable definitions keyed by asset identity
 * @param definitionSources sources of loaded reusable definitions
 */
public record CompositionPlan(
        Kind kind,
        AssetId rootDefinition,
        URI rootSource,
        List<CompositionEntity> entities,
        List<CompositionConnection> connections,
        List<CompositionEndpointExport> endpointExports,
        Map<AssetId, EntityDefinition> entityDefinitions,
        Map<AssetId, URI> definitionSources) {
    /** Copies one complete immutable plan. */
    public CompositionPlan {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(rootDefinition, "rootDefinition");
        Objects.requireNonNull(rootSource, "rootSource");
        entities = List.copyOf(entities);
        connections = List.copyOf(connections);
        endpointExports = List.copyOf(endpointExports);
        entityDefinitions = Collections.unmodifiableMap(new LinkedHashMap<>(entityDefinitions));
        definitionSources = Collections.unmodifiableMap(new LinkedHashMap<>(definitionSources));
    }

    /**
     * Returns all effective components in deterministic entity and declaration order.
     *
     * @return immutable flattened component plans
     */
    public List<CompositionComponent> components() {
        return entities.stream().flatMap(entity -> entity.components().stream()).toList();
    }

    /** Root authored definition kind. */
    public enum Kind {
        /** Root is a Scene definition. */
        SCENE_DEFINITION,
        /** Root is a reusable entity definition. */
        ENTITY_DEFINITION
    }
}
