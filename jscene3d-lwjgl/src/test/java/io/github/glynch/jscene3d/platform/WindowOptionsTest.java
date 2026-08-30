/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

final class WindowOptionsTest {
    @Test
    void providesDocumentedDefaults() {
        WindowOptions options = WindowOptions.defaults();

        assertThat(options.width()).isEqualTo(1280);
        assertThat(options.height()).isEqualTo(720);
        assertThat(options.title()).isEqualTo("JScene3D");
        assertThat(options.verticalSync()).isEqualTo(VerticalSync.ENABLED);
        assertThat(options.preferredFramebufferSampleCount()).isZero();
        assertThat(WindowOptions.builder().build()).isEqualTo(options);
    }

    @Test
    void buildsCustomizedOptions() {
        WindowOptions options = WindowOptions.builder()
                .size(1920, 1080)
                .title("Solar System")
                .verticalSync(VerticalSync.DISABLED)
                .preferredFramebufferSampleCount(4)
                .build();

        assertThat(options.width()).isEqualTo(1920);
        assertThat(options.height()).isEqualTo(1080);
        assertThat(options.title()).isEqualTo("Solar System");
        assertThat(options.verticalSync()).isEqualTo(VerticalSync.DISABLED);
        assertThat(options.preferredFramebufferSampleCount()).isEqualTo(4);
    }

    @Test
    void hasValueEquality() {
        WindowOptions first =
                WindowOptions.builder().size(800, 600).title("Equal").build();
        WindowOptions second =
                WindowOptions.builder().size(800, 600).title("Equal").build();
        WindowOptions different =
                WindowOptions.builder().size(801, 600).title("Equal").build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo(different);
        assertThat(first.toString()).contains("width=800", "height=600", "title=Equal", "verticalSync=ENABLED");
    }

    @Test
    void builtOptionsAreIndependentOfLaterBuilderChanges() {
        WindowOptions.Builder builder = WindowOptions.builder().size(800, 600);
        WindowOptions first = builder.build();

        WindowOptions second = builder.size(1024, 768).build();

        assertThat(first.width()).isEqualTo(800);
        assertThat(first.height()).isEqualTo(600);
        assertThat(second.width()).isEqualTo(1024);
        assertThat(second.height()).isEqualTo(768);
    }

    @Test
    void rejectsNonPositiveDimensionsWithoutPartiallyChangingBuilder() {
        WindowOptions.Builder builder = WindowOptions.builder().size(800, 600);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.size(1024, 0))
                .withMessage("height must be positive: 0");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> builder.size(-1, 768))
                .withMessage("width must be positive: -1");
        assertThat(builder.build())
                .isEqualTo(WindowOptions.builder().size(800, 600).build());
    }

    @Test
    void acceptsAnEmptyTitleButRejectsANullCharacter() {
        assertThat(WindowOptions.builder().title("").build().title()).isEmpty();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WindowOptions.builder().title("bad\0title"))
                .withMessage("title must not contain a null character");
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsANullTitle() {
        assertThatNullPointerException()
                .isThrownBy(() -> WindowOptions.builder().title(null))
                .withMessage("title");
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsANullVerticalSyncMode() {
        assertThatNullPointerException()
                .isThrownBy(() -> WindowOptions.builder().verticalSync(null))
                .withMessage("verticalSync");
    }

    @Test
    void rejectsANegativePreferredFramebufferSampleCount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WindowOptions.builder().preferredFramebufferSampleCount(-1))
                .withMessage("preferredFramebufferSampleCount must not be negative: -1");
    }
}
