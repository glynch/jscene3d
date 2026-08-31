/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.GL_ACTIVE_ATTRIBUTES;
import static org.lwjgl.opengl.GL20.GL_ACTIVE_UNIFORMS;
import static org.lwjgl.opengl.GL20.GL_BOOL;
import static org.lwjgl.opengl.GL20.GL_FLOAT_MAT3;
import static org.lwjgl.opengl.GL20.GL_FLOAT_MAT4;
import static org.lwjgl.opengl.GL20.GL_FLOAT_VEC2;
import static org.lwjgl.opengl.GL20.GL_FLOAT_VEC3;
import static org.lwjgl.opengl.GL20.GL_FLOAT_VEC4;
import static org.lwjgl.opengl.GL20.GL_INT;
import static org.lwjgl.opengl.GL20.GL_SAMPLER_2D;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetActiveAttrib;
import static org.lwjgl.opengl.GL20.glGetActiveUniform;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.core.ShaderAttribute;
import io.github.glynch.jscene3d.core.ShaderMaterial;
import io.github.glynch.jscene3d.core.ShaderUniformType;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

/** Linked and validated context-local realization of one custom shader structure. */
final class ShaderProgram implements AutoCloseable {
    private static final Map<String, Integer> ATTRIBUTE_BINDINGS = Map.of(
            ShaderAttribute.POSITION.shaderName(), 0,
            ShaderAttribute.NORMAL.shaderName(), 1,
            ShaderAttribute.UV.shaderName(), 2,
            ShaderAttribute.COLOR.shaderName(), 3);

    private final int id;
    private final List<ActiveUniform> applicationUniforms;
    private final Matrix4f modelViewMatrix;
    private final Matrix3f normalMatrix;
    private final float[] matrix4Values;
    private final float[] matrix3Values;

    private int modelMatrixLocation = -1;
    private int viewMatrixLocation = -1;
    private int projectionMatrixLocation = -1;
    private int modelViewMatrixLocation = -1;
    private int normalMatrixLocation = -1;

    /** Introspects and validates one successfully linked custom program. */
    private ShaderProgram(int id, ShaderProgramKey key) {
        this.id = id;
        applicationUniforms = inspectUniforms();
        validateAttributes(key.requiredAttributes());
        modelViewMatrix = new Matrix4f();
        normalMatrix = new Matrix3f();
        matrix4Values = new float[16];
        matrix3Values = new float[9];
    }

    /** Preprocesses, links, and validates one structural program key. */
    static ShaderProgram create(ShaderProgramKey key) {
        String vertexSource = preprocess(key.vertexShader(), key.definitions());
        String fragmentSource = preprocess(key.fragmentShader(), key.definitions());
        int program =
                ProgramSupport.createLinkedProgram("ShaderMaterial", vertexSource, fragmentSource, ATTRIBUTE_BINDINGS);
        try {
            return new ShaderProgram(program, key);
        } catch (RuntimeException exception) {
            glDeleteProgram(program);
            throw exception;
        }
    }

    /** Returns the context-local OpenGL program name. */
    int id() {
        return id;
    }

    /** Returns active application-controlled uniforms in linked-program order. */
    List<ActiveUniform> applicationUniforms() {
        return applicationUniforms;
    }

    /** Uploads active renderer-controlled transform uniforms without allocating. */
    void uploadAutomaticUniforms(Matrix4fc modelMatrix, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        uploadMatrix4(modelMatrixLocation, modelMatrix);
        uploadMatrix4(viewMatrixLocation, viewMatrix);
        uploadMatrix4(projectionMatrixLocation, projectionMatrix);
        if (modelViewMatrixLocation >= 0 || normalMatrixLocation >= 0) {
            modelViewMatrix.set(viewMatrix).mul(modelMatrix);
            uploadMatrix4(modelViewMatrixLocation, modelViewMatrix);
            if (normalMatrixLocation >= 0) {
                normalMatrix.set(modelViewMatrix).normal().get(matrix3Values);
                glUniformMatrix3fv(normalMatrixLocation, false, matrix3Values);
            }
        }
    }

