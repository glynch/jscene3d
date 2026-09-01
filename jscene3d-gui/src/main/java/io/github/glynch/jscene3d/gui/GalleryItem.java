/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import io.github.glynch.jscene3d.gui.internal.Preconditions;
import io.github.glynch.jscene3d.render.OverlayImage;
import java.util.List;
import java.util.Objects;

/**
 * Immutable searchable item displayed by a {@link GalleryPanel}.
 *
 * @param id stable non-blank selection identifier
 * @param title non-blank display title
 * @param category non-blank category label
 * @param description non-blank searchable description
 * @param tags non-null searchable non-blank tags
 * @param thumbnail immutable full-colour thumbnail
 * @param attributions immutable third-party asset attributions
 */
public record GalleryItem(
        String id,
        String title,
        String category,
        String description,
        List<String> tags,
        OverlayImage thumbnail,
        List<GalleryAttribution> attributions) {
    /**
     * Creates an item without third-party asset attribution.
     *
     * @param id stable non-blank selection identifier
     * @param title non-blank display title
     * @param category non-blank category label
     * @param description non-blank searchable description
     * @param tags non-null searchable non-blank tags
     * @param thumbnail immutable full-colour thumbnail
     */
    public GalleryItem(
            String id, String title, String category, String description, List<String> tags, OverlayImage thumbnail) {
        this(id, title, category, description, tags, thumbnail, List.of());
    }

    /**
     * Validates text, copies tags, and retains the immutable thumbnail.
     *
     * @throws NullPointerException if a component other than {@code tags} contains a null element,
     *     or if {@code tags} itself is null
     * @throws IllegalArgumentException if required text is blank
     */
    public GalleryItem {
        id = Preconditions.requireNonBlank(id, "id");
        title = Preconditions.requireNonBlank(title, "title");
        category = Preconditions.requireNonBlank(category, "category");
        description = Preconditions.requireNonBlank(description, "description");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        for (String tag : tags) {
            Preconditions.requireNonBlank(tag, "tag");
        }
        Objects.requireNonNull(thumbnail, "thumbnail");
        attributions = List.copyOf(Objects.requireNonNull(attributions, "attributions"));
        for (GalleryAttribution attribution : attributions) {
            Objects.requireNonNull(attribution, "attribution");
        }
    }
}
