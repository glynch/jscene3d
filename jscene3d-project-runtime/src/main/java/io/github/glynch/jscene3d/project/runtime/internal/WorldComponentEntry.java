/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentLifecycle;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import java.util.Objects;
import java.util.Set;

/** One constructed component plus its descriptor-authorized lifecycle and update participation. */
record WorldComponentEntry(
        InternalEntity owner,
        ComponentId id,
        ComponentType type,
        Object value,
        Set<ComponentLifecycle> lifecycle,
        Set<ComponentUpdatePhase> updatePhases) {
    /** Copies and validates one world-owned component entry. */
    WorldComponentEntry {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        lifecycle = Set.copyOf(lifecycle);
        updatePhases = Set.copyOf(updatePhases);
    }

    /** Returns whether the descriptor opts into one lifecycle callback. */
    boolean participates(ComponentLifecycle event) {
        return lifecycle.contains(event);
    }

    /** Returns the required callback implementation after construction-time compatibility validation. */
    ComponentLifecycleCallbacks callbacks() {
        return (ComponentLifecycleCallbacks) value;
    }

    /** Returns whether the descriptor opts into one update phase. */
    boolean participates(ComponentUpdatePhase phase) {
        return updatePhases.contains(phase);
    }

    /** Returns the required update implementation after construction-time compatibility validation. */
    ComponentUpdateCallbacks updateCallbacks() {
        return (ComponentUpdateCallbacks) value;
    }
}
