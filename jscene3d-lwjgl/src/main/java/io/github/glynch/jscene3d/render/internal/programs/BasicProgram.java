/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

/** Compiled built-in unlit mesh program. */
public final class BasicProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 3) in vec4 vertexColor;
            layout(location = 2) in vec2 textureCoordinate;

            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform mat3 colorMapTransform;
            uniform bool useVertexColor;
            uniform bool useColorMap;

            out vec4 resolvedVertexColor;
            out vec2 resolvedTextureCoordinate;

            void main() {
                resolvedVertexColor = useVertexColor ? vertexColor : vec4(1.0);
                if (useColorMap) {
                    vec2 transformedTextureCoordinate =
                            (colorMapTransform * vec3(textureCoordinate, 1.0)).xy;
                    resolvedTextureCoordinate =
                            vec2(transformedTextureCoordinate.x, 1.0 - transformedTextureCoordinate.y);
                } else {
                    resolvedTextureCoordinate = vec2(0.0);
                }
                gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec4 resolvedVertexColor;
            in vec2 resolvedTextureCoordinate;

            uniform vec4 baseColor;
            uniform sampler2D colorMap;
            uniform bool useColorMap;

            out vec4 fragmentColor;

            void main() {
                vec4 textureColor = useColorMap ? texture(colorMap, resolvedTextureCoordinate) : vec4(1.0);
                fragmentColor = baseColor * resolvedVertexColor * textureColor;
            }
            """;

    private final int id;
    private final int modelMatrixLocation;
    private final int viewMatrixLocation;
    private final int projectionMatrixLocation;
    private final int colorMapTransformLocation;
    private final int baseColorLocation;
    private final int useVertexColorLocation;
    private final int colorMapLocation;
    private final int useColorMapLocation;

    /** Retains a linked program and its required uniform locations. */
    private BasicProgram(int id) {
        this.id = id;
        modelMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "modelMatrix");
        viewMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "viewMatrix");
        projectionMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "projectionMatrix");
        colorMapTransformLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "colorMapTransform");
        baseColorLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "baseColor");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "useVertexColor");
        colorMapLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "colorMap");
        useColorMapLocation = ProgramSupport.requiredUniform(id, "Built-in basic", "useColorMap");
    }

    /**
     * Compiles, links, and validates the built-in basic-material program.
     *
     * @return linked basic-material program
     */
    public static BasicProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in basic", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new BasicProgram(program);
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
     * Returns the required model-matrix uniform location.
     *
     * @return uniform location
     */
    public int modelMatrixLocation() {
        return modelMatrixLocation;
    }

    /**
     * Returns the required view-matrix uniform location.
     *
     * @return uniform location
     */
    public int viewMatrixLocation() {
        return viewMatrixLocation;
    }

    /**
     * Returns the required projection-matrix uniform location.
     *
     * @return uniform location
     */
    public int projectionMatrixLocation() {
        return projectionMatrixLocation;
    }

    /**
     * Returns the required color-map texture-coordinate transform uniform location.
     *
     * @return uniform location
     */
    public int colorMapTransformLocation() {
        return colorMapTransformLocation;
    }

    /**
     * Returns the required base-color uniform location.
     *
     * @return uniform location
     */
    public int baseColorLocation() {
        return baseColorLocation;
    }

    /**
     * Returns the required vertex-color switch uniform location.
     *
     * @return uniform location
     */
    public int useVertexColorLocation() {
        return useVertexColorLocation;
    }

    /**
     * Returns the required base-color sampler uniform location.
     *
     * @return uniform location
     */
    public int colorMapLocation() {
        return colorMapLocation;
    }

    /**
     * Returns the required base-color texture switch uniform location.
     *
     * @return uniform location
     */
    public int useColorMapLocation() {
        return useColorMapLocation;
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
