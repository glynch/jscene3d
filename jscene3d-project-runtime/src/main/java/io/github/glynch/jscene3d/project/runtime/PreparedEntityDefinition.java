/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.runtime.internal.InternalPreparedEntityDefinition;

/**
 * Opaque world-bound preparation of one reusable entity definition.
 *
 * <p>A preparation proves that its transitive authored graph and declared runtime resources were resolved before
 * simulation. It may be reused for any number of independent spawn operations in its owning world. It does not expose
 * a live entity and becomes unusable when that world closes.
 */
public sealed interface PreparedEntityDefinition permits InternalPreparedEntityDefinition {
    /**
     * Returns the stable definition reference represented by this preparation.
     *
     * @return reusable definition reference
     */
    AssetRef<EntityDefinition> reference();

    /**
     * Returns the world which exclusively owns this preparation.
     *
     * @return owning world
     */
    World world();
}
