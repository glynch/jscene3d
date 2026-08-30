/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.core.Angles.PI_OVER_THREE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.core.Group;
import io.github.glynch.jscene3d.core.OrthographicCamera;
import io.github.glynch.jscene3d.core.PerspectiveCamera;
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
}
