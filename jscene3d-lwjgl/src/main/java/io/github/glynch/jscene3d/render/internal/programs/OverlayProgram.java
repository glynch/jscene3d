/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

/** Compiled renderer-owned program for logical-coordinate colored and alpha-masked overlays. */
public final class OverlayProgram implements AutoCloseable {
    private static final String LABEL = "Built-in overlay";
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec2 position;
            layout(location = 1) in vec2 textureCoordinate;
            layout(location = 2) in vec4 color;

            uniform vec2 logicalSize;

            out vec4 vertexColor;
            out vec2 vertexTextureCoordinate;

            void main() {
                vec2 normalized = vec2(
                    position.x / logicalSize.x * 2.0 - 1.0,
                    1.0 - position.y / logicalSize.y * 2.0
                );
                vertexColor = color;
                vertexTextureCoordinate = textureCoordinate;
                gl_Position = vec4(normalized, 0.0, 1.0);
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec4 vertexColor;
            in vec2 vertexTextureCoordinate;

            uniform sampler2D alphaMask;
            uniform bool usesAlphaMask;

            out vec4 fragmentColor;

            void main() {
                fragmentColor = vertexColor;
                if (usesAlphaMask) {
                    fragmentColor.a *= texture(alphaMask, vertexTextureCoordinate).r;
                }
            }
            """;

    private final int id;
    private final int logicalSizeLocation;
    private final int alphaMaskLocation;
    private final int usesAlphaMaskLocation;

    /** Retains a linked program and its required logical-size uniform. */
    private OverlayProgram(int id, int logicalSizeLocation, int alphaMaskLocation, int usesAlphaMaskLocation) {
        this.id = id;
        this.logicalSizeLocation = logicalSizeLocation;
        this.alphaMaskLocation = alphaMaskLocation;
        this.usesAlphaMaskLocation = usesAlphaMaskLocation;
    }

    /**
     * Compiles, links, and validates the built-in overlay program.
     *
     * @return linked overlay program
     */
    public static OverlayProgram create() {
        int program = ProgramSupport.createLinkedProgram(LABEL, VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new OverlayProgram(
                    program,
                    ProgramSupport.requiredUniform(program, LABEL, "logicalSize"),
                    ProgramSupport.requiredUniform(program, LABEL, "alphaMask"),
                    ProgramSupport.requiredUniform(program, LABEL, "usesAlphaMask"));
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /**
     * Returns the context-local OpenGL program name.
     *
     * @return OpenGL program name
     */
    public int id() {
        return id;
    }

    /**
     * Returns the required logical-size uniform location.
     *
     * @return uniform location
     */
    public int logicalSizeLocation() {
        return logicalSizeLocation;
    }

    /**
     * Returns the required alpha-mask sampler uniform location.
     *
     * @return uniform location
     */
    public int alphaMaskLocation() {
        return alphaMaskLocation;
    }

    /**
     * Returns the required alpha-mask enabled uniform location.
     *
     * @return uniform location
     */
    public int usesAlphaMaskLocation() {
        return usesAlphaMaskLocation;
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
