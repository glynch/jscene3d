/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.shapes;

import io.github.glynch.jscene3d.physics.internal.Preconditions;

/**
 * A sphere centered at its local origin.
 *
 * @param radius sphere radius
 */
public record SphereShape(float radius) implements CollisionShape {
    /** Creates a sphere with a positive, finite radius. */
    public SphereShape {
        Preconditions.requirePositive(radius, "radius");
    }
}
