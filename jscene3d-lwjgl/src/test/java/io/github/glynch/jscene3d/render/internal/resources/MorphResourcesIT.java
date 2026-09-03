/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BINDING_BUFFER;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.platform.Window;
import org.junit.jupiter.api.Test;

final class MorphResourcesIT {
    @Test
    void bindsCompleteFallbackBuffersForANonMorphingMesh() {
        try (Window ignored = Window.create("Morph fallback resource test");
                BufferGeometry geometry = new BufferGeometry();
                BasicMaterial material = new BasicMaterial(Color.WHITE);
                MorphResources resources = new MorphResources()) {
            Mesh mesh = new Mesh(geometry, material);

            MorphResources.Binding binding = resources.bind(mesh);

            assertThat(binding.enabled()).isFalse();
            assertThat(textureBufferAt(MorphResources.TARGET_TEXTURE_UNIT)).isNotZero();
            assertThat(textureBufferAt(MorphResources.WEIGHT_TEXTURE_UNIT)).isNotZero();
        }
    }

    /** Returns the texture-buffer object bound to one combined texture unit. */
    private static int textureBufferAt(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        return glGetInteger(GL_TEXTURE_BINDING_BUFFER);
    }
}
