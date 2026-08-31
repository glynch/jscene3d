/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.lwjgl.opengl.GL20.glDeleteProgram;

import io.github.glynch.jscene3d.materials.ShaderAttribute;
import io.github.glynch.jscene3d.platform.Window;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ShaderProgramIT {
    private static final String VERTEX_SHADER = """
            #version 330 core
            in vec3 position;
            void main() {
                gl_Position = vec4(position, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            out vec4 fragmentColor;
            void main() {
                fragmentColor = vec4(1.0);
            }
            """;

    @Test
    void reportsVertexFragmentAndLinkFailuresWithStableDiagnostics() {
        String invalidVertex = "#version 330 core\nin vec3 position;\nvoid main() { invalidToken }";
        String invalidFragment = "#version 330 core\nout vec4 fragmentColor;\nvoid main() { invalidToken }";
        String mismatchedVertex = """
                #version 330 core
                in vec3 position;
                out vec3 varyingValue;
                void main() {
                    varyingValue = position;
                    gl_Position = vec4(position, 1.0);
                }
                """;
        String mismatchedFragment = """
                #version 330 core
                in vec4 varyingValue;
                out vec4 fragmentColor;
                void main() {
                    fragmentColor = varyingValue;
                }
                """;

        try (Window ignored = Window.create("Shader failure diagnostics test")) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ProgramSupport.createLinkedProgram("Test", invalidVertex, FRAGMENT_SHADER))
                    .withMessageContaining("vertex shader compilation failed")
                    .withMessageContaining("2 | in vec3 position;")
                    .withMessageContaining("3 | void main()");
            assertThatIllegalStateException()
                    .isThrownBy(() -> ProgramSupport.createLinkedProgram("Test", VERTEX_SHADER, invalidFragment))
                    .withMessageContaining("fragment shader compilation failed")
                    .withMessageContaining("Numbered source:");
            assertThatIllegalStateException()
                    .isThrownBy(() ->
                            ProgramSupport.createLinkedProgram("Test", mismatchedVertex, mismatchedFragment, Map.of()))
                    .withMessageContaining("program link failed");
        }
    }

    @Test
    void resolvesRequiredUniformsAndRejectsMissingOnes() {
        String vertexShader = """
                #version 330 core
                in vec3 position;
                uniform mat4 transform;
                void main() {
                    gl_Position = transform * vec4(position, 1.0);
                }
                """;

        try (Window ignored = Window.create("Required shader uniform test")) {
            int program = ProgramSupport.createLinkedProgram("Test", vertexShader, FRAGMENT_SHADER);
            try {
                assertThat(ProgramSupport.requiredUniform(program, "Test", "transform"))
                        .isNotNegative();
                assertThatIllegalStateException()
                        .isThrownBy(() -> ProgramSupport.requiredUniform(program, "Test", "missing"))
                        .withMessage("Test program has no active missing uniform");
            } finally {
                glDeleteProgram(program);
            }
        }
    }

    @Test
    void acceptsSupportedVersionDirectiveAndDeterministicDefinitions() {
        String versionedVertex = """

                #version 330 core
                in vec3 position;
                void main() {
                #ifdef MOVE_RIGHT
                    gl_Position = vec4(position.x + OFFSET, position.yz, 1.0);
                #else
                    gl_Position = vec4(position, 1.0);
                #endif
                }
                """;
        ShaderProgramKey key = new ShaderProgramKey(
                versionedVertex,
                FRAGMENT_SHADER,
                Map.of("MOVE_RIGHT", "1", "OFFSET", "0.25"),
                Set.of(ShaderAttribute.POSITION));

        try (Window ignored = Window.create("Shader preprocessing test");
                ShaderProgram program = ShaderProgram.create(key)) {
            assertThat(program.id()).isPositive();
            assertThat(program.applicationUniforms()).isEmpty();
        }
    }

    @Test
    void rejectsUnsupportedOrMisplacedVersionDirectives() {
        ShaderProgramKey unsupported = key("#version 450 core\n" + VERTEX_SHADER, FRAGMENT_SHADER);
        ShaderProgramKey misplaced = key("// comment\n#version 330 core\n" + VERTEX_SHADER, FRAGMENT_SHADER);

        try (Window ignored = Window.create("Shader version validation test")) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShaderProgram.create(unsupported))
                    .withMessageContaining("supports only an optional '#version 330 core'");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ShaderProgram.create(misplaced))
                    .withMessage("ShaderMaterial #version directive must be the first source token");
        }
    }

    @Test
    void rejectsUniformArraysAndUnsupportedUniformTypes() {
        String arrayFragment = """
                uniform float weights[2];
                out vec4 fragmentColor;
                void main() {
                    fragmentColor = vec4(weights[0] + weights[1]);
                }
                """;
        String unsupportedFragment = """
                uniform samplerCube environmentMap;
                out vec4 fragmentColor;
                void main() {
                    fragmentColor = texture(environmentMap, vec3(1.0, 0.0, 0.0));
                }
                """;

        try (Window ignored = Window.create("Shader uniform validation test")) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(VERTEX_SHADER, arrayFragment)))
                    .withMessageContaining("uniform arrays are unsupported");
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(VERTEX_SHADER, unsupportedFragment)))
                    .withMessageContaining("Unsupported ShaderMaterial uniform type for environmentMap");
        }
    }

    @Test
    void rejectsWrongAutomaticUniformTypes() {
        String wrongModelMatrix = """
                in vec3 position;
                uniform mat3 modelMatrix;
                void main() {
                    gl_Position = vec4(modelMatrix * position, 1.0);
                }
                """;
        String wrongNormalMatrix = """
                in vec3 position;
                uniform mat4 normalMatrix;
                void main() {
                    gl_Position = normalMatrix * vec4(position, 1.0);
                }
                """;

        try (Window ignored = Window.create("Automatic shader uniform validation test")) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(wrongModelMatrix, FRAGMENT_SHADER)))
                    .withMessageContaining("modelMatrix has the wrong GLSL type")
                    .withMessageContaining("expected mat4");
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(wrongNormalMatrix, FRAGMENT_SHADER)))
                    .withMessageContaining("normalMatrix has the wrong GLSL type")
                    .withMessageContaining("expected mat3");
        }
    }

    @Test
    void rejectsAttributeArraysUnknownNamesAndIncompatibleTypes() {
        String arrayVertex = """
                in vec3 position[2];
                void main() {
                    gl_Position = vec4(position[0] + position[1], 1.0);
                }
                """;
        String unknownVertex = """
                in vec3 position;
                in vec3 tangent;
                void main() {
                    gl_Position = vec4(position + tangent, 1.0);
                }
                """;
        String wrongPositionType = """
                in vec2 position;
                void main() {
                    gl_Position = vec4(position, 0.0, 1.0);
                }
                """;

        try (Window ignored = Window.create("Shader attribute validation test")) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(arrayVertex, FRAGMENT_SHADER)))
                    .withMessageContaining("attribute arrays are unsupported");
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(unknownVertex, FRAGMENT_SHADER)))
                    .withMessage("Unsupported ShaderMaterial attribute: tangent");
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(wrongPositionType, FRAGMENT_SHADER)))
                    .withMessage("ShaderMaterial attribute position has an incompatible GLSL type");
        }
    }

    @Test
    void requiresEveryActiveStandardAttributeToBeDeclared() {
        String vertexShader = """
                in vec3 position;
                in vec3 normal;
                void main() {
                    gl_Position = vec4(position + normal, 1.0);
                }
                """;

        try (Window ignored = Window.create("Shader attribute declaration test")) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ShaderProgram.create(key(vertexShader, FRAGMENT_SHADER)))
                    .withMessageContaining("active attributes were not declared as required")
                    .withMessageContaining("NORMAL");
        }
    }

    /** Creates a structural key requiring only vertex positions. */
    private static ShaderProgramKey key(String vertexShader, String fragmentShader) {
        return new ShaderProgramKey(vertexShader, fragmentShader, Map.of(), Set.of(ShaderAttribute.POSITION));
    }
}
