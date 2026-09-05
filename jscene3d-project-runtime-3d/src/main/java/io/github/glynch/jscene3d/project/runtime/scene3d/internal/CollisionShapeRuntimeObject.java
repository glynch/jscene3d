/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.CollisionObject;
import java.util.Objects;

/** Runtime object retaining one declarative collider owned by its body parent. */
final class CollisionShapeRuntimeObject extends SpatialRuntimeObject {
    private final CollisionObject body;
    private final Collider collider;

    /** Stores one spatial marker and its registered collider. */
    CollisionShapeRuntimeObject(Object3D object, CollisionObject body, Collider collider) {
        super(object);
        this.body = Objects.requireNonNull(body, "body");
        this.collider = Objects.requireNonNull(collider, "collider");
    }

    @Override
    public void close() {
        if (collider.isRegistered() && body.isRegistered()) {
            body.removeCollider(collider);
        }
        super.close();
    }
}
