/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.core.Angles.PI;
import static io.github.glynch.jscene3d.core.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.core.Angles.PI_OVER_TWO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.core.Camera;
import io.github.glynch.jscene3d.core.OrthographicCamera;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

final class OrbitStateTest {
    private static final Offset<Float> TOLERANCE = offset(1.0e-5f);

    @Test
    void orbitsAroundTheTarget() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.orbit(25.0, 0.0, 100, 1.0f);
        apply(state, camera);

        assertThat(camera.position().x()).isCloseTo(-5.0f, TOLERANCE);
        assertThat(camera.position().y()).isCloseTo(0.0f, TOLERANCE);
        assertThat(camera.position().z()).isCloseTo(0.0f, TOLERANCE);
    }

    @Test
    void pansTheCameraAndTargetTogether() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.pan(10.0, 0.0, 100, 100, camera, 1.0f, true);
        apply(state, camera);

        assertThat(state.target().x()).isCloseTo(-1.0f, TOLERANCE);
        assertThat(camera.position().x()).isCloseTo(-1.0f, TOLERANCE);
        assertThat(camera.position().z()).isCloseTo(5.0f, TOLERANCE);
    }

    @Test
    void dolliesAndClampsPerspectiveDistance() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.dolly(1.0, 1.0f);
        OrbitLimits limits = new OrbitLimits();
        limits.setDistance(2.0f, 4.0f);
        state.apply(camera, limits, 1.0f);

        assertThat(state.distance()).isEqualTo(4.0f);
        assertThat(camera.position().z()).isCloseTo(4.0f, TOLERANCE);
    }

    @Test
    void changesAndClampsOrthographicZoomWithoutChangingDistance() {
        OrthographicCamera camera = new OrthographicCamera(-2.0f, 2.0f, 2.0f, -2.0f, 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.dollyIn(4.0f);
        OrbitLimits limits = new OrbitLimits();
        limits.setZoom(0.5f, 2.0f);
        state.apply(camera, limits, 1.0f);

        assertThat(camera.zoom()).isEqualTo(2.0f);
        assertThat(camera.position().z()).isCloseTo(5.0f, TOLERANCE);
    }

    @Test
    void retainsUnappliedRotationWhenDamped() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.rotateLeft(PI_OVER_TWO);
        apply(state, camera, 0.5f);

        assertThat(state.hasPendingMotion()).isTrue();
        assertThat(camera.position().x()).isCloseTo(-5.0f * (float) Math.sin(PI_OVER_FOUR), TOLERANCE);

        state.synchronize(camera.position());
        apply(state, camera, 1.0f);
        assertThat(state.hasPendingMotion()).isFalse();
        assertThat(camera.position().x()).isCloseTo(-5.0f, TOLERANCE);
    }

    @Test
    void clampsPolarAndAzimuthAngles() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.rotateLeft(PI);
        state.rotateUp(PI);
        OrbitLimits limits = new OrbitLimits();
        limits.setPolarAngle(PI_OVER_FOUR, PI_OVER_TWO);
        limits.setAzimuthAngle(-PI_OVER_FOUR, PI_OVER_FOUR);
        state.apply(camera, limits, 1.0f);

        assertThat(state.azimuthAngle()).isCloseTo(-PI_OVER_FOUR, TOLERANCE);
        assertThat(state.polarAngle()).isCloseTo(PI_OVER_FOUR, TOLERANCE);
    }

    @Test
    void supportsAzimuthLimitsAcrossThePiSeam() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);
        OrbitLimits limits = new OrbitLimits();
        limits.setAzimuthAngle(3.0f, 4.0f);

        state.apply(camera, limits, 1.0f);

        assertThat(state.azimuthAngle()).isCloseTo(4.0f - 2.0f * PI, TOLERANCE);
    }

    @Test
    void pansFromAWorldUpViewingDirection() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 5.0f, 0.0f);
        OrbitState state = synchronizedState(camera);

        state.pan(10.0, 0.0, 100, 100, camera, 1.0f, true);
        apply(state, camera);

        assertThat(camera.position().isFinite()).isTrue();
        assertThat(state.target().isFinite()).isTrue();
    }

    private static OrbitState synchronizedState(Camera camera) {
        OrbitState state = new OrbitState();
        state.synchronize(camera.position());
        return state;
    }

    private static void apply(OrbitState state, Camera camera) {
        apply(state, camera, 1.0f);
    }

    private static void apply(OrbitState state, Camera camera, float dampingFraction) {
        state.apply(camera, new OrbitLimits(), dampingFraction);
    }

    private static PerspectiveCamera createPerspectiveCamera(float x, float y, float z) {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_TWO, 1.0f, 0.1f, 100.0f);
        camera.setPosition(x, y, z);
        return camera;
    }
}
