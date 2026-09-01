/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glUniform1fv;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.HemisphereLight;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.lights.SpotLight;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.internal.LightCollection;
import io.github.glynch.jscene3d.render.internal.ShadowFrame;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Shared transform and light-uniform staging for built-in lit mesh programs. */
final class LitProgramState {
    private final ShadowProgramState shadowState;
    private final InstancingProgramState instancingState;
    private final MorphProgramState morphState;
    private final int modelMatrixLocation;
    private final int viewMatrixLocation;
    private final int projectionMatrixLocation;
    private final int normalMatrixLocation;
    private final int ambientLightColorLocation;
    private final int pointLightCountLocation;
    private final int pointLightPositionsLocation;
    private final int pointLightColorsLocation;
    private final int pointLightDistancesLocation;
    private final int pointLightDecaysLocation;
    private final int directionalLightCountLocation;
    private final int directionalLightDirectionsLocation;
    private final int directionalLightColorsLocation;
    private final int spotLightCountLocation;
    private final int spotLightPositionsLocation;
    private final int spotLightDirectionsLocation;
    private final int spotLightColorsLocation;
    private final int spotLightDistancesLocation;
    private final int spotLightDecaysLocation;
    private final int spotLightConeCosinesLocation;
    private final int spotLightPenumbraCosinesLocation;
    private final int hemisphereLightCountLocation;
    private final int hemisphereLightDirectionsLocation;
    private final int hemisphereLightSkyColorsLocation;
    private final int hemisphereLightGroundColorsLocation;
    private final Matrix4f modelViewMatrix;
    private final Matrix3f normalMatrix;
    private final Vector3f viewPosition;
    private final Vector3f directionalTarget;
    private final Vector3f directionalWorldPosition;
    private final Vector3f directionalViewDirection;
    private final Vector3f spotTarget;
    private final Vector3f spotWorldPosition;
    private final Vector3f spotViewPosition;
    private final Vector3f spotViewDirection;
    private final Vector3f hemisphereWorldPosition;
    private final Vector3f hemisphereViewDirection;
    private final Vector3f worldOrigin;
    private final float[] matrix4Values;
    private final float[] matrix3Values;
    private final float[] pointLightPositions;
    private final float[] pointLightColors;
    private final float[] pointLightDistances;
    private final float[] pointLightDecays;
    private final float[] directionalLightDirections;
    private final float[] directionalLightColors;
    private final float[] spotLightPositions;
    private final float[] spotLightDirections;
    private final float[] spotLightColors;
    private final float[] spotLightDistances;
    private final float[] spotLightDecays;
    private final float[] spotLightConeCosines;
    private final float[] spotLightPenumbraCosines;
    private final float[] hemisphereLightDirections;
    private final float[] hemisphereLightSkyColors;
    private final float[] hemisphereLightGroundColors;

