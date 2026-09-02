/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.MouseButton;
import java.util.EnumSet;
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

    /** Deterministic adapter for the package-private physical-input seam. */
    private static final class FakeInput implements PhysicalInput {
        private final Set<Key> keysDown = EnumSet.noneOf(Key.class);
        private final Set<Key> keysPressed = EnumSet.noneOf(Key.class);
        private final Set<Key> keysReleased = EnumSet.noneOf(Key.class);
        private final Set<MouseButton> buttonsDown = EnumSet.noneOf(MouseButton.class);
        private final Set<MouseButton> buttonsPressed = EnumSet.noneOf(MouseButton.class);
        private final Set<MouseButton> buttonsReleased = EnumSet.noneOf(MouseButton.class);
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
        public double pointerDeltaX() {
            return deltaX;
        }

        @Override
        public double pointerDeltaY() {
            return deltaY;
        }
    }
}
