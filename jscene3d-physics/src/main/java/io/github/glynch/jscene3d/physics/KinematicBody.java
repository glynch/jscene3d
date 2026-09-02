/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** A solid collision body moved explicitly by its caller. */
public final class KinematicBody extends CollisionBody {
    KinematicBody(PhysicsWorld world, long id, Vector3fc position, Quaternionfc orientation) {
        super(world, id, position, orientation);
    }

    /**
     * Replaces the world-space transform and updates every attached collider.
     *
     * @param newPosition new world-space position
     * @param newOrientation new world-space orientation; normalized internally
     */
    public void setTransform(Vector3fc newPosition, Quaternionfc newOrientation) {
        requireRegistered();
        world().updateTransform(this, newPosition, newOrientation);
    }
}
