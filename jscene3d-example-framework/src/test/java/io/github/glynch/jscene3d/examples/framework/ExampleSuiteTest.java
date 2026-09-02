/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies suite identity and thumbnail-resource invariants. */
final class ExampleSuiteTest {
    /** Creates stable absolute thumbnail names from suite metadata. */
    @Test
    void resolvesDefinitionThumbnailResource() {
        ExampleDefinition definition = definition("first");
        ExampleSuite suite = suite(List.of(definition));

        assertThat(suite.thumbnailResource(definition)).isEqualTo("/thumbnails/first.png");
    }

    /** Rejects identifiers that would make browser selection ambiguous. */
    @Test
    void rejectsDuplicateDefinitionIdentifiers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> suite(List.of(definition("duplicate"), definition("duplicate"))))
                .withMessageContaining("duplicate");
    }

    /** Rejects definitions owned by another suite. */
    @Test
    void rejectsForeignThumbnailDefinition() {
        ExampleSuite suite = suite(List.of(definition("first")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> suite.thumbnailResource(definition("second")))
                .withMessageContaining("second");
    }

    private static ExampleSuite suite(List<ExampleDefinition> definitions) {
        return new ExampleSuite("Examples", "JScene3D", ExampleSuiteTest.class, "/thumbnails", definitions);
    }

    private static ExampleDefinition definition(String id) {
        return new ExampleDefinition(id, "Title", "Category", "Description", List.of(), List.of(), ignored -> {
            throw new AssertionError("Factory must not run in metadata tests");
        });
    }
}
