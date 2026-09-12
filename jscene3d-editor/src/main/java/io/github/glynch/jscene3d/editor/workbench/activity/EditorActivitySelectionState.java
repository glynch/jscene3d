/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.activity;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable selected Activity Bar container and its primary-side-bar view membership. */
public record EditorActivitySelectionState(
        Optional<ActivityId> selected, List<ViewId> selectedViews, Set<ViewId> activityViews) {
    /** Copies one consistent selection snapshot. */
    public EditorActivitySelectionState {
        Objects.requireNonNull(selected, "selected");
        selectedViews = List.copyOf(Objects.requireNonNull(selectedViews, "selectedViews"));
        activityViews = Set.copyOf(Objects.requireNonNull(activityViews, "activityViews"));
    }

    /** Returns whether a primary-side-bar view belongs in the currently selected container. */
    public boolean includes(ViewId view) {
        ViewId candidate = Objects.requireNonNull(view, "view");
        return !activityViews.contains(candidate) || selectedViews.contains(candidate);
    }
}
