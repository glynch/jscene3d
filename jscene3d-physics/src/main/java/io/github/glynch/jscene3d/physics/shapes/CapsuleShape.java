/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.shapes;

import io.github.glynch.jscene3d.physics.internal.Preconditions;

/**
 * A capsule centered at its local origin and aligned to local Y.
 *
 * @param radius radius of the cylindrical body and hemispherical caps
 * @param segmentLength distance between the cap centers, excluding both hemispheres
 */
public record CapsuleShape(float radius, float segmentLength) implements CollisionShape {
    /** Creates a capsule. Segment length excludes both hemispherical caps. */
    public CapsuleShape {
        Preconditions.requirePositive(radius, "radius");
        Preconditions.requireNonNegative(segmentLength, "segmentLength");
    }
}
