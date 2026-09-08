/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import io.github.glynch.jscene3d.platform.GamepadAxis;
import io.github.glynch.jscene3d.platform.GamepadButton;
import io.github.glynch.jscene3d.platform.GamepadState;
import io.github.glynch.jscene3d.platform.InputState;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.project.input.InputActionDefinition;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Compiled mapping from portable physical controls to typed semantic actions. */
public final class InputMap {
    private final Map<InputAction, List<ButtonBinding>> buttons;
    private final Map<InputAction, List<Axis1dBinding>> axes1d;
    private final Map<InputAction, List<Axis2dBinding>> axes2d;

    /** Copies one completed compiled map. */
    private InputMap(
            Map<InputAction, List<ButtonBinding>> buttons,
            Map<InputAction, List<Axis1dBinding>> axes1d,
            Map<InputAction, List<Axis2dBinding>> axes2d) {
        this.buttons = copy(buttons);
        this.axes1d = copy(axes1d);
        this.axes2d = copy(axes2d);
    }

    /** Compiles one validated authored input definition.
     *
     * @param definition validated project input map
     * @return compiled runtime input map
     */
    public static InputMap compile(InputMapDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        Map<InputAction, List<ButtonBinding>> buttons = new LinkedHashMap<>();
        Map<InputAction, List<Axis1dBinding>> axes1d = new LinkedHashMap<>();
        Map<InputAction, List<Axis2dBinding>> axes2d = new LinkedHashMap<>();
        definition
                .actions()
                .forEach((name, action) -> compileAction(new InputAction(name), action, buttons, axes1d, axes2d));
        return new InputMap(buttons, axes1d, axes2d);
    }

    /** Returns the programmatic button-map builder retained for non-project callers.
     *
     * @return empty button-map builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Creates the no-action map used when a project omits input authoring. */
    static InputMap empty() {
        return new InputMap(Map.of(), Map.of(), Map.of());
    }

    /** Samples keyboard and mouse input when no gamepad has been assigned.
     *
     * @param input current window input
     * @param capture host-interface ownership for this update
     * @return immutable semantic snapshot
     */
    public ActionSnapshot sample(InputState input, InputCapture capture) {
        return sample(input, null, capture);
    }

    /** Samples keyboard, mouse, and one runtime-assigned standard gamepad.
     *
     * @param input current window input
     * @param gamepad assigned gamepad state, or {@code null}
     * @param capture host-interface ownership for this update
     * @return immutable semantic snapshot
     */
    public ActionSnapshot sample(InputState input, @Nullable GamepadState gamepad, InputCapture capture) {
        return sample(new WindowInput(Objects.requireNonNull(input, "input"), gamepad), capture);
    }

    /** Resolves semantic state through the deterministic physical-input seam. */
    ActionSnapshot sample(PhysicalInput input, InputCapture capture) {
        PhysicalInput validInput = Objects.requireNonNull(input, "input");
        InputCapture validCapture = Objects.requireNonNull(capture, "capture");
        ActionSnapshot.Builder snapshot = ActionSnapshot.builder();
        buttons.forEach((action, bindings) -> sampleButton(action, bindings, validInput, validCapture, snapshot));
        axes1d.forEach((action, bindings) -> snapshot.axis1d(
                action,
                clamp(bindings.stream()
                        .mapToDouble(binding -> binding.value(validInput))
                        .sum())));
        axes2d.forEach((action, bindings) -> sampleAxis2d(action, bindings, validInput, validCapture, snapshot));
        if (!validCapture.pointer()) {
            snapshot.pointerDelta(validInput.pointerDeltaX(), validInput.pointerDeltaY());
        }
        return snapshot.build();
    }

    /** Aggregates every digital binding assigned to one semantic action. */
    private static void sampleButton(
            InputAction action,
            List<ButtonBinding> bindings,
            PhysicalInput input,
            InputCapture capture,
            ActionSnapshot.Builder snapshot) {
        boolean down = bindings.stream().anyMatch(binding -> binding.isDown(input, capture));
        boolean pressed = bindings.stream().anyMatch(binding -> binding.wasPressed(input, capture));
        boolean released = !down && bindings.stream().anyMatch(binding -> binding.wasReleased(input, capture));
        if (pressed) {
            snapshot.pressed(action);
        } else if (down) {
            snapshot.down(action);
        }
        if (released) {
            snapshot.released(action);
        }
    }

