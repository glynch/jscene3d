/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.WorldModule;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** World-scoped seam that realizes live three-dimensional transform components. */
public interface Spatial3dWorldModule extends WorldModule {
    /**
     * Creates and registers the unique primary three-dimensional transform for one entity.
     *
     * <p>The composed-world runtime invokes this operation owner before child. When the direct ownership parent has a
     * transform registered in this module, the new transform inherits it; otherwise the transform begins a spatial
     * root. Inputs are copied before this method returns. The returned component owns removal of its registration when
     * the world closes it.
     *
     * @param owner live component owner
     * @param position finite local position
     * @param orientation finite non-zero local orientation
     * @param scale finite local scale
     * @return registered world-owned transform component
     * @throws IllegalArgumentException if an input is invalid, the owner belongs to another world, or the owner already
     *     has a registered transform
     * @throws IllegalStateException if this module is closed
     */
    Transform3d createTransform(Entity owner, Vector3fc position, Quaternionfc orientation, Vector3fc scale);

    /**
     * Returns whether this adapter has released all registered transforms.
     *
     * @return {@code true} after closure
     */
    boolean isClosed();
}