    /** Resolves common uniform locations and allocates reusable upload staging. */
    LitProgramState(int program, String label) {
        shadowState = new ShadowProgramState(program, label);
        instancingState = new InstancingProgramState(program, label);
        morphState = new MorphProgramState(program, label);
        modelMatrixLocation = ProgramSupport.requiredUniform(program, label, "modelMatrix");
        viewMatrixLocation = ProgramSupport.requiredUniform(program, label, "viewMatrix");
        projectionMatrixLocation = ProgramSupport.requiredUniform(program, label, "projectionMatrix");
        normalMatrixLocation = ProgramSupport.requiredUniform(program, label, "normalMatrix");
        ambientLightColorLocation = ProgramSupport.requiredUniform(program, label, "ambientLightColor");
        pointLightCountLocation = ProgramSupport.requiredUniform(program, label, "pointLightCount");
        pointLightPositionsLocation = ProgramSupport.requiredUniform(program, label, "pointLightPositions[0]");
        pointLightColorsLocation = ProgramSupport.requiredUniform(program, label, "pointLightColors[0]");
        pointLightDistancesLocation = ProgramSupport.requiredUniform(program, label, "pointLightDistances[0]");
        pointLightDecaysLocation = ProgramSupport.requiredUniform(program, label, "pointLightDecays[0]");
        directionalLightCountLocation = ProgramSupport.requiredUniform(program, label, "directionalLightCount");
        directionalLightDirectionsLocation =
                ProgramSupport.requiredUniform(program, label, "directionalLightDirections[0]");
        directionalLightColorsLocation = ProgramSupport.requiredUniform(program, label, "directionalLightColors[0]");
        spotLightCountLocation = ProgramSupport.requiredUniform(program, label, "spotLightCount");
        spotLightPositionsLocation = ProgramSupport.requiredUniform(program, label, "spotLightPositions[0]");
        spotLightDirectionsLocation = ProgramSupport.requiredUniform(program, label, "spotLightDirections[0]");
        spotLightColorsLocation = ProgramSupport.requiredUniform(program, label, "spotLightColors[0]");
        spotLightDistancesLocation = ProgramSupport.requiredUniform(program, label, "spotLightDistances[0]");
        spotLightDecaysLocation = ProgramSupport.requiredUniform(program, label, "spotLightDecays[0]");
        spotLightConeCosinesLocation = ProgramSupport.requiredUniform(program, label, "spotLightConeCosines[0]");
        spotLightPenumbraCosinesLocation =
                ProgramSupport.requiredUniform(program, label, "spotLightPenumbraCosines[0]");
        hemisphereLightCountLocation = ProgramSupport.requiredUniform(program, label, "hemisphereLightCount");
        hemisphereLightDirectionsLocation =
                ProgramSupport.requiredUniform(program, label, "hemisphereLightDirections[0]");
        hemisphereLightSkyColorsLocation =
                ProgramSupport.requiredUniform(program, label, "hemisphereLightSkyColors[0]");
        hemisphereLightGroundColorsLocation =
                ProgramSupport.requiredUniform(program, label, "hemisphereLightGroundColors[0]");
        modelViewMatrix = new Matrix4f();
        normalMatrix = new Matrix3f();
        viewPosition = new Vector3f();
        directionalTarget = new Vector3f();
        directionalWorldPosition = new Vector3f();
        directionalViewDirection = new Vector3f();
        spotTarget = new Vector3f();
        spotWorldPosition = new Vector3f();
        spotViewPosition = new Vector3f();
        spotViewDirection = new Vector3f();
        hemisphereWorldPosition = new Vector3f();
        hemisphereViewDirection = new Vector3f();
        worldOrigin = new Vector3f();
        matrix4Values = new float[16];
        matrix3Values = new float[9];
        pointLightPositions = new float[Renderer.MAX_POINT_LIGHTS * 3];
        pointLightColors = new float[Renderer.MAX_POINT_LIGHTS * 3];
        pointLightDistances = new float[Renderer.MAX_POINT_LIGHTS];
        pointLightDecays = new float[Renderer.MAX_POINT_LIGHTS];
        directionalLightDirections = new float[Renderer.MAX_DIRECTIONAL_LIGHTS * 3];
        directionalLightColors = new float[Renderer.MAX_DIRECTIONAL_LIGHTS * 3];
        spotLightPositions = new float[Renderer.MAX_SPOT_LIGHTS * 3];
        spotLightDirections = new float[Renderer.MAX_SPOT_LIGHTS * 3];
        spotLightColors = new float[Renderer.MAX_SPOT_LIGHTS * 3];
        spotLightDistances = new float[Renderer.MAX_SPOT_LIGHTS];
        spotLightDecays = new float[Renderer.MAX_SPOT_LIGHTS];
        spotLightConeCosines = new float[Renderer.MAX_SPOT_LIGHTS];
        spotLightPenumbraCosines = new float[Renderer.MAX_SPOT_LIGHTS];
        hemisphereLightDirections = new float[Renderer.MAX_HEMISPHERE_LIGHTS * 3];
        hemisphereLightSkyColors = new float[Renderer.MAX_HEMISPHERE_LIGHTS * 3];
        hemisphereLightGroundColors = new float[Renderer.MAX_HEMISPHERE_LIGHTS * 3];
    }

