/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

/** Compiled fullscreen ACES-filmic tone mapping program. */
public final class ToneMappingProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            out vec2 textureCoordinate;

            void main() {
                vec2 position = vec2(
                        gl_VertexID == 1 ? 3.0 : -1.0,
                        gl_VertexID == 2 ? 3.0 : -1.0);
                textureCoordinate = position * 0.5 + 0.5;
                gl_Position = vec4(position, 0.0, 1.0);
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec2 textureCoordinate;

            uniform sampler2D hdrScene;
            uniform float exposure;

            out vec4 fragmentColor;

            vec3 acesFilmic(vec3 color) {
                const float a = 2.51;
                const float b = 0.03;
                const float c = 2.43;
                const float d = 0.59;
                const float e = 0.14;
                return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
            }

            void main() {
                vec4 hdr = texture(hdrScene, textureCoordinate);
                fragmentColor = vec4(acesFilmic(hdr.rgb * exposure), hdr.a);
            }
            """;

    private final int id;
    private final int sceneLocation;
    private final int exposureLocation;

    /** Resolves required fullscreen-program uniforms. */
    private ToneMappingProgram(int id) {
        this.id = id;
        String label = "Tone mapping";
        sceneLocation = ProgramSupport.requiredUniform(id, label, "hdrScene");
        exposureLocation = ProgramSupport.requiredUniform(id, label, "exposure");
    }

    /**
     * Compiles and links the fullscreen tone mapping program.
     *
     * @return linked program
     */
    public static ToneMappingProgram create() {
        int program = ProgramSupport.createLinkedProgram("Tone mapping", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new ToneMappingProgram(program);
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /**
     * Returns the OpenGL program name.
     *
     * @return context-local program name
     */
    public int id() {
        return id;
    }

    /**
     * Returns the HDR scene sampler location.
     *
     * @return uniform location
     */
    public int sceneLocation() {
        return sceneLocation;
    }

    /**
     * Returns the exposure location.
     *
     * @return uniform location
     */
    public int exposureLocation() {
        return exposureLocation;
    }

    /** Deletes the linked program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
