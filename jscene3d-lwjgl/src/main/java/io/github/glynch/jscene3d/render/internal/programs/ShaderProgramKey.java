/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import io.github.glynch.jscene3d.materials.ShaderAttribute;
import io.github.glynch.jscene3d.materials.ShaderMaterial;
import java.util.Map;
import java.util.Set;

/**
 * Immutable structural identity used to share custom shader programs within one renderer.
 *
 * @param vertexShader vertex-shader source
 * @param fragmentShader fragment-shader source
 * @param definitions immutable preprocessor definitions
 * @param requiredAttributes immutable required vertex attributes
 */
public record ShaderProgramKey(
        String vertexShader,
        String fragmentShader,
        Map<String, String> definitions,
        Set<ShaderAttribute> requiredAttributes) {
    /**
     * Captures the immutable program structure of one open shader material.
     *
     * @param material open shader material
     * @return immutable structural program key
     */
    public static ShaderProgramKey from(ShaderMaterial material) {
        return new ShaderProgramKey(
                material.vertexShader(),
                material.fragmentShader(),
                material.definitions(),
                material.requiredAttributes());
    }
}
