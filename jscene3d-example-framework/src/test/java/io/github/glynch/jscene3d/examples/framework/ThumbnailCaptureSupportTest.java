/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies thumbnail selection without requiring an OpenGL context. */
final class ThumbnailCaptureSupportTest {
    private final ExampleDefinition first = definition("first");
    private final ExampleDefinition second = definition("second");
    private final ExampleSuite suite =
            new ExampleSuite("Examples", "JScene3D", getClass(), "/thumbnails", List.of(first, second));

    /** Empty selection retains every suite definition. */
    @Test
    void selectsEveryDefinitionWhenNoIdentifiersAreSupplied() {
        assertThat(ThumbnailCaptureSupport.selectDefinitions(suite, List.of())).containsExactly(first, second);
    }

    /** Explicit identifiers retain suite order rather than request order. */
    @Test
    void selectsRequestedDefinitionsInSuiteOrder() {
        assertThat(ThumbnailCaptureSupport.selectDefinitions(suite, List.of("second", "first")))
                .containsExactly(first, second);
    }

    /** Unknown identifiers fail with both invalid and available identifiers. */
    @Test
    void rejectsUnknownIdentifiers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ThumbnailCaptureSupport.selectDefinitions(suite, List.of("missing")))
                .withMessageContaining("missing")
                .withMessageContaining("first");
    }

    private static ExampleDefinition definition(String id) {
        return new ExampleDefinition(id, "Title", "Category", "Description", List.of(), List.of(), ignored -> {
            throw new AssertionError("Factory must not run in selection tests");
        });
    }
}
