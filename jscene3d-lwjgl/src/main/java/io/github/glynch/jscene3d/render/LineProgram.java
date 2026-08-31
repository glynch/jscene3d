/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

/** Compiled built-in unlit line program. */
final class LineProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 3) in vec4 vertexColor;

            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform bool useVertexColor;

            out vec4 resolvedVertexColor;

            void main() {
                resolvedVertexColor = useVertexColor ? vertexColor : vec4(1.0);
                gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(position, 1.0);
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec4 resolvedVertexColor;

            uniform vec4 baseColor;

            out vec4 fragmentColor;

            void main() {
                fragmentColor = baseColor * resolvedVertexColor;
            }
            """;

    private final int id;
    private final int modelMatrixLocation;
    private final int viewMatrixLocation;
    private final int projectionMatrixLocation;
    private final int baseColorLocation;
    private final int useVertexColorLocation;

    /** Retains a linked program and its required uniform locations. */
    private LineProgram(int id) {
        this.id = id;
        modelMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in line", "modelMatrix");
        viewMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in line", "viewMatrix");
        projectionMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in line", "projectionMatrix");
        baseColorLocation = ProgramSupport.requiredUniform(id, "Built-in line", "baseColor");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, "Built-in line", "useVertexColor");
    }

    /** Compiles, links, and validates the built-in line-material program. */
    static LineProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in line", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new LineProgram(program);
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /** Returns the context-local OpenGL program name. */
    int id() {
        return id;
    }

    /** Returns the required model-matrix uniform location. */
    int modelMatrixLocation() {
        return modelMatrixLocation;
    }

    /** Returns the required view-matrix uniform location. */
    int viewMatrixLocation() {
        return viewMatrixLocation;
    }

    /** Returns the required projection-matrix uniform location. */
    int projectionMatrixLocation() {
        return projectionMatrixLocation;
    }

    /** Returns the required base-color uniform location. */
    int baseColorLocation() {
        return baseColorLocation;
    }

    /** Returns the required vertex-color switch uniform location. */
    int useVertexColorLocation() {
        return useVertexColorLocation;
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
