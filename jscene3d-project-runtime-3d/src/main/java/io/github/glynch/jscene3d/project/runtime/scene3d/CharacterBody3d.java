/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d;

import io.github.glynch.jscene3d.physics.CharacterController;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** Runtime view of a declarative character body and its generic movement controller. */
public interface CharacterBody3d extends Scene3dRuntimeObject {
    /**
     * Returns the controller that resolves character movement against the shared physics world.
     *
     * @return retained character controller
     */
    CharacterController controller();

    /**
     * Repositions the character and immediately synchronizes its scene transform.
     *
     * @param position new world-space body position
     * @param orientation new world-space body orientation
     */
    void teleport(Vector3fc position, Quaternionfc orientation);
}
