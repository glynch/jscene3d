/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.runtime.PhysicsStepWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.util.List;
import java.util.Optional;
import org.joml.Vector3fc;

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
     * Registers one initially disabled explicitly moved character body.
     *
     * @param body descriptor-backed character component
     * @param transform authoritative entity transform
     * @param shapes non-empty explicitly referenced shape components
     * @param settings immutable authored movement settings
     * @return lifecycle-controlled movement registration
     */
    CharacterBody3dRegistration registerCharacterBody(
            CharacterBody3d body,
            Transform3d transform,
            List<CollisionShape3d> shapes,
            CharacterBody3dSettings settings);

    /**
     * Finds the nearest enabled solid collision shape reached by a world-space ray.
     *
     * <p>Collision sensors are excluded. The direction need not be normalized.
     *
     * @param origin finite world-space ray origin
     * @param direction finite non-zero ray direction
     * @param maximumDistance finite non-negative distance limit
     * @return nearest hit, or empty when no solid shape is reached
     */
    Optional<CollisionRaycastHit3d> raycast(Vector3fc origin, Vector3fc direction, float maximumDistance);

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
