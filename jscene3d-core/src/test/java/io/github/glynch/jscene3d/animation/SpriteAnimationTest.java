/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.textures.TextureRegion;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SpriteAnimationTest {
    private static final TextureRegion FIRST = new TextureRegion(0.0f, 0.0f, 0.5f, 1.0f);
    private static final TextureRegion SECOND = new TextureRegion(0.5f, 0.0f, 0.5f, 1.0f);

    @Test
    void samplesVariableDurationFramesIncludingTheTerminalEndpoint() {
        SpriteAnimation animation = new SpriteAnimation(
                "pulse", List.of(new SpriteFrame(FIRST, 0.25f), new SpriteFrame(SECOND, 0.75f)), LoopMode.ONCE);

        assertThat(animation.name()).isEqualTo("pulse");
        assertThat(animation.loopMode()).isEqualTo(LoopMode.ONCE);
        assertThat(animation.duration()).isEqualTo(1.0f);
        assertThat(animation.sample(0.125f))
                .isEqualTo(new SpriteAnimationSample(animation.frames().getFirst(), 0, 0.5f));
        assertThat(animation.sample(0.25f).frameIndex()).isOne();
        assertThat(animation.sample(1.0f).frameProgress()).isEqualTo(1.0f);
    }

    @Test
    void createsUniformFramesFromAFrameRate() {
        SpriteAnimation animation = SpriteAnimation.uniform("walk", List.of(FIRST, SECOND), 8.0f, LoopMode.REPEAT);

        assertThat(animation.frames()).hasSize(2);
        assertThat(animation.frames()).extracting(SpriteFrame::durationSeconds).containsOnly(0.125f);
        assertThat(animation.duration()).isEqualTo(0.25f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidDefinitionsAndSampleTimes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SpriteAnimation(" ", List.of(new SpriteFrame(FIRST, 1.0f)), LoopMode.ONCE));
        assertThatIllegalArgumentException().isThrownBy(() -> new SpriteAnimation("empty", List.of(), LoopMode.ONCE));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SpriteAnimation.uniform("still", List.of(FIRST), 0.0f, LoopMode.ONCE));
        assertThatNullPointerException()
                .isThrownBy(() -> new SpriteAnimation("null mode", List.of(new SpriteFrame(FIRST, 1.0f)), null));

        SpriteAnimation animation = SpriteAnimation.uniform("valid", List.of(FIRST), 1.0f, LoopMode.ONCE);
        assertThatIllegalArgumentException().isThrownBy(() -> animation.sample(1.1f));
    }
}
