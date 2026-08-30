/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import org.lwjgl.opengl.GL11;

/** Development-only visible smoke test for the public window lifecycle. */
final class WindowSmoke {
    private WindowSmoke() {}

    /**
     * Opens a resizable blue window until the user closes it or presses Escape.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        WindowOptions options = WindowOptions.builder()
                .size(960, 540)
                .title("JScene3D Window Smoke Test")
                .preferredFramebufferSampleCount(4)
                .build();

        try (Window window = Window.create(options)) {
            window.show();
            while (!window.shouldClose()) {
                Window.pollEvents();
                if (window.input().wasKeyPressed(Key.ESCAPE)) {
                    window.requestClose();
                }

                int width = window.framebufferWidth();
                int height = window.framebufferHeight();
                if (width > 0 && height > 0) {
                    GL11.glViewport(0, 0, width, height);
                    GL11.glClearColor(0.02F, 0.10F, 0.24F, 1.0F);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                    window.swapBuffers();
                }
            }
        }
    }
}
