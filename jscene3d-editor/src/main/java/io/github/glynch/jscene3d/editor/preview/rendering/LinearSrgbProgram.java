/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.preview.rendering;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
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
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/** Owns the fullscreen shader program that presents linear color as sRGB. */
final class LinearSrgbProgram implements AutoCloseable {
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

            uniform sampler2D linearFrame;

            out vec4 fragmentColor;

            vec3 linearToSrgb(vec3 linearColor) {
                vec3 bounded = clamp(linearColor, 0.0, 1.0);
                vec3 lower = bounded * 12.92;
                vec3 upper = 1.055 * pow(bounded, vec3(1.0 / 2.4)) - 0.055;
                return mix(upper, lower, lessThanEqual(bounded, vec3(0.0031308)));
            }

            void main() {
                vec4 linearColor = texture(linearFrame, textureCoordinate);
                fragmentColor = vec4(linearToSrgb(linearColor.rgb), linearColor.a);
            }
            """;

    private final int program;
    private final int linearFrameLocation;
    private final int vertexArray;

    /** Stores successfully linked context-local presentation resources. */
    private LinearSrgbProgram(int program, int linearFrameLocation, int vertexArray) {
        this.program = program;
        this.linearFrameLocation = linearFrameLocation;
        this.vertexArray = vertexArray;
    }

    /** Creates the immutable shader and vertex-array resources used by each framebuffer size. */
    static LinearSrgbProgram create() {
        int program = createProgram();
        int vertexArray = 0;
        try {
            int linearFrameLocation = glGetUniformLocation(program, "linearFrame");
            if (linearFrameLocation < 0) {
                throw new IllegalStateException("sRGB presentation program has no active linearFrame uniform");
            }
            vertexArray = glGenVertexArrays();
            return new LinearSrgbProgram(program, linearFrameLocation, vertexArray);
        } catch (RuntimeException exception) {
            if (vertexArray != 0) {
                glDeleteVertexArrays(vertexArray);
            }
            glDeleteProgram(program);
            throw exception;
        }
    }

    /** Draws the supplied linear color texture through the sRGB conversion shader. */
    void present(int colorTexture) {
        glUseProgram(program);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glUniform1i(linearFrameLocation, 0);
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    /** Deletes the program and vertex array from their owning OpenGL context. */
    @Override
    public void close() {
        glDeleteVertexArrays(vertexArray);
        glDeleteProgram(program);
    }

    /** Compiles and links the fullscreen linear-to-sRGB presentation program. */
    private static int createProgram() {
        int vertexShader = compileShader(GL_VERTEX_SHADER, "vertex", VERTEX_SOURCE);
        int fragmentShader = 0;
        int result = 0;
        try {
            fragmentShader = compileShader(GL_FRAGMENT_SHADER, "fragment", FRAGMENT_SOURCE);
            result = glCreateProgram();
            glAttachShader(result, vertexShader);
            glAttachShader(result, fragmentShader);
            glLinkProgram(result);
            if (glGetProgrami(result, GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(
                        "sRGB presentation program link failed:\n" + glGetProgramInfoLog(result));
            }
            return result;
        } catch (RuntimeException exception) {
            if (result != 0) {
                glDeleteProgram(result);
            }
            throw exception;
        } finally {
            glDeleteShader(vertexShader);
            if (fragmentShader != 0) {
                glDeleteShader(fragmentShader);
            }
        }
    }

    /** Compiles one presentation shader stage or deletes it before reporting failure. */
    private static int compileShader(int type, String stage, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String information = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(
                    "sRGB presentation " + stage + " shader compilation failed:\n" + information);
        }
        return shader;
    }
}
