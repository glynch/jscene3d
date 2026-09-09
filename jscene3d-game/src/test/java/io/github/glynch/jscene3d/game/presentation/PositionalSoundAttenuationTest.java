/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies positional attenuation rejects values OpenAL cannot represent safely. */
final class PositionalSoundAttenuationTest {
    /** Rejects invalid distances and rolloff before a native source is allocated. */
    @Test
    void rejectsInvalidSettings() {
        assertThatThrownBy(() -> new PositionalSoundAttenuation(0.0F, 10.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PositionalSoundAttenuation(10.0F, 5.0F, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PositionalSoundAttenuation(1.0F, 10.0F, -1.0F))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
