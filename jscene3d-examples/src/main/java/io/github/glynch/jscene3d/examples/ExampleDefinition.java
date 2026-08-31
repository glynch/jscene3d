/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleFactory;
import io.github.glynch.jscene3d.gui.GalleryItem;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.List;
import java.util.Objects;

/** Immutable catalogue metadata and factory for one live example. */
record ExampleDefinition(
        String id,
        String title,
        String category,
        String description,
        List<String> tags,
        OverlayImage thumbnail,
        ExampleFactory factory) {
    /** Defensively validates and copies catalogue metadata. */
    ExampleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(description, "description");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        Objects.requireNonNull(thumbnail, "thumbnail");
        Objects.requireNonNull(factory, "factory");
    }

    /** Creates the GUI-facing immutable item without exposing the example factory. */
    GalleryItem galleryItem() {
        return new GalleryItem(id, title, category, description, tags, thumbnail);
    }
}
