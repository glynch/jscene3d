/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.objects.RotationOrder;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** Package-private yaw and pitch state separated from native pointer ownership. */
final class PointerLockState {
    private final Vector3f forward = new Vector3f();

    private float yaw;
    private float pitch;

    /** Derives yaw and pitch from an existing camera orientation. */
    void synchronize(Quaternionfc orientation) {
        forward.set(0.0f, 0.0f, -1.0f);
        orientation.transform(forward);
        yaw = normalizeYaw((float) Math.atan2(-forward.x, -forward.z));
        pitch = (float) Math.asin(Math.clamp(forward.y, -1.0f, 1.0f));
    }

    /** Applies relative pointer motion and reports whether either angle changed. */
    boolean rotate(
            double pointerDeltaX, double pointerDeltaY, float sensitivity, float minimumPitch, float maximumPitch) {
        if (pointerDeltaX == 0.0 && pointerDeltaY == 0.0) {
            return false;
        }
        float nextYaw = normalizeYaw(yaw - (float) pointerDeltaX * sensitivity);
        float nextPitch = Math.clamp(pitch - (float) pointerDeltaY * sensitivity, minimumPitch, maximumPitch);
        boolean changed = nextYaw != yaw || nextPitch != pitch;
        yaw = nextYaw;
        pitch = nextPitch;
        return changed;
    }

    /** Replaces both controlled angles after validation by the public control. */
    void setAngles(float yaw, float pitch) {
        this.yaw = normalizeYaw(yaw);
        this.pitch = pitch;
    }

    /** Applies the current roll-free orientation to an unparented camera. */
    void apply(Camera camera) {
        camera.setRotationFromEuler(pitch, yaw, 0.0f, RotationOrder.YXZ);
    }

    /** Returns the normalized world-Y yaw angle. */
    float yaw() {
        return yaw;
    }

    /** Returns the local-X pitch angle. */
    float pitch() {
        return pitch;
    }

    /** Normalizes yaw to the stable negative-pi through positive-pi interval. */
    private static float normalizeYaw(float yaw) {
        return (float) Math.atan2(Math.sin(yaw), Math.cos(yaw));
    }
}
