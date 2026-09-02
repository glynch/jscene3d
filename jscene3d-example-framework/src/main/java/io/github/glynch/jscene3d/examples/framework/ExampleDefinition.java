/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import io.github.glynch.jscene3d.gui.GalleryAttribution;
import java.util.List;
import java.util.Objects;

/**
 * Immutable searchable metadata and factory for one hosted example.
 *
 * @param id stable identifier used for selection and thumbnail naming
 * @param title human-readable gallery title
 * @param category gallery category used for discovery
 * @param description concise summary of the example
 * @param tags additional searchable terms
 * @param attributions asset and design credits displayed by the gallery
 * @param factory factory that creates the hosted example
 */
public record ExampleDefinition(
        String id,
        String title,
        String category,
        String description,
        List<String> tags,
        List<GalleryAttribution> attributions,
        ExampleFactory factory) {
    /** Defensively validates and copies all metadata. */
    public ExampleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        attributions = List.copyOf(Objects.requireNonNull(attributions, "attributions"));
        Objects.requireNonNull(factory, "factory");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
