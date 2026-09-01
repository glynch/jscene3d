/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

import io.github.glynch.jscene3d.fogs.Fog;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.internal.LightCollection;
import io.github.glynch.jscene3d.render.internal.ShadowFrame;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Compiled built-in diffuse Lambert mesh program. */
public final class LambertProgram implements AutoCloseable {
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec3 normal;
            layout(location = 2) in vec2 textureCoordinate;
            layout(location = 3) in vec4 vertexColor;

            INSTANCING_SOURCE
            MORPH_SOURCE

            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform mat3 normalMatrix;
            uniform mat3 colorMapTransform;
            uniform bool useVertexColor;
            uniform bool useColorMap;
            uniform bool flipColorMapVertically;

            out vec3 resolvedViewPosition;
            out vec3 resolvedViewNormal;
            out vec4 resolvedVertexColor;
            out vec2 resolvedTextureCoordinate;

            void main() {
                mat4 instanceMatrix = resolvedInstanceMatrix();
                vec4 viewPosition = viewMatrix * modelMatrix * instanceMatrix
                        * vec4(resolvedMorphPosition(position), 1.0);
                resolvedViewPosition = viewPosition.xyz;
                resolvedViewNormal = normalize(normalMatrix * resolvedInstanceNormalMatrix(instanceMatrix)
                        * resolvedMorphNormal(normal));
                resolvedVertexColor = (useVertexColor ? vertexColor : vec4(1.0)) * resolvedInstanceColor();
                if (useColorMap) {
                    vec2 transformedTextureCoordinate =
                            (colorMapTransform * vec3(textureCoordinate, 1.0)).xy;
                    resolvedTextureCoordinate = flipColorMapVertically
                            ? vec2(transformedTextureCoordinate.x, 1.0 - transformedTextureCoordinate.y)
                            : transformedTextureCoordinate;
                } else {
                    resolvedTextureCoordinate = vec2(0.0);
                }
                gl_Position = projectionMatrix * viewPosition;
            }
            """.replace("INSTANCING_SOURCE", InstancingShaderSource.source())
            .replace("MORPH_SOURCE", MorphShaderSource.source());
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            const float RECIPROCAL_PI = 0.3183098861837907;
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

            SHADOW_SOURCE
            FOG_SOURCE

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

            float spotAttenuation(float angleCosine, float coneCosine, float penumbraCosine) {
                if (penumbraCosine <= coneCosine) {
                    return step(coneCosine, angleCosine);
                }
                return smoothstep(coneCosine, penumbraCosine, angleCosine);
            }

            void main() {
                vec3 surfaceNormal = gl_FrontFacing ? resolvedViewNormal : -resolvedViewNormal;
                vec3 diffuseIrradiance = ambientLightColor;
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
                    float visibility = pointShadow(
                            pointShadowIndices[index], resolvedViewPosition, surfaceNormal);
                    diffuseIrradiance += pointLightColors[index] * diffuse * attenuation * visibility;
                }
                for (int index = 0; index < MAX_DIRECTIONAL_LIGHTS; index++) {
                    if (index >= directionalLightCount) {
                        break;
                    }
                    float diffuse = max(dot(surfaceNormal, directionalLightDirections[index]), 0.0);
                    float visibility = twoDimensionalShadow(
                            directionalShadowIndices[index], resolvedViewPosition, surfaceNormal);
                    diffuseIrradiance += directionalLightColors[index] * diffuse * visibility;
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
                    float diffuse = max(dot(surfaceNormal, lightDirection), 0.0);
                    float visibility = twoDimensionalShadow(
                            spotShadowIndices[index], resolvedViewPosition, surfaceNormal);
                    diffuseIrradiance += spotLightColors[index]
                            * diffuse
                            * distanceFalloff
                            * coneAttenuation
                            * visibility;
                }
                for (int index = 0; index < MAX_HEMISPHERE_LIGHTS; index++) {
                    if (index >= hemisphereLightCount) {
                        break;
                    }
                    float skyWeight = dot(surfaceNormal, hemisphereLightDirections[index]) * 0.5 + 0.5;
                    diffuseIrradiance += mix(
                            hemisphereLightGroundColors[index],
                            hemisphereLightSkyColors[index],
                            skyWeight);
                }

                vec4 textureColor = useColorMap ? texture(colorMap, resolvedTextureCoordinate) : vec4(1.0);
                vec4 surfaceColor = baseColor * resolvedVertexColor * textureColor;
                if (alphaCutoff >= 0.0 && surfaceColor.a < alphaCutoff) {
                    discard;
                }
                vec3 reflected = surfaceColor.rgb * diffuseIrradiance * RECIPROCAL_PI;
                fragmentColor = vec4(
                        applyFog(reflected, -resolvedViewPosition.z),
                        surfaceColor.a);
            }
            """.replace("SHADOW_SOURCE", ShadowShaderSource.source())
            .replace("FOG_SOURCE", FogShaderSource.source())
            .replace("POINT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_POINT_LIGHTS))
            .replace("DIRECTIONAL_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_DIRECTIONAL_LIGHTS))
            .replace("SPOT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_SPOT_LIGHTS))
            .replace("HEMISPHERE_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_HEMISPHERE_LIGHTS));

    private final int id;
    private final LitProgramState litState;
    private final FogProgramState fogState;
    private final int colorMapTransformLocation;
    private final int flipColorMapVerticallyLocation;
    private final int baseColorLocation;
    private final int useVertexColorLocation;
    private final int colorMapLocation;
    private final int useColorMapLocation;
    private final int alphaCutoffLocation;

    /** Retains a linked program and all reusable transform and light staging. */
    private LambertProgram(int id) {
        this.id = id;
        litState = new LitProgramState(id, "Built-in Lambert");
        fogState = new FogProgramState(id, "Built-in Lambert");
        colorMapTransformLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "colorMapTransform");
        flipColorMapVerticallyLocation =
                ProgramSupport.requiredUniform(id, "Built-in Lambert", "flipColorMapVertically");
        baseColorLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "baseColor");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "useVertexColor");
        colorMapLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "colorMap");
        useColorMapLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "useColorMap");
        alphaCutoffLocation = ProgramSupport.requiredUniform(id, "Built-in Lambert", "alphaCutoff");
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
     * Returns the required alpha-cutoff uniform location.
     *
     * @return uniform location
     */
    public int alphaCutoffLocation() {
        return alphaCutoffLocation;
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
     * Returns the color-map vertical-orientation switch uniform location.
     *
     * @return uniform location
     */
    public int flipColorMapVerticallyLocation() {
        return flipColorMapVerticallyLocation;
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

    /**
     * Uploads shadow maps and the current mesh receiver switch.
     *
     * @param receiveShadow whether the current mesh receives shadows
     * @param frame completed shadow frame
     * @param lights ordered visible lights
     * @param viewMatrix current camera view matrix
     */
    public void uploadShadows(boolean receiveShadow, ShadowFrame frame, LightCollection lights, Matrix4fc viewMatrix) {
        litState.uploadShadows(receiveShadow, frame, lights, viewMatrix);
    }

    /**
     * Uploads the current optional scene fog.
     *
     * @param fog scene fog, or {@code null} when disabled
     */
    public void uploadFog(@Nullable Fog fog) {
        fogState.upload(fog);
    }

    /**
     * Uploads optional batch-transform and color switches.
     *
     * @param instanced whether the draw consumes per-instance transforms
     * @param colors whether the draw consumes per-instance colors
     */
    public void uploadInstancing(boolean instanced, boolean colors) {
        litState.uploadInstancing(instanced, colors);
    }

    /**
     * Uploads the current morph-target data layout.
     *
     * @param enabled whether morph deformation is enabled
     * @param targetCount number of morph targets
     * @param vertexCount number of vertices in each target
     * @param instanceWeights whether weights vary by instance
     */
    public void uploadMorphing(boolean enabled, int targetCount, int vertexCount, boolean instanceWeights) {
        litState.uploadMorphing(enabled, targetCount, vertexCount, instanceWeights);
    }

    /** Deletes the linked context-local program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }
}
