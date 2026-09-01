/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.render.internal.resources.ShadowMapResource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Immutable renderer-internal shadow maps and light mappings for one completed frame. */
public final class ShadowFrame {
    /** Maximum combined number of directional and spot shadow maps sampled by one draw. */
    public static final int MAX_TWO_DIMENSIONAL_SHADOWS = 4;

    /** Maximum number of point-light cube shadow maps sampled by one draw. */
    public static final int MAX_POINT_SHADOWS = 4;

    private final List<TwoDimensionalShadow> twoDimensionalShadows;
    private final List<PointShadow> pointShadows;
    private final int[] directionalIndices;
    private final int[] spotIndices;
    private final int[] pointIndices;
    private final ShadowRenderMetrics metrics;

    /**
     * Retains completed shadow entries and light-order mappings.
     *
     * @param twoDimensionalShadows directional and spot shadow entries
     * @param pointShadows point-light cube shadow entries
     * @param directionalIndices directional-light-order to shadow-slot mapping
     * @param spotIndices spotlight-order to shadow-slot mapping
     * @param pointIndices point-light-order to shadow-slot mapping
     * @param metrics completed depth-pass activity
     */
    public ShadowFrame(
            List<TwoDimensionalShadow> twoDimensionalShadows,
            List<PointShadow> pointShadows,
            int[] directionalIndices,
            int[] spotIndices,
            int[] pointIndices,
            ShadowRenderMetrics metrics) {
        this.twoDimensionalShadows = Collections.unmodifiableList(new ArrayList<>(twoDimensionalShadows));
        this.pointShadows = Collections.unmodifiableList(new ArrayList<>(pointShadows));
        this.directionalIndices = directionalIndices.clone();
        this.spotIndices = spotIndices.clone();
        this.pointIndices = pointIndices.clone();
        this.metrics = metrics;
    }

    /**
     * Returns completed two-dimensional shadow entries.
     *
     * @return immutable directional and spot entry list
     */
    public List<TwoDimensionalShadow> twoDimensionalShadows() {
        return twoDimensionalShadows;
    }

    /**
     * Returns completed point-light cube shadow entries.
     *
     * @return immutable point entry list
     */
    public List<PointShadow> pointShadows() {
        return pointShadows;
    }

    /**
     * Returns a directional light's two-dimensional slot, or {@code -1}.
     *
     * @param lightIndex zero-based directional-light order
     * @return two-dimensional slot or {@code -1}
     */
    public int directionalIndex(int lightIndex) {
        return directionalIndices[lightIndex];
    }

    /**
     * Returns a spotlight's two-dimensional slot, or {@code -1}.
     *
     * @param lightIndex zero-based spotlight order
     * @return two-dimensional slot or {@code -1}
     */
    public int spotIndex(int lightIndex) {
        return spotIndices[lightIndex];
    }

    /**
     * Returns a point light's cube slot, or {@code -1}.
     *
     * @param lightIndex zero-based point-light order
     * @return point shadow slot or {@code -1}
     */
    public int pointIndex(int lightIndex) {
        return pointIndices[lightIndex];
    }

    /**
     * Returns shadow-pass activity accumulated while building this frame.
     *
     * @return immutable shadow activity
     */
    public ShadowRenderMetrics metrics() {
        return metrics;
    }

    /**
     * One directional or spot shadow map with its view-position projection.
     *
     * @param resource context-local depth map
     * @param textureFromView main view position to shadow texture transform
     * @param bias normalized comparison bias
     * @param normalBias scene-unit receiver normal offset
     */
    public record TwoDimensionalShadow(
            ShadowMapResource resource, Matrix4f textureFromView, float bias, float normalBias) {}

    /**
     * One point-light cube shadow map with its world-space sampling data.
     *
     * @param resource context-local depth cube map
     * @param worldPosition world-space point-light position
     * @param farPlane shadow-camera far distance
     * @param bias normalized comparison bias
     * @param normalBias scene-unit receiver normal offset
     */
    public record PointShadow(
            ShadowMapResource resource, Vector3f worldPosition, float farPlane, float bias, float normalBias) {}

    /**
     * Shadow rendering work generated before the main scene pass.
     *
     * @param maps generated per-light maps
     * @param passes generated depth passes
     * @param drawCalls caster draw calls
     * @param triangles submitted caster triangles
     * @param bufferUploads geometry buffer uploads
     * @param uploadedBytes geometry bytes uploaded
     */
    public record ShadowRenderMetrics(
            int maps, int passes, int drawCalls, long triangles, int bufferUploads, long uploadedBytes) {}
}
