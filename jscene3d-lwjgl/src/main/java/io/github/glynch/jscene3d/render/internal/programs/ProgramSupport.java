/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
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

import java.util.Map;

/** Shared compilation and validation for renderer-owned OpenGL programs. */
final class ProgramSupport {
    /** Prevents instantiation of this shader-program utility class. */
    private ProgramSupport() {
        throw new AssertionError("ProgramSupport cannot be instantiated");
    }

    /** Compiles two stages, links them, and releases intermediate shader objects. */
    public static int createLinkedProgram(String label, String vertexSource, String fragmentSource) {
        return createLinkedProgram(label, vertexSource, fragmentSource, Map.of());
    }

    /** Compiles two stages, binds named attributes, links them, and releases shader objects. */
    public static int createLinkedProgram(
            String label, String vertexSource, String fragmentSource, Map<String, Integer> attributeBindings) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, label, "vertex", vertexSource);
        int fragmentShader = 0;
        int program = 0;
        try {
            fragmentShader = compileShader(GL_FRAGMENT_SHADER, label, "fragment", fragmentSource);
            program = glCreateProgram();
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            for (Map.Entry<String, Integer> binding : attributeBindings.entrySet()) {
                glBindAttribLocation(program, binding.getValue(), binding.getKey());
            }
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(label + " program link failed:\n" + glGetProgramInfoLog(program));
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

    /** Returns an active uniform location or rejects a mismatched renderer-owned program. */
    public static int requiredUniform(int program, String programLabel, String name) {
        int location = glGetUniformLocation(program, name);
        if (location < 0) {
            throw new IllegalStateException(programLabel + " program has no active " + name + " uniform");
        }
        return location;
    }

    /** Compiles one shader stage or deletes it before reporting failure. */
    private static int compileShader(int type, String programLabel, String stageLabel, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String infoLog = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException(programLabel
                    + ' '
                    + stageLabel
                    + " shader compilation failed:\n"
                    + infoLog
                    + "\nNumbered source:\n"
                    + numberSource(source));
        }
        return shader;
    }

    /** Adds stable one-based line numbers to diagnostic shader source. */
    private static String numberSource(String source) {
        String[] lines = source.split("\\R", -1);
        StringBuilder numbered = new StringBuilder(source.length() + lines.length * 8);
        for (int index = 0; index < lines.length; index++) {
            numbered.append(index + 1).append(" | ").append(lines[index]).append('\n');
        }
        return numbered.toString();
    }
}
