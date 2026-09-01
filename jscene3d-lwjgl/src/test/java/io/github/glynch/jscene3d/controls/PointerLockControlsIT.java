/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_FOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.platform.CursorMode;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.InputStateTestDriver;
import io.github.glynch.jscene3d.platform.Window;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

final class PointerLockControlsIT {
    @Test
    void locksUpdatesAndRestoresTheCursor() {
        try (Window window = Window.create(320, 240, "Pointer-lock controls integration test")) {
            PerspectiveCamera camera = new PerspectiveCamera(1.0f, 4.0f / 3.0f, 0.1f, 100.0f);
            InputState input = window.input();
            try (PointerLockControls controls = new PointerLockControls(camera, window)) {
                controls.setRawMouseMotionPreferred(false);
                controls.setSensitivity(0.01f);
                controls.lock();
                InputStateTestDriver.movePointer(input, input.pointerX() + 10.0, input.pointerY() - 5.0);

                assertThat(controls.isLocked()).isTrue();
                assertThat(controls.update()).isTrue();
                assertThat(controls.yaw()).isCloseTo(-0.1f, Offset.offset(1.0e-6f));
                assertThat(controls.pitch()).isCloseTo(0.05f, Offset.offset(1.0e-6f));
            }

            assertThat(window.cursorMode()).isEqualTo(CursorMode.NORMAL);
        }
    }

    @Test
    void configuresAnglesLimitsAndSavedState() {
        try (Window window = Window.create(320, 240, "Pointer-lock state integration test");
                PointerLockControls controls =
                        new PointerLockControls(new PerspectiveCamera(1.0f, 4.0f / 3.0f, 0.1f, 100.0f), window)) {
            controls.setPitchLimits(-PI_OVER_FOUR, PI_OVER_FOUR);
            controls.setAngles(PI_OVER_FOUR, -PI_OVER_FOUR);
            controls.saveState();
            controls.setAngles(0.0f, 0.0f);
            controls.reset();

            assertThat(controls.minimumPitch()).isEqualTo(-PI_OVER_FOUR);
            assertThat(controls.maximumPitch()).isEqualTo(PI_OVER_FOUR);
            assertThat(controls.yaw()).isEqualTo(PI_OVER_FOUR);
            assertThat(controls.pitch()).isEqualTo(-PI_OVER_FOUR);
        }
    }

    @Test
    void rejectsInvalidConfigurationAndParentedCameras() {
        try (Window window = Window.create(320, 240, "Pointer-lock validation test")) {
            PerspectiveCamera camera = new PerspectiveCamera(1.0f, 4.0f / 3.0f, 0.1f, 100.0f);
            Group group = new Group();
            group.add(camera);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new PointerLockControls(camera, window))
                    .withMessage("PointerLockControls requires an unparented camera");

            group.remove(camera);
            PointerLockControls controls = new PointerLockControls(camera, window);
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setSensitivity(-1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> controls.setPitchLimits(1.0f, -1.0f));
            controls.close();
            assertThatIllegalStateException().isThrownBy(controls::update).withMessage("PointerLockControls is closed");
        }
    }
}
