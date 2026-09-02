/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

/** Controls how a billboard faces the active camera. */
public enum BillboardAlignment {
    /** Copies the camera orientation so the complete billboard plane faces the camera. */
    SPHERICAL,

    /** Rotates around world positive Y while keeping the billboard vertically upright. */
    CYLINDRICAL
}
