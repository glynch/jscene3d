/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.render.Overlay;
import io.github.glynch.jscene3d.render.OverlayCanvas;
import java.util.Objects;

/** Presents descriptor-selected content from one ordinary entity hierarchy as a screen overlay. */
final class ScreenCanvas implements Overlay, AutoCloseable {
    private final Entity owner;
    private final float referenceWidth;
    private final float referenceHeight;
    private final OverlayRegistration registration;
    private boolean closed;

    ScreenCanvas(Entity owner, PresentationWorldModule presentation, float referenceWidth, float referenceHeight) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.referenceWidth = requirePositive(referenceWidth, "referenceWidth");
        this.referenceHeight = requirePositive(referenceHeight, "referenceHeight");
        registration = Objects.requireNonNull(presentation, "presentation").registerOverlay(this);
    }

    @Override
    public void paint(OverlayCanvas canvas, int width, int height) {
        Objects.requireNonNull(canvas, "canvas");
        paint(
                (image, x, y, imageWidth, imageHeight) ->
                        canvas.image(image.fullRegion(), x, y, imageWidth, imageHeight, Color.WHITE, 1.0F),
                width,
                height);
    }

    /** Paints through the renderer-independent screen seam used by deterministic tests. */
    void paint(ScreenPainter painter, int width, int height) {
        Objects.requireNonNull(painter, "painter");
        if (closed || !owner.isEnabled()) {
            return;
        }
        float scale = Math.clamp(Math.min(width / referenceWidth, height / referenceHeight), 1.0F, Float.MAX_VALUE);
        paintEntity(painter, owner, new ScreenRegion.Bounds(0.0F, 0.0F, width, height), scale);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        registration.close();
    }

    private static void paintEntity(
            ScreenPainter painter, Entity entity, ScreenRegion.Bounds parentBounds, float scale) {
        if (!entity.isEnabled()) {
            return;
        }
        ScreenRegion.Bounds bounds = entity.capability(
                        GamePresentationDescriptors.screenRegionCapability(), ScreenRegion.class)
                .map(region -> region.resolve(parentBounds, scale))
                .orElse(parentBounds);
        entity.capability(GamePresentationDescriptors.screenContentCapability(), ScreenContent.class)
                .ifPresent(content -> content.paint(painter, bounds, scale));
        entity.children().forEach(child -> paintEntity(painter, child, bounds, scale));
    }

    private static float requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
        return value;
    }
}
