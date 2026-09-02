/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** A solid collision object that can block movement. */
public abstract sealed class CollisionBody extends CollisionObject permits KinematicBody, StaticBody {
    CollisionBody(PhysicsWorld world, long id, Vector3fc position, Quaternionfc orientation) {
        super(world, id, position, orientation);
    }
}
