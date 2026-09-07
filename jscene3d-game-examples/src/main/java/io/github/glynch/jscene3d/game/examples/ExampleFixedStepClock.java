/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.examples;

import io.github.glynch.jscene3d.game.GameLoopSettings;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import java.time.Duration;
import java.util.Objects;

/** Example-only fixed-step clock for lower-level movement demonstrations without a project world. */
final class ExampleFixedStepClock {
    private final GameLoopSettings settings;
    private final long fixedNanos;
    private final long maximumFrameNanos;
    private final long maximumAccumulatedNanos;
    private ActionSnapshot pendingInput = ActionSnapshot.empty();
    private long accumulatorNanos;
    private long simulationNanos;

    /** Creates a clock with the standard game timing settings. */
    ExampleFixedStepClock() {
        settings = GameLoopSettings.DEFAULT;
        fixedNanos = settings.fixedStep().toNanos();
        maximumFrameNanos = settings.maximumFrameTime().toNanos();
        maximumAccumulatedNanos = Math.multiplyExact(fixedNanos, settings.maximumFixedUpdates());
    }

    /** Advances due demonstration updates and returns their count. */
    int advance(Duration elapsed, ActionSnapshot input, FixedStep update) {
        Duration validElapsed = Objects.requireNonNull(elapsed, "elapsed");
        if (validElapsed.isNegative()) {
            throw new IllegalArgumentException("elapsed must be non-negative: " + elapsed);
        }
        pendingInput = pendingInput.merge(Objects.requireNonNull(input, "input"));
        long acceptedNanos = Math.min(validElapsed.toNanos(), maximumFrameNanos);
        accumulatorNanos = Math.min(Math.addExact(accumulatorNanos, acceptedNanos), maximumAccumulatedNanos);
        int updates = 0;
        while (accumulatorNanos >= fixedNanos && updates < settings.maximumFixedUpdates()) {
            update.advance(settings.fixedStep(), pendingInput);
            pendingInput = pendingInput.heldOnly();
            accumulatorNanos -= fixedNanos;
            simulationNanos = Math.addExact(simulationNanos, fixedNanos);
            updates++;
        }
        return updates;
    }

    /** Returns the completed simulation duration. */
    Duration simulationTime() {
        return Duration.ofNanos(simulationNanos);
    }

    /** Returns the fractional time remaining before the next fixed update. */
    float interpolation() {
        return (float) accumulatorNanos / fixedNanos;
    }

    /** One example-specific fixed update. */
    @FunctionalInterface
    interface FixedStep {
        /** Advances application state by one exact step. */
        void advance(Duration step, ActionSnapshot input);
    }
}
