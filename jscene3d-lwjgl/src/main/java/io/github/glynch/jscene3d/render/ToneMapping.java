/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

/** High-dynamic-range scene-to-display mapping applied before overlays are drawn. */
public enum ToneMapping {
    /** Preserves the existing direct rendering path without an HDR intermediate target. */
    NONE,

    /** Uses the Academy-inspired filmic approximation popularized for real-time rendering. */
    ACES_FILMIC
}
