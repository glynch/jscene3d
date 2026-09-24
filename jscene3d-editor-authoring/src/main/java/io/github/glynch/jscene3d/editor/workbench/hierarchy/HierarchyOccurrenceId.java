/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.hierarchy;

import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stable identity of one hierarchy occurrence within an opened world.
 *
 * <p>The world asset identifies the containing authored root and the ordered entity path contains each authored local
 * entity or placement identity traversed from that root. Descendants from one reusable definition therefore remain
 * distinct beneath different placement identities.
 *
 * @param world containing world asset identity
 * @param entityPath immutable authored entity/placement path from the world root
 */
public record HierarchyOccurrenceId(AssetId world, List<EntityId> entityPath) {
    /** Copies and validates the occurrence path. */
    public HierarchyOccurrenceId {
        Objects.requireNonNull(world, "world");
        entityPath = List.copyOf(entityPath);
    }

    /**
     * Returns a descendant identity by appending one authored entity or placement identity.
     *
     * @param entity descendant entity or placement identity
     * @return descendant occurrence identity
     */
    public HierarchyOccurrenceId child(EntityId entity) {
        ArrayList<EntityId> path = new ArrayList<>(entityPath);
        path.add(Objects.requireNonNull(entity, "entity"));
        return new HierarchyOccurrenceId(world, path);
    }
}
