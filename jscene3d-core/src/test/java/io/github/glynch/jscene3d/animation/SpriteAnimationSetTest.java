/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.animation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.textures.TextureRegion;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SpriteAnimationSetTest {
    @Test
    void preservesDeclarationOrderAndIndexesAnimationsByName() {
        SpriteAnimation idle = animation("idle");
        SpriteAnimation run = animation("run");
        SpriteAnimationSet set = new SpriteAnimationSet(List.of(idle, run));

        assertThat(set.animations()).containsExactly(idle, run);
        assertThat(set.animation("run")).isSameAs(run);
    }

    @Test
    void rejectsEmptyDuplicateAndUnknownDefinitions() {
        SpriteAnimation firstIdle = animation("idle");
        SpriteAnimation secondIdle = animation("idle");

        assertThatIllegalArgumentException().isThrownBy(() -> new SpriteAnimationSet(List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new SpriteAnimationSet(List.of(firstIdle, secondIdle)));

        SpriteAnimationSet set = new SpriteAnimationSet(List.of(firstIdle));
        assertThatIllegalArgumentException().isThrownBy(() -> set.animation("missing"));
    }

    private static SpriteAnimation animation(String name) {
        return SpriteAnimation.uniform(name, List.of(TextureRegion.full()), 1.0f, LoopMode.REPEAT);
    }
}
