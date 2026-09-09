/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import io.github.glynch.jscene3d.render.OverlayImage;

/** Minimal renderer-independent image command used by built-in screen content. */
@FunctionalInterface
interface ScreenPainter {
    /** Draws one full image at a logical-coordinate rectangle. */
    void image(OverlayImage image, float x, float y, float width, float height);
}