    /** Adds and bounds all two-dimensional bindings for one semantic action. */
    private static void sampleAxis2d(
            InputAction action,
            List<Axis2dBinding> bindings,
            PhysicalInput input,
            InputCapture capture,
            ActionSnapshot.Builder snapshot) {
        float x = 0.0F;
        float y = 0.0F;
        for (Axis2dBinding binding : bindings) {
            InputVector2 value = binding.value(input, capture);
            x += value.x();
            y += value.y();
        }
        snapshot.axis2d(action, clamp(x), clamp(y));
    }

    /** Compiles every binding of one structurally validated action. */
    private static void compileAction(
            InputAction action,
            InputActionDefinition definition,
            Map<InputAction, List<ButtonBinding>> buttons,
            Map<InputAction, List<Axis1dBinding>> axes1d,
            Map<InputAction, List<Axis2dBinding>> axes2d) {
        switch (definition.valueType()) {
            case BUTTON ->
                buttons.put(
                        action,
                        definition.bindings().stream()
                                .map(InputMap::compileButton)
                                .toList());
            case AXIS_1D ->
                axes1d.put(
                        action,
                        definition.bindings().stream()
                                .map(InputMap::compileAxis1d)
                                .toList());
            case AXIS_2D ->
                axes2d.put(
                        action,
                        definition.bindings().stream()
                                .map(InputMap::compileAxis2d)
                                .toList());
        }
    }

    /** Compiles one digital binding. */
    private static ButtonBinding compileButton(InputBinding binding) {
        return switch (binding) {
            case InputBinding.KeyboardKey(String control) -> new KeyBinding(key(control));
            case InputBinding.MouseButton(String control) -> new MouseBinding(mouseButton(control));
            case InputBinding.GamepadButton(String control) -> new GamepadButtonBinding(gamepadButton(control));
            default -> throw new IllegalArgumentException("not a button binding: " + binding);
        };
    }

    /** Compiles one scalar binding. */
    private static Axis1dBinding compileAxis1d(InputBinding binding) {
        return switch (binding) {
            case InputBinding.GamepadAxis(String control, float deadZone, float scale) ->
                new GamepadAxisBinding(gamepadAxis(control), deadZone, scale);
            default -> throw new IllegalArgumentException("not an axis-1d binding: " + binding);
        };
    }

    /** Compiles one vector binding. */
    private static Axis2dBinding compileAxis2d(InputBinding binding) {
        return switch (binding) {
            case InputBinding.DirectionalKeys(String up, String down, String left, String right) ->
                new DirectionalKeysBinding(key(up), key(down), key(left), key(right));
            case InputBinding.MouseDelta(float scaleX, float scaleY) -> new MouseDeltaBinding(scaleX, scaleY);
            case InputBinding.GamepadStick(String control, float deadZone, boolean invertY) ->
                new GamepadStickBinding(stickAxes(control), deadZone, invertY);
            default -> throw new IllegalArgumentException("not an axis-2d binding: " + binding);
        };
    }

    /** Resolves a portable enum control name. */
    private static Key key(String control) {
        return enumValue(Key.class, control, "keyboard");
    }

    /** Resolves a portable enum control name. */
    private static MouseButton mouseButton(String control) {
        return enumValue(MouseButton.class, control, "mouse");
    }

    /** Resolves a portable enum control name. */
    private static GamepadButton gamepadButton(String control) {
        return enumValue(GamepadButton.class, control, "gamepad button");
    }

    /** Resolves a portable enum control name. */
    private static GamepadAxis gamepadAxis(String control) {
        return enumValue(GamepadAxis.class, control, "gamepad axis");
    }

    /** Resolves a standard stick to its two physical axes. */
    private static StickAxes stickAxes(String control) {
        return switch (control) {
            case "left-stick" -> new StickAxes(GamepadAxis.LEFT_STICK_X, GamepadAxis.LEFT_STICK_Y);
            case "right-stick" -> new StickAxes(GamepadAxis.RIGHT_STICK_X, GamepadAxis.RIGHT_STICK_Y);
            default -> throw new IllegalArgumentException("unsupported gamepad stick control: " + control);
        };
    }

    /** Converts lowercase-hyphen authoring names to platform enum constants. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String control, String device) {
        try {
            return Enum.valueOf(type, control.replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("unsupported " + device + " control: " + control, failure);
        }
    }

    /** Clamps one aggregate to the semantic axis range. */
    private static float clamp(double value) {
        return (float) Math.clamp(value, -1.0, 1.0);
    }

