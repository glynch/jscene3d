/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.StaticBody;
import java.util.Objects;

/** Runtime object pairing one authored spatial node with an immovable collision body. */
final class StaticBodyRuntimeObject extends SpatialRuntimeObject implements CollisionBodyRuntimeObject {
    private final PhysicsWorld world;
    private final StaticBody body;

    /** Stores the scene and physics objects created for one declarative static body. */
    StaticBodyRuntimeObject(Object3D object, PhysicsWorld world, StaticBody body) {
        super(object);
        this.world = Objects.requireNonNull(world, "world");
        this.body = Objects.requireNonNull(body, "body");
    }

    /** Returns the body to which direct collision-shape children attach. */
    public StaticBody collisionObject() {
        return body;
    }

    @Override
    public void close() {
        if (body.isRegistered()) {
            world.remove(body);
        }
        super.close();
    }
}
