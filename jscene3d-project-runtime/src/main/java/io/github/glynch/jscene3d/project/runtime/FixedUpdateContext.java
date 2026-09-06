/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static io.github.glynch.jscene3d.project.runtime.internal.Preconditions.requireNonNegative;
import static io.github.glynch.jscene3d.project.runtime.internal.Preconditions.requirePositive;

import java.time.Duration;

/** Immutable timing state supplied to one descriptor-authorized fixed update. */
public final class FixedUpdateContext {
    private final long tick;
    private final Duration step;
    private final Duration simulationTime;

    /**
     * Creates timing state for one fixed update.
     *
     * @param tick zero-based fixed-update index
     * @param step positive duration advanced by the update
     * @param simulationTime non-negative simulation time at the beginning of the update
     * @throws IllegalArgumentException if the tick or simulation time is negative, or the step is not positive
     */
    public FixedUpdateContext(long tick, Duration step, Duration simulationTime) {
        if (tick < 0L) {
            throw new IllegalArgumentException("tick must be non-negative: " + tick);
        }
        this.tick = tick;
        this.step = requirePositive(step, "step");
        this.simulationTime = requireNonNegative(simulationTime, "simulationTime");
    }

    /**
     * Returns the zero-based fixed-update index.
     *
     * @return fixed-update index
     */
    public long tick() {
        return tick;
    }

    /**
     * Returns the exact duration advanced by this update.
     *
     * @return fixed-step duration
     */
    public Duration step() {
        return step;
    }

    /**
     * Returns simulation time at the beginning of this update.
     *
     * @return elapsed simulation time
     */
    public Duration simulationTime() {
        return simulationTime;
    }
}
