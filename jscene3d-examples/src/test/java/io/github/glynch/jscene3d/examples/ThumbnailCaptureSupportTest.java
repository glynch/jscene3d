/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies thumbnail-capture catalogue selection without requiring an OpenGL context. */
final class ThumbnailCaptureSupportTest {
    /** Empty selection retains every catalogue entry. */
    @Test
    void selectsEveryEntryWhenNoIdentifiersAreSupplied() {
        assertThat(ThumbnailCaptureSupport.selectEntries(List.of()))
                .containsExactlyElementsOf(ExampleCatalog.entries());
    }

    /** Explicit identifiers select only matching entries in stable catalogue order. */
    @Test
    void selectsRequestedEntriesInCatalogueOrder() {
        List<ExampleCatalogEntry> selected =
                ThumbnailCaptureSupport.selectEntries(List.of("shadows", "basic-triangle"));

        assertThat(selected).extracting(ExampleCatalogEntry::id).containsExactly("basic-triangle", "shadows");
    }

    /** Unknown identifiers fail with both the invalid and available identifiers. */
    @Test
    void rejectsUnknownIdentifiers() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ThumbnailCaptureSupport.selectEntries(List.of("missing-example")))
                .withMessageContaining("missing-example")
                .withMessageContaining("shadows");
    }
}