    /** Applies a symmetric dead zone and rescales the remaining range. */
    private static float deadZone(float value, float threshold) {
        float magnitude = Math.abs(value);
        return magnitude <= threshold ? 0.0F : Math.copySign((magnitude - threshold) / (1.0F - threshold), value);
    }

    /** Immutably copies ordered binding groups. */
    private static <T> Map<InputAction, List<T>> copy(Map<InputAction, List<T>> source) {
        Map<InputAction, List<T>> result = new LinkedHashMap<>();
        source.forEach((action, bindings) -> result.put(action, List.copyOf(bindings)));
        return Collections.unmodifiableMap(result);
    }

    /** A sampled digital binding. */
    private sealed interface ButtonBinding permits KeyBinding, MouseBinding, GamepadButtonBinding {
        boolean isDown(PhysicalInput input, InputCapture capture);

        boolean wasPressed(PhysicalInput input, InputCapture capture);

        boolean wasReleased(PhysicalInput input, InputCapture capture);
    }

    /** A sampled one-dimensional binding. */
    private interface Axis1dBinding {
        float value(PhysicalInput input);
    }

    /** A sampled two-dimensional binding. */
    private interface Axis2dBinding {
        InputVector2 value(PhysicalInput input, InputCapture capture);
    }

    /** Keyboard implementation of one digital binding. */
    private record KeyBinding(Key key) implements ButtonBinding {
        @Override
        public boolean isDown(PhysicalInput input, InputCapture capture) {
            return !capture.keyboard() && input.isKeyDown(key);
        }

        @Override
        public boolean wasPressed(PhysicalInput input, InputCapture capture) {
            return !capture.keyboard() && input.wasKeyPressed(key);
        }

        @Override
        public boolean wasReleased(PhysicalInput input, InputCapture capture) {
            return !capture.keyboard() && input.wasKeyReleased(key);
        }
    }

    /** Mouse implementation of one digital binding. */
    private record MouseBinding(MouseButton button) implements ButtonBinding {
        @Override
        public boolean isDown(PhysicalInput input, InputCapture capture) {
            return !capture.pointer() && input.isMouseButtonDown(button);
        }

        @Override
        public boolean wasPressed(PhysicalInput input, InputCapture capture) {
            return !capture.pointer() && input.wasMouseButtonPressed(button);
        }

        @Override
        public boolean wasReleased(PhysicalInput input, InputCapture capture) {
            return !capture.pointer() && input.wasMouseButtonReleased(button);
        }
    }

    /** Standard-gamepad implementation of one digital binding. */
    private record GamepadButtonBinding(GamepadButton button) implements ButtonBinding {
        @Override
        public boolean isDown(PhysicalInput input, InputCapture capture) {
            return input.isGamepadButtonDown(button);
        }

        @Override
        public boolean wasPressed(PhysicalInput input, InputCapture capture) {
            return input.wasGamepadButtonPressed(button);
        }

        @Override
        public boolean wasReleased(PhysicalInput input, InputCapture capture) {
            return input.wasGamepadButtonReleased(button);
        }
    }

    /** Standard-gamepad implementation of one scalar binding. */
    private record GamepadAxisBinding(GamepadAxis axis, float deadZone, float scale) implements Axis1dBinding {
        @Override
        public float value(PhysicalInput input) {
            float raw = input.gamepadAxis(axis);
            float normalized =
                    axis == GamepadAxis.LEFT_TRIGGER || axis == GamepadAxis.RIGHT_TRIGGER ? (raw + 1.0F) * 0.5F : raw;
            return clamp(InputMap.deadZone(normalized, deadZone) * scale);
        }
    }

    /** Four keyboard keys combined into one vector binding. */
    private record DirectionalKeysBinding(Key up, Key down, Key left, Key right) implements Axis2dBinding {
        @Override
        public InputVector2 value(PhysicalInput input, InputCapture capture) {
            if (capture.keyboard()) {
                return InputVector2.ZERO;
            }
            float x = (input.isKeyDown(right) ? 1.0F : 0.0F) - (input.isKeyDown(left) ? 1.0F : 0.0F);
            float y = (input.isKeyDown(up) ? 1.0F : 0.0F) - (input.isKeyDown(down) ? 1.0F : 0.0F);
            return new InputVector2(x, y);
        }
    }

