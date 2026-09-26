/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.internal.Preconditions;
import org.joml.Quaternionf;

/** Order in which Euler-angle rotations are applied. */
public enum RotationOrder {
    /** X, then Y, then Z. */
    XYZ,

    /** X, then Z, then Y. */
    XZY,

    /** Y, then X, then Z. */
    YXZ,

    /** Y, then Z, then X. */
    YZX,

    /** Z, then X, then Y. */
    ZXY,

    /** Z, then Y, then X. */
    ZYX;

    /**
     * Creates the normalized quaternion represented by finite Euler angles in this order.
     *
     * @param x rotation about the X axis in radians
     * @param y rotation about the Y axis in radians
     * @param z rotation about the Z axis in radians
     * @return normalized quaternion for the ordered rotation
     * @throws IllegalArgumentException if any angle is not finite
     */
    public Quaternionf quaternion(float x, float y, float z) {
        Quaternionf result = new Quaternionf();
        setQuaternion(
                result,
                Preconditions.requireFinite(x, "x"),
                Preconditions.requireFinite(y, "y"),
                Preconditions.requireFinite(z, "z"));
        return result.normalize();
    }

    /** Replaces a quaternion with the rotation represented by ordered Euler angles. */
    final void setQuaternion(Quaternionf destination, float x, float y, float z) {
        switch (this) {
            case XYZ -> destination.rotationXYZ(x, y, z);
            case XZY -> destination.rotationX(x).rotateZ(z).rotateY(y);
            case YXZ -> destination.rotationYXZ(y, x, z);
            case YZX -> destination.rotationY(y).rotateZ(z).rotateX(x);
            case ZXY -> destination.rotationZ(z).rotateX(x).rotateY(y);
            case ZYX -> destination.rotationZYX(z, y, x);
        }
    }
}
