/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.examples.framework.ExampleDefinition;
import io.github.glynch.jscene3d.examples.framework.ExampleSuite;
import io.github.glynch.jscene3d.loaders.OverlayImageLoader;
import io.github.glynch.jscene3d.render.OverlayImage;
import org.junit.jupiter.api.Test;

/** Verifies that the example catalogue is complete and internally consistent. */
final class ExampleCatalogTest {
    private static final int MINIMUM_THUMBNAIL_WIDTH = 760;
    private static final int MINIMUM_THUMBNAIL_HEIGHT = 356;

    /** Ensures every catalogued example has a captured thumbnail rather than placeholder artwork. */
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

    /** Ensures glTF model cards present a category rather than an apparent loading status. */
    @Test
    void gltfModelCardsUseAnUnambiguousCategory() {
        assertThat(ExampleCatalog.definitions())
                .filteredOn(entry -> entry.category().equals("glTF Models"))
                .extracting(ExampleDefinition::id)
                .containsExactly("avocado-model", "water-bottle-model", "boom-box-model", "gltf-loading");
        assertThat(ExampleCatalog.definitions())
                .extracting(ExampleDefinition::category)
                .doesNotContain("Loading");
    }

    /** Loads one thumbnail through the same suite-owned classpath contract as the browser. */
    private static OverlayImage thumbnail(ExampleSuite suite, ExampleDefinition definition) {
        return OverlayImageLoader.loadResource(suite.resourceAnchor(), suite.thumbnailResource(definition));
    }
}
