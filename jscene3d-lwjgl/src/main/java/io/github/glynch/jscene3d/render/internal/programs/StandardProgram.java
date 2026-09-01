/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;

import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.internal.LightCollection;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;

/** Compiled built-in metallic-roughness physically based mesh program. */
public final class StandardProgram implements AutoCloseable {
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
                resolvedTextureCoordinate = textureCoordinate;
                gl_Position = projectionMatrix * viewPosition;
            }
            """;
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            const float PI = 3.141592653589793;
            const int MAX_POINT_LIGHTS = POINT_LIGHT_CAPACITY;
            const int MAX_DIRECTIONAL_LIGHTS = DIRECTIONAL_LIGHT_CAPACITY;
            const int MAX_SPOT_LIGHTS = SPOT_LIGHT_CAPACITY;
            const int MAX_HEMISPHERE_LIGHTS = HEMISPHERE_LIGHT_CAPACITY;

            in vec3 resolvedViewPosition;
            in vec3 resolvedViewNormal;
            in vec4 resolvedVertexColor;
            in vec2 resolvedTextureCoordinate;

            uniform vec4 baseColor;
            uniform float metalness;
            uniform float roughness;
            uniform vec3 emissiveColor;
            uniform vec2 normalScale;
            uniform float occlusionStrength;
            uniform float alphaCutoff;
            uniform float environmentIntensity;
            uniform float maximumReflectionLevel;
            uniform bool useEnvironmentMap;
            uniform mat3 viewToWorldMatrix;
            uniform mat3 environmentRotationMatrix;

            uniform sampler2D colorMap;
            uniform sampler2D metalnessRoughnessMap;
            uniform sampler2D normalMap;
            uniform sampler2D occlusionMap;
            uniform sampler2D emissiveMap;
            uniform bool useColorMap;
            uniform bool useMetalnessRoughnessMap;
            uniform bool useNormalMap;
            uniform bool useOcclusionMap;
            uniform bool useEmissiveMap;
            uniform mat3 colorMapTransform;
            uniform mat3 metalnessRoughnessMapTransform;
            uniform mat3 normalMapTransform;
            uniform mat3 occlusionMapTransform;
            uniform mat3 emissiveMapTransform;
            uniform bool flipColorMapVertically;
            uniform bool flipMetalnessRoughnessMapVertically;
            uniform bool flipNormalMapVertically;
            uniform bool flipOcclusionMapVertically;
            uniform bool flipEmissiveMapVertically;
            uniform sampler2D environmentIrradianceMap;
            uniform sampler2D environmentReflectionMap;
            uniform sampler2D environmentBrdfMap;

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

            vec2 transformedCoordinate(mat3 transform, bool flipVertically) {
                vec2 transformed = (transform * vec3(resolvedTextureCoordinate, 1.0)).xy;
                return flipVertically
                        ? vec2(transformed.x, 1.0 - transformed.y)
                        : transformed;
            }

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

            mat3 cotangentFrame(vec3 surfaceNormal, vec3 viewPosition, vec2 textureCoordinate) {
                vec3 positionDerivativeX = dFdx(viewPosition);
                vec3 positionDerivativeY = dFdy(viewPosition);
                vec2 textureDerivativeX = dFdx(textureCoordinate);
                vec2 textureDerivativeY = dFdy(textureCoordinate);
                vec3 perpendicularY = cross(positionDerivativeY, surfaceNormal);
                vec3 perpendicularX = cross(surfaceNormal, positionDerivativeX);
                vec3 tangent = perpendicularY * textureDerivativeX.x
                        + perpendicularX * textureDerivativeY.x;
                vec3 bitangent = perpendicularY * textureDerivativeX.y
                        + perpendicularX * textureDerivativeY.y;
                float scale = inversesqrt(max(max(dot(tangent, tangent), dot(bitangent, bitangent)), 1e-8));
                return mat3(tangent * scale, bitangent * scale, surfaceNormal);
            }

            vec3 resolveSurfaceNormal() {
                vec3 surfaceNormal = normalize(gl_FrontFacing ? resolvedViewNormal : -resolvedViewNormal);
                if (!useNormalMap) {
                    return surfaceNormal;
                }
                vec2 coordinate = transformedCoordinate(normalMapTransform, flipNormalMapVertically);
                vec3 sampledNormal = texture(normalMap, coordinate).xyz * 2.0 - 1.0;
                sampledNormal.xy *= normalScale;
                return normalize(cotangentFrame(surfaceNormal, resolvedViewPosition, coordinate) * sampledNormal);
            }

            float normalDistribution(float normalDotHalf, float alpha) {
                float alphaSquared = alpha * alpha;
                float denominator = normalDotHalf * normalDotHalf * (alphaSquared - 1.0) + 1.0;
                return alphaSquared / max(PI * denominator * denominator, 1e-7);
            }

            float geometryOcclusion(float normalDotDirection, float roughnessValue) {
                float factor = roughnessValue + 1.0;
                float k = factor * factor / 8.0;
                return normalDotDirection
                        / max(normalDotDirection * (1.0 - k) + k, 1e-7);
            }

            vec3 fresnelSchlick(float directionDotHalf, vec3 reflectance) {
                return reflectance + (vec3(1.0) - reflectance)
                        * pow(clamp(1.0 - directionDotHalf, 0.0, 1.0), 5.0);
            }

            vec3 fresnelSchlickRoughness(float normalDotView, vec3 reflectance, float roughnessValue) {
                vec3 grazing = max(vec3(1.0 - roughnessValue), reflectance);
                return reflectance + (grazing - reflectance)
                        * pow(clamp(1.0 - normalDotView, 0.0, 1.0), 5.0);
            }

            vec2 equirectangularCoordinate(vec3 direction) {
                vec3 normalizedDirection = normalize(direction);
                float u = atan(normalizedDirection.z, normalizedDirection.x) / (2.0 * PI) + 0.5;
                float v = acos(clamp(normalizedDirection.y, -1.0, 1.0)) / PI;
                return vec2(u, v);
            }

            vec3 directContribution(
                    vec3 surfaceNormal,
                    vec3 viewDirection,
                    vec3 lightDirection,
                    vec3 radiance,
                    vec3 diffuseColor,
                    vec3 reflectance,
                    float roughnessValue) {
                float normalDotLight = max(dot(surfaceNormal, lightDirection), 0.0);
                float normalDotView = max(dot(surfaceNormal, viewDirection), 0.0);
                if (normalDotLight <= 0.0 || normalDotView <= 0.0) {
                    return vec3(0.0);
                }
                vec3 halfDirection = normalize(viewDirection + lightDirection);
                float normalDotHalf = max(dot(surfaceNormal, halfDirection), 0.0);
                float viewDotHalf = max(dot(viewDirection, halfDirection), 0.0);
                float alpha = max(roughnessValue * roughnessValue, 0.0025);
                float distribution = normalDistribution(normalDotHalf, alpha);
                float geometry = geometryOcclusion(normalDotView, roughnessValue)
                        * geometryOcclusion(normalDotLight, roughnessValue);
                vec3 fresnel = fresnelSchlick(viewDotHalf, reflectance);
                vec3 specular = distribution * geometry * fresnel
                        / max(4.0 * normalDotView * normalDotLight, 1e-7);
                vec3 diffuse = (vec3(1.0) - fresnel) * diffuseColor / PI;
                return (diffuse + specular) * radiance * normalDotLight;
            }

            void main() {
                vec4 sampledBaseColor = useColorMap
                        ? texture(colorMap, transformedCoordinate(colorMapTransform, flipColorMapVertically))
                        : vec4(1.0);
                vec4 surfaceColor = baseColor * resolvedVertexColor * sampledBaseColor;
                if (alphaCutoff >= 0.0 && surfaceColor.a < alphaCutoff) {
                    discard;
                }

                float resolvedMetalness = metalness;
                float resolvedRoughness = roughness;
                if (useMetalnessRoughnessMap) {
                    vec4 sampledProperties = texture(
                            metalnessRoughnessMap,
                            transformedCoordinate(
                                    metalnessRoughnessMapTransform,
                                    flipMetalnessRoughnessMapVertically));
                    resolvedRoughness *= sampledProperties.g;
                    resolvedMetalness *= sampledProperties.b;
                }
                resolvedMetalness = clamp(resolvedMetalness, 0.0, 1.0);
                resolvedRoughness = clamp(resolvedRoughness, 0.04, 1.0);

                vec3 surfaceNormal = resolveSurfaceNormal();
                vec3 viewDirection = normalize(-resolvedViewPosition);
                vec3 reflectance = mix(vec3(0.04), surfaceColor.rgb, resolvedMetalness);
                vec3 diffuseColor = surfaceColor.rgb * (1.0 - resolvedMetalness);
                vec3 reflected = vec3(0.0);

                for (int index = 0; index < MAX_POINT_LIGHTS; index++) {
                    if (index >= pointLightCount) {
                        break;
                    }
                    vec3 lightOffset = pointLightPositions[index] - resolvedViewPosition;
                    float lightDistance = length(lightOffset);
                    vec3 lightDirection = lightOffset / max(lightDistance, 0.01);
                    vec3 radiance = pointLightColors[index] * distanceAttenuation(
                            lightDistance,
                            pointLightDistances[index],
                            pointLightDecays[index]);
                    reflected += directContribution(
                            surfaceNormal,
                            viewDirection,
                            lightDirection,
                            radiance,
                            diffuseColor,
                            reflectance,
                            resolvedRoughness);
                }
                for (int index = 0; index < MAX_DIRECTIONAL_LIGHTS; index++) {
                    if (index >= directionalLightCount) {
                        break;
                    }
                    reflected += directContribution(
                            surfaceNormal,
                            viewDirection,
                            directionalLightDirections[index],
                            directionalLightColors[index],
                            diffuseColor,
                            reflectance,
                            resolvedRoughness);
                }
                for (int index = 0; index < MAX_SPOT_LIGHTS; index++) {
                    if (index >= spotLightCount) {
                        break;
                    }
                    vec3 lightOffset = spotLightPositions[index] - resolvedViewPosition;
                    float lightDistance = length(lightOffset);
                    vec3 lightDirection = lightOffset / max(lightDistance, 0.01);
                    float cone = spotAttenuation(
                            dot(-lightDirection, spotLightDirections[index]),
                            spotLightConeCosines[index],
                            spotLightPenumbraCosines[index]);
                    vec3 radiance = spotLightColors[index] * cone * distanceAttenuation(
                            lightDistance,
                            spotLightDistances[index],
                            spotLightDecays[index]);
                    reflected += directContribution(
                            surfaceNormal,
                            viewDirection,
                            lightDirection,
                            radiance,
                            diffuseColor,
                            reflectance,
                            resolvedRoughness);
                }

                vec3 indirectLight = ambientLightColor;
                for (int index = 0; index < MAX_HEMISPHERE_LIGHTS; index++) {
                    if (index >= hemisphereLightCount) {
                        break;
                    }
                    float skyWeight = dot(surfaceNormal, hemisphereLightDirections[index]) * 0.5 + 0.5;
                    indirectLight += mix(
                            hemisphereLightGroundColors[index],
                            hemisphereLightSkyColors[index],
                            skyWeight);
                }
                float occlusion = 1.0;
                if (useOcclusionMap) {
                    float sampledOcclusion = texture(
                            occlusionMap,
                            transformedCoordinate(occlusionMapTransform, flipOcclusionMapVertically)).r;
                    occlusion = mix(1.0, sampledOcclusion, occlusionStrength);
                }
                reflected += indirectLight * diffuseColor * occlusion;

                if (useEnvironmentMap) {
                    vec3 worldNormal = normalize(viewToWorldMatrix * surfaceNormal);
                    vec3 worldViewDirection = normalize(viewToWorldMatrix * viewDirection);
                    float normalDotView = max(dot(surfaceNormal, viewDirection), 0.0);
                    vec3 environmentNormal = environmentRotationMatrix * worldNormal;
                    vec3 reflectionDirection = reflect(-worldViewDirection, worldNormal);
                    vec3 environmentReflection = environmentRotationMatrix * reflectionDirection;
                    vec3 irradiance = texture(
                            environmentIrradianceMap,
                            equirectangularCoordinate(environmentNormal)).rgb;
                    vec3 prefilteredRadiance = textureLod(
                            environmentReflectionMap,
                            equirectangularCoordinate(environmentReflection),
                            resolvedRoughness * maximumReflectionLevel).rgb;
                    vec2 integratedBrdf = texture(
                            environmentBrdfMap,
                            vec2(normalDotView, resolvedRoughness)).rg;
                    vec3 environmentFresnel = fresnelSchlickRoughness(
                            normalDotView,
                            reflectance,
                            resolvedRoughness);
                    vec3 environmentDiffuse = (vec3(1.0) - environmentFresnel)
                            * diffuseColor
                            * irradiance;
                    vec3 environmentSpecular = prefilteredRadiance
                            * (reflectance * integratedBrdf.x + integratedBrdf.y);
                    reflected += (environmentDiffuse + environmentSpecular)
                            * environmentIntensity
                            * occlusion;
                }

                vec3 emissiveSample = useEmissiveMap
                        ? texture(
                                emissiveMap,
                                transformedCoordinate(emissiveMapTransform, flipEmissiveMapVertically)).rgb
                        : vec3(1.0);
                reflected += emissiveColor * emissiveSample;
                fragmentColor = vec4(reflected, surfaceColor.a);
            }
            """.replace(
                    "POINT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_POINT_LIGHTS))
            .replace("DIRECTIONAL_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_DIRECTIONAL_LIGHTS))
            .replace("SPOT_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_SPOT_LIGHTS))
            .replace("HEMISPHERE_LIGHT_CAPACITY", Integer.toString(Renderer.MAX_HEMISPHERE_LIGHTS));

    private final int id;
    private final LitProgramState litState;
    private final int baseColorLocation;
    private final int metalnessLocation;
    private final int roughnessLocation;
    private final int emissiveColorLocation;
    private final int normalScaleLocation;
    private final int occlusionStrengthLocation;
    private final int alphaCutoffLocation;
    private final int useVertexColorLocation;
    private final int environmentIntensityLocation;
    private final int maximumReflectionLevelLocation;
    private final int useEnvironmentMapLocation;
    private final int viewToWorldMatrixLocation;
    private final int environmentRotationMatrixLocation;
    private final int environmentIrradianceMapLocation;
    private final int environmentReflectionMapLocation;
    private final int environmentBrdfMapLocation;
    private final float[] matrix3Values = new float[9];
    private final TextureLocations colorMap;
    private final TextureLocations metalnessRoughnessMap;
    private final TextureLocations normalMap;
    private final TextureLocations occlusionMap;
    private final TextureLocations emissiveMap;

    /** Resolves all required uniforms and allocates reusable light staging. */
    private StandardProgram(int id) {
        this.id = id;
        String label = "Built-in standard";
        litState = new LitProgramState(id, label);
        baseColorLocation = ProgramSupport.requiredUniform(id, label, "baseColor");
        metalnessLocation = ProgramSupport.requiredUniform(id, label, "metalness");
        roughnessLocation = ProgramSupport.requiredUniform(id, label, "roughness");
        emissiveColorLocation = ProgramSupport.requiredUniform(id, label, "emissiveColor");
        normalScaleLocation = ProgramSupport.requiredUniform(id, label, "normalScale");
        occlusionStrengthLocation = ProgramSupport.requiredUniform(id, label, "occlusionStrength");
        alphaCutoffLocation = ProgramSupport.requiredUniform(id, label, "alphaCutoff");
        useVertexColorLocation = ProgramSupport.requiredUniform(id, label, "useVertexColor");
        environmentIntensityLocation = ProgramSupport.requiredUniform(id, label, "environmentIntensity");
        maximumReflectionLevelLocation = ProgramSupport.requiredUniform(id, label, "maximumReflectionLevel");
        useEnvironmentMapLocation = ProgramSupport.requiredUniform(id, label, "useEnvironmentMap");
        viewToWorldMatrixLocation = ProgramSupport.requiredUniform(id, label, "viewToWorldMatrix");
        environmentRotationMatrixLocation = ProgramSupport.requiredUniform(id, label, "environmentRotationMatrix");
        environmentIrradianceMapLocation = ProgramSupport.requiredUniform(id, label, "environmentIrradianceMap");
        environmentReflectionMapLocation = ProgramSupport.requiredUniform(id, label, "environmentReflectionMap");
        environmentBrdfMapLocation = ProgramSupport.requiredUniform(id, label, "environmentBrdfMap");
        colorMap = TextureLocations.resolve(id, label, "colorMap");
        metalnessRoughnessMap = TextureLocations.resolve(id, label, "metalnessRoughnessMap");
        normalMap = TextureLocations.resolve(id, label, "normalMap");
        occlusionMap = TextureLocations.resolve(id, label, "occlusionMap");
        emissiveMap = TextureLocations.resolve(id, label, "emissiveMap");
    }

    /**
     * Compiles, links, validates, and returns the standard material program.
     *
     * @return linked context-local program
     */
    public static StandardProgram create() {
        int program = ProgramSupport.createLinkedProgram("Built-in standard", VERTEX_SOURCE, FRAGMENT_SOURCE);
        try {
            return new StandardProgram(program);
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
     * Uploads object, camera, and inverse-transpose normal transforms without allocating.
     *
     * @param modelMatrix object-to-world transform
     * @param viewMatrix world-to-view transform
     * @param projectionMatrix view-to-clip transform
     */
    public void uploadTransforms(Matrix4fc modelMatrix, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        litState.uploadTransforms(modelMatrix, viewMatrix, projectionMatrix);
    }

    /**
     * Uploads all visible supported light state without allocating.
     *
     * @param lights collected supported lights
     * @param viewMatrix world-to-view transform
     */
    public void uploadLights(LightCollection lights, Matrix4fc viewMatrix) {
        litState.uploadLights(lights, viewMatrix);
    }

    /**
     * Returns the base-color uniform location.
     *
     * @return uniform location
     */
    public int baseColorLocation() {
        return baseColorLocation;
    }

    /**
     * Returns the metalness uniform location.
     *
     * @return uniform location
     */
    public int metalnessLocation() {
        return metalnessLocation;
    }

    /**
     * Returns the roughness uniform location.
     *
     * @return uniform location
     */
    public int roughnessLocation() {
        return roughnessLocation;
    }

    /**
     * Returns the emissive-color uniform location.
     *
     * @return uniform location
     */
    public int emissiveColorLocation() {
        return emissiveColorLocation;
    }

    /**
     * Returns the normal-scale uniform location.
     *
     * @return uniform location
     */
    public int normalScaleLocation() {
        return normalScaleLocation;
    }

    /**
     * Returns the occlusion-strength uniform location.
     *
     * @return uniform location
     */
    public int occlusionStrengthLocation() {
        return occlusionStrengthLocation;
    }

    /**
     * Returns the alpha-cutoff uniform location.
     *
     * @return uniform location
     */
    public int alphaCutoffLocation() {
        return alphaCutoffLocation;
    }

    /**
     * Returns the vertex-color switch uniform location.
     *
     * @return uniform location
     */
    public int useVertexColorLocation() {
        return useVertexColorLocation;
    }

    /**
     * Returns the combined scene/material environment-intensity uniform location.
     *
     * @return uniform location
     */
    public int environmentIntensityLocation() {
        return environmentIntensityLocation;
    }

    /**
     * Returns the largest prefiltered-reflection mip uniform location.
     *
     * @return uniform location
     */
    public int maximumReflectionLevelLocation() {
        return maximumReflectionLevelLocation;
    }

    /**
     * Returns the image-based-lighting enable-switch uniform location.
     *
     * @return uniform location
     */
    public int useEnvironmentMapLocation() {
        return useEnvironmentMapLocation;
    }

    /**
     * Returns the diffuse irradiance sampler location.
     *
     * @return sampler location
     */
    public int environmentIrradianceMapLocation() {
        return environmentIrradianceMapLocation;
    }

    /**
     * Returns the prefiltered reflection sampler location.
     *
     * @return sampler location
     */
    public int environmentReflectionMapLocation() {
        return environmentReflectionMapLocation;
    }

    /**
     * Returns the integrated BRDF sampler location.
     *
     * @return sampler location
     */
    public int environmentBrdfMapLocation() {
        return environmentBrdfMapLocation;
    }

    /**
     * Uploads current camera and environment rotations without allocation.
     *
     * @param viewToWorld camera-view to world-space rotation
     * @param environmentRotation world-to-environment rotation
     */
    public void uploadEnvironmentMatrices(Matrix3fc viewToWorld, Matrix3fc environmentRotation) {
        viewToWorld.get(matrix3Values);
        glUniformMatrix3fv(viewToWorldMatrixLocation, false, matrix3Values);
        environmentRotation.get(matrix3Values);
        glUniformMatrix3fv(environmentRotationMatrixLocation, false, matrix3Values);
    }

    /**
     * Returns the base-color map locations.
     *
     * @return sampler, enable switch, and transform locations
     */
    public TextureLocations colorMap() {
        return colorMap;
    }

    /**
     * Returns the metallic-roughness map locations.
     *
     * @return sampler, enable switch, and transform locations
     */
    public TextureLocations metalnessRoughnessMap() {
        return metalnessRoughnessMap;
    }

    /**
     * Returns the normal-map locations.
     *
     * @return sampler, enable switch, and transform locations
     */
    public TextureLocations normalMap() {
        return normalMap;
    }

    /**
     * Returns the occlusion-map locations.
     *
     * @return sampler, enable switch, and transform locations
     */
    public TextureLocations occlusionMap() {
        return occlusionMap;
    }

    /**
     * Returns the emissive-map locations.
     *
     * @return sampler, enable switch, and transform locations
     */
    public TextureLocations emissiveMap() {
        return emissiveMap;
    }

    /** Deletes the linked context-local program. */
    @Override
    public void close() {
        glDeleteProgram(id);
    }

    /**
     * Uniform locations shared by one optional two-dimensional texture role.
     *
     * @param sampler sampler uniform location
     * @param enabled texture-role enable-switch uniform location
     * @param transform texture-coordinate transform uniform location
     * @param verticalFlip vertical-orientation switch uniform location
     */
    public record TextureLocations(int sampler, int enabled, int transform, int verticalFlip) {
        /** Resolves one texture role's required uniforms. */
        private static TextureLocations resolve(int program, String label, String name) {
            return new TextureLocations(
                    ProgramSupport.requiredUniform(program, label, name),
                    ProgramSupport.requiredUniform(program, label, "use" + capitalize(name)),
                    ProgramSupport.requiredUniform(program, label, name + "Transform"),
                    ProgramSupport.requiredUniform(program, label, "flip" + capitalize(name) + "Vertically"));
        }

        /** Converts the first ASCII character of a fixed shader identifier to uppercase. */
        private static String capitalize(String name) {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}
