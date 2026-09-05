/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d.internal;

import io.github.glynch.jscene3d.game.FixedUpdate;
import io.github.glynch.jscene3d.game.FrameUpdate;
import io.github.glynch.jscene3d.game.physics.PhysicsBinding;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.physics.CharacterController;
import io.github.glynch.jscene3d.physics.KinematicBody;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateParticipant;
import io.github.glynch.jscene3d.project.runtime.FixedUpdatePhase;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateParticipant;
import io.github.glynch.jscene3d.project.runtime.scene3d.CharacterBody3d;
import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** Runtime object synchronizing one declarative character between physics and presentation. */
final class CharacterBodyRuntimeObject extends SpatialRuntimeObject
        implements CharacterBody3d, CollisionBodyRuntimeObject, FixedUpdateParticipant, FrameUpdateParticipant {
    private final PhysicsWorld world;
    private final KinematicBody body;
    private final CharacterController controller;
    private final PhysicsBinding binding;

    /** Stores the engine objects created for one declarative character body. */
    CharacterBodyRuntimeObject(
            Object3D object, PhysicsWorld world, KinematicBody body, CharacterController controller) {
        super(object);
        this.world = Objects.requireNonNull(world, "world");
        this.body = Objects.requireNonNull(body, "body");
        this.controller = Objects.requireNonNull(controller, "controller");
        binding = new PhysicsBinding(body, object);
    }

    @Override
    public KinematicBody collisionObject() {
        return body;
    }

    @Override
    public CharacterController controller() {
        return controller;
    }

    @Override
    public void teleport(Vector3fc position, Quaternionfc orientation) {
        controller.teleport(position, orientation);
        binding.snap();
    }

    @Override
    public FixedUpdatePhase fixedUpdatePhase() {
        return FixedUpdatePhase.AFTER_PHYSICS;
    }

    @Override
    public void fixedUpdate(FixedUpdate update) {
        Objects.requireNonNull(update, "update");
        binding.capture();
    }

    @Override
    public void update(FrameUpdate update) {
        binding.apply(Objects.requireNonNull(update, "update").interpolation());
    }

    @Override
    public void close() {
        if (body.isRegistered()) {
            world.remove(body);
        }
        super.close();
    }
}
