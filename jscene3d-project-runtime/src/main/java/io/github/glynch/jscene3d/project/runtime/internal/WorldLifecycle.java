/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.runtime.WorldLifecycleException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Owns world lifecycle state, callback ordering, compensation, and component-value release. */
final class WorldLifecycle {
    private State state = State.BUILDING;
    private List<WorldComponentEntry> components = List.of();
    private final List<WorldComponentEntry> created = new ArrayList<>();
    private final List<WorldComponentEntry> active = new ArrayList<>();

    /** Takes ownership of fully constructed components and publishes the inactive state. */
    void complete(List<WorldComponentEntry> values) {
        requireState(State.BUILDING, "world composition is not open");
        components = new ArrayList<>(List.copyOf(values));
        state = State.INACTIVE;
    }

    /** Marks failed construction terminal; component construction owns its separate rollback list. */
    void failComposition() {
        state = State.CLOSED;
    }

    /** Delivers creation and initial activation atomically in forward ownership order. */
    void activate() {
        requireState(State.INACTIVE, activationStateMessage());
        state = State.ACTIVATING;
        try {
            createComponents();
            activateComponents();
            state = State.ACTIVE;
        } catch (RuntimeException failure) {
            state = State.CLOSING;
            RuntimeException compensated = Objects.requireNonNull(releaseLifecycle(failure), "activation failure");
            compensated = closeComponents(compensated);
            state = State.CLOSED;
            throw Objects.requireNonNull(compensated, "activation failure");
        }
    }

    /** Returns whether activation completed and closure has not begun. */
    boolean isActive() {
        return state == State.ACTIVE;
    }

    /** Returns whether composition failed, activation rolled back, or closure completed. */
    boolean isClosed() {
        return state == State.CLOSED;
    }

    /** Synchronizes active callbacks after committed entity enablement changes. */
    void synchronizeActivation() {
        requireState(State.ACTIVE, "world is not active");
        @Nullable RuntimeException failure = deactivateDisabledComponents();
        if (failure != null) {
            throw failure;
        }
        activateNewlyEnabledComponents();
    }

    /** Creates and activates one newly composed component set before taking ownership of it. */
    void add(List<WorldComponentEntry> additions) {
        requireState(State.ACTIVE, "world is not active");
        List<WorldComponentEntry> values = List.copyOf(Objects.requireNonNull(additions, "additions"));
        List<WorldComponentEntry> newlyCreated = new ArrayList<>();
        List<WorldComponentEntry> newlyActive = new ArrayList<>();
        try {
            for (WorldComponentEntry component : values) {
                invoke(component, ComponentLifecycle.CREATED);
                newlyCreated.add(component);
            }
            for (WorldComponentEntry component : values) {
                if (component.owner().isEnabled()) {
                    invoke(component, ComponentLifecycle.ACTIVATED);
                    newlyActive.add(component);
                }
            }
        } catch (RuntimeException failure) {
            throw Objects.requireNonNull(
                    compensateAddition(newlyCreated, newlyActive, failure), "spawn activation failure");
        }
        components.addAll(values);
        created.addAll(newlyCreated);
        active.addAll(newlyActive);
    }

    /** Permanently releases every component owned by a committed destroyed subtree. */
    void destroy(Set<InternalEntity> entities) {
        requireState(State.ACTIVE, "world is not active");
        Set<InternalEntity> targets = Set.copyOf(Objects.requireNonNull(entities, "entities"));
        @Nullable RuntimeException failure = deactivate(targets, null);
        failure = destroyCreated(targets, failure);
        failure = closeAndRemove(targets, failure);
        if (failure != null) {
            throw failure;
        }
    }

    /** Releases semantic lifecycle and component values once in reverse order. */
    void close() {
        if (state == State.CLOSED || state == State.CLOSING) {
            return;
        }
        if (state == State.ACTIVATING) {
            throw new IllegalStateException("world activation is in progress");
        }
        state = State.CLOSING;
        @Nullable RuntimeException failure = null;
        if (!created.isEmpty()) {
            failure = releaseLifecycle(null);
        }
        failure = closeComponents(failure);
        state = State.CLOSED;
        if (failure != null) {
            throw failure;
        }
    }

    /** Crosses the semantic creation stage for every component in owner-first order. */
    private void createComponents() {
        for (WorldComponentEntry component : components) {
            invoke(component, ComponentLifecycle.CREATED);
            created.add(component);
        }
    }

    /** Crosses the active stage for components owned by initially enabled entities. */
    private void activateComponents() {
        for (WorldComponentEntry component : components) {
            if (component.owner().isEnabled()) {
                invoke(component, ComponentLifecycle.ACTIVATED);
                active.add(component);
            }
        }
    }

    /** Deactivates every component whose owner became effectively disabled. */
    private @Nullable RuntimeException deactivateDisabledComponents() {
        @Nullable RuntimeException failure = null;
        for (int index = active.size() - 1; index >= 0; index--) {
            WorldComponentEntry component = active.get(index);
            if (!component.owner().isEnabled()) {
                failure = invokeForCleanup(component, ComponentLifecycle.DEACTIVATED, failure);
                active.remove(index);
            }
        }
        return failure;
    }

    /** Activates every created component whose owner became effectively enabled. */
    private void activateNewlyEnabledComponents() {
        for (WorldComponentEntry component : components) {
            if (created.contains(component) && component.owner().isEnabled() && !active.contains(component)) {
                invoke(component, ComponentLifecycle.ACTIVATED);
                active.add(component);
            }
        }
    }

