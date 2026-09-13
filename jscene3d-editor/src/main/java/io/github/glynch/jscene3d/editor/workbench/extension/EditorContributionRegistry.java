/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.activity.EditorActivityRegistry;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKey;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.EditorViewRegistry;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.context.EditorContextState;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Owns views, Activity Bar contributions, and their context-sensitive availability. */
final class EditorContributionRegistry implements EditorActivityRegistry, EditorViewRegistry, AutoCloseable {
    private final EditorContextState context = new EditorContextState();
    private final Map<ActivityId, EditorActivityContribution> activities = new LinkedHashMap<>();
    private final Map<ViewId, EditorViewContribution> views = new LinkedHashMap<>();
    private final EditorSnapshotObservers<List<EditorActivityContribution>> activityObservers =
            new EditorSnapshotObservers<>();
    private final EditorSnapshotObservers<List<EditorViewContribution>> viewObservers = new EditorSnapshotObservers<>();
    private boolean closed;

    @Override
    public EditorRegistration register(EditorActivityContribution contribution) {
        requireOpen();
        EditorActivityContribution registered = Objects.requireNonNull(contribution, "contribution");
        registered.views().forEach(this::requirePrimarySidebarView);
        activities.values().stream()
                .filter(activity -> activity.views().stream().anyMatch(registered.views()::contains))
                .findFirst()
                .ifPresent(activity -> {
                    throw new IllegalArgumentException(
                            "activity view already belongs to another container: " + activity.id());
                });
        if (activities.putIfAbsent(registered.id(), registered) != null) {
            throw new IllegalArgumentException("activity identity is already registered: " + registered.id());
        }
        notifyActivityObservers();
        return EditorRegistrationOnce.of(() -> removeActivity(registered));
    }

    @Override
    public EditorRegistration register(EditorViewContribution contribution) {
        requireOpen();
        EditorViewContribution registered = Objects.requireNonNull(contribution, "contribution");
        ViewId id = registered.view().id();
        if (views.putIfAbsent(id, registered) != null) {
            throw new IllegalArgumentException("view identity is already registered: " + id);
        }
        notifyViewObservers();
        return EditorRegistrationOnce.of(() -> removeView(id, registered));
    }

    EditorRegistration observeActivities(Consumer<List<EditorActivityContribution>> observer) {
        requireOpen();
        return activityObservers.observe(observer, activitySnapshot());
    }

    EditorRegistration observeViews(Consumer<List<EditorViewContribution>> observer) {
        requireOpen();
        return viewObservers.observe(observer, viewSnapshot());
    }

    boolean isViewRegistered(ViewId view) {
        requireOpen();
        return views.containsKey(Objects.requireNonNull(view, "view"));
    }

    void requireViewAvailable(ViewId view) {
        requireOpen();
        ViewId id = Objects.requireNonNull(view, "view");
        EditorViewContribution contribution = views.get(id);
        if (contribution == null) {
            throw new IllegalArgumentException("view identity is not registered: " + id);
        }
        if (!matches(contribution.condition())) {
            throw new IllegalStateException("view is not available in the current editor context: " + id);
        }
    }

    <T> void setContext(EditorContextKey<T> key, T value) {
        requireOpen();
        if (context.set(key, value)) {
            notifyViewObservers();
            notifyActivityObservers();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        activities.clear();
        views.clear();
        notifyActivityObservers();
        notifyViewObservers();
        activityObservers.clear();
        viewObservers.clear();
    }

    private void requirePrimarySidebarView(ViewId view) {
        EditorViewContribution activityView = views.get(view);
        if (activityView == null) {
            throw new IllegalArgumentException("activity view is not registered: " + view);
        }
        if (!activityView.container().equals(EditorViewContainers.PRIMARY_SIDEBAR)) {
            throw new IllegalArgumentException("activity view must be contributed to the primary sidebar: " + view);
        }
    }

    private boolean matches(Optional<EditorContextCondition<?>> condition) {
        return condition.map(candidate -> candidate.matches(context)).orElse(true);
    }

    private List<EditorViewContribution> viewSnapshot() {
        return views.values().stream()
                .filter(contribution -> matches(contribution.condition()))
                .sorted(Comparator.comparingInt(EditorViewContribution::order))
                .toList();
    }

    private List<EditorActivityContribution> activitySnapshot() {
        return activities.values().stream()
                .filter(this::isAvailable)
                .sorted(Comparator.comparingInt(EditorActivityContribution::order))
                .toList();
    }

    private boolean isAvailable(EditorActivityContribution contribution) {
        return matches(contribution.condition())
                && contribution.views().stream()
                        .map(views::get)
                        .filter(Objects::nonNull)
                        .anyMatch(view -> matches(view.condition()));
    }

    private void removeActivity(EditorActivityContribution contribution) {
        if (activities.remove(contribution.id(), contribution)) {
            notifyActivityObservers();
        }
    }

    private void removeView(ViewId id, EditorViewContribution contribution) {
        if (views.remove(id, contribution)) {
            notifyViewObservers();
        }
    }

    private void notifyActivityObservers() {
        activityObservers.publish(activitySnapshot());
    }

    private void notifyViewObservers() {
        viewObservers.publish(viewSnapshot());
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("contribution registry is closed");
        }
    }
}
