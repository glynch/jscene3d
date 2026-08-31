/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

/**
 * Unlit diagnostic material that maps view-space surface normals to RGB colors.
 *
 * <p>Rendering requires geometry normals. The normal components in the range {@code [-1, 1]} are
 * mapped to color channels in {@code [0, 1]}, making surface orientation directly visible.
 * Instances are mutable through inherited render-state properties, shareable, and not thread-safe.
 */
public final class NormalMaterial extends Material {
    /** Creates an opaque normal-visualization material. */
    public NormalMaterial() {
        // This material has no properties beyond the shared render state.
    }
}
