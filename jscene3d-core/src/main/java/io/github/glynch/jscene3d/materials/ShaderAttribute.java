/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import io.github.glynch.jscene3d.geometries.BufferGeometry;

/** Standard mesh attributes supported by version 0.1 custom shaders. */
public enum ShaderAttribute {
    /** Three-component local-space position, always required for mesh geometry. */
    POSITION(BufferGeometry.POSITION),

    /** Three-component local-space normal. */
    NORMAL(BufferGeometry.NORMAL),

    /** Two-component texture coordinate. */
    UV(BufferGeometry.UV),

    /** Three- or four-component linear vertex color. */
    COLOR(BufferGeometry.COLOR);

    private final String shaderName;

    /** Retains the standardized shader and geometry attribute name. */
    ShaderAttribute(String shaderName) {
        this.shaderName = shaderName;
    }

    /**
     * Returns the standardized GLSL input and geometry attribute name.
     *
     * @return attribute name
     */
    public String shaderName() {
        return shaderName;
    }
}
