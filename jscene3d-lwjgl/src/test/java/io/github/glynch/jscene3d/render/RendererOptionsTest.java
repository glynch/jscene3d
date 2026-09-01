/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class RendererOptionsTest {
    @Test
    void providesCompleteDefaults() {
        RendererOptions options = RendererOptions.defaults();

        assertThat(options.automaticClear()).isTrue();
        assertThat(options.clearColor()).isEqualTo(Color.BLACK);
        assertThat(options.clearAlpha()).isEqualTo(1.0f);
        assertThat(options.toneMapping()).isEqualTo(ToneMapping.NONE);
        assertThat(options.exposure()).isOne();
    }

    @Test
    void buildsConfiguredOptions() {
        RendererOptions options = RendererOptions.builder()
                .automaticClear(false)
                .clearColor(Color.BLUE)
                .clearAlpha(0.5f)
                .toneMapping(ToneMapping.ACES_FILMIC)
                .exposure(1.25f)
                .build();

        assertThat(options.automaticClear()).isFalse();
        assertThat(options.clearColor()).isEqualTo(Color.BLUE);
        assertThat(options.clearAlpha()).isEqualTo(0.5f);
        assertThat(options.toneMapping()).isEqualTo(ToneMapping.ACES_FILMIC);
        assertThat(options.exposure()).isEqualTo(1.25f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidValues() {
        RendererOptions.Builder builder = RendererOptions.builder();

        assertThatNullPointerException().isThrownBy(() -> builder.clearColor(null));
        assertThatNullPointerException().isThrownBy(() -> builder.toneMapping(null));
        assertThatIllegalArgumentException().isThrownBy(() -> builder.clearAlpha(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> builder.clearAlpha(-0.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> builder.clearAlpha(1.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> builder.exposure(0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> builder.exposure(Float.NaN));
    }
}
