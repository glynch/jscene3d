/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.objects.Object3D;

/** Immutable three-component keyframes controlling local position or scale. */
public final class Vector3KeyframeTrack extends AnimationTrack {
    private static final int COMPONENTS = 3;

    /** Retains copied keyframes for one vector-valued transform property. */
    private Vector3KeyframeTrack(
            Object3D target, TransformProperty property, float[] times, float[] values, Interpolation interpolation) {
        super(target, property, times, values, COMPONENTS, interpolation);
    }

    /**
     * Creates local-position keyframes.
     *
     * <p>Ordinary values contain three components per key. Cubic-spline values contain incoming
     * tangent, value, and outgoing tangent vectors for each key, in that order.
     *
     * @param target object whose local position is controlled
     * @param times non-decreasing, non-negative key times in seconds; adjacent duplicates encode
     *     an instantaneous change
     * @param values flat keyframe data
     * @param interpolation interpolation between keys
     * @return immutable position track
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the arrays are empty, malformed, or non-finite
     */
    public static Vector3KeyframeTrack position(
            Object3D target, float[] times, float[] values, Interpolation interpolation) {
        return new Vector3KeyframeTrack(target, TransformProperty.POSITION, times, values, interpolation);
    }

    /**
     * Creates local-scale keyframes.
     *
     * <p>Ordinary values contain three components per key. Cubic-spline values contain incoming
     * tangent, value, and outgoing tangent vectors for each key, in that order.
     *
     * @param target object whose local scale is controlled
     * @param times non-decreasing, non-negative key times in seconds; adjacent duplicates encode
     *     an instantaneous change
     * @param values flat keyframe data
     * @param interpolation interpolation between keys
     * @return immutable scale track
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the arrays are empty, malformed, or non-finite
     */
    public static Vector3KeyframeTrack scale(
            Object3D target, float[] times, float[] values, Interpolation interpolation) {
        return new Vector3KeyframeTrack(target, TransformProperty.SCALE, times, values, interpolation);
    }

    /** Samples one vector value into caller-owned scalar storage. */
    @Override
    void sample(float time, float[] destination) {
        KeyframeSampler.vector(keyframes(), interpolation(), time, destination);
    }
}
