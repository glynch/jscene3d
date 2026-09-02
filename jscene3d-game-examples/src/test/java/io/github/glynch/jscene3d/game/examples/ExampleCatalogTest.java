/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.examples.framework.ExampleDefinition;
import io.github.glynch.jscene3d.examples.framework.ExampleSuite;
import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.render.OverlayImage;
import org.junit.jupiter.api.Test;

/** Verifies that the game-runtime suite is complete and internally consistent. */
final class ExampleCatalogTest {
    private static final int MINIMUM_THUMBNAIL_WIDTH = 760;
    private static final int MINIMUM_THUMBNAIL_HEIGHT = 356;

    /** Ensures every game example has a real captured thumbnail. */
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

    /** Keeps the initial suite focused on the complete reusable game-runtime seam. */
    @Test
    void containsTheFirstPersonSandbox() {
        assertThat(ExampleCatalog.definitions())
                .extracting(ExampleDefinition::id)
                .containsExactly("first-person-sandbox");
    }

    /** Loads one required thumbnail through the suite's resource anchor. */
    private static OverlayImage thumbnail(ExampleSuite suite, ExampleDefinition definition) {
        return OverlayImageLoader.loadResource(suite.resourceAnchor(), suite.thumbnailResource(definition));
    }
}
