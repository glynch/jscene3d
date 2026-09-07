/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.platform.GamepadAxis;
import io.github.glynch.jscene3d.platform.GamepadButton;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import io.github.glynch.jscene3d.project.input.InputActionDefinition;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.input.InputValueType;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class InputMapTest {
    private static final InputAction MOVE = new InputAction("move");
    private static final InputAction FIRE = new InputAction("fire");

    @Test
    void combinesPhysicalBindingsIntoSemanticActions() {
        InputMap map = InputMap.builder()
                .bind(MOVE, Key.W)
                .bind(MOVE, Key.UP)
                .bind(FIRE, MouseButton.LEFT)
                .build();
        FakeInput input = new FakeInput();
        input.keysDown.add(Key.UP);
        input.keysPressed.add(Key.UP);
        input.buttonsDown.add(MouseButton.LEFT);
        input.buttonsPressed.add(MouseButton.LEFT);
        input.deltaX = 4.0;
        input.deltaY = -3.0;

        ActionSnapshot snapshot = map.sample(input, InputCapture.NONE);

        assertThat(snapshot.isDown(MOVE)).isTrue();
        assertThat(snapshot.wasPressed(MOVE)).isTrue();
        assertThat(snapshot.isDown(FIRE)).isTrue();
        assertThat(snapshot.wasPressed(FIRE)).isTrue();
        assertThat(snapshot.pointerDeltaX()).isEqualTo(4.0);
        assertThat(snapshot.pointerDeltaY()).isEqualTo(-3.0);
    }

    @Test
    void suppressesOnlyInputOwnedByTheHostInterface() {
        InputMap map = InputMap.builder()
                .bind(MOVE, Key.W)
                .bind(FIRE, MouseButton.LEFT)
                .build();
        FakeInput input = new FakeInput();
        input.keysDown.add(Key.W);
        input.buttonsDown.add(MouseButton.LEFT);
        input.deltaX = 2.0;

        ActionSnapshot keyboardCaptured = map.sample(input, new InputCapture(true, false));
        ActionSnapshot pointerCaptured = map.sample(input, new InputCapture(false, true));

        assertThat(keyboardCaptured.isDown(MOVE)).isFalse();
        assertThat(keyboardCaptured.isDown(FIRE)).isTrue();
        assertThat(keyboardCaptured.pointerDeltaX()).isEqualTo(2.0);
        assertThat(pointerCaptured.isDown(MOVE)).isTrue();
        assertThat(pointerCaptured.isDown(FIRE)).isFalse();
        assertThat(pointerCaptured.pointerDeltaX()).isZero();
    }

    @Test
    void ignoresDuplicateBindingsAndRejectsAnEmptyMap() {
        InputMap map = InputMap.builder().bind(MOVE, Key.W).bind(MOVE, Key.W).build();
        FakeInput input = new FakeInput();
        input.keysReleased.add(Key.W);

        assertThat(map.sample(input, InputCapture.ALL).wasReleased(MOVE)).isFalse();
        assertThat(map.sample(input, InputCapture.NONE).wasReleased(MOVE)).isTrue();
        InputMap.Builder emptyBuilder = InputMap.builder();
        assertThatThrownBy(emptyBuilder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void releasesAnActionOnlyAfterEveryPhysicalBindingIsUp() {
        InputMap map = InputMap.builder().bind(MOVE, Key.W).bind(MOVE, Key.UP).build();
        FakeInput input = new FakeInput();
        input.keysDown.add(Key.UP);
        input.keysReleased.add(Key.W);

        ActionSnapshot stillDown = map.sample(input, InputCapture.NONE);
        input.keysDown.clear();
        ActionSnapshot released = map.sample(input, InputCapture.NONE);

        assertThat(stillDown.isDown(MOVE)).isTrue();
        assertThat(stillDown.wasReleased(MOVE)).isFalse();
        assertThat(released.wasReleased(MOVE)).isTrue();
    }

    @Test
    void compilesTypedAuthoredKeyboardMouseAndGamepadBindings() {
        InputMapDefinition definition = new InputMapDefinition(
                Path.of("/project/input-map.json"),
                Map.of(
                        "fire",
                        new InputActionDefinition(
                                InputValueType.BUTTON,
                                List.of(
                                        new InputBinding.KeyboardKey("SPACE"),
                                        new InputBinding.GamepadButton("button-south"))),
                        "move",
                        new InputActionDefinition(
                                InputValueType.AXIS_2D,
                                List.of(
                                        new InputBinding.DirectionalKeys("W", "S", "A", "D"),
                                        new InputBinding.GamepadStick("left-stick", 0.2F, true))),
                        "look",
                        new InputActionDefinition(
                                InputValueType.AXIS_2D, List.of(new InputBinding.MouseDelta(0.1F, -0.1F))),
                        "throttle",
                        new InputActionDefinition(
                                InputValueType.AXIS_1D,
                                List.of(new InputBinding.GamepadAxis("right-trigger", 0.1F, 1.0F)))));
        FakeInput input = new FakeInput();
        input.gamepadButtonsPressed.add(GamepadButton.BUTTON_SOUTH);
        input.gamepadButtonsDown.add(GamepadButton.BUTTON_SOUTH);
        input.keysDown.add(Key.D);
        input.axes.put(GamepadAxis.LEFT_STICK_Y, 0.6F);
        input.axes.put(GamepadAxis.RIGHT_TRIGGER, 0.1F);
        input.deltaX = 5.0;
        input.deltaY = 2.0;

        ActionSnapshot snapshot = InputMap.compile(definition).sample(input, InputCapture.NONE);

        assertThat(snapshot.wasPressed(new InputAction("fire"))).isTrue();
        assertThat(snapshot.axis2d(new InputAction("move")).x()).isEqualTo(1.0F);
        assertThat(snapshot.axis2d(new InputAction("move")).y()).isNegative();
        assertThat(snapshot.axis2d(new InputAction("look"))).isEqualTo(new InputVector2(0.5F, -0.2F));
        assertThat(snapshot.axis1d(new InputAction("throttle"))).isCloseTo(0.5F, within(0.00001F));
    }

    @Test
    void samplesCapturedCompositesDeadZonesAndGamepadRelease() {
        InputAction turn = new InputAction("turn");
        InputMapDefinition definition = new InputMapDefinition(
                Path.of("/project/input-map.json"),
                Map.of(
                        "fire",
                        new InputActionDefinition(
                                InputValueType.BUTTON,
                                List.of(
                                        new InputBinding.MouseButton("LEFT"),
                                        new InputBinding.GamepadButton("button-east"))),
                        "move",
                        new InputActionDefinition(
                                InputValueType.AXIS_2D,
                                List.of(
                                        new InputBinding.DirectionalKeys("W", "S", "A", "D"),
                                        new InputBinding.GamepadStick("right-stick", 0.25F, false))),
                        "look",
                        new InputActionDefinition(
                                InputValueType.AXIS_2D, List.of(new InputBinding.MouseDelta(0.5F, 0.5F))),
                        "turn",
                        new InputActionDefinition(
                                InputValueType.AXIS_1D,
                                List.of(new InputBinding.GamepadAxis("left-stick-x", 0.2F, 2.0F)))));
        FakeInput input = new FakeInput();
        input.buttonsPressed.add(MouseButton.LEFT);
        input.buttonsDown.add(MouseButton.LEFT);
        input.gamepadButtonsReleased.add(GamepadButton.BUTTON_EAST);
        input.keysDown.add(Key.A);
        input.keysDown.add(Key.W);
        input.keysDown.add(Key.S);
        input.axes.put(GamepadAxis.LEFT_STICK_X, -0.8F);
        input.axes.put(GamepadAxis.RIGHT_STICK_X, 0.1F);
        input.deltaX = 20.0;

        InputMap map = InputMap.compile(definition);
        ActionSnapshot snapshot = map.sample(input, InputCapture.ALL);
        ActionSnapshot uncaptured = map.sample(input, InputCapture.NONE);

        assertThat(snapshot.isDown(FIRE)).isFalse();
        assertThat(snapshot.wasReleased(FIRE)).isTrue();
        assertThat(snapshot.axis2d(MOVE)).isEqualTo(InputVector2.ZERO);
        assertThat(snapshot.axis2d(new InputAction("look"))).isEqualTo(InputVector2.ZERO);
        assertThat(snapshot.axis1d(turn)).isEqualTo(-1.0F);
        assertThat(snapshot.pointerDeltaX()).isZero();
        assertThat(uncaptured.axis2d(MOVE)).isEqualTo(new InputVector2(-1.0F, 0.0F));
        assertThat(uncaptured.axis2d(new InputAction("look")).x()).isEqualTo(1.0F);
    }

    @Test
    void rejectsUnsupportedAuthoredControls() {
        InputMapDefinition keyboard = definition(InputValueType.BUTTON, new InputBinding.KeyboardKey("NOT-A-KEY"));
        InputMapDefinition mouse = definition(InputValueType.BUTTON, new InputBinding.MouseButton("NOT-A-BUTTON"));
        InputMapDefinition gamepad =
                definition(InputValueType.AXIS_2D, new InputBinding.GamepadStick("middle-stick", 0.1F, false));

        assertThatThrownBy(() -> InputMap.compile(keyboard)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InputMap.compile(mouse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InputMap.compile(gamepad)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Creates one single-action authored map for compiler failure tests. */
    private static InputMapDefinition definition(InputValueType type, InputBinding binding) {
        return new InputMapDefinition(
                Path.of("/project/input-map.json"),
                Map.of("action", new InputActionDefinition(type, List.of(binding))));
    }

    /** Deterministic adapter for the package-private physical-input seam. */
    private static final class FakeInput implements PhysicalInput {
        private final Set<Key> keysDown = EnumSet.noneOf(Key.class);
        private final Set<Key> keysPressed = EnumSet.noneOf(Key.class);
        private final Set<Key> keysReleased = EnumSet.noneOf(Key.class);
        private final Set<MouseButton> buttonsDown = EnumSet.noneOf(MouseButton.class);
        private final Set<MouseButton> buttonsPressed = EnumSet.noneOf(MouseButton.class);
        private final Set<MouseButton> buttonsReleased = EnumSet.noneOf(MouseButton.class);
        private final Set<GamepadButton> gamepadButtonsDown = EnumSet.noneOf(GamepadButton.class);
        private final Set<GamepadButton> gamepadButtonsPressed = EnumSet.noneOf(GamepadButton.class);
        private final Set<GamepadButton> gamepadButtonsReleased = EnumSet.noneOf(GamepadButton.class);
        private final Map<GamepadAxis, Float> axes = new EnumMap<>(GamepadAxis.class);
        private double deltaX;
        private double deltaY;

        @Override
        public boolean isKeyDown(Key key) {
            return keysDown.contains(key);
        }

        @Override
        public boolean wasKeyPressed(Key key) {
            return keysPressed.contains(key);
        }

        @Override
        public boolean wasKeyReleased(Key key) {
            return keysReleased.contains(key);
        }

        @Override
        public boolean isMouseButtonDown(MouseButton button) {
            return buttonsDown.contains(button);
        }

        @Override
        public boolean wasMouseButtonPressed(MouseButton button) {
            return buttonsPressed.contains(button);
        }

        @Override
        public boolean wasMouseButtonReleased(MouseButton button) {
            return buttonsReleased.contains(button);
        }

        @Override
        public boolean isGamepadButtonDown(GamepadButton button) {
            return gamepadButtonsDown.contains(button);
        }

        @Override
        public boolean wasGamepadButtonPressed(GamepadButton button) {
            return gamepadButtonsPressed.contains(button);
        }

        @Override
        public boolean wasGamepadButtonReleased(GamepadButton button) {
            return gamepadButtonsReleased.contains(button);
        }

        @Override
        public float gamepadAxis(GamepadAxis axis) {
            return axes.getOrDefault(axis, 0.0F);
        }

        @Override
        public double pointerDeltaX() {
            return deltaX;
        }

        @Override
        public double pointerDeltaY() {
            return deltaY;
        }
    }
}
