/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUniform1fv;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.internal.LightCollection;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Compiled built-in diffuse Lambert mesh program. */
public final class LambertProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec3 normal;
            layout(location = 2) in vec2 textureCoordinate;
            layout(location = 3) in vec4 vertexColor;

            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform mat3 normalMatrix;
            uniform bool useVertexColor;

            out vec3 resolvedViewPosition;
            out vec3 resolvedViewNormal;
            out vec4 resolvedVertexColor;
            out vec2 resolvedTextureCoordinate;

            void main() {
                vec4 viewPosition = viewMatrix * modelMatrix * vec4(position, 1.0);
                resolvedViewPosition = viewPosition.xyz;
                resolvedViewNormal = normalize(normalMatrix * normal);
                resolvedVertexColor = useVertexColor ? vertexColor : vec4(1.0);
                resolvedTextureCoordinate = vec2(textureCoordinate.x, 1.0 - textureCoordinate.y);
                gl_Position = projectionMatrix * viewPosition;
            }
            """;
    private static final String FRAGMENT_SOURCE =
            """
            #version 330 core
            const int MAX_POINT_LIGHTS = POINT_LIGHT_CAPACITY;

            in vec3 resolvedViewPosition;
            in vec3 resolvedViewNormal;
            in vec4 resolvedVertexColor;
            in vec2 resolvedTextureCoordinate;

            uniform vec4 baseColor;
            uniform sampler2D colorMap;
            uniform bool useColorMap;
            uniform vec3 ambientLightColor;
            uniform int pointLightCount;
            uniform vec3 pointLightPositions[MAX_POINT_LIGHTS];
            uniform vec3 pointLightColors[MAX_POINT_LIGHTS];
            uniform float pointLightDistances[MAX_POINT_LIGHTS];
            uniform float pointLightDecays[MAX_POINT_LIGHTS];

            out vec4 fragmentColor;

            float distanceAttenuation(float lightDistance, float cutoffDistance, float decay) {
                float falloff = decay == 0.0 ? 1.0 : 1.0 / pow(max(lightDistance, 0.01), decay);
                if (cutoffDistance > 0.0) {
                    float ratio = lightDistance / cutoffDistance;
                    float smoothCutoff = pow(clamp(1.0 - pow(ratio, 4.0), 0.0, 1.0), 2.0);
                    falloff *= smoothCutoff;
                }
                return falloff;
            }

            void main() {
                vec3 surfaceNormal = gl_FrontFacing ? resolvedViewNormal : -resolvedViewNormal;
                vec3 illumination = ambientLightColor;
                for (int index = 0; index < MAX_POINT_LIGHTS; index++) {
                    if (index >= pointLightCount) {
                        break;
                    }
                    vec3 lightOffset = pointLightPositions[index] - resolvedViewPosition;
                    float lightDistance = length(lightOffset);
                    vec3 lightDirection = lightOffset / max(lightDistance, 0.01);
                    float diffuse = max(dot(surfaceNormal, lightDirection), 0.0);
                    float attenuation = distanceAttenuation(
                            lightDistance,
                            pointLightDistances[index],
                            pointLightDecays[index]);
                    illumination += pointLightColors[index] * diffuse * attenuation;
                }

                vec4 textureColor = useColorMap ? texture(colorMap, resolvedTextureCoordinate) : vec4(1.0);
                vec4 surfaceColor = baseColor * resolvedVertexColor * textureColor;
                fragmentColor = vec4(surfaceColor.rgb * illumination, surfaceColor.a);
            }
            """.replace("POINT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_POINT_LIGHTS));

    private final int id;
    private final int modelMatrixLocation;
    private final int viewMatrixLocation;
    private final int projectionMatrixLocation;
    private final int normalMatrixLocation;
    private final int baseColorLocation;
    private final int useVertexColorLocation;
    private final int colorMapLocation;
    private final int useColorMapLocation;
    private final int ambientLightColorLocation;
    private final int pointLightCountLocation;
    private final int pointLightPositionsLocation;
    private final int pointLightColorsLocation;
    private final int pointLightDistancesLocation;
    private final int pointLightDecaysLocation;
    private final Matrix4f modelViewMatrix;
    private final Matrix3f normalMatrix;
    private final Vector3f viewPosition;
    private final float[] matrix4Values;
    private final float[] matrix3Values;
    private final float[] pointLightPositions;
    private final float[] pointLightColors;
    private final float[] pointLightDistances;
    private final float[] pointLightDecays;

    /** Retains a linked program and all reusable transform and light staging. */
    private LambertProgram(int id) {
        this.id = id;
        modelMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "modelMatrix");
        viewMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "viewMatrix");
        projectionMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "projectionMatrix");
        normalMatrixLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "normalMatrix");
        baseColorLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "baseColor");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "useVertexColor");
        colorMapLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "colorMap");
        useColorMapLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "useColorMap");
        ambientLightColorLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "ambientLightColor");
        pointLightCountLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "pointLightCount");
        pointLightPositionsLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "pointLightPositions[0]");
        pointLightColorsLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "pointLightColors[0]");
        pointLightDistancesLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "pointLightDistances[0]");
        pointLightDecaysLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "pointLightDecays[0]");
        modelViewMatrix = new Matrix4f();
        normalMatrix = new Matrix3f();
        viewPosition = new Vector3f();
        matrix4Values = new float[16];
        matrix3Values = new float[9];
        pointLightPositions = new float[Renderer.MAX_POINT_LIGHTS * 3];
        pointLightColors = new float[Renderer.MAX_POINT_LIGHTS * 3];
        pointLightDistances = new float[Renderer.MAX_POINT_LIGHTS];
        pointLightDecays = new float[Renderer.MAX_POINT_LIGHTS];
    }

    /**
     * Compiles, links, validates, and returns the built-in Lambert program.
     *
     * @return linked Lambert program
     */
    public static LambertProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in Lambert", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new LambertProgram(program);
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
     * Returns the required color-map sampler uniform location.
     *
     * @return uniform location
     */
    public int colorMapLocation() {
        return colorMapLocation;
    }

    /**
     * Returns the required color-map switch uniform location.
     *
     * @return uniform location
     */
    public int useColorMapLocation() {
        return useColorMapLocation;
    }

    /**
     * Uploads object, camera, and inverse-transpose normal transforms without allocating.
     *
     * @param modelMatrix object model matrix
     * @param viewMatrix current view matrix
     * @param projectionMatrix current projection matrix
     */
    public void uploadTransforms(Matrix4fc modelMatrix, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        uploadMatrix4(modelMatrixLocation, modelMatrix);
        uploadMatrix4(viewMatrixLocation, viewMatrix);
        uploadMatrix4(projectionMatrixLocation, projectionMatrix);
        modelViewMatrix.set(viewMatrix).mul(modelMatrix);
        normalMatrix.set(modelViewMatrix).normal().get(matrix3Values);
        glUniformMatrix3fv(normalMatrixLocation, false, matrix3Values);
    }

    /**
     * Uploads combined ambient and ordered point-light state without allocating.
     *
     * @param lights active visible lights
     * @param viewMatrix current view matrix
     */
    public void uploadLights(LightCollection lights, Matrix4fc viewMatrix) {
        glUniform3f(ambientLightColorLocation, lights.ambientRed(), lights.ambientGreen(), lights.ambientBlue());
        int count = lights.pointLightCount();
        glUniform1i(pointLightCountLocation, count);
        for (int index = 0; index < count; index++) {
            PointLight light = lights.pointLight(index);
            light.worldPosition(viewPosition);
            viewMatrix.transformPosition(viewPosition);
            int componentIndex = index * 3;
            pointLightPositions[componentIndex] = viewPosition.x();
            pointLightPositions[componentIndex + 1] = viewPosition.y();
            pointLightPositions[componentIndex + 2] = viewPosition.z();
            pointLightColors[componentIndex] = light.color().red() * light.intensity();
            pointLightColors[componentIndex + 1] = light.color().green() * light.intensity();
            pointLightColors[componentIndex + 2] = light.color().blue() * light.intensity();
            pointLightDistances[index] = light.distance();
            pointLightDecays[index] = light.decay();
        }
        glUniform3fv(pointLightPositionsLocation, pointLightPositions);
        glUniform3fv(pointLightColorsLocation, pointLightColors);
        glUniform1fv(pointLightDistancesLocation, pointLightDistances);
        glUniform1fv(pointLightDecaysLocation, pointLightDecays);
    }

    /** Deletes the linked context-local program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }

    /** Copies and uploads one four-by-four matrix. */
    private void uploadMatrix4(int location, Matrix4fc matrix) {
        matrix.get(matrix4Values);
        glUniformMatrix4fv(location, false, matrix4Values);
    }
}