    /** Relative mouse movement converted into one vector binding. */
    private record MouseDeltaBinding(float scaleX, float scaleY) implements Axis2dBinding {
        @Override
        public InputVector2 value(PhysicalInput input, InputCapture capture) {
            return capture.pointer()
                    ? InputVector2.ZERO
                    : new InputVector2(clamp(input.pointerDeltaX() * scaleX), clamp(input.pointerDeltaY() * scaleY));
        }
    }

    /** Two standard-gamepad axes converted with one radial dead zone. */
    private record GamepadStickBinding(StickAxes axes, float deadZone, boolean invertY) implements Axis2dBinding {
        @Override
        public InputVector2 value(PhysicalInput input, InputCapture capture) {
            float x = input.gamepadAxis(axes.x());
            float y = input.gamepadAxis(axes.y()) * (invertY ? -1.0F : 1.0F);
            float magnitude = Math.min(1.0F, (float) Math.sqrt(x * x + y * y));
            if (magnitude <= deadZone) {
                return InputVector2.ZERO;
            }
            float outputMagnitude = (magnitude - deadZone) / (1.0F - deadZone);
            float scale = outputMagnitude / magnitude;
            return new InputVector2(clamp(x * scale), clamp(y * scale));
        }
    }

    /** Physical axes underlying one standard gamepad stick. */
    private record StickAxes(GamepadAxis x, GamepadAxis y) {}

    /** Window and optional gamepad adapter for the internal physical-input seam. */
    private record WindowInput(InputState input, @Nullable GamepadState gamepad) implements PhysicalInput {
        @Override
        public boolean isKeyDown(Key key) {
            return input.isKeyDown(key);
        }

        @Override
        public boolean wasKeyPressed(Key key) {
            return input.wasKeyPressed(key);
        }

        @Override
        public boolean wasKeyReleased(Key key) {
            return input.wasKeyReleased(key);
        }

        @Override
        public boolean isMouseButtonDown(MouseButton button) {
            return input.isMouseButtonDown(button);
        }

        @Override
        public boolean wasMouseButtonPressed(MouseButton button) {
            return input.wasMouseButtonPressed(button);
        }

        @Override
        public boolean wasMouseButtonReleased(MouseButton button) {
            return input.wasMouseButtonReleased(button);
        }

        @Override
        public boolean isGamepadButtonDown(GamepadButton button) {
            return gamepad != null && gamepad.isButtonDown(button);
        }

        @Override
        public boolean wasGamepadButtonPressed(GamepadButton button) {
            return gamepad != null && gamepad.wasButtonPressed(button);
        }

        @Override
        public boolean wasGamepadButtonReleased(GamepadButton button) {
            return gamepad != null && gamepad.wasButtonReleased(button);
        }

        @Override
        public float gamepadAxis(GamepadAxis axis) {
            return gamepad == null ? 0.0F : gamepad.axis(axis);
        }

        @Override
        public double pointerDeltaX() {
            return input.pointerDeltaX();
        }

        @Override
        public double pointerDeltaY() {
            return input.pointerDeltaY();
        }
    }

    /** Builds an immutable button map for non-project clients. */
    public static final class Builder {
        private final Map<InputAction, List<ButtonBinding>> bindings = new LinkedHashMap<>();

        private Builder() {}

        /** Binds a keyboard key to an action.
         *
         * @param action semantic action
         * @param key physical keyboard key
         * @return this builder
         */
        public Builder bind(InputAction action, Key key) {
            return add(action, new KeyBinding(Objects.requireNonNull(key, "key")));
        }

        /** Binds a mouse button to an action.
         *
         * @param action semantic action
         * @param button physical mouse button
         * @return this builder
         */
        public Builder bind(InputAction action, MouseButton button) {
            return add(action, new MouseBinding(Objects.requireNonNull(button, "button")));
        }

        /** Builds a map containing at least one binding.
         *
         * @return immutable input map
         */
        public InputMap build() {
            if (bindings.isEmpty()) {
                throw new IllegalStateException("An input map requires at least one binding");
            }
            return new InputMap(bindings, Map.of(), Map.of());
        }

        /** Adds one unique physical binding to an action. */
        private Builder add(InputAction action, ButtonBinding binding) {
            List<ButtonBinding> actionBindings =
                    bindings.computeIfAbsent(Objects.requireNonNull(action, "action"), ignored -> new ArrayList<>());
            if (!actionBindings.contains(binding)) {
                actionBindings.add(binding);
            }
            return this;
        }
    }
}
