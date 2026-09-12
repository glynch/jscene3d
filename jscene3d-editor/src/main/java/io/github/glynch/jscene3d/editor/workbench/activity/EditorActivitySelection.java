/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.activity;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchPart;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Owns Activity Bar container selection independently from primary-side-bar visibility. */
public final class EditorActivitySelection implements AutoCloseable {
    private final EditorWorkbenchLayout layout;
    private final List<Consumer<EditorActivitySelectionState>> observers = new ArrayList<>();
    private final EditorRegistration activityRegistration;
    private final EditorRegistration viewRequestRegistration;
    private List<EditorActivityContribution> activities = List.of();
    private Optional<ActivityId> selected = Optional.empty();
    private boolean closed;

    /** Creates selection state which follows registered activities and view reveal requests. */
    public EditorActivitySelection(EditorExtensionHost extensions, EditorWorkbenchLayout layout) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        this.layout = Objects.requireNonNull(layout, "layout");
        activityRegistration = host.observeActivities(this::replaceActivities);
        viewRequestRegistration = host.observeViewRequests(this::revealView);
    }

    /** Returns the current immutable selection and membership snapshot. */
    public EditorActivitySelectionState current() {
        requireOpen();
        return state();
    }

    /**
     * Selects one Activity Bar container, or toggles sidebar visibility when it is already selected.
     *
     * @param activity registered activity identity
     */
    public void select(ActivityId activity) {
        requireOpen();
        ActivityId requested = Objects.requireNonNull(activity, "activity");
        requireActivity(requested);
        if (isSelected(requested)) {
            boolean currentlyVisible = layout.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR);
            layout.setVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR, !currentlyVisible);
            if (!currentlyVisible) {
                notifyObservers();
            }
            return;
        }
        selected = Optional.of(requested);
        layout.setVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR, true);
        notifyObservers();
    }

    /** Observes selection state and immediately receives the current snapshot. */
    public EditorRegistration observe(Consumer<EditorActivitySelectionState> observer) {
        requireOpen();
        Consumer<EditorActivitySelectionState> listener = Objects.requireNonNull(observer, "observer");
        observers.add(listener);
        listener.accept(state());
        return once(() -> observers.remove(listener));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        viewRequestRegistration.close();
        activityRegistration.close();
        activities = List.of();
        selected = Optional.empty();
        observers.clear();
    }

    private void replaceActivities(List<EditorActivityContribution> replacement) {
        activities = List.copyOf(Objects.requireNonNull(replacement, "replacement"));
        Optional<ActivityId> replacementSelection = selected.filter(this::containsActivity);
        if (replacementSelection.isEmpty()) {
            replacementSelection = activities.stream().findFirst().map(EditorActivityContribution::id);
        }
        selected = replacementSelection;
        notifyObservers();
    }

    private void revealView(ViewId view) {
        activities.stream()
                .filter(activity -> activity.views().contains(view))
                .findFirst()
                .ifPresent(this::reveal);
    }

    private void reveal(EditorActivityContribution activity) {
        boolean changed = !isSelected(activity.id());
        selected = Optional.of(activity.id());
        layout.setVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR, true);
        if (changed) {
            notifyObservers();
        }
    }

    private void requireActivity(ActivityId activity) {
        if (!containsActivity(activity)) {
            throw new IllegalArgumentException("activity is not registered: " + activity);
        }
    }

    private boolean containsActivity(ActivityId activity) {
        return activities.stream().anyMatch(candidate -> candidate.id().equals(activity));
    }

    private boolean isSelected(ActivityId activity) {
        return selected.filter(activity::equals).isPresent();
    }

    private EditorActivitySelectionState state() {
        List<ViewId> selectedViews = selected.flatMap(this::findActivity)
                .map(EditorActivityContribution::views)
                .orElseGet(List::of);
        Set<ViewId> allActivityViews = new LinkedHashSet<>();
        activities.forEach(activity -> allActivityViews.addAll(activity.views()));
        return new EditorActivitySelectionState(selected, selectedViews, allActivityViews);
    }

    private Optional<EditorActivityContribution> findActivity(ActivityId activity) {
        return activities.stream()
                .filter(candidate -> candidate.id().equals(activity))
                .findFirst();
    }

    private void notifyObservers() {
        if (closed) {
            return;
        }
        EditorActivitySelectionState snapshot = state();
        List.copyOf(observers).forEach(observer -> observer.accept(snapshot));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("activity selection is closed");
        }
    }

    private static EditorRegistration once(Runnable closeAction) {
        return new EditorRegistration() {
            private boolean registrationClosed;

            @Override
            public void close() {
                if (!registrationClosed) {
                    registrationClosed = true;
                    closeAction.run();
                }
            }
        };
    }
}
