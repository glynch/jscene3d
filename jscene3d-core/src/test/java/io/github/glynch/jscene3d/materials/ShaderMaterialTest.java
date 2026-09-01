/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.Map;
import java.util.Objects;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

final class ShaderMaterialTest {
    private static final String VERTEX_SHADER = "void main() { gl_Position = vec4(0.0); }";
    private static final String FRAGMENT_SHADER = "out vec4 color; void main() { color = vec4(1.0); }";

    @Test
    void createsImmutableProgramStructureWithRequiredPositions() {
        ShaderMaterial.Builder builder = ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                .define("MODE", "2")
                .requireAttribute(ShaderAttribute.NORMAL);
        try (ShaderMaterial material = builder.build()) {
            assertThat(material.vertexShader()).isEqualTo(VERTEX_SHADER);
            assertThat(material.fragmentShader()).isEqualTo(FRAGMENT_SHADER);
            assertThat(material.definitions()).containsExactly(Map.entry("MODE", "2"));
            assertThat(material.requiredAttributes()).containsExactly(ShaderAttribute.POSITION, ShaderAttribute.NORMAL);
            assertThat(material.uniforms()).isEmpty();
            assertThatIllegalStateException().isThrownBy(builder::build);
        }
    }

    @Test
    void declaresPortableCustomInstanceInputsInBindingOrder() {
        ShaderMaterial.Builder builder = ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                .enableInstancing()
                .requireInstanceAttribute("phase", 1)
                .requireInstanceAttribute("tint", 3);

        try (ShaderMaterial material = builder.build()) {
            assertThat(material.instancingEnabled()).isTrue();
            assertThat(material.instanceAttributes()).containsExactly(Map.entry("phase", 1), Map.entry("tint", 3));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                            .requireInstanceAttribute(ShaderMaterial.INSTANCE_COLOR, 3));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                            .requireInstanceAttribute("position", 3));
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER)
                            .requireInstanceAttribute("oversized", 5));
        }
    }

    @Test
    void storesAllSupportedUniformTypesAndVersionsOnlyChanges() {
        try (Texture texture = Texture.baseColor(1, 1, new byte[4]);
                ShaderMaterial material = new ShaderMaterial(VERTEX_SHADER, FRAGMENT_SHADER)) {
            material.setUniform("floatValue", 0.0f);
            material.setUniform("integerValue", 0);
            material.setUniform("booleanValue", false);
            material.setUniform("vector2Value", new Vector2f(1.0f, 2.0f));
            material.setUniform("vector3Value", new Vector3f(3.0f, 4.0f, 5.0f));
            material.setUniform("vector4Value", new Vector4f(6.0f, 7.0f, 8.0f, 9.0f));
            material.setUniform("matrix3Value", new Matrix3f().scaling(2.0f));
            material.setUniform("matrix4Value", new Matrix4f().translation(1.0f, 2.0f, 3.0f));
            material.setUniform("colorValue", Color.RED);
            material.setUniform("textureValue", texture);

            assertThat(material.version()).isEqualTo(10L);
            assertThat(requireUniform(material, "floatValue").type()).isEqualTo(ShaderUniformType.FLOAT);
            assertThat(requireUniform(material, "integerValue").integerValue()).isZero();
            assertThat(requireUniform(material, "booleanValue").booleanValue()).isFalse();
            assertThat(requireUniform(material, "vector3Value").floatComponent(2))
                    .isEqualTo(5.0f);
            assertThat(requireUniform(material, "matrix4Value").floatComponent(12))
                    .isEqualTo(1.0f);
            assertThat(requireUniform(material, "colorValue").floatComponent(0)).isEqualTo(1.0f);
            assertThat(requireUniform(material, "textureValue").textureValue()).isSameAs(texture);

            material.setUniform("floatValue", 0.0f);
            material.setUniform("integerValue", 0);
            material.setUniform("booleanValue", false);
            material.setUniform("textureValue", texture);
            assertThat(material.version()).isEqualTo(10L);

            assertThat(material.removeUniform("floatValue")).isTrue();
            assertThat(material.removeUniform("floatValue")).isFalse();
            assertThat(material.version()).isEqualTo(11L);
        }
    }

    @Test
    void copiesMutableVectorAndMatrixInputs() {
        Vector3f vector = new Vector3f(1.0f, 2.0f, 3.0f);
        Matrix4f matrix = new Matrix4f().translation(4.0f, 5.0f, 6.0f);
        try (ShaderMaterial material = new ShaderMaterial(VERTEX_SHADER, FRAGMENT_SHADER)) {
            material.setUniform("vectorValue", vector);
            material.setUniform("matrixValue", matrix);
            vector.zero();
            matrix.identity();

            assertThat(requireUniform(material, "vectorValue").floatComponent(0))
                    .isEqualTo(1.0f);
            assertThat(requireUniform(material, "matrixValue").floatComponent(12))
                    .isEqualTo(4.0f);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidProgramStructureAndUniformValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ShaderMaterial(" ", FRAGMENT_SHADER));
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER).define("bad-name"));
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER).define("MODE", "a\nb"));
        assertThatNullPointerException()
                .isThrownBy(() ->
                        ShaderMaterial.builder(VERTEX_SHADER, FRAGMENT_SHADER).requireAttribute(null));

        try (ShaderMaterial material = new ShaderMaterial(VERTEX_SHADER, FRAGMENT_SHADER)) {
            assertThatIllegalArgumentException().isThrownBy(() -> material.setUniform("modelMatrix", 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setUniform("bad-name", 1.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> material.setUniform("value", Float.NaN));
            material.setUniform("value", 1.0f);
            assertThatIllegalArgumentException().isThrownBy(() -> material.setUniform("value", 1));
        }
    }

    @Test
    void doesNotOwnTextureUniformsAndRejectsClosedTextures() {
        Texture sharedTexture = Texture.baseColor(1, 1, new byte[4]);
        ShaderMaterial material = new ShaderMaterial(VERTEX_SHADER, FRAGMENT_SHADER);
        material.setUniform("map", sharedTexture);
        material.close();
        assertThat(sharedTexture.isClosed()).isFalse();

        Texture closedTexture = Texture.baseColor(1, 1, new byte[4]);
        closedTexture.close();
        try (ShaderMaterial openMaterial = new ShaderMaterial(VERTEX_SHADER, FRAGMENT_SHADER)) {
            assertThatIllegalArgumentException().isThrownBy(() -> openMaterial.setUniform("map", closedTexture));
        }
        sharedTexture.close();
    }

    private static ShaderUniform requireUniform(ShaderMaterial material, String name) {
        return Objects.requireNonNull(material.uniform(name));
    }
}
