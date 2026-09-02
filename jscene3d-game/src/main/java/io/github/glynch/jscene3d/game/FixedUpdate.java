/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.internal.Preconditions;
import java.time.Duration;
import java.util.Objects;

/** Immutable state for one deterministic simulation update. */
public final class FixedUpdate {
    private final long tick;
    private final Duration step;
    private final Duration simulationTime;
    private final ActionSnapshot input;

    /** Stores validated fixed-update state. */
    FixedUpdate(long tick, Duration step, Duration simulationTime, ActionSnapshot input) {
        if (tick < 0L) {
            throw new IllegalArgumentException("tick must be non-negative: " + tick);
        }
        this.tick = tick;
        this.step = Preconditions.requirePositive(step, "step");
        this.simulationTime = Preconditions.requireNonNegative(simulationTime, "simulationTime");
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Returns the zero-based simulation update index.
     *
     * @return simulation update index
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

    /**
     * Returns action state buffered for this simulation update.
     *
     * @return semantic input state
     */
    public ActionSnapshot input() {
        return input;
    }
}
