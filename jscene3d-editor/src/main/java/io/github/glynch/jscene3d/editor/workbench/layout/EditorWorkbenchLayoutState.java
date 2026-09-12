/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable current state of the session workbench layout.
 *
 * @param views resolved view placements
 * @param visibleParts major workbench regions enabled by the user
 * @param availableParts major workbench regions which currently contain available contributions
 * @param primarySidebarPosition current primary-side-bar position
 */
public record EditorWorkbenchLayoutState(
        List<EditorViewPlacement> views,
        Set<EditorWorkbenchPart> visibleParts,
        Set<EditorWorkbenchPart> availableParts,
        EditorPrimarySidebarPosition primarySidebarPosition) {
    /** Copies and validates one layout snapshot. */
    public EditorWorkbenchLayoutState {
        views = List.copyOf(Objects.requireNonNull(views, "views"));
        visibleParts = Set.copyOf(Objects.requireNonNull(visibleParts, "visibleParts"));
        availableParts = Set.copyOf(Objects.requireNonNull(availableParts, "availableParts"));
        Objects.requireNonNull(primarySidebarPosition, "primarySidebarPosition");
    }

    /**
     * Returns whether one major workbench region is visible.
     *
     * @param part workbench region
     * @return whether the region is visible
     */
    public boolean isVisible(EditorWorkbenchPart part) {
        EditorWorkbenchPart requested = Objects.requireNonNull(part, "part");
        return visibleParts.contains(requested) && availableParts.contains(requested);
    }
}
