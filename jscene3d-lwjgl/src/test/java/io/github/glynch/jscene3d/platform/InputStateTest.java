/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;

import org.junit.jupiter.api.Test;

final class InputStateTest {
    @Test
    void tracksKeyTransitionsAcrossPollingCycles() {
        InputState input = new InputState();

        input.updateKey(Key.A, GLFW_PRESS);

        assertThat(input.isKeyDown(Key.A)).isTrue();
        assertThat(input.wasKeyPressed(Key.A)).isTrue();
        assertThat(input.wasKeyReleased(Key.A)).isFalse();

        input.beginPoll();

        assertThat(input.isKeyDown(Key.A)).isTrue();
        assertThat(input.wasKeyPressed(Key.A)).isFalse();

        input.updateKey(Key.A, GLFW_REPEAT);
        input.updateKey(Key.A, GLFW_RELEASE);

        assertThat(input.isKeyDown(Key.A)).isFalse();
        assertThat(input.wasKeyReleased(Key.A)).isTrue();
    }

    @Test
    void tracksMouseButtonTransitionsAcrossPollingCycles() {
        InputState input = new InputState();

        input.updateMouseButton(MouseButton.LEFT, GLFW_PRESS);

        assertThat(input.isMouseButtonDown(MouseButton.LEFT)).isTrue();
        assertThat(input.wasMouseButtonPressed(MouseButton.LEFT)).isTrue();
        assertThat(input.wasMouseButtonReleased(MouseButton.LEFT)).isFalse();

        input.beginPoll();
        input.updateMouseButton(MouseButton.LEFT, GLFW_RELEASE);

        assertThat(input.isMouseButtonDown(MouseButton.LEFT)).isFalse();
        assertThat(input.wasMouseButtonPressed(MouseButton.LEFT)).isFalse();
        assertThat(input.wasMouseButtonReleased(MouseButton.LEFT)).isTrue();
    }

    @Test
    void accumulatesPointerAndScrollDeltasThenClearsOnlyTransientState() {
        InputState input = new InputState();
        input.initializePointer(10.0, 20.0);

        input.updatePointer(13.0, 18.0);
        input.updatePointer(15.0, 25.0);
        input.updateScroll(1.5, -2.0);
        input.updateScroll(-0.5, 3.0);

        assertThat(input.pointerX()).isEqualTo(15.0);
        assertThat(input.pointerY()).isEqualTo(25.0);
        assertThat(input.pointerDeltaX()).isEqualTo(5.0);
        assertThat(input.pointerDeltaY()).isEqualTo(5.0);
        assertThat(input.scrollDeltaX()).isEqualTo(1.0);
        assertThat(input.scrollDeltaY()).isEqualTo(1.0);

        input.beginPoll();

        assertThat(input.pointerX()).isEqualTo(15.0);
        assertThat(input.pointerY()).isEqualTo(25.0);
        assertThat(input.pointerDeltaX()).isZero();
        assertThat(input.pointerDeltaY()).isZero();
        assertThat(input.scrollDeltaX()).isZero();
        assertThat(input.scrollDeltaY()).isZero();
    }

    @Test
    void releasesEveryHeldKeyAndMouseButtonWhenFocusIsLost() {
        InputState input = new InputState();
        input.updateKey(Key.A, GLFW_PRESS);
        input.updateKey(Key.B, GLFW_PRESS);
        input.updateMouseButton(MouseButton.LEFT, GLFW_PRESS);
        input.updateMouseButton(MouseButton.RIGHT, GLFW_PRESS);
        input.beginPoll();

        input.releaseHeldButtons();

        assertThat(input.isKeyDown(Key.A)).isFalse();
        assertThat(input.isKeyDown(Key.B)).isFalse();
        assertThat(input.wasKeyReleased(Key.A)).isTrue();
        assertThat(input.wasKeyReleased(Key.B)).isTrue();
        assertThat(input.isMouseButtonDown(MouseButton.LEFT)).isFalse();
        assertThat(input.isMouseButtonDown(MouseButton.RIGHT)).isFalse();
        assertThat(input.wasMouseButtonReleased(MouseButton.LEFT)).isTrue();
        assertThat(input.wasMouseButtonReleased(MouseButton.RIGHT)).isTrue();
    }

    @Test
    void ignoresUnsupportedInputsAndUnknownActions() {
        InputState input = new InputState();

        input.updateKey(null, GLFW_PRESS);
        input.updateKey(Key.A, -1);
        input.updateMouseButton(null, GLFW_PRESS);
        input.updateMouseButton(MouseButton.LEFT, -1);
        input.releaseHeldButtons();

        assertThat(input.isKeyDown(Key.A)).isFalse();
        assertThat(input.isMouseButtonDown(MouseButton.LEFT)).isFalse();
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullPublicQueryArguments() {
        InputState input = new InputState();

        assertThatNullPointerException().isThrownBy(() -> input.isKeyDown(null));
        assertThatNullPointerException().isThrownBy(() -> input.wasKeyPressed(null));
        assertThatNullPointerException().isThrownBy(() -> input.wasKeyReleased(null));
        assertThatNullPointerException().isThrownBy(() -> input.isMouseButtonDown(null));
        assertThatNullPointerException().isThrownBy(() -> input.wasMouseButtonPressed(null));
        assertThatNullPointerException().isThrownBy(() -> input.wasMouseButtonReleased(null));
    }
}
