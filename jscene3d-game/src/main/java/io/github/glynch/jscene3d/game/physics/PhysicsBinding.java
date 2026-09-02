/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.physics;

import io.github.glynch.jscene3d.game.internal.Preconditions;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.physics.CollisionObject;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Presents a collision object's fixed-update transforms through a scene object.
 *
 * <p>Call {@link #capture()} after each completed physics update and {@link #apply(float)} before
 * rendering. The collision object and scene object remain caller-owned. World-space physics
 * transforms are converted through the current parent transform when the scene object is
 * parented.
 */
public final class PhysicsBinding {
    private final CollisionObject collisionObject;
    private final Object3D sceneObject;
    private final Vector3f previousPosition = new Vector3f();
    private final Vector3f currentPosition = new Vector3f();
    private final Quaternionf previousOrientation = new Quaternionf();
    private final Quaternionf currentOrientation = new Quaternionf();
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Quaternionf interpolatedOrientation = new Quaternionf();
    private final Vector3f localPosition = new Vector3f();
    private final Quaternionf localOrientation = new Quaternionf();
    private final Quaternionf parentOrientation = new Quaternionf();
    private final Matrix4f inverseParentTransform = new Matrix4f();

    /**
     * Creates a binding initialized from the collision object's current transform.
     *
     * @param collisionObject registered physics object
     * @param sceneObject scene object used for presentation
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if the physics object is removed
     */
    public PhysicsBinding(CollisionObject collisionObject, Object3D sceneObject) {
        this.collisionObject = Objects.requireNonNull(collisionObject, "collisionObject");
        this.sceneObject = Objects.requireNonNull(sceneObject, "sceneObject");
        if (!collisionObject.isRegistered()) {
            throw new IllegalArgumentException("collisionObject must remain registered");
        }
        snap();
    }

    /**
     * Returns the caller-owned collision object.
     *
     * @return bound collision object
     */
    public CollisionObject collisionObject() {
        return collisionObject;
    }

    /**
     * Returns the caller-owned scene object.
     *
     * @return bound scene object
     */
    public Object3D sceneObject() {
        return sceneObject;
    }

    /**
     * Shifts the current physics transform to previous and captures the latest transform.
     *
     * @throws IllegalStateException if the physics object was removed or a parent transform is not
     *     invertible
     */
    public void capture() {
        requireUsable();
        previousPosition.set(currentPosition);
        previousOrientation.set(currentOrientation);
        collisionObject.position(currentPosition);
        collisionObject.orientation(currentOrientation);
    }

    /**
     * Discards interpolation history and immediately applies the current physics transform.
     *
     * @throws IllegalStateException if the physics object was removed or a parent transform is not
     *     invertible
     */
    public void snap() {
        requireUsable();
        collisionObject.position(currentPosition);
        collisionObject.orientation(currentOrientation);
        previousPosition.set(currentPosition);
        previousOrientation.set(currentOrientation);
        apply(1.0F);
    }

    /**
     * Applies an interpolated presentation transform without changing physics state.
     *
     * @param interpolation finite fraction from zero for previous to one for current
     * @throws IllegalArgumentException if {@code interpolation} is outside the unit interval
     * @throws IllegalStateException if the physics object was removed or a parent transform is not
     *     invertible
     */
    public void apply(float interpolation) {
        requireUsable();
        float validInterpolation = Preconditions.requireUnitInterval(interpolation, "interpolation");
        previousPosition.lerp(currentPosition, validInterpolation, interpolatedPosition);
        previousOrientation.slerp(currentOrientation, validInterpolation, interpolatedOrientation);
        applyWorldTransform();
    }

    /** Rejects use after the physics object is removed. */
    private void requireUsable() {
        if (!collisionObject.isRegistered()) {
            throw new IllegalStateException("bound collisionObject is no longer registered");
        }
    }

    /** Converts the interpolated world transform through the current scene parent. */
    private void applyWorldTransform() {
        Object3D parent = sceneObject.parent();
        if (parent == null) {
            sceneObject.setPosition(interpolatedPosition);
            sceneObject.setQuaternion(interpolatedOrientation);
            return;
        }
        Matrix4fc parentTransform = parent.matrixWorld();
        if (Math.abs(parentTransform.determinant()) < 1.0E-8F) {
            throw new IllegalStateException("bound sceneObject parent transform is not invertible");
        }
        parentTransform.invert(inverseParentTransform).transformPosition(interpolatedPosition, localPosition);
        parent.worldQuaternion(parentOrientation).conjugate().mul(interpolatedOrientation, localOrientation);
        sceneObject.setPosition(localPosition);
        sceneObject.setQuaternion(localOrientation);
    }
}
