/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

/** Selects how a material interprets its resolved fragment alpha. */
public enum AlphaMode {
    /** Ignores resolved alpha and renders every covered fragment opaquely. */
    OPAQUE,

    /** Discards fragments whose resolved alpha is below the material cutoff. */
    MASK,

    /** Blends resolved alpha over previously rendered color. */
    BLEND
}
