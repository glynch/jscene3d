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
 * @param instancingEnabled whether renderer-managed instance inputs are enabled
 * @param instanceAttributes immutable custom instance input declarations
 */
public record ShaderProgramKey(
        String vertexShader,
        String fragmentShader,
        Map<String, String> definitions,
        Set<ShaderAttribute> requiredAttributes,
        boolean instancingEnabled,
        Map<String, Integer> instanceAttributes) {
    /**
     * Creates a non-instanced shader key.
     *
     * @param vertexShader vertex-shader source
     * @param fragmentShader fragment-shader source
     * @param definitions immutable preprocessor definitions
     * @param requiredAttributes immutable required vertex attributes
     */
    public ShaderProgramKey(
            String vertexShader,
            String fragmentShader,
            Map<String, String> definitions,
            Set<ShaderAttribute> requiredAttributes) {
        this(vertexShader, fragmentShader, definitions, requiredAttributes, false, Map.of());
    }

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
                material.requiredAttributes(),
                material.instancingEnabled(),
                material.instanceAttributes());
    }
}
