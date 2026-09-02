/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.internal.Preconditions;
import java.time.Duration;
import java.util.Objects;

/** Immutable presentation state for one rendered frame. */
public final class FrameUpdate {
    private final Duration elapsed;
    private final Duration simulationTime;
    private final Duration droppedTime;
    private final float interpolation;
    private final int fixedUpdateCount;
    private final ActionSnapshot input;

    /** Stores validated frame state. */
    FrameUpdate(
            Duration elapsed,
            Duration simulationTime,
            Duration droppedTime,
            float interpolation,
            int fixedUpdateCount,
            ActionSnapshot input) {
        this.elapsed = Preconditions.requireNonNegative(elapsed, "elapsed");
        this.simulationTime = Preconditions.requireNonNegative(simulationTime, "simulationTime");
        this.droppedTime = Preconditions.requireNonNegative(droppedTime, "droppedTime");
        this.interpolation = Preconditions.requireUnitInterval(interpolation, "interpolation");
        if (fixedUpdateCount < 0) {
            throw new IllegalArgumentException("fixedUpdateCount must be non-negative: " + fixedUpdateCount);
        }
        this.fixedUpdateCount = fixedUpdateCount;
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Returns accepted real time for this rendered frame.
     *
     * @return accepted elapsed time
     */
    public Duration elapsed() {
        return elapsed;
    }

    /**
     * Returns total completed simulation time.
     *
     * @return completed simulation time
     */
    public Duration simulationTime() {
        return simulationTime;
    }

    /**
     * Returns real time discarded by overload protection during this frame.
     *
     * @return discarded real time
     */
    public Duration droppedTime() {
        return droppedTime;
    }

    /**
     * Returns the accumulator fraction between the previous and current simulation states.
     *
     * @return interpolation fraction in the inclusive unit interval
     */
    public float interpolation() {
        return interpolation;
    }

    /**
     * Returns the number of fixed updates completed during this frame.
     *
     * @return completed fixed-update count
     */
    public int fixedUpdateCount() {
        return fixedUpdateCount;
    }

    /**
     * Returns unbuffered semantic input sampled for this rendered frame.
     *
     * @return semantic frame input
     */
    public ActionSnapshot input() {
        return input;
    }
}
