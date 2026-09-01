/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies that the example catalogue is complete and internally consistent. */
final class ExampleCatalogTest {
    private static final int MINIMUM_THUMBNAIL_WIDTH = 760;
    private static final int MINIMUM_THUMBNAIL_HEIGHT = 356;

    /** Ensures every catalogued example has a captured thumbnail rather than placeholder artwork. */
    @Test
    void everyExampleHasCapturedThumbnail() {
        assertThat(ExampleCatalog.definitions()).allSatisfy(definition -> {
            int width = definition.thumbnail().width();
            int height = definition.thumbnail().height();
            assertThat(width).isGreaterThanOrEqualTo(MINIMUM_THUMBNAIL_WIDTH);
            assertThat(height).isGreaterThanOrEqualTo(MINIMUM_THUMBNAIL_HEIGHT);
            assertThat((long) width * MINIMUM_THUMBNAIL_HEIGHT).isEqualTo((long) height * MINIMUM_THUMBNAIL_WIDTH);
        });
    }

    /** Ensures glTF model cards present a category rather than an apparent loading status. */
    @Test
    void gltfModelCardsUseAnUnambiguousCategory() {
        assertThat(ExampleCatalog.entries())
                .filteredOn(entry -> entry.category().equals("glTF Models"))
                .extracting(ExampleCatalogEntry::id)
                .containsExactly("avocado-model", "water-bottle-model", "boom-box-model", "gltf-loading");
        assertThat(ExampleCatalog.entries())
                .extracting(ExampleCatalogEntry::category)
                .doesNotContain("Loading");
    }
}
