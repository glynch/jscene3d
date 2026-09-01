/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.objects.Object3D;
import java.util.List;
import org.junit.jupiter.api.Test;

final class AnimationMixerTest {
    private static final float EPSILON = 0.00001f;

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

    /** Completes a partial action blend with the base pose captured before playback. */
    @Test
    void blendsPartialWeightsWithTheBasePose() {
        Object3D target = new Object3D();
        target.setPosition(2.0f, 0.0f, 0.0f);
        AnimationMixer mixer = new AnimationMixer();
        mixer.action(constantPositionClip("first", target, 10.0f))
                .setWeight(0.25f)
                .play();
        mixer.action(constantPositionClip("second", target, 6.0f))
                .setWeight(0.25f)
                .play();

        assertThat(target.position().x()).isEqualTo(5.0f);
    }

    /** Normalizes concurrent action weights above one independently of registration order. */
    @Test
    void normalizesOverweightActionBlends() {
        Object3D target = new Object3D();
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction first =
                mixer.action(constantPositionClip("first", target, 10.0f)).play();
        AnimationAction second =
                mixer.action(constantPositionClip("second", target, 6.0f)).play();

        assertThat(target.position().x()).isEqualTo(8.0f);

        first.setWeight(0.25f);
        second.setWeight(0.75f);
        assertThat(target.position().x()).isEqualTo(7.0f);
    }

    /** Aligns equivalent quaternion signs before accumulation so they cannot cancel. */
    @Test
    void alignsEquivalentQuaternionSigns() {
        Object3D target = new Object3D();
        AnimationMixer mixer = new AnimationMixer();
        mixer.action(constantRotationClip("positive", target, 1.0f))
                .setWeight(0.5f)
                .play();
        mixer.action(constantRotationClip("negative", target, -1.0f))
                .setWeight(0.5f)
                .play();

        assertThat(Math.abs(target.quaternion().y())).isCloseTo(1.0f, within(EPSILON));
        assertThat(target.quaternion().w()).isCloseTo(0.0f, within(EPSILON));
    }

    /** Retains a paused action's pose and influence while another action continues advancing. */
    @Test
    void retainsPausedActionContribution() {
        Object3D target = new Object3D();
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction moving =
                mixer.action(positionClip(target)).setWeight(0.5f).play();
        mixer.action(constantPositionClip("held", target, 2.0f))
                .setWeight(0.5f)
                .play()
                .pause();

        mixer.update(0.25f);
        assertThat(target.position().x()).isEqualTo(1.25f);
        mixer.update(0.25f);
        assertThat(target.position().x()).isEqualTo(1.5f);
        assertThat(moving.time()).isEqualTo(0.5f);
    }

    /** Cross-fades linearly, then leaves only the destination running and contributing. */
    @Test
    void crossFadesBetweenActions() {
        Object3D target = new Object3D();
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction source =
                mixer.action(constantPositionClip("source", target, 0.0f)).play();
        AnimationAction destination = mixer.action(constantPositionClip("destination", target, 10.0f));

        mixer.crossFade(source, destination, 1.0f);
        mixer.update(0.25f);

        assertThat(target.position().x()).isEqualTo(2.5f);
        assertThat(source.effectiveWeight()).isEqualTo(0.75f);
        assertThat(destination.effectiveWeight()).isEqualTo(0.25f);

        mixer.update(0.75f);
        assertThat(target.position().x()).isEqualTo(10.0f);
        assertThat(source.effectiveWeight()).isZero();
        assertThat(source.isRunning()).isFalse();
        assertThat(destination.effectiveWeight()).isEqualTo(1.0f);
        assertThat(destination.isRunning()).isTrue();
    }

    /** Applies direct fades and restores the captured base pose after a completed fade-out. */
    @Test
    void fadesActionsAndRestoresTheBasePose() {
        Object3D target = new Object3D();
        target.setPosition(2.0f, 0.0f, 0.0f);
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction action = mixer.action(constantPositionClip("fade", target, 10.0f))
                .fadeIn(1.0f)
                .play();

        mixer.update(0.25f);
        assertThat(target.position().x()).isEqualTo(4.0f);
        action.fadeOut(0.5f);
        mixer.update(0.5f);

        assertThat(target.position().x()).isEqualTo(2.0f);
        assertThat(action.effectiveWeight()).isZero();
        assertThat(action.isRunning()).isFalse();
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
        assertThatIllegalArgumentException().isThrownBy(() -> action.setWeight(-0.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> action.setWeight(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> action.fadeIn(-0.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> action.fadeOut(Float.NaN));
        assertThatNullPointerException().isThrownBy(() -> mixer.action(null));
        assertThatNullPointerException().isThrownBy(() -> action.setLoopMode(null));
    }

    /** Rejects cross-fades that cannot be resolved by one mixer. */
    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidCrossFades() {
        Object3D target = new Object3D();
        AnimationMixer mixer = new AnimationMixer();
        AnimationAction source = mixer.action(constantPositionClip("source", target, 0.0f));
        AnimationAction destination = mixer.action(constantPositionClip("destination", target, 1.0f));
        AnimationAction foreign = new AnimationMixer().action(constantPositionClip("foreign", target, 2.0f));

        assertThatIllegalStateException().isThrownBy(() -> mixer.crossFade(source, destination, 1.0f));
        source.play();
        assertThatIllegalArgumentException().isThrownBy(() -> mixer.crossFade(source, source, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> mixer.crossFade(source, foreign, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> mixer.crossFade(source, destination, -1.0f));
        assertThatNullPointerException().isThrownBy(() -> mixer.crossFade(null, destination, 1.0f));
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

    /** Creates a one-second clip holding one X position. */
    private static AnimationClip constantPositionClip(String name, Object3D target, float positionX) {
        AnimationTrack track = Vector3KeyframeTrack.position(
                target,
                new float[] {0.0f, 1.0f},
                new float[] {positionX, 0.0f, 0.0f, positionX, 0.0f, 0.0f},
                Interpolation.LINEAR);
        return new AnimationClip(name, List.of(track));
    }

    /** Creates a one-key 180-degree Y-rotation clip with the requested quaternion sign. */
    private static AnimationClip constantRotationClip(String name, Object3D target, float sign) {
        AnimationTrack track = QuaternionKeyframeTrack.rotation(
                target, new float[] {0.0f}, new float[] {0.0f, sign, 0.0f, 0.0f}, Interpolation.LINEAR);
        return new AnimationClip(name, List.of(track));
    }
}
