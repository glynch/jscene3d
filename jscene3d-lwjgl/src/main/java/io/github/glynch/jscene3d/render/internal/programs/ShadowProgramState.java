/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import static org.lwjgl.opengl.GL20.glUniform1fv;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform1iv;
import static org.lwjgl.opengl.GL20.glUniform3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.render.internal.LightCollection;
import io.github.glynch.jscene3d.render.internal.ShadowFrame;
import io.github.glynch.jscene3d.render.internal.resources.DefaultShadowMaps;
import java.util.Arrays;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Shared built-in shadow-uniform locations and reusable upload staging. */
final class ShadowProgramState {
    private final int receiveShadowLocation;
    private final int directionalIndicesLocation;
    private final int spotIndicesLocation;
    private final int pointIndicesLocation;
    private final int shadowMatricesLocation;
    private final int shadowBiasesLocation;
    private final int shadowNormalBiasesLocation;
    private final int shadowMapsLocation;
    private final int pointPositionsLocation;
    private final int pointFarPlanesLocation;
    private final int pointBiasesLocation;
    private final int pointNormalBiasesLocation;
    private final int pointMapsLocation;
    private final int viewToWorldLocation;
    private final int[] directionalIndices;
    private final int[] spotIndices;
    private final int[] pointIndices;
    private final int[] twoDimensionalTextureUnits;
    private final int[] pointTextureUnits;
    private final float[] matrices;
    private final float[] biases;
    private final float[] normalBiases;
    private final float[] pointPositions;
    private final float[] pointFarPlanes;
    private final float[] pointBiases;
    private final float[] pointNormalBiases;
    private final float[] matrix3Values;
    private final Matrix3f viewToWorld;
    private final Vector3f transformedPoint;

    /** Resolves all shadow uniforms for one built-in lit program. */
    ShadowProgramState(int program, String label) {
        receiveShadowLocation = ProgramSupport.requiredUniform(program, label, "receiveShadow");
        directionalIndicesLocation = ProgramSupport.requiredUniform(program, label, "directionalShadowIndices[0]");
        spotIndicesLocation = ProgramSupport.requiredUniform(program, label, "spotShadowIndices[0]");
        pointIndicesLocation = ProgramSupport.requiredUniform(program, label, "pointShadowIndices[0]");
        shadowMatricesLocation = ProgramSupport.requiredUniform(program, label, "shadowMatrices[0]");
        shadowBiasesLocation = ProgramSupport.requiredUniform(program, label, "shadowBiases[0]");
        shadowNormalBiasesLocation = ProgramSupport.requiredUniform(program, label, "shadowNormalBiases[0]");
        shadowMapsLocation = ProgramSupport.requiredUniform(program, label, "shadowMaps[0]");
        pointPositionsLocation = ProgramSupport.requiredUniform(program, label, "pointShadowPositions[0]");
        pointFarPlanesLocation = ProgramSupport.requiredUniform(program, label, "pointShadowFarPlanes[0]");
        pointBiasesLocation = ProgramSupport.requiredUniform(program, label, "pointShadowBiases[0]");
        pointNormalBiasesLocation = ProgramSupport.requiredUniform(program, label, "pointShadowNormalBiases[0]");
        pointMapsLocation = ProgramSupport.requiredUniform(program, label, "pointShadowMaps[0]");
        viewToWorldLocation = ProgramSupport.requiredUniform(program, label, "shadowViewToWorldMatrix");
        directionalIndices = new int[Renderer.MAX_DIRECTIONAL_LIGHTS];
        spotIndices = new int[Renderer.MAX_SPOT_LIGHTS];
        pointIndices = new int[Renderer.MAX_POINT_LIGHTS];
        twoDimensionalTextureUnits =
                sequence(DefaultShadowMaps.TWO_DIMENSIONAL_TEXTURE_UNIT, ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS);
        pointTextureUnits = sequence(DefaultShadowMaps.POINT_TEXTURE_UNIT, ShadowFrame.MAX_POINT_SHADOWS);
        matrices = new float[ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS * 16];
        biases = new float[ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS];
        normalBiases = new float[ShadowFrame.MAX_TWO_DIMENSIONAL_SHADOWS];
        pointPositions = new float[ShadowFrame.MAX_POINT_SHADOWS * 3];
        pointFarPlanes = new float[ShadowFrame.MAX_POINT_SHADOWS];
        pointBiases = new float[ShadowFrame.MAX_POINT_SHADOWS];
        pointNormalBiases = new float[ShadowFrame.MAX_POINT_SHADOWS];
        matrix3Values = new float[9];
        viewToWorld = new Matrix3f();
        transformedPoint = new Vector3f();
    }