    /** Uploads object, camera, and inverse-transpose normal transforms without allocating. */
    void uploadTransforms(Matrix4fc modelMatrix, Matrix4fc viewMatrix, Matrix4fc projectionMatrix) {
        uploadMatrix4(modelMatrixLocation, modelMatrix);
        uploadMatrix4(viewMatrixLocation, viewMatrix);
        uploadMatrix4(projectionMatrixLocation, projectionMatrix);
        modelViewMatrix.set(viewMatrix).mul(modelMatrix);
        normalMatrix.set(modelViewMatrix).normal().get(matrix3Values);
        glUniformMatrix3fv(normalMatrixLocation, false, matrix3Values);
    }

    /** Uploads optional batch-transform and color switches. */
    void uploadInstancing(boolean instanced, boolean colors) {
        instancingState.upload(instanced, colors);
    }

    /** Uploads optional morph-target deformation state. */
    void uploadMorphing(boolean enabled, int targetCount, int vertexCount, boolean instanceWeights) {
        morphState.upload(enabled, targetCount, vertexCount, instanceWeights);
    }

    /** Uploads combined ambient and ordered point- and directional-light state without allocating. */
    void uploadLights(LightCollection lights, Matrix4fc viewMatrix) {
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
        uploadDirectionalLights(lights, viewMatrix);
        uploadSpotLights(lights, viewMatrix);
        uploadHemisphereLights(lights, viewMatrix);
    }

    /** Uploads receiver-specific shadow state and the completed frame's maps. */
    void uploadShadows(boolean receiveShadow, ShadowFrame frame, LightCollection lights, Matrix4fc viewMatrix) {
        shadowState.upload(receiveShadow, frame, lights, viewMatrix);
    }

    /** Uploads ordered directional-light state without allocating. */
    private void uploadDirectionalLights(LightCollection lights, Matrix4fc viewMatrix) {
        int count = lights.directionalLightCount();
        glUniform1i(directionalLightCountLocation, count);
        for (int index = 0; index < count; index++) {
            DirectionalLight light = lights.directionalLight(index);
            light.worldPosition(directionalWorldPosition);
            light.target(directionalTarget);
            LightDirectionSupport.setNormalizedDifference(
                    directionalTarget,
                    directionalWorldPosition,
                    directionalViewDirection,
                    "DirectionalLight position must differ from its target: " + light);
            viewMatrix.transformDirection(directionalViewDirection).normalize();
            int componentIndex = index * 3;
            directionalLightDirections[componentIndex] = directionalViewDirection.x();
            directionalLightDirections[componentIndex + 1] = directionalViewDirection.y();
            directionalLightDirections[componentIndex + 2] = directionalViewDirection.z();
            directionalLightColors[componentIndex] = light.color().red() * light.intensity();
            directionalLightColors[componentIndex + 1] = light.color().green() * light.intensity();
            directionalLightColors[componentIndex + 2] = light.color().blue() * light.intensity();
        }
        glUniform3fv(directionalLightDirectionsLocation, directionalLightDirections);
        glUniform3fv(directionalLightColorsLocation, directionalLightColors);
    }

