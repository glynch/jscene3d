/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** Horizontal relationship between an overlay text anchor and the rendered line. */
public enum OverlayTextAlignment {
    /** Places the line's left edge at the anchor. */
    LEFT,

    /** Centers the line horizontally on the anchor. */
    CENTER,

    /** Places the line's right edge at the anchor. */
    RIGHT
}
