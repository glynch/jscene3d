/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
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
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

/** Owns a linear editor render target and converts its completed frame to sRGB for JavaFX. */
final class SrgbPresentationFramebuffer implements AutoCloseable {
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

    private int framebuffer;
    private int colorTexture;
    private int depthRenderbuffer;
    private int width;
    private int height;

    /** Stores successfully created context-local presentation resources. */
    private SrgbPresentationFramebuffer(int program, int linearFrameLocation, int vertexArray) {
        this.program = program;
        this.linearFrameLocation = linearFrameLocation;
        this.vertexArray = vertexArray;
    }

    /** Creates the immutable shader and vertex-array resources used by every framebuffer size. */
    static SrgbPresentationFramebuffer create() {
        int program = createProgram();
        int vertexArray = 0;
        try {
            int linearFrameLocation = glGetUniformLocation(program, "linearFrame");
            if (linearFrameLocation < 0) {
                throw new IllegalStateException("sRGB presentation program has no active linearFrame uniform");
            }
            vertexArray = glGenVertexArrays();
            return new SrgbPresentationFramebuffer(program, linearFrameLocation, vertexArray);
        } catch (RuntimeException exception) {
            if (vertexArray != 0) {
                glDeleteVertexArrays(vertexArray);
            }
            glDeleteProgram(program);
            throw exception;
        }
    }

    /** Recreates the linear color and depth attachments when the OpenGLFX framebuffer changes size. */
    void resize(int width, int height) {
        if (this.width == width && this.height == height) {
            return;
        }
        releaseFramebuffer();
        if (width <= 0 || height <= 0) {
            return;
        }
        this.width = width;
        this.height = height;
        try {
            createColorTexture();
            createFramebuffer();
            createDepthRenderbuffer();
            requireComplete();
        } catch (RuntimeException exception) {
            releaseFramebuffer();
            throw exception;
        }
    }

    /** Binds the complete linear framebuffer that receives all renderer output for one editor frame. */
    void bindLinearFramebuffer() {
        if (framebuffer == 0) {
            throw new IllegalStateException("sRGB presentation framebuffer has no drawable size");
        }
        glEnable(GL_FRAMEBUFFER_SRGB);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    }

    /** Encodes the completed linear frame directly into OpenGLFX's host-owned color attachment. */
    void present(int presentationFramebuffer) {
        if (framebuffer == 0) {
            return;
        }
        glBindFramebuffer(GL_FRAMEBUFFER, presentationFramebuffer);
        glViewport(0, 0, width, height);
        glDisable(GL_FRAMEBUFFER_SRGB);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glUseProgram(program);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glUniform1i(linearFrameLocation, 0);
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        resetBindings();
    }

    /** Deletes all context-local storage and shader resources. */
    @Override
    public void close() {
        releaseFramebuffer();
        glDeleteVertexArrays(vertexArray);
        glDeleteProgram(program);
    }

    /** Allocates the floating-point linear color attachment sampled during presentation. */
    private void createColorTexture() {
        colorTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    /** Creates the framebuffer and attaches its linear color texture. */
    private void createFramebuffer() {
        framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
    }

    /** Allocates and attaches the depth storage required by ordinary scene rendering. */
    private void createDepthRenderbuffer() {
        depthRenderbuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);
    }

    /** Rejects an unsupported framebuffer configuration before the renderer can use it. */
    private static void requireComplete() {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(
                    "Cannot create editor linear presentation framebuffer: 0x" + Integer.toHexString(status));
        }
    }

    /** Restores neutral bindings and the renderer's expected framebuffer conversion state. */
    private static void resetBindings() {
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        glUseProgram(0);
        glDepthMask(true);
        glEnable(GL_FRAMEBUFFER_SRGB);
    }

    /** Deletes size-dependent attachments while preserving reusable presentation resources. */
    private void releaseFramebuffer() {
        if (framebuffer != 0) {
            glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }
        if (depthRenderbuffer != 0) {
            glDeleteRenderbuffers(depthRenderbuffer);
            depthRenderbuffer = 0;
        }
        if (colorTexture != 0) {
            glDeleteTextures(colorTexture);
            colorTexture = 0;
        }
        width = 0;
        height = 0;
    }

    /** Compiles and links the fullscreen linear-to-sRGB presentation program. */
    private static int createProgram() {
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
                throw new IllegalStateException(
                        "sRGB presentation program link failed:\n" + glGetProgramInfoLog(program));
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