    /** Deactivates active components owned by a destroyed subtree in reverse order. */
    private @Nullable RuntimeException deactivate(Set<InternalEntity> entities, @Nullable RuntimeException existing) {
        @Nullable RuntimeException failure = existing;
        for (int index = active.size() - 1; index >= 0; index--) {
            WorldComponentEntry component = active.get(index);
            if (entities.contains(component.owner())) {
                failure = invokeForCleanup(component, ComponentLifecycle.DEACTIVATED, failure);
                active.remove(index);
            }
        }
        return failure;
    }

    /** Destroys created components owned by a destroyed subtree in reverse order. */
    private @Nullable RuntimeException destroyCreated(
            Set<InternalEntity> entities, @Nullable RuntimeException existing) {
        @Nullable RuntimeException failure = existing;
        for (int index = created.size() - 1; index >= 0; index--) {
            WorldComponentEntry component = created.get(index);
            if (entities.contains(component.owner())) {
                failure = invokeForCleanup(component, ComponentLifecycle.DESTROYED, failure);
                created.remove(index);
            }
        }
        return failure;
    }

    /** Closes and forgets component values owned by a destroyed subtree in reverse order. */
    private @Nullable RuntimeException closeAndRemove(
            Set<InternalEntity> entities, @Nullable RuntimeException existing) {
        @Nullable RuntimeException failure = existing;
        for (int index = components.size() - 1; index >= 0; index--) {
            WorldComponentEntry component = components.get(index);
            if (entities.contains(component.owner())) {
                failure = closeValue(component.value(), failure);
                components.remove(index);
            }
        }
        return failure;
    }

    /** Deactivates active entries and destroys created entries while retaining the first failure. */
    private @Nullable RuntimeException releaseLifecycle(@Nullable RuntimeException existing) {
        @Nullable RuntimeException failure = invokeReverse(active, ComponentLifecycle.DEACTIVATED, existing);
        active.clear();
        failure = invokeReverse(created, ComponentLifecycle.DESTROYED, failure);
        created.clear();
        return failure;
    }

    /** Invokes one declared event in reverse list order and accumulates callback failures. */
    private static @Nullable RuntimeException invokeReverse(
            List<WorldComponentEntry> entries, ComponentLifecycle event, @Nullable RuntimeException existing) {
        @Nullable RuntimeException failure = existing;
        for (int index = entries.size() - 1; index >= 0; index--) {
            failure = invokeForCleanup(entries.get(index), event, failure);
        }
        return failure;
    }

    /** Compensates lifecycle callbacks delivered for an addition which could not become live. */
    private static @Nullable RuntimeException compensateAddition(
            List<WorldComponentEntry> newlyCreated, List<WorldComponentEntry> newlyActive, RuntimeException failure) {
        RuntimeException result = invokeReverse(newlyActive, ComponentLifecycle.DEACTIVATED, failure);
        return invokeReverse(newlyCreated, ComponentLifecycle.DESTROYED, result);
    }

    /** Delivers one cleanup callback while retaining earlier failure precedence. */
    private static @Nullable RuntimeException invokeForCleanup(
            WorldComponentEntry component, ComponentLifecycle event, @Nullable RuntimeException existing) {
        try {
            invoke(component, event);
            return existing;
        } catch (RuntimeException callbackFailure) {
            return accumulate(existing, callbackFailure);
        }
    }

    /** Invokes one descriptor-authorized callback and identifies any implementation failure. */
    private static void invoke(WorldComponentEntry component, ComponentLifecycle event) {
        if (!component.participates(event)) {
            return;
        }
        try {
            switch (event) {
                case CREATED -> component.callbacks().onCreated();
                case ACTIVATED -> component.callbacks().onActivated();
                case DEACTIVATED -> component.callbacks().onDeactivated();
                case DESTROYED -> component.callbacks().onDestroyed();
            }
        } catch (RuntimeException failure) {
            throw new WorldLifecycleException(event, component.owner().id(), component.id(), failure);
        }
    }

    /** Closes every factory-created value in reverse construction order. */
    private @Nullable RuntimeException closeComponents(@Nullable RuntimeException existing) {
        @Nullable RuntimeException failure = existing;
        for (int index = components.size() - 1; index >= 0; index--) {
            failure = closeValue(components.get(index).value(), failure);
        }
        components = List.of();
        return failure;
    }

    /** Closes one component value and retains earlier failure precedence. */
    private static @Nullable RuntimeException closeValue(Object value, @Nullable RuntimeException existing) {
        if (!(value instanceof AutoCloseable closeable)) {
            return existing;
        }
        try {
            closeable.close();
            return existing;
        } catch (Exception closeFailure) {
            return accumulate(existing, new IllegalStateException("component cleanup failed", closeFailure));
        }
    }

    /** Retains the first cleanup failure and suppresses every later failure. */
    private static RuntimeException accumulate(@Nullable RuntimeException existing, RuntimeException additional) {
        if (existing == null) {
            return additional;
        }
        existing.addSuppressed(additional);
        return existing;
    }

    /** Requires one exact lifecycle state before a transition. */
    private void requireState(State expected, String message) {
        if (state != expected) {
            throw new IllegalStateException(message);
        }
    }

    /** Describes why the current state cannot begin its one permitted activation. */
    private String activationStateMessage() {
        return switch (state) {
            case BUILDING -> "world composition is incomplete";
            case INACTIVE -> "world is inactive";
            case ACTIVATING -> "world activation is already in progress";
            case ACTIVE -> "world is already active";
            case CLOSING -> "world is closing";
            case CLOSED -> "world is closed";
        };
    }

    /** Closed lifecycle transition states for one world instance. */
    private enum State {
        BUILDING,
        INACTIVE,
        ACTIVATING,
        ACTIVE,
        CLOSING,
        CLOSED
    }
}
