/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.shapes;

import io.github.glynch.jscene3d.physics.internal.Preconditions;

/**
 * A box centered at its local origin. Dimensions are full extents.
 *
 * @param width full local X extent
 * @param height full local Y extent
 * @param depth full local Z extent
 */
public record BoxShape(float width, float height, float depth) implements CollisionShape {
    /** Creates a box with positive, finite dimensions. */
    public BoxShape {
        Preconditions.requirePositive(width, "width");
        Preconditions.requirePositive(height, "height");
        Preconditions.requirePositive(depth, "depth");
    }
}
