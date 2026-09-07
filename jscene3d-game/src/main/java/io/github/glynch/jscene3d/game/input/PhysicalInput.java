/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import io.github.glynch.jscene3d.platform.GamepadAxis;
import io.github.glynch.jscene3d.platform.GamepadButton;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;

/** Internal physical-input seam shared by the window adapter and deterministic tests. */
interface PhysicalInput {
    boolean isKeyDown(Key key);

    boolean wasKeyPressed(Key key);

    boolean wasKeyReleased(Key key);

    boolean isMouseButtonDown(MouseButton button);

    boolean wasMouseButtonPressed(MouseButton button);

    boolean wasMouseButtonReleased(MouseButton button);

    default boolean isGamepadButtonDown(GamepadButton button) {
        return false;
    }

    default boolean wasGamepadButtonPressed(GamepadButton button) {
        return false;
    }

    default boolean wasGamepadButtonReleased(GamepadButton button) {
        return false;
    }

    default float gamepadAxis(GamepadAxis axis) {
        return 0.0F;
    }

    double pointerDeltaX();

    double pointerDeltaY();
}
