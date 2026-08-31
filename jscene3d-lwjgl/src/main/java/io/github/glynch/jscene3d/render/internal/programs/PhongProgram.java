/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.internal.LightCollection;
import org.joml.Matrix4fc;

/** Compiled built-in Blinn-Phong mesh program. */
public final class PhongProgram implements AutoCloseable {
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
            uniform mat3 colorMapTransform;
            uniform bool useVertexColor;
            uniform bool useColorMap;

            out vec3 resolvedViewPosition;
            out vec3 resolvedViewNormal;
            out vec4 resolvedVertexColor;
            out vec2 resolvedTextureCoordinate;

            void main() {
                vec4 viewPosition = viewMatrix * modelMatrix * vec4(position, 1.0);
                resolvedViewPosition = viewPosition.xyz;
                resolvedViewNormal = normalize(normalMatrix * normal);
                resolvedVertexColor = useVertexColor ? vertexColor : vec4(1.0);
                if (useColorMap) {
                    vec2 transformedTextureCoordinate =
                            (colorMapTransform * vec3(textureCoordinate, 1.0)).xy;
                    resolvedTextureCoordinate =
                            vec2(transformedTextureCoordinate.x, 1.0 - transformedTextureCoordinate.y);
                } else {
                    resolvedTextureCoordinate = vec2(0.0);
                }
                gl_Position = projectionMatrix * viewPosition;
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            const int MAX_POINT_LIGHTS = POINT_LIGHT_CAPACITY;
            const int MAX_DIRECTIONAL_LIGHTS = DIRECTIONAL_LIGHT_CAPACITY;
            const int MAX_SPOT_LIGHTS = SPOT_LIGHT_CAPACITY;
            const int MAX_HEMISPHERE_LIGHTS = HEMISPHERE_LIGHT_CAPACITY;

            in vec3 resolvedViewPosition;
            in vec3 resolvedViewNormal;
            in vec4 resolvedVertexColor;
            in vec2 resolvedTextureCoordinate;

            uniform vec4 baseColor;
            uniform sampler2D colorMap;
            uniform bool useColorMap;
            uniform float alphaCutoff;
            uniform vec3 emissiveColor;
            uniform vec3 specularColor;
            uniform float shininess;
            uniform vec3 ambientLightColor;
            uniform int pointLightCount;
            uniform vec3 pointLightPositions[MAX_POINT_LIGHTS];
            uniform vec3 pointLightColors[MAX_POINT_LIGHTS];
            uniform float pointLightDistances[MAX_POINT_LIGHTS];
            uniform float pointLightDecays[MAX_POINT_LIGHTS];
            uniform int directionalLightCount;
            uniform vec3 directionalLightDirections[MAX_DIRECTIONAL_LIGHTS];
            uniform vec3 directionalLightColors[MAX_DIRECTIONAL_LIGHTS];
            uniform int spotLightCount;
            uniform vec3 spotLightPositions[MAX_SPOT_LIGHTS];
            uniform vec3 spotLightDirections[MAX_SPOT_LIGHTS];
            uniform vec3 spotLightColors[MAX_SPOT_LIGHTS];
            uniform float spotLightDistances[MAX_SPOT_LIGHTS];
            uniform float spotLightDecays[MAX_SPOT_LIGHTS];
            uniform float spotLightConeCosines[MAX_SPOT_LIGHTS];
            uniform float spotLightPenumbraCosines[MAX_SPOT_LIGHTS];
            uniform int hemisphereLightCount;
            uniform vec3 hemisphereLightDirections[MAX_HEMISPHERE_LIGHTS];
            uniform vec3 hemisphereLightSkyColors[MAX_HEMISPHERE_LIGHTS];
            uniform vec3 hemisphereLightGroundColors[MAX_HEMISPHERE_LIGHTS];

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

            float specularStrength(vec3 normal, vec3 lightDirection, vec3 viewDirection) {
                float diffuse = dot(normal, lightDirection);
                if (diffuse <= 0.0) {
                    return 0.0;
                }
                vec3 halfDirection = normalize(lightDirection + viewDirection);
                return pow(max(dot(normal, halfDirection), 0.0), shininess);
            }

            float spotAttenuation(float angleCosine, float coneCosine, float penumbraCosine) {
                if (penumbraCosine <= coneCosine) {
                    return step(coneCosine, angleCosine);
                }
                return smoothstep(coneCosine, penumbraCosine, angleCosine);
            }

            void main() {
                vec3 surfaceNormal = normalize(gl_FrontFacing ? resolvedViewNormal : -resolvedViewNormal);
                vec3 viewDirection = normalize(-resolvedViewPosition);
                vec3 diffuseIllumination = ambientLightColor;
                vec3 specularIllumination = vec3(0.0);

                for (int index = 0; index < MAX_POINT_LIGHTS; index++) {
                    if (index >= pointLightCount) {
                        break;
                    }
                    vec3 lightOffset = pointLightPositions[index] - resolvedViewPosition;
                    float lightDistance = length(lightOffset);
                    vec3 lightDirection = lightOffset / max(lightDistance, 0.01);
                    float attenuation = distanceAttenuation(
                            lightDistance,
                            pointLightDistances[index],
                            pointLightDecays[index]);
                    vec3 radiance = pointLightColors[index] * attenuation;
                    diffuseIllumination += radiance * max(dot(surfaceNormal, lightDirection), 0.0);
                    specularIllumination += radiance
                            * specularStrength(surfaceNormal, lightDirection, viewDirection);
                }
                for (int index = 0; index < MAX_DIRECTIONAL_LIGHTS; index++) {
                    if (index >= directionalLightCount) {
                        break;
                    }
                    vec3 lightDirection = directionalLightDirections[index];
                    vec3 radiance = directionalLightColors[index];
                    diffuseIllumination += radiance * max(dot(surfaceNormal, lightDirection), 0.0);
                    specularIllumination += radiance
                            * specularStrength(surfaceNormal, lightDirection, viewDirection);
                }
                for (int index = 0; index < MAX_SPOT_LIGHTS; index++) {
                    if (index >= spotLightCount) {
                        break;
                    }
                    vec3 lightOffset = spotLightPositions[index] - resolvedViewPosition;
                    float lightDistance = length(lightOffset);
                    vec3 lightDirection = lightOffset / max(lightDistance, 0.01);
                    float angleCosine = dot(-lightDirection, spotLightDirections[index]);
                    float coneAttenuation = spotAttenuation(
                            angleCosine,
                            spotLightConeCosines[index],
                            spotLightPenumbraCosines[index]);
                    float distanceFalloff = distanceAttenuation(
                            lightDistance,
                            spotLightDistances[index],
                            spotLightDecays[index]);
                    vec3 radiance = spotLightColors[index] * distanceFalloff * coneAttenuation;
                    diffuseIllumination += radiance * max(dot(surfaceNormal, lightDirection), 0.0);
                    specularIllumination += radiance
                            * specularStrength(surfaceNormal, lightDirection, viewDirection);
                }
                for (int index = 0; index < MAX_HEMISPHERE_LIGHTS; index++) {
                    if (index >= hemisphereLightCount) {
                        break;
                    }
                    float skyWeight = dot(surfaceNormal, hemisphereLightDirections[index]) * 0.5 + 0.5;
                    diffuseIllumination += mix(
                            hemisphereLightGroundColors[index],
                            hemisphereLightSkyColors[index],
                            skyWeight);
                }

                vec4 textureColor = useColorMap ? texture(colorMap, resolvedTextureCoordinate) : vec4(1.0);
                vec4 surfaceColor = baseColor * resolvedVertexColor * textureColor;
                if (alphaCutoff >= 0.0 && surfaceColor.a < alphaCutoff) {
                    discard;
                }
                vec3 reflected = surfaceColor.rgb * diffuseIllumination
                        + specularColor * specularIllumination
                        + emissiveColor;
                fragmentColor = vec4(reflected, surfaceColor.a);
            }
            """.replace(
                    "POINT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_POINT_LIGHTS))
            .replace("DIRECTIONAL_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_DIRECTIONAL_LIGHTS))
            .replace("SPOT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_SPOT_LIGHTS))
            .replace("HEMISPHERE_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_HEMISPHERE_LIGHTS));

    private final int id;
    private final LitProgramState litState;
    private final int colorMapTransformLocation;
    private final int baseColorLocation;
    private final int useVertexColorLocation;
    private final int colorMapLocation;
    private final int useColorMapLocation;
    private final int emissiveColorLocation;
    private final int specularColorLocation;
    private final int shininessLocation;
    private final int alphaCutoffLocation;

    /** Retains a linked program and all reusable transform and light staging. */
    private PhongProgram(int id) {
        this.id = id;
        litState = new LitProgramState(id, "Built-in Phong");
        colorMapTransformLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "colorMapTransform");
        baseColorLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "baseColor");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "useVertexColor");
        colorMapLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "colorMap");
        useColorMapLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "useColorMap");
        emissiveColorLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "emissiveColor");
        specularColorLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "specularColor");
        shininessLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "shininess");
        alphaCutoffLocation = ProgramSupport.requiredUniform(id, "Built-in Phong", "alphaCutoff");
    }

    /**
     * Compiles, links, validates, and returns the built-in Phong program.
     *
     * @return linked Phong program
     */
    public static PhongProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in Phong", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new PhongProgram(program);
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
     * Returns the required color-map transform uniform location.
     *
     * @return uniform location
     */
    public int colorMapTransformLocation() {
        return colorMapTransformLocation;
    }

    /**
     * Returns the required emissive-color uniform location.
     *
     * @return uniform location
     */
    public int emissiveColorLocation() {
        return emissiveColorLocation;
    }

    /**
     * Returns the required specular-color uniform location.
     *
     * @return uniform location
     */
    public int specularColorLocation() {
        return specularColorLocation;
    }

    /**
     * Returns the required shininess uniform location.
     *
     * @return uniform location
     */
    public int shininessLocation() {
        return shininessLocation;
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
     * Uploads object, camera, and inverse-transpose normal transforms without allocating.
     *
     * @param modelMatrix object model matrix
     * @param viewMatrix current view matrix
     * @param projectionMatrix current projection matrix
     */
    public void uploadTransforms(Matrix4fc modelMatrix, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        litState.uploadTransforms(modelMatrix, viewMatrix, projectionMatrix);
    }

    /**
     * Uploads combined ambient and ordered point- and directional-light state without allocating.
     *
     * @param lights active visible lights
     * @param viewMatrix current view matrix
     */
    public void uploadLights(LightCollection lights, Matrix4fc viewMatrix) {
        litState.uploadLights(lights, viewMatrix);
    }

    /** Deletes the linked context-local program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
