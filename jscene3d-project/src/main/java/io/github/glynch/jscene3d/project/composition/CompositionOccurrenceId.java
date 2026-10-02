/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Stable identity of one expanded entity occurrence.
 *
 * @param rootDefinition root world or reusable-definition asset
 * @param entityPath ordered local-entity and placement identities from the root
 */
public record CompositionOccurrenceId(AssetId rootDefinition, List<EntityId> entityPath) {
    /** Copies and validates one occurrence identity. */
    public CompositionOccurrenceId {
        Objects.requireNonNull(rootDefinition, "rootDefinition");
        entityPath = List.copyOf(entityPath);
    }

    /**
     * Returns a deterministic child occurrence.
     *
     * @param entity child local-entity or placement identity
     * @return child occurrence
     */
    public CompositionOccurrenceId child(EntityId entity) {
        List<EntityId> path = new ArrayList<>(entityPath);
        path.add(Objects.requireNonNull(entity, "entity"));
        return new CompositionOccurrenceId(rootDefinition, path);
    }
}
