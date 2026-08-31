/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

/** Selects which triangle orientations a material renders. */
public enum MaterialSide {
    /** Renders counter-clockwise front faces. */
    FRONT,

    /** Renders clockwise back faces. */
    BACK,

    /** Renders both face orientations. */
    DOUBLE
}
