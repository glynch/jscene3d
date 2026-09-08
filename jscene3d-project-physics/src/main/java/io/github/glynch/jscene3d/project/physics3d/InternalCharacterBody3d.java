/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.spatial3d.Transform3d;
import java.time.Duration;
import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Standard character-body component independent of a concrete physics backend. */
final class InternalCharacterBody3d extends AbstractCollisionObject3d implements CharacterBody3d {
    private final CharacterBody3dSettings settings;

    /** Stores world-module dependencies and immutable authored movement settings. */
    InternalCharacterBody3d(
            Entity owner,
            ComponentId componentId,
            Spatial3dWorldModule spatial,
            Physics3dWorldModule physics,
            CharacterBody3dSettings settings) {
        super(owner, componentId, spatial, physics);
        this.settings = settings;
    }

    @Override
    CharacterBody3dRegistration register(
            Physics3dWorldModule physics, Transform3d transform, List<CollisionShape3d> resolvedShapes) {
        return physics.registerCharacterBody(this, transform, resolvedShapes, settings);
    }

    @Override
    public CharacterMove3dResult move(Vector3fc planarVelocity, Duration fixedStep) {
        return registration().move(planarVelocity, fixedStep);
    }

    @Override
    public boolean tryJump() {
        return registration().tryJump();
    }

    @Override
    public boolean isGrounded() {
        return registration().isGrounded();
    }

    @Override
    public Vector3f groundNormal(Vector3f destination) {
        return registration().groundNormal(destination);
    }

    /** Returns the character-specific backend registration established during reference binding. */
    private CharacterBody3dRegistration registration() {
        return (CharacterBody3dRegistration) requireRegistration();
    }
}
