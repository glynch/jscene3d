/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** An immovable collision body for floors, walls, and other fixed geometry. */
public final class StaticBody extends CollisionBody {
    StaticBody(PhysicsWorld world, long id, Vector3fc position, Quaternionfc orientation) {
        super(world, id, position, orientation);
    }
}