    /** Deletes the linked context-local program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }

    /** Discovers active uniforms, validating automatic and supported application types. */
    private List<ActiveUniform> inspectUniforms() {
        int uniformCount = glGetProgrami(id, GL_ACTIVE_UNIFORMS);
        List<ActiveUniform> discovered = new ArrayList<>(uniformCount);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int index = 0; index < uniformCount; index++) {
                size.clear();
                type.clear();
                String name = glGetActiveUniform(id, index, size, type);
                int uniformSize = size.get(0);
                int uniformType = type.get(0);
                if (uniformSize != 1 || name.endsWith("[0]")) {
                    throw new IllegalStateException(
                            "ShaderMaterial uniform arrays are unsupported in version 0.1: " + name);
                }
                int location = glGetUniformLocation(id, name);
                if (assignAutomaticUniform(name, uniformType, location)) {
                    continue;
                }
                discovered.add(new ActiveUniform(name, location, toUniformType(name, uniformType)));
            }
        }
        return List.copyOf(discovered);
    }

    /** Validates active mesh inputs against supported names, types, bindings, and declarations. */
    private void validateAttributes(Set<ShaderAttribute> requiredAttributes) {
        int attributeCount = glGetProgrami(id, GL_ACTIVE_ATTRIBUTES);
        EnumSet<ShaderAttribute> activeAttributes = EnumSet.noneOf(ShaderAttribute.class);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int index = 0; index < attributeCount; index++) {
                size.clear();
                type.clear();
                String name = glGetActiveAttrib(id, index, size, type);
                if (size.get(0) != 1) {
                    throw new IllegalStateException(
                            "ShaderMaterial attribute arrays are unsupported in version 0.1: " + name);
                }
                ShaderAttribute attribute = findAttribute(name);
                if (attribute == null) {
                    throw new IllegalStateException("Unsupported ShaderMaterial attribute: " + name);
                }
                requireAttributeType(attribute, type.get(0));
                int actualLocation = glGetAttribLocation(id, name);
                int expectedLocation = attributeLocation(attribute);
                if (actualLocation != expectedLocation) {
                    throw new IllegalStateException("ShaderMaterial attribute "
                            + name
                            + " must use renderer location "
                            + expectedLocation
                            + ", but linked at "
                            + actualLocation);
                }
                activeAttributes.add(attribute);
            }
        }
        activeAttributes.remove(ShaderAttribute.POSITION);
        if (!requiredAttributes.containsAll(activeAttributes)) {
            activeAttributes.removeAll(requiredAttributes);
            throw new IllegalStateException(
                    "ShaderMaterial active attributes were not declared as required: " + activeAttributes);
        }
    }

    /** Assigns one active automatic uniform after exact type validation. */
    private boolean assignAutomaticUniform(String name, int type, int location) {
        int expectedType;
        switch (name) {
            case ShaderMaterial.MODEL_MATRIX -> {
                expectedType = GL_FLOAT_MAT4;
                modelMatrixLocation = location;
            }
            case ShaderMaterial.VIEW_MATRIX -> {
                expectedType = GL_FLOAT_MAT4;
                viewMatrixLocation = location;
            }
            case ShaderMaterial.PROJECTION_MATRIX -> {
                expectedType = GL_FLOAT_MAT4;
                projectionMatrixLocation = location;
            }
            case ShaderMaterial.MODEL_VIEW_MATRIX -> {
                expectedType = GL_FLOAT_MAT4;
                modelViewMatrixLocation = location;
            }
            case ShaderMaterial.NORMAL_MATRIX -> {
                expectedType = GL_FLOAT_MAT3;
                normalMatrixLocation = location;
            }
            default -> {
                return false;
            }
        }
        if (type != expectedType) {
            throw new IllegalStateException("Reserved ShaderMaterial uniform "
                    + name
                    + " has the wrong GLSL type; expected "
                    + openGlTypeName(expectedType));
        }
        return true;
    }

    /** Converts one supported active GLSL uniform type into the core contract. */
    private static ShaderUniformType toUniformType(String name, int type) {
        return switch (type) {
            case GL_FLOAT -> ShaderUniformType.FLOAT;
            case GL_INT -> ShaderUniformType.INTEGER;
            case GL_BOOL -> ShaderUniformType.BOOLEAN;
            case GL_FLOAT_VEC2 -> ShaderUniformType.VECTOR2;
            case GL_FLOAT_VEC3 -> ShaderUniformType.VECTOR3;
            case GL_FLOAT_VEC4 -> ShaderUniformType.VECTOR4;
            case GL_FLOAT_MAT3 -> ShaderUniformType.MATRIX3;
            case GL_FLOAT_MAT4 -> ShaderUniformType.MATRIX4;
            case GL_SAMPLER_2D -> ShaderUniformType.TEXTURE;
            default ->
                throw new IllegalStateException(
                        "Unsupported ShaderMaterial uniform type for " + name + ": 0x" + Integer.toHexString(type));
        };
    }

    /** Finds one supported standard attribute by shader name. */
    private static @Nullable ShaderAttribute findAttribute(String name) {
        for (ShaderAttribute attribute : ShaderAttribute.values()) {
            if (attribute.shaderName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    /** Requires the exact vector type supported for one standard attribute. */
    private static void requireAttributeType(ShaderAttribute attribute, int type) {
        boolean compatible =
                switch (attribute) {
                    case POSITION, NORMAL -> type == GL_FLOAT_VEC3;
                    case UV -> type == GL_FLOAT_VEC2;
                    case COLOR -> type == GL_FLOAT_VEC3 || type == GL_FLOAT_VEC4;
                };
        if (!compatible) {
            throw new IllegalStateException(
                    "ShaderMaterial attribute " + attribute.shaderName() + " has an incompatible GLSL type");
        }
    }

    /** Returns the renderer-owned location assigned to one standard attribute. */
    private static int attributeLocation(ShaderAttribute attribute) {
        return switch (attribute) {
            case POSITION -> 0;
            case NORMAL -> 1;
            case UV -> 2;
            case COLOR -> 3;
        };
    }

    /** Adds deterministic definitions after an optional supported version directive. */
    private static String preprocess(String source, Map<String, String> definitions) {
        int firstContent = 0;
        while (firstContent < source.length() && Character.isWhitespace(source.charAt(firstContent))) {
            firstContent++;
        }
        int versionStart = source.indexOf("#version");
        StringBuilder result = new StringBuilder(source.length() + definitions.size() * 24 + 32);
        int remainingStart;
        int originalLine;
        if (versionStart == firstContent) {
            int lineEnd = source.indexOf('\n', versionStart);
            int directiveEnd = lineEnd < 0 ? source.length() : lineEnd;
            String directive = source.substring(versionStart, directiveEnd).trim();
            if (!directive.equals("#version 330 core")) {
                throw new IllegalArgumentException(
                        "ShaderMaterial supports only an optional '#version 330 core' directive");
            }
            result.append(source, 0, directiveEnd).append('\n');
            remainingStart = lineEnd < 0 ? source.length() : lineEnd + 1;
            originalLine = 2;
        } else {
            if (versionStart >= 0) {
                throw new IllegalArgumentException("ShaderMaterial #version directive must be the first source token");
            }
            result.append("#version 330 core\n");
            remainingStart = 0;
            originalLine = 1;
        }
        for (Map.Entry<String, String> definition : definitions.entrySet()) {
            result.append("#define ")
                    .append(definition.getKey())
                    .append(' ')
                    .append(definition.getValue())
                    .append('\n');
        }
        result.append("#line ").append(originalLine).append('\n').append(source, remainingStart, source.length());
        return result.toString();
    }

    /** Uploads one active four-by-four matrix when its location is present. */
    private void uploadMatrix4(int location, Matrix4fc matrix) {
        if (location >= 0) {
            matrix.get(matrix4Values);
            glUniformMatrix4fv(location, false, matrix4Values);
        }
    }

    /** Returns a readable name for automatic-uniform mismatch diagnostics. */
    private static String openGlTypeName(int type) {
        return type == GL_FLOAT_MAT3 ? "mat3" : "mat4";
    }

    /** One active application-controlled uniform binding. */
    record ActiveUniform(String name, int location, ShaderUniformType type) {}
}
