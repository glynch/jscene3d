/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.Set;
import java.util.function.BiConsumer;

/** Internal seam which rebuilds Hierarchy and Inspector projections for one document revision. */
@FunctionalInterface
interface EditorHierarchyProjection {
    /** Projects a world, its modified entries, and enabled-state commands. */
    EditorHierarchyNode project(
            WorldDefinition world, Set<EntityId> modifiedEntityIds, BiConsumer<EntityId, Boolean> enabledEditor);
}
