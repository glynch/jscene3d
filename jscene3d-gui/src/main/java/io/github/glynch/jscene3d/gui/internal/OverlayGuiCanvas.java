/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui.internal;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Reusable adapter from the internal GUI drawing boundary to the renderer-owned canvas. */
public final class OverlayGuiCanvas implements GuiCanvas {
    private @Nullable OverlayCanvas delegate;

    /** Creates an unbound reusable overlay adapter. */
    public OverlayGuiCanvas() {
        // Binding is established separately for each paint call.
    }

    /**
     * Binds the renderer-owned canvas for one paint call.
     *
     * @param canvas renderer-owned overlay canvas
     */
    public void bind(OverlayCanvas canvas) {
        if (delegate != null) {
            throw new IllegalStateException("GUI canvas is already bound");
        }
        delegate = Objects.requireNonNull(canvas, "canvas");
    }

    /** Releases the renderer-owned canvas after one paint call. */
    public void unbind() {
        delegate = null;
    }

    /** Delegates a rectangle command to the bound renderer canvas. */
    @Override
    public void rectangle(float x, float y, float width, float height, Color color, float alpha) {
        requireDelegate().rectangle(x, y, width, height, color, alpha);
    }

    /** Delegates a rounded-rectangle command to the bound renderer canvas. */
    @Override
    public void roundedRectangle(float x, float y, float width, float height, float radius, Color color, float alpha) {
        requireDelegate().roundedRectangle(x, y, width, height, radius, color, alpha);
    }

    /** Delegates a line command to the bound renderer canvas. */
    @Override
    public void line(float startX, float startY, float endX, float endY, float thickness, Color color, float alpha) {
        requireDelegate().line(startX, startY, endX, endY, thickness, color, alpha);
    }

    /** Delegates an alpha-mask command to the bound renderer canvas. */
    @Override
    public void alphaMask(
            OverlayImage.Region region, float x, float y, float width, float height, Color color, float alpha) {
        requireDelegate().alphaMask(region, x, y, width, height, color, alpha);
    }

    /** Returns the currently bound renderer canvas. */
    private OverlayCanvas requireDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("GUI canvas is not bound");
        }
        return delegate;
    }
}
