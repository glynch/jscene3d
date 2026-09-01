/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import org.assertj.core.data.Offset;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class PointerLockStateTest {
    @Test
    void derivesYawAndPitchFromAnExistingOrientation() {
        PointerLockState state = new PointerLockState();
        Quaternionf orientation = new Quaternionf().rotationYXZ(PI_OVER_FOUR, -PI_OVER_FOUR, 0.0f);

        state.synchronize(orientation);

        assertThat(state.yaw()).isCloseTo(PI_OVER_FOUR, withinTolerance());
        assertThat(state.pitch()).isCloseTo(-PI_OVER_FOUR, withinTolerance());
    }

    @Test
    void appliesRelativeMotionAndClampsPitch() {
        PointerLockState state = new PointerLockState();
        state.setAngles(0.0f, 0.0f);

        boolean changed = state.rotate(100.0, -1000.0, 0.01f, -1.0f, 1.0f);

        assertThat(changed).isTrue();
        assertThat(state.yaw()).isCloseTo(-1.0f, withinTolerance());
        assertThat(state.pitch()).isEqualTo(1.0f);
        assertThat(state.rotate(0.0, 0.0, 0.01f, -1.0f, 1.0f)).isFalse();
        assertThat(state.rotate(10.0, 10.0, 0.0f, -1.0f, 1.0f)).isFalse();
    }

    @Test
    void appliesRollFreeYawAndPitchToCamera() {
        PointerLockState state = new PointerLockState();
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        state.setAngles(PI_OVER_FOUR, -PI_OVER_FOUR);

        state.apply(camera);

        PointerLockState reconstructed = new PointerLockState();
        reconstructed.synchronize(camera.quaternion());
        assertThat(reconstructed.yaw()).isCloseTo(PI_OVER_FOUR, withinTolerance());
        assertThat(reconstructed.pitch()).isCloseTo(-PI_OVER_FOUR, withinTolerance());
    }

    /** Returns a tolerance suitable for single-precision quaternion reconstruction. */
    private static Offset<Float> withinTolerance() {
        return Offset.offset(1.0e-5f);
    }
}
