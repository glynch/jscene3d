/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.activity;

import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.Objects;

/**
 * Contributes one selectable Activity Bar entry backed by an editor view.
 *
 * @param id stable activity identity
 * @param title non-blank user-facing title
 * @param icon semantic icon and accessible tooltip
 * @param view primary-sidebar view owned and revealed by this pinned activity
 * @param order ascending Activity Bar presentation order
 */
public record EditorActivityContribution(ActivityId id, String title, EditorIcon icon, ViewId view, int order) {
    /**
     * Copies and validates one activity contribution.
     *
     * <p>An Activity Bar contribution pins its referenced view to the primary sidebar. It remains in the Activity Bar
     * until its registration is closed.
     */
    public EditorActivityContribution {
        Objects.requireNonNull(id, "id");
        if (Objects.requireNonNull(title, "title").isBlank()) {
            throw new IllegalArgumentException("activity title must not be blank");
        }
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(view, "view");
    }
}
