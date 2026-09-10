/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.Key;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.Objects;

/** Persistent native failure surface used when startup cannot reach the main menu. */
final class DesktopStartupErrorScreen {
    private static final Color BACKGROUND = Color.srgb(0x100b0d);
    private static final Color PANEL = Color.srgb(0x571821);
    private static final Color ACCENT = Color.srgb(0xef354c);

    private final String windowTitle;

    /** Creates a concise title from one startup failure. */
    DesktopStartupErrorScreen(String projectName, RuntimeException failure) {
        Objects.requireNonNull(projectName, "projectName");
        Objects.requireNonNull(failure, "failure");
        String detail = failure.getMessage();
        windowTitle = projectName + " — Startup failed" + (detail == null ? "" : ": " + detail);
    }

    /** Keeps the error visible and responsive until the player closes it or presses Escape. */
    void run(Renderer renderer, Window window) {
        window.setTitle(windowTitle);
        window.show();
        while (!window.shouldClose()) {
            Window.pollEvents();
            if (window.input().wasKeyPressed(Key.ESCAPE)) {
                window.requestClose();
            }
            if (window.framebufferWidth() > 0 && window.framebufferHeight() > 0) {
                renderer.clear();
                renderer.render(this::paint);
                window.swapBuffers();
            }
        }
    }

    /** Paints an unmistakable error panel while the detailed diagnostic remains in the title. */
    private void paint(OverlayCanvas canvas, int width, int height) {
        canvas.roundedRectangle(0.0F, 0.0F, width, height, 0.0F, BACKGROUND, 1.0F);
        float panelWidth = width * 0.62F;
        float panelHeight = Math.max(54.0F, height * 0.12F);
        float x = (width - panelWidth) * 0.5F;
        float y = (height - panelHeight) * 0.5F;
        canvas.roundedRectangle(x, y, panelWidth, panelHeight, 10.0F, PANEL, 0.98F);
        canvas.roundedRectangle(x, y, Math.max(8.0F, panelWidth * 0.018F), panelHeight, 5.0F, ACCENT, 1.0F);
    }
}
