/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

final class WindowIT {
    @Test
    void createsAndClosesAHiddenWindow() {
        WindowOptions options = WindowOptions.builder()
                .size(320, 240)
                .title("JScene3D integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        Window window = Window.create(options);

        try {
            assertThat(window.isVisible()).isFalse();
            assertThat(window.isClosed()).isFalse();
            assertThat(window.title()).isEqualTo("JScene3D integration test");
            assertThat(window.width()).isEqualTo(320);
            assertThat(window.height()).isEqualTo(240);
            assertThat(window.framebufferWidth()).isPositive();
            assertThat(window.framebufferHeight()).isPositive();
            assertThat(window.framebufferSampleCount()).isNotNegative();
            assertThat(window.verticalSync()).isEqualTo(VerticalSync.DISABLED);
        } finally {
            window.close();
        }

        window.close();

        assertThat(window.isClosed()).isTrue();
        assertThatIllegalStateException().isThrownBy(window::show).withMessage("Window is closed");
    }

    @Test
    void createsWithDefaultOptionsAndACustomTitle() {
        try (Window window = Window.create("Convenient title")) {
            assertThat(window.title()).isEqualTo("Convenient title");
            assertThat(window.width()).isPositive();
            assertThat(window.height()).isPositive();
        }
    }

    @Test
    void changesItsTitle() {
        try (Window window = Window.create(320, 240, "Initial title")) {
            window.setTitle("Updated title");

            assertThat(window.title()).isEqualTo("Updated title");
        }
    }

    @Test
    void changesItsVerticalSynchronizationMode() {
        try (Window window = Window.create(WindowOptions.defaults())) {
            window.setVerticalSync(VerticalSync.DISABLED);

            assertThat(window.verticalSync()).isEqualTo(VerticalSync.DISABLED);
        }
    }

    @Test
    void recordsAProgrammaticCloseRequest() {
        try (Window window = Window.create(WindowOptions.defaults())) {
            assertThat(window.shouldClose()).isFalse();

            window.requestClose();

            assertThat(window.shouldClose()).isTrue();
        }
    }

    @Test
    void keepsAnotherWindowUsableWhenOneWindowCloses() {
        Window first = Window.create(320, 240, "First");
        Window second = Window.create(320, 240, "Second");

        try {
            first.swapBuffers();
            first.close();
            second.swapBuffers();

            assertThat(second.isClosed()).isFalse();
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void exposesStableInitialStateAfterPollingEvents() {
        try (Window window = Window.create(320, 240, "Input state")) {
            InputState input = window.input();

            Window.pollEvents();

            assertThat(window.input()).isSameAs(input);
            assertThat(input.isKeyDown(Key.W)).isFalse();
            assertThat(input.wasKeyPressed(Key.W)).isFalse();
            assertThat(input.wasKeyReleased(Key.W)).isFalse();
            assertThat(input.isMouseButtonDown(MouseButton.LEFT)).isFalse();
            assertThat(input.pointerDeltaX()).isZero();
            assertThat(input.pointerDeltaY()).isZero();
            assertThat(input.scrollDeltaX()).isZero();
            assertThat(input.scrollDeltaY()).isZero();
            assertThat(window.framebufferSizeChanged()).isFalse();
            assertThat(window.framebufferAspectRatio()).isEqualTo(4.0f / 3.0f);
        }
    }
}
