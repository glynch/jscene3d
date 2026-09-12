/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.activity;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Contributes one selectable Activity Bar entry backed by a primary-side-bar view container.
 *
 * @param id stable activity identity
 * @param title non-blank user-facing title
 * @param icon semantic icon and accessible tooltip
 * @param views ordered primary-sidebar views belonging to this activity container
 * @param order ascending Activity Bar presentation order
 */
public record EditorActivityContribution(ActivityId id, String title, EditorIcon icon, List<ViewId> views, int order) {
    /**
     * Copies and validates one activity contribution.
     *
     * <p>An Activity Bar contribution pins its referenced views to the primary sidebar. Selecting it replaces the
     * sidebar contents with the views in this container. It remains in the Activity Bar until its registration closes.
     */
    public EditorActivityContribution {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(title, "title").isBlank()) {
            throw new IllegalArgumentException("activity title must not be blank");
        }
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(views, "views");
        if (views.isEmpty()) {
            throw new IllegalArgumentException("activity container must contain at least one view");
        }
        if (views.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("activity views must not contain null");
        }
        if (new LinkedHashSet<>(views).size() != views.size()) {
            throw new IllegalArgumentException("activity container must not contain duplicate views");
        }
        views = List.copyOf(views);
    }

    /** Creates an Activity Bar container which initially contains one primary-side-bar view. */
    public EditorActivityContribution(ActivityId id, String title, EditorIcon icon, ViewId view, int order) {
        this(id, title, icon, List.of(Objects.requireNonNull(view, "view")), order);
    }
}