    /** Uploads ordered spotlight state without allocating. */
    private void uploadSpotLights(LightCollection lights, Matrix4fc viewMatrix) {
        int count = lights.spotLightCount();
        glUniform1i(spotLightCountLocation, count);
        for (int index = 0; index < count; index++) {
            SpotLight light = lights.spotLight(index);
            light.worldPosition(spotWorldPosition);
            light.target(spotTarget);
            LightDirectionSupport.setNormalizedDifference(
                    spotWorldPosition,
                    spotTarget,
                    spotViewDirection,
                    "SpotLight position must differ from its target: " + light);
            viewMatrix.transformDirection(spotViewDirection).normalize();
            spotViewPosition.set(spotWorldPosition);
            viewMatrix.transformPosition(spotViewPosition);
            int componentIndex = index * 3;
            spotLightPositions[componentIndex] = spotViewPosition.x();
            spotLightPositions[componentIndex + 1] = spotViewPosition.y();
            spotLightPositions[componentIndex + 2] = spotViewPosition.z();
            spotLightDirections[componentIndex] = spotViewDirection.x();
            spotLightDirections[componentIndex + 1] = spotViewDirection.y();
            spotLightDirections[componentIndex + 2] = spotViewDirection.z();
            spotLightColors[componentIndex] = light.color().red() * light.intensity();
            spotLightColors[componentIndex + 1] = light.color().green() * light.intensity();
            spotLightColors[componentIndex + 2] = light.color().blue() * light.intensity();
            spotLightDistances[index] = light.distance();
            spotLightDecays[index] = light.decay();
            spotLightConeCosines[index] = (float) Math.cos(light.angle());
            spotLightPenumbraCosines[index] = (float) Math.cos(light.angle() * (1.0f - light.penumbra()));
        }
        glUniform3fv(spotLightPositionsLocation, spotLightPositions);
        glUniform3fv(spotLightDirectionsLocation, spotLightDirections);
        glUniform3fv(spotLightColorsLocation, spotLightColors);
        glUniform1fv(spotLightDistancesLocation, spotLightDistances);
        glUniform1fv(spotLightDecaysLocation, spotLightDecays);
        glUniform1fv(spotLightConeCosinesLocation, spotLightConeCosines);
        glUniform1fv(spotLightPenumbraCosinesLocation, spotLightPenumbraCosines);
    }

    /** Uploads ordered hemisphere-light state without allocating. */
    private void uploadHemisphereLights(LightCollection lights, Matrix4fc viewMatrix) {
        int count = lights.hemisphereLightCount();
        glUniform1i(hemisphereLightCountLocation, count);
        for (int index = 0; index < count; index++) {
            HemisphereLight light = lights.hemisphereLight(index);
            light.worldPosition(hemisphereWorldPosition);
            LightDirectionSupport.setNormalizedDifference(
                    worldOrigin,
                    hemisphereWorldPosition,
                    hemisphereViewDirection,
                    "HemisphereLight world position must not be zero: " + light);
            viewMatrix.transformDirection(hemisphereViewDirection).normalize();
            int componentIndex = index * 3;
            hemisphereLightDirections[componentIndex] = hemisphereViewDirection.x();
            hemisphereLightDirections[componentIndex + 1] = hemisphereViewDirection.y();
            hemisphereLightDirections[componentIndex + 2] = hemisphereViewDirection.z();
            hemisphereLightSkyColors[componentIndex] = light.color().red() * light.intensity();
            hemisphereLightSkyColors[componentIndex + 1] = light.color().green() * light.intensity();
            hemisphereLightSkyColors[componentIndex + 2] = light.color().blue() * light.intensity();
            hemisphereLightGroundColors[componentIndex] = light.groundColor().red() * light.intensity();
            hemisphereLightGroundColors[componentIndex + 1] =
                    light.groundColor().green() * light.intensity();
            hemisphereLightGroundColors[componentIndex + 2] =
                    light.groundColor().blue() * light.intensity();
        }
        glUniform3fv(hemisphereLightDirectionsLocation, hemisphereLightDirections);
        glUniform3fv(hemisphereLightSkyColorsLocation, hemisphereLightSkyColors);
        glUniform3fv(hemisphereLightGroundColorsLocation, hemisphereLightGroundColors);
    }

    /** Copies and uploads one four-by-four matrix. */
    private void uploadMatrix4(int location, Matrix4fc matrix) {
        matrix.get(matrix4Values);
        glUniformMatrix4fv(location, false, matrix4Values);
    }
}
