/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** Computes broad-phase bounds for supported shapes. */
final class ShapeBounds {
    private ShapeBounds() {}

    static Aabb of(ShapePose pose) {
        Vector3f center = pose.position();
        Vector3f extent = extent(pose);
        return new Aabb(new Vector3f(center).sub(extent), new Vector3f(center).add(extent));
    }

    static Aabb swept(ShapePose pose, Vector3f translation) {
        Aabb start = of(pose);
        Aabb end = new Aabb(
                new Vector3f(start.minimum()).add(translation), new Vector3f(start.maximum()).add(translation));
        return Aabb.combine(start, end);
    }

    private static Vector3f extent(ShapePose pose) {
        return switch (pose.shape()) {
            case SphereShape sphere -> new Vector3f(sphere.radius());
            case BoxShape box -> boxExtent(box, pose);
            case CapsuleShape capsule -> capsuleExtent(capsule, pose);
        };
    }

    private static Vector3f boxExtent(BoxShape box, ShapePose pose) {
        Vector3f half = new Vector3f(box.width(), box.height(), box.depth()).mul(0.5F);
        Matrix3f rotation = new Matrix3f().set(pose.orientation());
        return new Vector3f(
                Math.abs(rotation.m00()) * half.x
                        + Math.abs(rotation.m10()) * half.y
                        + Math.abs(rotation.m20()) * half.z,
                Math.abs(rotation.m01()) * half.x
                        + Math.abs(rotation.m11()) * half.y
                        + Math.abs(rotation.m21()) * half.z,
                Math.abs(rotation.m02()) * half.x
                        + Math.abs(rotation.m12()) * half.y
                        + Math.abs(rotation.m22()) * half.z);
    }

    private static Vector3f capsuleExtent(CapsuleShape capsule, ShapePose pose) {
        Vector3f axis = new Vector3f(0.0F, capsule.segmentLength() * 0.5F, 0.0F).rotate(pose.orientation());
        return new Vector3f(Math.abs(axis.x), Math.abs(axis.y), Math.abs(axis.z))
                .add(capsule.radius(), capsule.radius(), capsule.radius());
    }
}
