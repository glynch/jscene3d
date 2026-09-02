/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.animation.LoopMode;
import io.github.glynch.jscene3d.animation.SpriteAnimation;
import io.github.glynch.jscene3d.animation.SpriteAnimationEvent;
import io.github.glynch.jscene3d.animation.SpriteAnimationEventType;
import io.github.glynch.jscene3d.animation.SpriteAnimationSet;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.textures.TextureRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

final class AnimatedBillboardTest {
    private static final TextureRegion LEFT = new TextureRegion(0.0f, 0.0f, 0.5f, 1.0f);
    private static final TextureRegion RIGHT = new TextureRegion(0.5f, 0.0f, 0.5f, 1.0f);

    @Test
    void advancesRepeatedAnimationAndEmitsFrameAndLoopEvents() {
        SpriteAnimationSet set = new SpriteAnimationSet(List.of(animation("walk", LoopMode.REPEAT)));
        List<SpriteAnimationEvent> events = new ArrayList<>();
        try (BasicMaterial material = new BasicMaterial();
                AnimatedBillboard billboard = new AnimatedBillboard(material, set)) {
            billboard.addAnimationListener(events::add).play();

            billboard.update(0.6f);
            assertThat(billboard.frameIndex()).isOne();
            assertThat(billboard.textureRegion()).isEqualTo(RIGHT);

            billboard.update(0.5f);
            assertThat(billboard.frameIndex()).isZero();
            assertThat(events).extracting(SpriteAnimationEvent::type).contains(SpriteAnimationEventType.LOOPED);
        }
    }

    @Test
    void completesOneShotPlaybackAndRestartsFromItsDirectionalEndpoint() {
        SpriteAnimationSet set = new SpriteAnimationSet(List.of(animation("once", LoopMode.ONCE)));
        List<SpriteAnimationEventType> eventTypes = new ArrayList<>();
        try (BasicMaterial material = new BasicMaterial();
                AnimatedBillboard billboard = new AnimatedBillboard(material, set)) {
            billboard
                    .addAnimationListener(event -> eventTypes.add(event.type()))
                    .play();
            billboard.update(2.0f);

            assertThat(billboard.isRunning()).isFalse();
            assertThat(billboard.frameIndex()).isOne();
            assertThat(eventTypes).contains(SpriteAnimationEventType.FINISHED);

            billboard.play();
            assertThat(billboard.isRunning()).isTrue();
            assertThat(billboard.frameIndex()).isZero();
        }
    }

    @Test
    void switchesAnimationsSupportsSeekingAndPausing() {
        SpriteAnimation first = animation("idle", LoopMode.REPEAT);
        SpriteAnimation second = animation("run", LoopMode.PING_PONG);
        try (BasicMaterial material = new BasicMaterial();
                AnimatedBillboard billboard =
                        new AnimatedBillboard(material, new SpriteAnimationSet(List.of(first, second)))) {
            billboard
                    .play("run")
                    .setPlaybackSpeed(2.0f)
                    .setFrameAndProgress(1, 0.5f)
                    .pause();
            float pausedTime = billboard.time();
            billboard.update(0.25f);

            assertThat(billboard.animationName()).isEqualTo("run");
            assertThat(billboard.playbackSpeed()).isEqualTo(2.0f);
            assertThat(billboard.isPaused()).isTrue();
            assertThat(billboard.time()).isEqualTo(pausedTime);

            billboard.play().update(0.375f);
            assertThat(billboard.time()).isLessThan(pausedTime);
        }
    }

    @Test
    void replacesAnimationSetsStopsAndRemovesListenersByIdentity() {
        SpriteAnimationSet firstSet = new SpriteAnimationSet(List.of(animation("first", LoopMode.REPEAT)));
        SpriteAnimationSet secondSet = new SpriteAnimationSet(List.of(animation("second", LoopMode.REPEAT)));
        List<SpriteAnimationEvent> events = new ArrayList<>();
        Consumer<SpriteAnimationEvent> listener = events::add;
        try (BasicMaterial material = new BasicMaterial();
                AnimatedBillboard billboard = new AnimatedBillboard(material, firstSet)) {
            billboard.addAnimationListener(listener).play();
            billboard.setAnimationSet(secondSet);

            assertThat(billboard.animationSet()).isSameAs(secondSet);
            assertThat(billboard.animationName()).isEqualTo("second");
            assertThat(billboard.isRunning()).isFalse();
            assertThat(events).hasSize(1);
            assertThat(billboard.removeAnimationListener(listener)).isTrue();
            assertThat(billboard.removeAnimationListener(listener)).isFalse();
        }
    }

    @Test
    void rejectsInvalidPlaybackValues() {
        SpriteAnimationSet set = new SpriteAnimationSet(List.of(animation("walk", LoopMode.REPEAT)));
        try (BasicMaterial material = new BasicMaterial();
                AnimatedBillboard billboard = new AnimatedBillboard(material, set)) {
            assertThatIllegalArgumentException().isThrownBy(() -> billboard.setPlaybackSpeed(Float.NaN));
            assertThatIllegalArgumentException().isThrownBy(() -> billboard.update(-0.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> billboard.setFrameAndProgress(2, 0.0f));
            assertThatIllegalArgumentException().isThrownBy(() -> billboard.setFrameAndProgress(0, 1.1f));
            assertThatIllegalArgumentException().isThrownBy(() -> billboard.setAnimation("missing"));
        }
    }

    private static SpriteAnimation animation(String name, LoopMode loopMode) {
        return SpriteAnimation.uniform(name, List.of(LEFT, RIGHT), 2.0f, loopMode);
    }
}
