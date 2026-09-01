/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.objects.Object3D;

/** Immutable four-component keyframes controlling normalized local orientation. */
public final class QuaternionKeyframeTrack extends AnimationTrack {
    private static final int COMPONENTS = 4;

    private final float[] sample = new float[COMPONENTS];

    /** Retains copied keyframes for one local-orientation binding. */
    private QuaternionKeyframeTrack(Object3D target, float[] times, float[] values, Interpolation interpolation) {
        super(target, TransformProperty.ROTATION, times, values, COMPONENTS, interpolation);
    }

    /**
     * Creates local-orientation keyframes.
     *
     * <p>Linear interpolation uses normalized shortest-path spherical interpolation. Cubic-spline
     * values contain incoming tangent, quaternion value, and outgoing tangent groups per key; each
     * sampled result is normalized before it is applied.
     *
     * @param target object whose local orientation is controlled
     * @param times non-decreasing, non-negative key times in seconds; adjacent duplicates encode
     *     an instantaneous change
     * @param values flat quaternion keyframe data in XYZW order
     * @param interpolation interpolation between keys
     * @return immutable rotation track
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the arrays are empty, malformed, non-finite, or sample a
     *     zero quaternion
     */
    public static QuaternionKeyframeTrack rotation(
            Object3D target, float[] times, float[] values, Interpolation interpolation) {
        return new QuaternionKeyframeTrack(target, times, values, interpolation);
    }

    /** Samples, normalizes, and applies one quaternion value. */
    @Override
    void apply(float time) {
        KeyframeSampler.quaternion(keyframes(), interpolation(), time, sample);
        boundTarget().setQuaternion(sample[0], sample[1], sample[2], sample[3]);
    }
}
