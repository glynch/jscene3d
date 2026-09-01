/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Objects;

/**
 * Immutable keyframe data bound to one controlled local transform property.
 *
 * <p>Tracks copy all input arrays. A track retains its target scene object and is therefore tied to
 * that scene instance. Track evaluation is performed by {@link AnimationMixer}; callers do not
 * apply sampled values directly.
 */
public abstract sealed class AnimationTrack permits QuaternionKeyframeTrack, Vector3KeyframeTrack {
    private final Object3D target;
    private final TransformProperty property;
    private final Interpolation interpolation;
    private final KeyframeData keyframes;

    /** Retains the validated typed binding and copied keyframe data. */
    AnimationTrack(
            Object3D target,
            TransformProperty property,
            float[] times,
            float[] values,
            int components,
            Interpolation interpolation) {
        this.target = Objects.requireNonNull(target, "target");
        this.property = Objects.requireNonNull(property, "property");
        this.interpolation = Objects.requireNonNull(interpolation, "interpolation");
        keyframes = new KeyframeData(times, values, components, interpolation);
    }

    /**
     * Returns the scene object mutated when this track is evaluated.
     *
     * @return retained target object
     */
    public final Object3D target() {
        return target;
    }

    /**
     * Returns the controlled local transform property.
     *
     * @return transform property
     */
    public final TransformProperty property() {
        return property;
    }

    /**
     * Returns the interpolation applied between keys.
     *
     * @return interpolation mode
     */
    public final Interpolation interpolation() {
        return interpolation;
    }

    /**
     * Returns the final keyframe time in seconds.
     *
     * @return non-negative track duration
     */
    public final float duration() {
        return keyframes.duration();
    }

    /** Returns the retained target to package-local evaluators. */
    final Object3D boundTarget() {
        return target;
    }

    /** Returns copied keyframes to the owning concrete evaluator. */
    final KeyframeData keyframes() {
        return keyframes;
    }

    /** Samples and applies this track at one local clip time. */
    abstract void apply(float time);
}
