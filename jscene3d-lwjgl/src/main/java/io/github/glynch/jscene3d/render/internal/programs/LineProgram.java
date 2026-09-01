/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

import io.github.glynch.jscene3d.fogs.Fog;
import org.jspecify.annotations.Nullable;

/** Compiled built-in unlit line program. */
public final class LineProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 3) in vec4 vertexColor;

            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform bool useVertexColor;

            out vec4 resolvedVertexColor;
            out float resolvedFogDepth;

            void main() {
                resolvedVertexColor = useVertexColor ? vertexColor : vec4(1.0);
                vec4 viewPosition = viewMatrix * modelMatrix * vec4(position, 1.0);
                resolvedFogDepth = -viewPosition.z;
                gl_Position = projectionMatrix * viewPosition;
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec4 resolvedVertexColor;
            in float resolvedFogDepth;

            uniform vec4 baseColor;
            uniform float alphaCutoff;

            FOG_SOURCE

            out vec4 fragmentColor;

            void main() {
                vec4 resolvedColor = baseColor * resolvedVertexColor;
                if (alphaCutoff >= 0.0 && resolvedColor.a < alphaCutoff) {
                    discard;
                }
                fragmentColor = vec4(applyFog(resolvedColor.rgb, resolvedFogDepth), resolvedColor.a);
            }
            """.replace("FOG_SOURCE", FogShaderSource.source());

    private final int id;
    private final int modelMatrixLocation;
    private final int viewMatrixLocation;
    private final int projectionMatrixLocation;
    private final int baseColorLocation;
    private final int useVertexColorLocation;
    private final int alphaCutoffLocation;
    private final FogProgramState fogState;

    /** Retains a linked program and its required uniform locations. */
    private LineProgram(int id) {
        this.id = id;
        modelMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in line", "modelMatrix");
        viewMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in line", "viewMatrix");
        projectionMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in line", "projectionMatrix");
        baseColorLocation = ProgramSupport.requiredUniform(id, "Built-in line", "baseColor");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, "Built-in line", "useVertexColor");
        alphaCutoffLocation = ProgramSupport.requiredUniform(id, "Built-in line", "alphaCutoff");
        fogState = new FogProgramState(id, "Built-in line");
    }

    /**
     * Compiles, links, and validates the built-in line-material program.
     *
     * @return linked line-material program
     */
    public static LineProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in line", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new LineProgram(program);
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
     * Returns the required alpha-cutoff uniform location.
     *
     * @return uniform location
     */
    public int alphaCutoffLocation() {
        return alphaCutoffLocation;
    }

    /**
     * Uploads the current optional scene fog.
     *
     * @param fog scene fog, or {@code null} when disabled
     */
    public void uploadFog(@Nullable Fog fog) {
        fogState.upload(fog);
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
