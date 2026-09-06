/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.runtime.PhysicsStepWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.List;

/** World-scoped seam realizing descriptor-backed 3D collision components. */
public interface Physics3dWorldModule extends PhysicsStepWorldModule {
    /**
     * Registers one initially disabled static body.
     *
     * @param body descriptor-backed body component
     * @param transform authoritative entity transform
     * @param shapes non-empty explicitly referenced shape components
     * @return lifecycle-controlled backend registration
     */
    CollisionObject3dRegistration registerStaticBody(
            StaticBody3d body, Transform3d transform, List<CollisionShape3d> shapes);

    /**
     * Registers one initially disabled non-blocking sensor.
     *
     * @param sensor descriptor-backed sensor component
     * @param transform authoritative entity transform
     * @param shapes non-empty explicitly referenced shape components
     * @param listener synchronous precise-overlap sink
     * @return lifecycle-controlled backend registration
     */
    CollisionObject3dRegistration registerSensor(
            CollisionSensor3d sensor,
            Transform3d transform,
            List<CollisionShape3d> shapes,
            CollisionOverlapListener listener);

    /**
     * Returns the current collision-object count.
     *
     * @return number of currently registered collision objects
     */
    int collisionObjectCount();

    /**
     * Returns the current collision-shape count.
     *
     * @return number of currently registered collision shapes
     */
    int collisionShapeCount();

    /**
     * Returns whether this module has released all backend state.
     *
     * @return whether this module has released all backend state
     */
    boolean isClosed();
}
