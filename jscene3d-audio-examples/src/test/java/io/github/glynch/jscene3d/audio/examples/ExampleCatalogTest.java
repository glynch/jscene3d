/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.examples.framework.ExampleDefinition;
import io.github.glynch.jscene3d.examples.framework.ExampleSuite;
import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.render.OverlayImage;
import org.junit.jupiter.api.Test;

/** Verifies that the audio suite is complete and internally consistent. */
final class ExampleCatalogTest {
    private static final int MINIMUM_THUMBNAIL_WIDTH = 760;
    private static final int MINIMUM_THUMBNAIL_HEIGHT = 356;

    /** Ensures every audio example has a real captured thumbnail. */
    @Test
    void everyExampleHasCapturedThumbnail() {
        ExampleSuite suite = ExampleCatalog.suite();
        assertThat(suite.definitions()).allSatisfy(definition -> {
            OverlayImage thumbnail = thumbnail(suite, definition);
            int width = thumbnail.width();
            int height = thumbnail.height();
            assertThat(width).isGreaterThanOrEqualTo(MINIMUM_THUMBNAIL_WIDTH);
            assertThat(height).isGreaterThanOrEqualTo(MINIMUM_THUMBNAIL_HEIGHT);
            assertThat((long) width * MINIMUM_THUMBNAIL_HEIGHT).isEqualTo((long) height * MINIMUM_THUMBNAIL_WIDTH);
        });
    }

    /** Keeps positional playback and category mixing in stable display order. */
    @Test
    void containsBothAudioExamples() {
        assertThat(ExampleCatalog.definitions())
                .extracting(ExampleDefinition::id)
                .containsExactly("positional-audio", "audio-mixing");
    }

    /** Loads one required thumbnail through the suite's resource anchor. */
    private static OverlayImage thumbnail(ExampleSuite suite, ExampleDefinition definition) {
        return OverlayImageLoader.loadResource(suite.resourceAnchor(), suite.thumbnailResource(definition));
    }
}
