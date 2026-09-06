/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static io.github.glynch.jscene3d.project.runtime.internal.Preconditions.requireNonNegative;
import static io.github.glynch.jscene3d.project.runtime.internal.Preconditions.requireUnitInterval;

import java.time.Duration;

/** Immutable timing state supplied to one descriptor-authorized frame update. */
public final class FrameUpdateContext {
    private final Duration elapsed;
    private final Duration simulationTime;
    private final float interpolation;

    /**
     * Creates timing state for one presentation update.
     *
     * @param elapsed non-negative real time accepted for the frame
     * @param simulationTime non-negative completed simulation time
     * @param interpolation finite fraction in the inclusive unit interval
     * @throws IllegalArgumentException if a duration is negative or interpolation is outside the unit interval
     */
    public FrameUpdateContext(Duration elapsed, Duration simulationTime, float interpolation) {
        this.elapsed = requireNonNegative(elapsed, "elapsed");
        this.simulationTime = requireNonNegative(simulationTime, "simulationTime");
        this.interpolation = requireUnitInterval(interpolation, "interpolation");
    }

    /**
     * Returns accepted real time for this frame.
     *
     * @return frame elapsed time
     */
    public Duration elapsed() {
        return elapsed;
    }

    /**
     * Returns total simulation time completed before this presentation update.
     *
     * @return completed simulation time
     */
    public Duration simulationTime() {
        return simulationTime;
    }

    /**
     * Returns the accumulator fraction between the previous and current simulation states.
     *
     * @return interpolation fraction in the inclusive unit interval
     */
    public float interpolation() {
        return interpolation;
    }
}
