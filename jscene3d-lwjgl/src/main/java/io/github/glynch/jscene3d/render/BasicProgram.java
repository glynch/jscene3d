/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;

/** Compiled built-in unlit mesh program. */
final class BasicProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec4 vertexColor;

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

    private BasicProgram(
            int id,
            int modelMatrixLocation,
            int viewMatrixLocation,
            int projectionMatrixLocation,
            int baseColorLocation,
            int useVertexColorLocation) {
        this.id = id;
        this.modelMatrixLocation = modelMatrixLocation;
        this.viewMatrixLocation = viewMatrixLocation;
        this.projectionMatrixLocation = projectionMatrixLocation;
        this.baseColorLocation = baseColorLocation;
        this.useVertexColorLocation = useVertexColorLocation;
    }

    static BasicProgram create() {
        int program = createLinkedProgram();
        int modelMatrixLocation;
        int viewMatrixLocation;
        int projectionMatrixLocation;
        int baseColorLocation;
        int useVertexColorLocation;
        try {
            modelMatrixLocation = requiredUniform(program, "modelMatrix");
            viewMatrixLocation = requiredUniform(program, "viewMatrix");
            projectionMatrixLocation = requiredUniform(program, "projectionMatrix");
            baseColorLocation = requiredUniform(program, "baseColor");
            useVertexColorLocation = requiredUniform(program, "useVertexColor");
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
        return new BasicProgram(
                program,
                modelMatrixLocation,
                viewMatrixLocation,
                projectionMatrixLocation,
                baseColorLocation,
                useVertexColorLocation);
    }

    private static int createLinkedProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, "vertex", VERTEX_SOURCE);
        int fragmentShader = 0;
        int program = 0;
        try {
            fragmentShader = compileShader(GL_FRAGMENT_SHADER, "fragment", FRAGMENT_SOURCE);
            program = glCreateProgram();
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                throw new IllegalStateException("Built-in basic program link failed:\n" + glGetProgramInfoLog(program));
            }
            return program;
        } catch (RuntimeException exception) {
            if (program != 0) {
                glDeleteProgram(program);
            }
            throw exception;
        } finally {
            glDeleteShader(vertexShader);
            if (fragmentShader != 0) {
                glDeleteShader(fragmentShader);
            }
        }
    }

    int id() {
        return id;
    }

    int modelMatrixLocation() {
        return modelMatrixLocation;
    }

    int viewMatrixLocation() {
        return viewMatrixLocation;
    }

    int projectionMatrixLocation() {
        return projectionMatrixLocation;
    }

    int baseColorLocation() {
        return baseColorLocation;
    }

    int useVertexColorLocation() {
        return useVertexColorLocation;
    }

    @Override
    public void close() {
        glDeleteProgram(id);
    }

    private static int compileShader(int type, String label, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String infoLog = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Built-in basic " + label + " shader compilation failed:\n" + infoLog);
        }
        return shader;
    }

    private static int requiredUniform(int program, String name) {
        int location = glGetUniformLocation(program, name);
        if (location < 0) {
            throw new IllegalStateException("Built-in basic program has no active " + name + " uniform");
        }
        return location;
    }
}
