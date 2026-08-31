/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.render.Renderer;
import java.util.Objects;

/** Stable host services plus the mutable drawable area assigned to one example. */
public final class ExampleContext {
    private final Window window;
    private final Renderer renderer;

    private int logicalLeft;
    private int logicalWidth;
    private int logicalHeight;
    private int framebufferLeft;
    private int framebufferWidth;
    private int framebufferHeight;

    /**
     * Creates a full-window content context.
     *
     * @param window stable open window
     * @param renderer renderer associated with {@code window}
     * @throws NullPointerException if either argument is {@code null}
     */
    public ExampleContext(Window window, Renderer renderer) {
        this.window = Objects.requireNonNull(window, "window");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        setSidebarWidth(0);
    }

    /**
     * Returns the stable open window shared by the host and example.
     *
     * @return shared open window
     */
    public Window window() {
        return window;
    }

    /**
     * Returns the stable renderer shared by the host and example.
     *
     * @return shared renderer
     */
    public Renderer renderer() {
        return renderer;
    }

    /**
     * Reserves a logical-width sidebar on the left and refreshes framebuffer-pixel dimensions.
     *
     * @param sidebarWidth non-negative logical sidebar width below the current window width
     * @throws IllegalArgumentException if the width is negative or leaves no content area
     */
    public void setSidebarWidth(int sidebarWidth) {
        if (sidebarWidth < 0 || sidebarWidth >= window.width()) {
            throw new IllegalArgumentException(
                    "sidebarWidth must be non-negative and below window width: " + sidebarWidth);
        }
        logicalLeft = sidebarWidth;
        refreshDimensions();
    }

    /** Refreshes logical and framebuffer dimensions after a window-size event. */
    public void refreshDimensions() {
        logicalWidth = Math.max(window.width() - logicalLeft, 1);
        logicalHeight = Math.max(window.height(), 1);
        int completeFramebufferWidth = Math.max(window.framebufferWidth(), 1);
        framebufferHeight = Math.max(window.framebufferHeight(), 1);
        double scale = completeFramebufferWidth / (double) Math.max(window.width(), 1);
        framebufferLeft = Math.clamp((int) Math.round(logicalLeft * scale), 0, completeFramebufferWidth - 1);
        framebufferWidth = Math.max(completeFramebufferWidth - framebufferLeft, 1);
    }

    /** Applies the assigned content area to subsequent scene rendering. */
    public void applyRendererViewport() {
        if (framebufferLeft == 0 && framebufferWidth == window.framebufferWidth()) {
            renderer.resetViewport();
        } else {
            renderer.setViewport(framebufferLeft, 0, framebufferWidth, framebufferHeight);
        }
    }

    /**
     * Returns the positive logical content width.
     *
     * @return logical content width
     */
    public int logicalWidth() {
        return logicalWidth;
    }

    /**
     * Returns the positive logical content height.
     *
     * @return logical content height
     */
    public int logicalHeight() {
        return logicalHeight;
    }

    /**
     * Returns the content area's logical left edge.
     *
     * @return logical left edge
     */
    public int logicalLeft() {
        return logicalLeft;
    }

    /**
     * Returns the positive framebuffer-pixel content width.
     *
     * @return framebuffer-pixel content width
     */
    public int framebufferWidth() {
        return framebufferWidth;
    }

    /**
     * Returns the positive framebuffer-pixel content height.
     *
     * @return framebuffer-pixel content height
     */
    public int framebufferHeight() {
        return framebufferHeight;
    }

    /**
     * Returns the current framebuffer-pixel content aspect ratio.
     *
     * @return positive width-to-height ratio
     */
    public float aspectRatio() {
        return (float) framebufferWidth / framebufferHeight;
    }

    /**
     * Returns whether the current pointer position is inside the assigned content area.
     *
     * @return whether the pointer lies within the content area
     */
    public boolean containsPointer() {
        double x = window.input().pointerX();
        double y = window.input().pointerY();
        return x >= logicalLeft && x < logicalLeft + logicalWidth && y >= 0.0 && y < logicalHeight;
    }

    /**
     * Returns current pointer x normalized to the content area's {@code [-1, 1]} range.
     *
     * @return normalized horizontal pointer coordinate
     */
    public float normalizedPointerX() {
        return (float) ((window.input().pointerX() - logicalLeft) * 2.0 / logicalWidth - 1.0);
    }

    /**
     * Returns current pointer y normalized to the content area's upward {@code [-1, 1]} range.
     *
     * @return normalized upward vertical pointer coordinate
     */
    public float normalizedPointerY() {
        return (float) (1.0 - window.input().pointerY() * 2.0 / logicalHeight);
    }
}
