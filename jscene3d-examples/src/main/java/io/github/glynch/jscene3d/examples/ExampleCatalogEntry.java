/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleFactory;
import io.github.glynch.jscene3d.gui.GalleryAttribution;
import java.util.List;
import java.util.Objects;

/** Immutable thumbnail-independent catalogue entry used by capture tooling. */
record ExampleCatalogEntry(
        String id,
        String title,
        String category,
        String description,
        List<String> tags,
        List<GalleryAttribution> attributions,
        ExampleFactory factory) {
    /** Validates and defensively copies immutable entry metadata. */
    ExampleCatalogEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        attributions = List.copyOf(Objects.requireNonNull(attributions, "attributions"));
        Objects.requireNonNull(factory, "factory");
    }
}
