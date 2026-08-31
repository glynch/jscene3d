/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.core.Angles.PI;
import static io.github.glynch.jscene3d.core.Angles.PI_OVER_FOUR;
import static io.github.glynch.jscene3d.core.Angles.PI_OVER_TWO;
import static io.github.glynch.jscene3d.core.Angles.TWO_PI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.core.Camera;
import io.github.glynch.jscene3d.core.OrthographicCamera;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import org.assertj.core.data.Offset;
import org.joml.Vector3f;
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

    @Test
    void exposesStableTargetAndSynchronizesRelativeToIt() {
        OrbitState state = new OrbitState();
        Object target = state.target();

        state.setTarget(1.0f, 2.0f, 3.0f);
        state.synchronize(new Vector3f(1.0f, 2.0f, 8.0f));

        assertThat(state.target()).isSameAs(target);
        assertThat(state.target()).isEqualTo(new Vector3f(1.0f, 2.0f, 3.0f));
        assertThat(state.distance()).isEqualTo(5.0f);
    }

    @Test
    void rejectsCoincidentNonFiniteAndOverflowingCameraDirections() {
        OrbitState state = new OrbitState();
        Vector3f coincident = new Vector3f();
        Vector3f nonFinite = new Vector3f(Float.NaN, 0.0f, 1.0f);
        Vector3f overflowing = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

        assertThatIllegalStateException().isThrownBy(() -> state.synchronize(coincident));
        assertThatIllegalStateException().isThrownBy(() -> state.synchronize(nonFinite));
        assertThatIllegalStateException().isThrownBy(() -> state.synchronize(overflowing));
    }

    @Test
    void ignoresZeroPanAndSupportsWorldPlaneOrthographicPan() {
        OrthographicCamera camera = new OrthographicCamera(-4.0f, 4.0f, 2.0f, -2.0f, 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.pan(0.0, 0.0, 200, 100, camera, 1.0f, false);
        state.pan(10.0, -10.0, 200, 100, camera, 0.0f, false);
        assertThat(state.hasPendingMotion()).isFalse();

        state.pan(10.0, -10.0, 200, 100, camera, 1.0f, false);
        apply(state, camera);

        assertThat(state.target().x()).isCloseTo(-0.4f, TOLERANCE);
        assertThat(state.target().y()).isZero();
        assertThat(state.target().z()).isCloseTo(0.4f, TOLERANCE);
    }

    @Test
    void supportsPointerDollyAndDollyOut() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);

        state.dollyFromPointer(10.0, 1.0f);
        apply(state, camera);
        float pointerDistance = state.distance();

        state.dollyOut(2.0f);
        apply(state, camera);

        assertThat(pointerDistance).isGreaterThan(5.0f);
        assertThat(state.distance()).isCloseTo(pointerDistance * 2.0f, TOLERANCE);
    }

    @Test
    void detectsPerspectiveAndOrthographicLimitViolations() {
        PerspectiveCamera perspective = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(perspective);
        OrbitLimits limits = new OrbitLimits();

        assertThat(state.violatesLimits(perspective, limits)).isFalse();
        limits.setDistance(1.0f, 4.0f);
        assertThat(state.violatesLimits(perspective, limits)).isTrue();

        OrthographicCamera orthographic = new OrthographicCamera(-2.0f, 2.0f, 2.0f, -2.0f, 0.1f, 100.0f);
        orthographic.setPosition(0.0f, 0.0f, 5.0f);
        state.synchronize(orthographic.position());
        limits.setDistance(1.0f, 10.0f);
        limits.setZoom(2.0f, 3.0f);

        assertThat(state.violatesLimits(orthographic, limits)).isTrue();
        orthographic.setZoom(2.0f);
        assertThat(state.violatesLimits(orthographic, limits)).isFalse();
    }

    @Test
    void detectsAngularLimitViolationsBeforeDistanceLimits() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);
        OrbitLimits limits = new OrbitLimits();
        limits.setPolarAngle(0.0f, PI_OVER_FOUR);

        assertThat(state.violatesLimits(camera, limits)).isTrue();
    }

    @Test
    void clearsAllPendingMotion() {
        PerspectiveCamera camera = createPerspectiveCamera(0.0f, 0.0f, 5.0f);
        OrbitState state = synchronizedState(camera);
        state.rotateLeft(1.0f);
        state.rotateUp(1.0f);
        state.pan(1.0, 1.0, 100, 100, camera, 1.0f, true);
        state.dolly(1.0, 1.0f);

        state.clearPendingMotion();

        assertThat(state.hasPendingMotion()).isFalse();
    }

    @Test
    void clampsBothSidesOfWrappedAzimuthIntervals() {
        OrbitLimits limits = new OrbitLimits();
        limits.setAzimuthAngle(-4.0f, -3.0f);

        PerspectiveCamera positiveSide = createPerspectiveCamera(5.0f, 0.0f, 0.0f);
        OrbitState positiveState = synchronizedState(positiveSide);
        positiveState.apply(positiveSide, limits, 1.0f);

        PerspectiveCamera negativeSide = createPerspectiveCamera(-5.0f, 0.0f, 0.0f);
        OrbitState negativeState = synchronizedState(negativeSide);
        negativeState.apply(negativeSide, limits, 1.0f);

        assertThat(positiveState.azimuthAngle()).isCloseTo(TWO_PI - 4.0f, TOLERANCE);
        assertThat(negativeState.azimuthAngle()).isCloseTo(-3.0f, TOLERANCE);
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
