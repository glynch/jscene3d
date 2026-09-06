/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentUpdatePhase;
import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateContext;
import io.github.glynch.jscene3d.project.runtime.WorldUpdateException;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/** Immutable descriptor-compiled schedules plus world-owned simulation timing. */
final class WorldSchedule {
    private static final Comparator<WorldComponentEntry> ENTRY_ORDER = Comparator.comparing(
                    (WorldComponentEntry entry) -> entry.type().id().value())
            .thenComparingInt(entry -> entry.type().version())
            .thenComparing(entry -> entry.owner().scheduleIdentity())
            .thenComparing(entry -> entry.id().toString());

    private final List<WorldComponentEntry> beforePhysics;
    private final List<WorldComponentEntry> afterPhysics;
    private final List<WorldComponentEntry> frameUpdates;
    private long tick;
    private Duration simulationTime = Duration.ZERO;
    private boolean executing;

    /** Compiles each update phase once from authoritative descriptor declarations. */
    WorldSchedule(List<WorldComponentEntry> components) {
        List<WorldComponentEntry> ordered = new ArrayList<>(List.copyOf(components));
        ordered.sort(ENTRY_ORDER);
        beforePhysics = select(ordered, ComponentUpdatePhase.BEFORE_PHYSICS);
        afterPhysics = select(ordered, ComponentUpdatePhase.AFTER_PHYSICS);
        frameUpdates = select(ordered, ComponentUpdatePhase.FRAME_UPDATE);
    }

    /** Returns a schedule with no participants for an incomplete world shell. */
    static WorldSchedule empty() {
        return new WorldSchedule(List.of());
    }

    /** Advances both component-visible fixed phases around the reserved physics seam. */
    void advanceFixed(Duration step) {
        requireIdle();
        FixedUpdateContext update = new FixedUpdateContext(tick, step, simulationTime);
        Duration nextSimulationTime = simulationTime.plus(update.step());
        long nextTick = Math.incrementExact(tick);
        executing = true;
        try {
            invokeFixed(
                    beforePhysics,
                    ComponentUpdatePhase.BEFORE_PHYSICS,
                    update,
                    ComponentUpdateCallbacks::onBeforePhysics);
            invokeFixed(
                    afterPhysics, ComponentUpdatePhase.AFTER_PHYSICS, update, ComponentUpdateCallbacks::onAfterPhysics);
            simulationTime = nextSimulationTime;
            tick = nextTick;
        } finally {
            executing = false;
        }
    }

    /** Advances the component-visible presentation phase using completed simulation time. */
    void advanceFrame(Duration elapsed, float interpolation) {
        requireIdle();
        FrameUpdateContext update = new FrameUpdateContext(elapsed, simulationTime, interpolation);
        executing = true;
        try {
            invokeFrame(frameUpdates, update);
        } finally {
            executing = false;
        }
    }

    /** Returns whether one callback schedule is currently on the call stack. */
    boolean isExecuting() {
        return executing;
    }

    /** Selects one immutable phase schedule from descriptor-declared participation. */
    private static List<WorldComponentEntry> select(List<WorldComponentEntry> components, ComponentUpdatePhase phase) {
        return components.stream()
                .filter(component -> component.participates(phase))
                .toList();
    }

    /** Invokes enabled entries in stable schedule order and identifies callback failures. */
    private static void invokeFixed(
            List<WorldComponentEntry> entries,
            ComponentUpdatePhase phase,
            FixedUpdateContext update,
            BiConsumer<ComponentUpdateCallbacks, FixedUpdateContext> callback) {
        for (WorldComponentEntry entry : entries) {
            if (!entry.owner().isEnabled()) {
                continue;
            }
            try {
                callback.accept(entry.updateCallbacks(), update);
            } catch (RuntimeException failure) {
                throw failure(entry, phase, failure);
            }
        }
    }

    /** Invokes enabled presentation entries in stable schedule order. */
    private static void invokeFrame(List<WorldComponentEntry> entries, FrameUpdateContext update) {
        for (WorldComponentEntry entry : entries) {
            if (!entry.owner().isEnabled()) {
                continue;
            }
            try {
                entry.updateCallbacks().onFrameUpdate(update);
            } catch (RuntimeException failure) {
                throw failure(entry, ComponentUpdatePhase.FRAME_UPDATE, failure);
            }
        }
    }

    /** Identifies one failed scheduled callback. */
    private static WorldUpdateException failure(
            WorldComponentEntry entry, ComponentUpdatePhase phase, RuntimeException cause) {
        return new WorldUpdateException(phase, entry.owner().id(), entry.id(), cause);
    }

    /** Prevents nested schedules from corrupting deterministic phase execution. */
    private void requireIdle() {
        if (executing) {
            throw new IllegalStateException("world update is already in progress");
        }
    }
}
