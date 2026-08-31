/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.core.ShaderAttribute;
import io.github.glynch.jscene3d.core.ShaderMaterial;
import java.util.Map;
import java.util.Set;

/** Immutable structural identity used to share custom shader programs within one renderer. */
record ShaderProgramKey(
        String vertexShader,
        String fragmentShader,
        Map<String, String> definitions,
        Set<ShaderAttribute> requiredAttributes) {
    /** Captures the immutable program structure of one open shader material. */
    static ShaderProgramKey from(ShaderMaterial material) {
        return new ShaderProgramKey(
                material.vertexShader(),
                material.fragmentShader(),
                material.definitions(),
                material.requiredAttributes());
    }
}
