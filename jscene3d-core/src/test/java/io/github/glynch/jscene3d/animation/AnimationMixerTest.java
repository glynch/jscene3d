/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.objects.Object3D;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AnimationMixerTest {
    /** Advances, pauses, resumes, resets, and stops through one stable mixer-owned action. */
    @Test
    void controlsPlaybackExplicitly() {
        Object3D target = new Object3D();
        AnimationClip clip = positionClip(target);
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(clip).setLoopMode(LoopMode.ONCE).play();

        mixer.update(0.25f);
        action.pause();
        mixer.update(0.25f);
        assertThat(target.position().x()).isEqualTo(0.5f);
        assertThat(action.time()).isEqualTo(0.25f);
        assertThat(action.isPaused()).isTrue();

        action.play();
        mixer.update(0.75f);
        assertThat(target.position().x()).isEqualTo(2.0f);
        assertThat(action.isRunning()).isFalse();

        action.reset();
        assertThat(target.position().x()).isZero();
        action.setTimeScale(2.0f).play();
        mixer.update(0.25f);
        assertThat(action.time()).isEqualTo(0.5f);

        mixer.stopAll();
        assertThat(target.position().x()).isZero();
        assertThat(action.isRunning()).isFalse();
    }

    /** Wraps repeated playback and reflects ping-pong playback across successive updates. */
    @Test
    void appliesRepeatAndPingPongLooping() {
        Object3D target = new Object3D();
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(positionClip(target)).play();

        mixer.update(1.25f);
        assertThat(action.time()).isEqualTo(0.25f);
        assertThat(target.position().x()).isEqualTo(0.5f);

        action.stop().setLoopMode(LoopMode.PING_PONG).play();
        mixer.update(1.25f);
        assertThat(action.time()).isEqualTo(0.75f);
        mixer.update(0.25f);
        assertThat(action.time()).isEqualTo(0.5f);
        assertThat(target.position().x()).isEqualTo(1.0f);
    }

    /** Supports reverse repeat playback and reuses one action for each clip identity. */
    @Test
    void supportsReversePlaybackAndStableActions() {
        Object3D target = new Object3D();
        AnimationClip clip = positionClip(target);
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(clip);

        assertThat(mixer.action(clip)).isSameAs(action);
        action.setTime(0.25f).setTimeScale(-1.0f).play();
        mixer.update(0.5f);

        assertThat(action.time()).isEqualTo(0.75f);
        assertThat(target.position().x()).isEqualTo(1.5f);
        assertThat(action.timeScale()).isEqualTo(-1.0f);
        assertThat(action.loopMode()).isEqualTo(LoopMode.REPEAT);
    }

    /** Rejects invalid elapsed times and action configuration. */
    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidMixerAndActionValues() {
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(positionClip(new Object3D()));

        assertThatIllegalArgumentException().isThrownBy(() -> mixer.update(-0.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> mixer.update(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> action.setTime(2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> action.setTimeScale(Float.POSITIVE_INFINITY));
        assertThatNullPointerException().isThrownBy(() -> mixer.action(null));
        assertThatNullPointerException().isThrownBy(() -> action.setLoopMode(null));
    }

    /** Creates a one-second position clip moving two units along X. */
    private static AnimationClip positionClip(Object3D target) {
        AnimationTrack track = Vector3KeyframeTrack.position(
                target,
                new float[] {0.0f, 1.0f},
                new float[] {0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f},
                Interpolation.LINEAR);
        return new AnimationClip("position", List.of(track));
    }
}
