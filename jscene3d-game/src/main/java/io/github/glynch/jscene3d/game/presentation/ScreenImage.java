/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.Objects;

/** Draws one immutable overlay image fitted inside an authored screen region. */
final class ScreenImage implements ScreenContent {
    private final OverlayImage image;

    ScreenImage(OverlayImageResource image) {
        this.image = Objects.requireNonNull(image, "image").image();
    }

    @Override
    public void paint(ScreenPainter painter, ScreenRegion.Bounds bounds, float scale) {
        Objects.requireNonNull(painter, "painter")
                .image(image, bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }
}
