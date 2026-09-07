/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

/** One portable physical binding contributing to a typed semantic action. */
public sealed interface InputBinding
        permits InputBinding.KeyboardKey,
                InputBinding.DirectionalKeys,
                InputBinding.MouseButton,
                InputBinding.MouseDelta,
                InputBinding.GamepadButton,
                InputBinding.GamepadAxis,
                InputBinding.GamepadStick {
    /** Returns the semantic value shape produced by this binding.
     *
     * @return compatible action value type
     */
    InputValueType valueType();

    /** One keyboard key contributing a digital button value.
     *
     * @param control portable keyboard control identifier
     */
    record KeyboardKey(String control) implements InputBinding {
        /** Validates the portable control identifier. */
        public KeyboardKey {
            control = requireNonBlank(control, "control");
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.BUTTON;
        }
    }

    /** Four keyboard keys combined into one two-dimensional directional value.
     *
     * @param up positive vertical control
     * @param down negative vertical control
     * @param left negative horizontal control
     * @param right positive horizontal control
     */
    record DirectionalKeys(String up, String down, String left, String right) implements InputBinding {
        /** Validates all four portable control identifiers. */
        public DirectionalKeys {
            up = requireNonBlank(up, "up");
            down = requireNonBlank(down, "down");
            left = requireNonBlank(left, "left");
            right = requireNonBlank(right, "right");
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.AXIS_2D;
        }
    }

    /** One mouse button contributing a digital button value.
     *
     * @param control portable mouse-button identifier
     */
    record MouseButton(String control) implements InputBinding {
        /** Validates the portable control identifier. */
        public MouseButton {
            control = requireNonBlank(control, "control");
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.BUTTON;
        }
    }

    /** Relative mouse movement contributing a two-dimensional value.
     *
     * @param scaleX finite horizontal multiplier
     * @param scaleY finite vertical multiplier
     */
    record MouseDelta(float scaleX, float scaleY) implements InputBinding {
        /** Rejects non-finite scaling. */
        public MouseDelta {
            requireFinite(scaleX, "scaleX");
            requireFinite(scaleY, "scaleY");
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.AXIS_2D;
        }
    }

    /** One standard gamepad button contributing a digital button value.
     *
     * @param control portable gamepad-button identifier
     */
    record GamepadButton(String control) implements InputBinding {
        /** Validates the portable control identifier. */
        public GamepadButton {
            control = requireNonBlank(control, "control");
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.BUTTON;
        }
    }

    /** One standard gamepad axis contributing a one-dimensional value.
     *
     * @param control portable gamepad-axis identifier
     * @param deadZone dead-zone magnitude in the half-open unit interval
     * @param scale finite output multiplier
     */
    record GamepadAxis(String control, float deadZone, float scale) implements InputBinding {
        /** Validates axis configuration. */
        public GamepadAxis {
            control = requireNonBlank(control, "control");
            requireDeadZone(deadZone);
            requireFinite(scale, "scale");
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.AXIS_1D;
        }
    }

    /** One standard gamepad stick contributing a two-dimensional value.
     *
     * @param control portable gamepad-stick identifier
     * @param deadZone radial dead-zone magnitude in the half-open unit interval
     * @param invertY whether to reverse the vertical axis
     */
    record GamepadStick(String control, float deadZone, boolean invertY) implements InputBinding {
        /** Validates stick configuration. */
        public GamepadStick {
            control = requireNonBlank(control, "control");
            requireDeadZone(deadZone);
        }

        @Override
        public InputValueType valueType() {
            return InputValueType.AXIS_2D;
        }
    }

    /** Rejects non-finite binding configuration. */
    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }

    /** Restricts dead zones to values that leave a usable output range. */
    private static void requireDeadZone(float deadZone) {
        if (!Float.isFinite(deadZone) || deadZone < 0.0F || deadZone >= 1.0F) {
            throw new IllegalArgumentException("deadZone must be finite and in [0, 1): " + deadZone);
        }
    }
}
