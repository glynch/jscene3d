/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.List;

/** Standard static-body component implementation independent of a concrete physics backend. */
final class InternalStaticBody3d extends AbstractCollisionObject3d implements StaticBody3d {
    /** Stores the world module dependencies used after reference binding. */
    InternalStaticBody3d(
            Entity owner, ComponentId componentId, Spatial3dWorldModule spatial, Physics3dWorldModule physics) {
        super(owner, componentId, spatial, physics);
    }

    @Override
    CollisionObject3dRegistration register(
            Physics3dWorldModule physics, Transform3d transform, List<CollisionShape3d> resolvedShapes) {
        return physics.registerStaticBody(this, transform, resolvedShapes);
    }
}
