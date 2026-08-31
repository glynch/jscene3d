/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.core.Group;
import io.github.glynch.jscene3d.core.OrthographicCamera;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.InputStateTestDriver;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.platform.Window;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class OrbitControlsIT {
    @Test
    void exposesAndAppliesThePublicControlInterface() {
        try (Window window = Window.create(320, 240, "Orbit controls integration test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);

            assertThat(controls.target()).isSameAs(controls.target());
            assertThat(controls.isEnabled()).isTrue();
            assertThat(controls.isRotationEnabled()).isTrue();
            assertThat(controls.isPanningEnabled()).isTrue();
            assertThat(controls.isZoomEnabled()).isTrue();
            assertThat(controls.update()).isFalse();

            controls.setTarget(1.0f, 0.0f, 0.0f);

            assertThat(controls.update()).isTrue();
            assertThat(controls.update()).isFalse();

            controls.rotateLeft(PI_OVER_THREE);
            controls.pan(2.0f, -1.0f);
            controls.dollyIn(2.0f);
            assertThat(controls.distance()).isPositive();

            controls.reset();
            assertThat(controls.target()).isEqualTo(new Vector3f());
        }
    }

    @Test
    void controlsOrthographicZoom() {
        try (Window window = Window.create(320, 240, "Orthographic orbit controls test")) {
            OrthographicCamera camera = new OrthographicCamera(-2.0f, 2.0f, 1.5f, -1.5f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);
            controls.setZoomLimits(0.5f, 3.0f);

            controls.dollyIn(2.0f);

            assertThat(camera.zoom()).isEqualTo(2.0f);
            assertThat(camera.position().z()).isEqualTo(5.0f);
        }
    }

    @Test
    void rejectsInvalidRelationshipsAndLifecycleState() {
        try (Window window = Window.create(320, 240, "Orbit controls validation test")) {
            PerspectiveCamera parentedCamera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            Group parent = new Group();
            parent.add(parentedCamera);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new OrbitControls(parentedCamera, window))
                    .withMessage("OrbitControls requires an unparented camera");

            PerspectiveCamera coincidentCamera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            assertThatIllegalStateException()
                    .isThrownBy(() -> new OrbitControls(coincidentCamera, window))
                    .withMessage("Orbit camera position must differ from its target");
        }
    }

    @Test
    void roundTripsConfigurationAndRejectsInvalidValues() {
        try (Window window = Window.create(320, 240, "Orbit controls configuration test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);

            controls.setEnabled(false);
            controls.setRotationEnabled(false);
            controls.setPanningEnabled(false);
            controls.setZoomEnabled(false);
            controls.setDistanceLimits(2.0f, 10.0f);
            controls.setZoomLimits(0.5f, 4.0f);
            controls.setPolarAngleLimits(0.1f, 2.0f);
            controls.setAzimuthAngleLimits(-1.0f, 1.0f);
            controls.setRotationSpeed(1.5f);
            controls.setPanSpeed(1.25f);
            controls.setZoomSpeed(0.75f);
            controls.setKeyPanSpeed(8.0f);
            controls.setKeyRotationSpeed(1.75f);
            controls.setDampingEnabled(true);
            controls.setDampingFactor(0.25f);
            controls.setAutoRotationEnabled(true);
            controls.setAutoRotationSpeed(-2.0f);
            controls.setScreenSpacePanning(false);

            assertThat(controls.isEnabled()).isFalse();
            assertThat(controls.isRotationEnabled()).isFalse();
            assertThat(controls.isPanningEnabled()).isFalse();
            assertThat(controls.isZoomEnabled()).isFalse();
            assertThat(controls.minimumDistance()).isEqualTo(2.0f);
            assertThat(controls.maximumDistance()).isEqualTo(10.0f);
            assertThat(controls.minimumZoom()).isEqualTo(0.5f);
            assertThat(controls.maximumZoom()).isEqualTo(4.0f);
            assertThat(controls.minimumPolarAngle()).isEqualTo(0.1f);
            assertThat(controls.maximumPolarAngle()).isEqualTo(2.0f);
            assertThat(controls.minimumAzimuthAngle()).isEqualTo(-1.0f);
            assertThat(controls.maximumAzimuthAngle()).isEqualTo(1.0f);
            assertThat(controls.rotationSpeed()).isEqualTo(1.5f);
            assertThat(controls.panSpeed()).isEqualTo(1.25f);
            assertThat(controls.zoomSpeed()).isEqualTo(0.75f);
            assertThat(controls.keyPanSpeed()).isEqualTo(8.0f);
            assertThat(controls.keyRotationSpeed()).isEqualTo(1.75f);
            assertThat(controls.isDampingEnabled()).isTrue();
            assertThat(controls.dampingFactor()).isEqualTo(0.25f);
            assertThat(controls.isAutoRotationEnabled()).isTrue();
            assertThat(controls.autoRotationSpeed()).isEqualTo(-2.0f);
            assertThat(controls.isScreenSpacePanning()).isFalse();

            assertThatIllegalArgumentException().isThrownBy(() -> controls.setDistanceLimits(0.0f, 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setDistanceLimits(2.0f, 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setZoomLimits(0.0f, 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setZoomLimits(2.0f, 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setPolarAngleLimits(-1.0f, 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setPolarAngleLimits(2.0f, 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setAzimuthAngleLimits(-4.0f, 4.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setRotationSpeed(-1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setPanSpeed(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setZoomSpeed(-1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setKeyPanSpeed(-1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setKeyRotationSpeed(-1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setDampingFactor(0.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setDampingFactor(2.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setAutoRotationSpeed(Float.NaN));
        }
    }

    @Test
    void processesPointerRotationPanDollyAndScroll() {
        try (Window window = Window.create(320, 240, "Orbit controls pointer-input test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);
            InputState input = window.input();
            InputStateTestDriver.initializePointer(input, 100.0, 100.0);

            InputStateTestDriver.press(input, MouseButton.LEFT);
            InputStateTestDriver.movePointer(input, 120.0, 90.0);
            assertThat(controls.update()).isTrue();
            InputStateTestDriver.release(input, MouseButton.LEFT);
            InputStateTestDriver.beginPoll(input);

            Vector3f targetBeforePan = new Vector3f(controls.target());
            InputStateTestDriver.press(input, MouseButton.RIGHT);
            InputStateTestDriver.movePointer(input, 130.0, 100.0);
            assertThat(controls.update()).isTrue();
            assertThat(controls.target()).isNotEqualTo(targetBeforePan);
            InputStateTestDriver.release(input, MouseButton.RIGHT);
            InputStateTestDriver.beginPoll(input);

            float distanceBeforeDolly = controls.distance();
            InputStateTestDriver.press(input, MouseButton.MIDDLE);
            InputStateTestDriver.movePointer(input, 130.0, 120.0);
            assertThat(controls.update()).isTrue();
            assertThat(controls.distance()).isGreaterThan(distanceBeforeDolly);
            InputStateTestDriver.release(input, MouseButton.MIDDLE);
            InputStateTestDriver.beginPoll(input);

            float distanceBeforeScroll = controls.distance();
            InputStateTestDriver.scroll(input, 0.0, 1.0);
            assertThat(controls.update()).isTrue();
            assertThat(controls.distance()).isLessThan(distanceBeforeScroll);
        }
    }

    @Test
    void processesKeyboardPanAndModifiedRotation() {
        try (Window window = Window.create(320, 240, "Orbit controls keyboard-input test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);
            InputState input = window.input();

            Vector3f initialTarget = new Vector3f(controls.target());
            InputStateTestDriver.press(input, Key.LEFT);
            assertThat(controls.update()).isTrue();
            assertThat(controls.target()).isNotEqualTo(initialTarget);
            InputStateTestDriver.release(input, Key.LEFT);
            InputStateTestDriver.beginPoll(input);

            float initialPolarAngle = controls.polarAngle();
            InputStateTestDriver.press(input, Key.RIGHT_SHIFT);
            InputStateTestDriver.press(input, Key.UP);
            assertThat(controls.update()).isTrue();
            assertThat(controls.polarAngle()).isNotEqualTo(initialPolarAngle);
            InputStateTestDriver.release(input, Key.UP);
            InputStateTestDriver.release(input, Key.RIGHT_SHIFT);
        }
    }

    @Test
    void suppressesPointerInputButRetainsAutomaticAndDampedMotion() {
        try (Window window = Window.create(320, 240, "Orbit controls suppressed-input test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);
            InputState input = window.input();
            InputStateTestDriver.initializePointer(input, 100.0, 100.0);
            InputStateTestDriver.press(input, MouseButton.LEFT);
            InputStateTestDriver.movePointer(input, 140.0, 100.0);

            assertThat(controls.updateWithoutPointerInput()).isFalse();
            InputStateTestDriver.release(input, MouseButton.LEFT);
            InputStateTestDriver.beginPoll(input);

            controls.setDampingEnabled(true);
            controls.setDampingFactor(0.5f);
            controls.rotateLeft(1.0f);
            assertThat(controls.update(1.0f)).isTrue();

            controls.setAutoRotationEnabled(true);
            assertThat(controls.updateWithoutPointerInput(1.0f)).isTrue();
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullTargetsAndInvalidProgrammaticOperations() {
        try (Window window = Window.create(320, 240, "Orbit controls argument-validation test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);

            assertThatNullPointerException().isThrownBy(() -> new OrbitControls(null, window));
            assertThatNullPointerException().isThrownBy(() -> new OrbitControls(camera, null));
            assertThatNullPointerException().isThrownBy(() -> controls.setTarget(null));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setTarget(Float.NaN, 0.0f, 0.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.rotateLeft(-1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.rotateUp(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.pan(Float.NaN, 0.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.dollyIn(1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.dollyOut(Float.POSITIVE_INFINITY));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.update(-1.0f));
        }
    }

    @Test
    void rejectsCameraParentingIntroducedAfterConstruction() {
        try (Window window = Window.create(320, 240, "Orbit controls late-parenting test")) {
            PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_THREE, 4.0f / 3.0f, 0.1f, 100.0f);
            camera.setPosition(0.0f, 0.0f, 5.0f);
            OrbitControls controls = new OrbitControls(camera, window);
            new Group().add(camera);

            assertThatIllegalStateException().isThrownBy(controls::update);
            assertThatIllegalStateException().isThrownBy(controls::saveState);
            assertThatIllegalStateException().isThrownBy(controls::reset);
        }
    }
}