    /** Uploads one mesh's receiver switch and all completed frame maps. */
    void upload(boolean receiveShadow, ShadowFrame frame, LightCollection lights, Matrix4fc viewMatrix) {
        glUniform1i(receiveShadowLocation, receiveShadow ? 1 : 0);
        Arrays.fill(directionalIndices, -1);
        for (int index = 0; index < lights.directionalLightCount(); index++) {
            directionalIndices[index] = frame.directionalIndex(index);
        }
        Arrays.fill(spotIndices, -1);
        for (int index = 0; index < lights.spotLightCount(); index++) {
            spotIndices[index] = frame.spotIndex(index);
        }
        Arrays.fill(pointIndices, -1);
        for (int index = 0; index < lights.pointLightCount(); index++) {
            pointIndices[index] = frame.pointIndex(index);
        }
        glUniform1iv(directionalIndicesLocation, directionalIndices);
        glUniform1iv(spotIndicesLocation, spotIndices);
        glUniform1iv(pointIndicesLocation, pointIndices);
        uploadTwoDimensional(frame.twoDimensionalShadows());
        uploadPoints(frame.pointShadows(), viewMatrix);
        glUniform1iv(shadowMapsLocation, twoDimensionalTextureUnits);
        glUniform1iv(pointMapsLocation, pointTextureUnits);
        viewToWorld.set(viewMatrix).invert().get(matrix3Values);
        glUniformMatrix3fv(viewToWorldLocation, false, matrix3Values);
    }

    /** Uploads filled two-dimensional entries and harmless defaults for unused slots. */
    private void uploadTwoDimensional(List<ShadowFrame.TwoDimensionalShadow> entries) {
        Arrays.fill(matrices, 0.0f);
        Arrays.fill(biases, 0.0f);
        Arrays.fill(normalBiases, 0.0f);
        for (int index = 0; index < entries.size(); index++) {
            ShadowFrame.TwoDimensionalShadow entry = entries.get(index);
            entry.textureFromView().get(matrices, index * 16);
            biases[index] = entry.bias();
            normalBiases[index] = entry.normalBias();
        }
        glUniformMatrix4fv(shadowMatricesLocation, false, matrices);
        glUniform1fv(shadowBiasesLocation, biases);
        glUniform1fv(shadowNormalBiasesLocation, normalBiases);
    }

    /** Uploads filled point entries in view space and harmless defaults for unused slots. */
    private void uploadPoints(List<ShadowFrame.PointShadow> entries, Matrix4fc viewMatrix) {
        Arrays.fill(pointPositions, 0.0f);
        Arrays.fill(pointFarPlanes, 1.0f);
        Arrays.fill(pointBiases, 0.0f);
        Arrays.fill(pointNormalBiases, 0.0f);
        for (int index = 0; index < entries.size(); index++) {
            ShadowFrame.PointShadow entry = entries.get(index);
            viewMatrix.transformPosition(entry.worldPosition(), transformedPoint);
            int component = index * 3;
            pointPositions[component] = transformedPoint.x();
            pointPositions[component + 1] = transformedPoint.y();
            pointPositions[component + 2] = transformedPoint.z();
            pointFarPlanes[index] = entry.farPlane();
            pointBiases[index] = entry.bias();
            pointNormalBiases[index] = entry.normalBias();
        }
        glUniform3fv(pointPositionsLocation, pointPositions);
        glUniform1fv(pointFarPlanesLocation, pointFarPlanes);
        glUniform1fv(pointBiasesLocation, pointBiases);
        glUniform1fv(pointNormalBiasesLocation, pointNormalBiases);
    }

    /** Builds consecutive fixed sampler units. */
    private static int[] sequence(int first, int count) {
        int[] values = new int[count];
        for (int index = 0; index < count; index++) {
            values[index] = first + index;
        }
        return values;
    }
}
