/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.objects.Mesh;
import java.util.Objects;

/** Immutable vector keyframes controlling all morph-target influences of one mesh. */
public final class MorphTargetKeyframeTrack extends AnimationTrack {
    private final Mesh mesh;

    /** Retains copied keyframes using the target's current morph-target count. */
    private MorphTargetKeyframeTrack(Mesh target, float[] times, float[] values, Interpolation interpolation) {
        super(
                Objects.requireNonNull(target, "target"),
                MorphProperty.INFLUENCES,
                times,
                values,
                requireMorphTargets(target),
                interpolation);
        mesh = target;
    }

    /**
     * Creates keyframes for the target's complete geometry-ordered influence vector.
     *
     * <p>Ordinary values contain {@link Mesh#morphTargetCount()} components per key. Cubic-spline
     * values contain incoming tangent, value, and outgoing tangent vectors per key, in that order.
     *
     * @param target mesh whose influences are controlled
     * @param times non-decreasing, non-negative key times in seconds
     * @param values flat influence keyframe data
     * @param interpolation interpolation between keys
     * @return immutable morph-target track
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the mesh has no targets or arrays are malformed
     */
    public static MorphTargetKeyframeTrack influences(
            Mesh target, float[] times, float[] values, Interpolation interpolation) {
        return new MorphTargetKeyframeTrack(target, times, values, interpolation);
    }

    /**
     * Returns the controlled mesh.
     *
     * @return retained target mesh
     */
    public Mesh mesh() {
        return mesh;
    }

    /** Samples one complete influence vector into caller-owned scalar storage. */
    @Override
    void sample(float time, float[] destination) {
        KeyframeSampler.vector(keyframes(), interpolation(), time, destination);
    }

    /** Requires a positive stable target count at track construction. */
    private static int requireMorphTargets(Mesh target) {
        int count = target.morphTargetCount();
        if (count == 0) {
            throw new IllegalArgumentException("target geometry must contain at least one morph target");
        }
        return count;
    }
}
