/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

final class ColorTest {
    private static final float EPSILON = 1.0e-6f;

    @Test
    void preservesAlreadyLinearChannels() {
        Color color = Color.linear(0.1f, 0.25f, 0.75f);

        assertThat(color.red()).isEqualTo(0.1f);
        assertThat(color.green()).isEqualTo(0.25f);
        assertThat(color.blue()).isEqualTo(0.75f);
    }

    @Test
    void convertsSrgbChannelsToTheLinearWorkingSpace() {
        Color color = Color.srgb(0.5f, 0.04045f, 1.0f);

        assertThat(color.red()).isCloseTo(0.21404114f, within(EPSILON));
        assertThat(color.green()).isCloseTo(0.003130805f, within(EPSILON));
        assertThat(color.blue()).isEqualTo(1.0f);
    }

    @Test
    void decodesPackedSrgbInRedGreenBlueOrder() {
        Color packed = Color.srgb(0xff8000);
        Color channels = Color.srgb(1.0f, 128.0f / 255.0f, 0.0f);

        assertThat(packed).isEqualTo(channels).hasSameHashCodeAs(channels);
    }

    @Test
    void providesTheAgreedRecognizableConstants() {
        assertThat(Color.BLACK).isEqualTo(Color.srgb(0x000000));
        assertThat(Color.WHITE).isEqualTo(Color.srgb(0xffffff));
        assertThat(Color.RED).isEqualTo(Color.srgb(0xff0000));
        assertThat(Color.GREEN).isEqualTo(Color.srgb(0x00ff00));
        assertThat(Color.BLUE).isEqualTo(Color.srgb(0x0000ff));
        assertThat(Color.YELLOW).isEqualTo(Color.srgb(0xffff00));
        assertThat(Color.CYAN).isEqualTo(Color.srgb(0x00ffff));
        assertThat(Color.MAGENTA).isEqualTo(Color.srgb(0xff00ff));
        assertThat(Color.GRAY).isEqualTo(Color.srgb(0x808080));
    }

    @Test
    void rejectsNonFiniteOrOutOfRangeChannelsAndPackedValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> Color.linear(Float.NaN, 0.0f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> Color.linear(0.0f, -0.1f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> Color.srgb(0.0f, 0.0f, 1.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> Color.srgb(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> Color.srgb(0x01000000));
    }
}
