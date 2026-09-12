/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Resolves extension defaults and session layout changes into current view placements. */
public final class EditorWorkbenchLayout implements AutoCloseable {
    private static final Set<ViewContainerId> MOVABLE_CONTAINERS = Set.of(
            EditorViewContainers.PRIMARY_SIDEBAR,
            EditorViewContainers.SECONDARY_SIDEBAR,
            EditorViewContainers.BOTTOM_PANEL);

    private final Map<ViewId, ViewContainerId> overrides = new LinkedHashMap<>();
    private final List<Consumer<List<EditorViewPlacement>>> observers = new ArrayList<>();
    private final List<Consumer<EditorWorkbenchLayoutState>> stateObservers = new ArrayList<>();
    private final Set<EditorWorkbenchPart> visibleParts = EnumSet.allOf(EditorWorkbenchPart.class);
    private final EditorRegistration sourceRegistration;
    private final EditorRegistration activityRegistration;
    private final EditorRegistration requestRegistration;
    private final EditorExtensionHost extensions;
    private Set<ViewId> activityViews = Set.of();
    private List<EditorViewContribution> contributions = List.of();
    private EditorPrimarySidebarPosition primarySidebarPosition = EditorPrimarySidebarPosition.LEFT;
    private boolean closed;

    /**
     * Creates a layout which continuously resolves the host's registered views.
     *
     * @param extensions active extension host
     */
    public EditorWorkbenchLayout(EditorExtensionHost extensions) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        this.extensions = host;
        sourceRegistration = host.observeViews(this::replaceContributions);
        activityRegistration = host.observeActivities(this::replaceActivities);
        requestRegistration = host.observeViewRequests(this::reveal);
    }

    /**
     * Observes the complete ordered placement snapshot.
     *
     * <p>The observer immediately receives the current snapshot and is notified after registrations or placements
     * change.
     *
     * @param observer synchronous placement observer
     * @return removable observer registration
     */
    public EditorRegistration observeViews(Consumer<List<EditorViewPlacement>> observer) {
        requireOpen();
        Consumer<List<EditorViewPlacement>> listener = Objects.requireNonNull(observer, "observer");
        observers.add(listener);
        listener.accept(snapshot());
        return once(() -> observers.remove(listener));
    }

    /**
     * Observes the complete session layout state.
     *
     * <p>The observer immediately receives the current state.
     *
     * @param observer synchronous layout observer
     * @return removable observer registration
     */
    public EditorRegistration observe(Consumer<EditorWorkbenchLayoutState> observer) {
        requireOpen();
        Consumer<EditorWorkbenchLayoutState> listener = Objects.requireNonNull(observer, "observer");
        stateObservers.add(listener);
        listener.accept(state());
        return once(() -> stateObservers.remove(listener));
    }

    /**
     * Returns the current immutable session layout.
     *
     * @return current layout state
     */
    public EditorWorkbenchLayoutState current() {
        requireOpen();
        return state();
    }

    /**
     * Returns the current container of a registered view.
     *
     * @param view registered view identity
     * @return current container, or empty when the view is not registered
     */
    public Optional<ViewContainerId> containerOf(ViewId view) {
        requireOpen();
        ViewId id = Objects.requireNonNull(view, "view");
        return contributions.stream()
                .filter(contribution -> contribution.view().id().equals(id))
                .findFirst()
                .map(contribution -> overrides.getOrDefault(id, contribution.container()));
    }

    /**
     * Returns the major workbench region currently containing a view.
     *
     * @param view registered view identity
     * @return containing region, or empty for an unknown or editor-area view
     */
    public Optional<EditorWorkbenchPart> partOf(ViewId view) {
        return containerOf(view).flatMap(EditorWorkbenchLayout::partFor);
    }

    /**
     * Moves one registered view to a movable workbench container for this editor session.
     *
     * @param view registered view identity
     * @param container target workbench container
     * @throws IllegalArgumentException if the view is unknown, Activity Bar-owned, or the target does not accept
     *     movable views
     */
    public void move(ViewId view, ViewContainerId container) {
        requireOpen();
        ViewId id = Objects.requireNonNull(view, "view");
        ViewContainerId target = Objects.requireNonNull(container, "container");
        if (!MOVABLE_CONTAINERS.contains(target)) {
            throw new IllegalArgumentException("view cannot be moved to workbench container: " + target);
        }
        EditorViewContribution contribution = contributions.stream()
                .filter(candidate -> candidate.view().id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("view is not registered: " + id));
        if (activityViews.contains(id)) {
            throw new IllegalArgumentException("Activity Bar view cannot be moved: " + id);
        }
        ViewContainerId current = overrides.getOrDefault(id, contribution.container());
        if (current.equals(target)) {
            return;
        }
        if (contribution.container().equals(target)) {
            overrides.remove(id);
        } else {
            overrides.put(id, target);
        }
        notifyObservers();
    }

    /**
     * Changes the visibility of one major workbench region for this editor session.
     *
     * @param part workbench region
     * @param visible whether the region should be visible
     */
    public void setVisible(EditorWorkbenchPart part, boolean visible) {
        requireOpen();
        EditorWorkbenchPart target = Objects.requireNonNull(part, "part");
        boolean changed = visible ? visibleParts.add(target) : visibleParts.remove(target);
        if (changed) {
            notifyStateObservers();
        }
    }

    /**
     * Reveals one major workbench region and republishes the resulting state to its presentation adapters.
     *
     * <p>Unlike {@link #setVisible(EditorWorkbenchPart, boolean)}, this is an idempotent presentation command. A
     * repeated reveal deliberately reapplies the current state so a newly available view cannot leave its physical
     * region behind the logical selection.
     *
     * @param part workbench region to reveal
     */
    public void reveal(EditorWorkbenchPart part) {
        requireOpen();
        EditorWorkbenchPart target = Objects.requireNonNull(part, "part");
        visibleParts.add(target);
        notifyStateObservers();
    }

    /**
     * Returns whether one major workbench region is visible.
     *
     * @param part workbench region
     * @return whether the region is visible
     */
    public boolean isVisible(EditorWorkbenchPart part) {
        requireOpen();
        return visibleParts.contains(Objects.requireNonNull(part, "part"));
    }

    /**
     * Changes the primary side bar position for this editor session.
     *
     * @param position requested position
     */
    public void setPrimarySidebarPosition(EditorPrimarySidebarPosition position) {
        requireOpen();
        EditorPrimarySidebarPosition target = Objects.requireNonNull(position, "position");
        if (primarySidebarPosition != target) {
            primarySidebarPosition = target;
            notifyStateObservers();
        }
    }

    /** Restores contributed view locations and all workbench regions to their defaults. */
    public void reset() {
        requireOpen();
        boolean viewsChanged = !overrides.isEmpty();
        boolean stateChanged = !visibleParts.containsAll(EnumSet.allOf(EditorWorkbenchPart.class))
                || primarySidebarPosition != EditorPrimarySidebarPosition.LEFT;
        overrides.clear();
        visibleParts.clear();
        visibleParts.addAll(EnumSet.allOf(EditorWorkbenchPart.class));
        primarySidebarPosition = EditorPrimarySidebarPosition.LEFT;
        if (viewsChanged) {
            notifyViewObservers();
        }
        if (viewsChanged || stateChanged) {
            notifyStateObservers();
        }
    }

    /** Stops observing contributions and discards session-only layout changes. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        requestRegistration.close();
        activityRegistration.close();
        sourceRegistration.close();
        contributions = List.of();
        activityViews = Set.of();
        overrides.clear();
        observers.clear();
        stateObservers.clear();
        visibleParts.clear();
    }

    private void replaceContributions(List<EditorViewContribution> replacement) {
        contributions = List.copyOf(Objects.requireNonNull(replacement, "replacement"));
        overrides.keySet().removeIf(view -> !extensions.isViewRegistered(view));
        notifyObservers();
    }

    private void replaceActivities(List<EditorActivityContribution> replacement) {
        Set<ViewId> pinned = new LinkedHashSet<>();
        Objects.requireNonNull(replacement, "replacement").forEach(activity -> pinned.addAll(activity.views()));
        activityViews = Set.copyOf(pinned);
        overrides.keySet().removeAll(activityViews);
        notifyObservers();
    }

    private void reveal(ViewId view) {
        partOf(view).ifPresent(part -> setVisible(part, true));
    }

    private static Optional<EditorWorkbenchPart> partFor(ViewContainerId container) {
        if (container.equals(EditorViewContainers.PRIMARY_SIDEBAR)) {
            return Optional.of(EditorWorkbenchPart.PRIMARY_SIDEBAR);
        }
        if (container.equals(EditorViewContainers.SECONDARY_SIDEBAR)) {
            return Optional.of(EditorWorkbenchPart.SECONDARY_SIDEBAR);
        }
        if (container.equals(EditorViewContainers.BOTTOM_PANEL)) {
            return Optional.of(EditorWorkbenchPart.PANEL);
        }
        return Optional.empty();
    }

    private List<EditorViewPlacement> snapshot() {
        return contributions.stream()
                .map(contribution -> new EditorViewPlacement(
                        contribution.view(),
                        overrides.getOrDefault(contribution.view().id(), contribution.container()),
                        contribution.order(),
                        isMovable(contribution)))
                .toList();
    }

    private boolean isMovable(EditorViewContribution contribution) {
        return !contribution.container().equals(EditorViewContainers.EDITOR_AREA)
                && !activityViews.contains(contribution.view().id());
    }

    private void notifyObservers() {
        notifyViewObservers();
        notifyStateObservers();
    }

    private void notifyViewObservers() {
        List<EditorViewPlacement> placementSnapshot = snapshot();
        List.copyOf(observers).forEach(observer -> observer.accept(placementSnapshot));
    }

    private EditorWorkbenchLayoutState state() {
        List<EditorViewPlacement> views = snapshot();
        return new EditorWorkbenchLayoutState(views, visibleParts, availableParts(views), primarySidebarPosition);
    }

    private static Set<EditorWorkbenchPart> availableParts(List<EditorViewPlacement> views) {
        Set<EditorWorkbenchPart> available =
                EnumSet.of(EditorWorkbenchPart.ACTIVITY_BAR, EditorWorkbenchPart.STATUS_BAR);
        views.stream()
                .map(EditorViewPlacement::container)
                .map(EditorWorkbenchLayout::partFor)
                .flatMap(Optional::stream)
                .forEach(available::add);
        return available;
    }

    private void notifyStateObservers() {
        EditorWorkbenchLayoutState current = state();
        List.copyOf(stateObservers).forEach(observer -> observer.accept(current));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("workbench layout is closed");
        }
    }

    private static EditorRegistration once(Runnable removal) {
        return new EditorRegistration() {
            private boolean removed;

            @Override
            public void close() {
                if (!removed) {
                    removed = true;
                    removal.run();
                }
            }
        };
    }
}
