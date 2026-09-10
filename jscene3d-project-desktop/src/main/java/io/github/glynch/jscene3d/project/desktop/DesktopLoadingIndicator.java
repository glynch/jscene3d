/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.project.runtime.ProjectLoadProgressReporter;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.Objects;

/** Compact progress treatment drawn over the retained menu during later world loads. */
final class DesktopLoadingIndicator {
    private static final Color PANEL_COLOR = Color.srgb(0x11161b);
    private static final Color TRACK_COLOR = Color.srgb(0x39434c);
    private static final Color PROGRESS_COLOR = Color.srgb(0xef762f);

    private final String projectName;
    private ProjectLoadProgressReporter.Phase phase = ProjectLoadProgressReporter.Phase.MANIFEST;
    private boolean presented;

    /** Creates an indicator owned by one named project window. */
    DesktopLoadingIndicator(String projectName) {
        this.projectName = Objects.requireNonNull(projectName, "projectName");
    }

    /** Presents a completed loading phase over an already rendered menu frame. */
    void present(ProjectLoadProgressReporter.Phase current, Renderer renderer, Window window) {
        phase = Objects.requireNonNull(current, "current");
        presented = true;
        Window.pollEvents();
        window.setTitle(projectName + " — " + phase.description());
        if (window.framebufferWidth() > 0 && window.framebufferHeight() > 0) {
            renderer.render(this::paint);
            window.swapBuffers();
        }
    }

    /** Restores the ordinary project title after a later world load completes. */
    void finish(Window window) {
        Objects.requireNonNull(window, "window");
        if (presented) {
            window.setTitle(projectName);
            presented = false;
        }
    }

    /** Paints a deliberately small progress panel so the menu remains visible. */
    private void paint(OverlayCanvas canvas, int width, int height) {
        float panelWidth = width * 0.34F;
        float panelHeight = Math.max(30.0F, height * 0.055F);
        float panelX = (width - panelWidth) * 0.5F;
        float panelY = height - panelHeight - Math.max(18.0F, height * 0.035F);
        float inset = Math.max(8.0F, panelHeight * 0.28F);
        float trackHeight = Math.max(6.0F, panelHeight * 0.18F);
        float trackWidth = panelWidth - inset * 2.0F;
        float trackY = panelY + (panelHeight - trackHeight) * 0.5F;
        canvas.roundedRectangle(panelX, panelY, panelWidth, panelHeight, 7.0F, PANEL_COLOR, 0.94F);
        canvas.roundedRectangle(panelX + inset, trackY, trackWidth, trackHeight, 3.0F, TRACK_COLOR, 1.0F);
        canvas.roundedRectangle(
                panelX + inset, trackY, trackWidth * phase.fraction(), trackHeight, 3.0F, PROGRESS_COLOR, 1.0F);
    }
}
