/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.objects.Object3D;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AnimationClipTest {
    private static final float EPSILON = 0.00001f;

    /** Interpolates typed vector and quaternion bindings without exposing mutable properties. */
    @Test
    void interpolatesTypedTransformTracks() {
        Object3D target = new Object3D();
        Vector3KeyframeTrack position = Vector3KeyframeTrack.position(
                target,
                new float[] {0.0f, 2.0f},
                new float[] {0.0f, 0.0f, 0.0f, 4.0f, 2.0f, -2.0f},
                Interpolation.LINEAR);
        QuaternionKeyframeTrack rotation = QuaternionKeyframeTrack.rotation(
                target,
                new float[] {0.0f, 2.0f},
                new float[] {0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f},
                Interpolation.LINEAR);
        AnimationClip clip = new AnimationClip("move and turn", List.of(position, rotation));
        AnimationAction action = new AnimationMixer().action(clip);

        action.setTime(1.0f);

        assertThat(target.position().x()).isEqualTo(2.0f);
        assertThat(target.position().y()).isEqualTo(1.0f);
        assertThat(target.position().z()).isEqualTo(-1.0f);
        assertThat(target.quaternion().y()).isCloseTo((float) Math.sqrt(0.5), within(EPSILON));
        assertThat(target.quaternion().w()).isCloseTo((float) Math.sqrt(0.5), within(EPSILON));
        assertThat(clip.duration()).isEqualTo(2.0f);
        assertThat(clip.tracks()).containsExactly(position, rotation);
    }

    /** Supports held values and glTF-compatible cubic Hermite vector interpolation. */
    @Test
    void supportsStepAndCubicSplineInterpolation() {
        Object3D stepped = new Object3D();
        Object3D cubic = new Object3D();
        Vector3KeyframeTrack stepTrack = Vector3KeyframeTrack.scale(
                stepped,
                new float[] {0.0f, 1.0f},
                new float[] {1.0f, 1.0f, 1.0f, 3.0f, 3.0f, 3.0f},
                Interpolation.STEP);
        Vector3KeyframeTrack cubicTrack = Vector3KeyframeTrack.position(
                cubic,
                new float[] {0.0f, 1.0f},
                new float[] {
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    2.0f, 4.0f, 6.0f,
                    0.0f, 0.0f, 0.0f
                },
                Interpolation.CUBIC_SPLINE);
        AnimationAction action =
                new AnimationMixer().action(new AnimationClip("interpolation", List.of(stepTrack, cubicTrack)));

        action.setTime(0.5f);

        assertThat(stepped.scale().x()).isEqualTo(1.0f);
        assertThat(cubic.position().x()).isEqualTo(1.0f);
        assertThat(cubic.position().y()).isEqualTo(2.0f);
        assertThat(cubic.position().z()).isEqualTo(3.0f);
    }

    /** Preserves both sides of an instantaneous change encoded by duplicate timestamps. */
    @Test
    void preservesDiscontinuitiesAtDuplicateTimestamps() {
        Object3D target = new Object3D();
        Vector3KeyframeTrack track = Vector3KeyframeTrack.position(
                target,
                new float[] {0.0f, 1.0f, 1.0f, 2.0f},
                new float[] {
                    0.0f, 0.0f, 0.0f,
                    10.0f, 0.0f, 0.0f,
                    20.0f, 0.0f, 0.0f,
                    30.0f, 0.0f, 0.0f
                },
                Interpolation.LINEAR);
        AnimationAction action = new AnimationMixer().action(new AnimationClip("jump", List.of(track)));

        action.setTime(0.5f);
        assertThat(target.position().x()).isEqualTo(5.0f);
        action.setTime(1.0f);
        assertThat(target.position().x()).isEqualTo(20.0f);
        action.setTime(1.5f);
        assertThat(target.position().x()).isEqualTo(25.0f);
    }

    /** Copies caller arrays before later evaluation. */
    @Test
    void copiesKeyframeArrays() {
        Object3D target = new Object3D();
        float[] times = {0.0f, 1.0f};
        float[] values = {0.0f, 0.0f, 0.0f, 2.0f, 4.0f, 6.0f};
        Vector3KeyframeTrack track = Vector3KeyframeTrack.position(target, times, values, Interpolation.LINEAR);
        AnimationAction action = new AnimationMixer().action(new AnimationClip("copied", List.of(track)));
        times[1] = 20.0f;
        values[3] = 200.0f;

        action.setTime(0.5f);

        assertThat(target.position().x()).isEqualTo(1.0f);
        assertThat(target.position().y()).isEqualTo(2.0f);
        assertThat(target.position().z()).isEqualTo(3.0f);
    }

    /** Rejects malformed keyframe domains and clip definitions at construction. */
    @Test
    void rejectsInvalidTrackAndClipData() {
        Object3D target = new Object3D();
        float[] validTimes = {0.0f, 1.0f};
        float[] validValues = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Vector3KeyframeTrack.position(
                        target, new float[] {1.0f, 0.0f}, validValues, Interpolation.LINEAR))
                .withMessageContaining("must not decrease");
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        Vector3KeyframeTrack.position(target, validTimes, new float[] {1.0f}, Interpolation.LINEAR))
                .withMessageContaining("values length");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Vector3KeyframeTrack.position(
                        target, new float[] {Float.NaN}, new float[] {0.0f, 0.0f, 0.0f}, Interpolation.STEP));
        assertThatIllegalArgumentException().isThrownBy(() -> new AnimationClip(" ", List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new AnimationClip("empty", List.of()));
    }

    /** Rejects two tracks that ambiguously control the same property on one target. */
    @Test
    void rejectsDuplicateBindingsWithinOneClip() {
        Object3D target = new Object3D();
        AnimationTrack first = Vector3KeyframeTrack.position(
                target, new float[] {0.0f}, new float[] {0.0f, 0.0f, 0.0f}, Interpolation.STEP);
        AnimationTrack second = Vector3KeyframeTrack.position(
                target, new float[] {0.0f}, new float[] {1.0f, 0.0f, 0.0f}, Interpolation.STEP);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AnimationClip("duplicate", List.of(first, second)))
                .withMessageContaining("duplicate target property bindings");
    }

    /** Rejects nulls at each supported construction seam. */
    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullTrackAndClipData() {
        Object3D target = new Object3D();
        float[] times = {0.0f};
        float[] values = {0.0f, 0.0f, 0.0f};

        assertThatNullPointerException()
                .isThrownBy(() -> Vector3KeyframeTrack.position(null, times, values, Interpolation.STEP));
        assertThatNullPointerException()
                .isThrownBy(() -> Vector3KeyframeTrack.position(target, null, values, Interpolation.STEP));
        assertThatNullPointerException()
                .isThrownBy(() -> Vector3KeyframeTrack.position(target, times, null, Interpolation.STEP));
        assertThatNullPointerException().isThrownBy(() -> Vector3KeyframeTrack.position(target, times, values, null));
        assertThatNullPointerException().isThrownBy(() -> new AnimationClip(null, List.of()));
        assertThatNullPointerException().isThrownBy(() -> new AnimationClip("clip", null));
    }
}
