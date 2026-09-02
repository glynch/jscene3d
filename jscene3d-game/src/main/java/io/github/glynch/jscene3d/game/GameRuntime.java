/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.internal.Preconditions;
import java.time.Duration;
import java.util.Objects;

/**
 * Coordinates one game application's start, fixed updates, frame updates, rendering, and closure.
 *
 * <p>The caller owns event polling, elapsed-time measurement, rendering buffers, and the execution
 * thread. This runtime clamps frame time, limits catch-up work, buffers input transitions until a
 * fixed update consumes them, and exposes interpolation for smooth presentation.
 */
public final class GameRuntime implements AutoCloseable {
    private final GameApplication application;
    private final GameLoopSettings settings;
    private final long fixedNanos;
    private final long maximumFrameNanos;
    private final long maximumAccumulatedNanos;

    private ActionSnapshot pendingInput = ActionSnapshot.empty();
    private FrameUpdate frame =
            new FrameUpdate(Duration.ZERO, Duration.ZERO, Duration.ZERO, 0.0F, 0, ActionSnapshot.empty());
    private long accumulatorNanos;
    private long simulationNanos;
    private long tick;
    private boolean started;
    private boolean closed;

    /**
     * Creates a runtime using default fixed-step settings.
     *
     * @param application caller-defined application owned by this runtime
     */
    public GameRuntime(GameApplication application) {
        this(application, GameLoopSettings.DEFAULT);
    }

    /**
     * Creates a runtime that owns one application.
     *
     * @param application caller-defined application
     * @param settings immutable loop settings
     */
    public GameRuntime(GameApplication application, GameLoopSettings settings) {
        this.application = Objects.requireNonNull(application, "application");
        this.settings = Objects.requireNonNull(settings, "settings");
        fixedNanos = settings.fixedStep().toNanos();
        maximumFrameNanos = settings.maximumFrameTime().toNanos();
        maximumAccumulatedNanos = Math.multiplyExact(fixedNanos, settings.maximumFixedUpdates());
    }

    /** Starts the application exactly once. */
    public void start() {
        requireOpen();
        if (started) {
            throw new IllegalStateException("GameRuntime has already started");
        }
        application.start();
        started = true;
    }

    /**
     * Advances fixed and variable updates for one host frame.
     *
     * @param elapsed non-negative real time since the preceding frame
     * @param input semantic input sampled after the current event poll
     * @return immutable current frame state
     */
    public FrameUpdate advance(Duration elapsed, ActionSnapshot input) {
        requireRunning();
        Duration validElapsed = Preconditions.requireNonNegative(elapsed, "elapsed");
        ActionSnapshot validInput = Objects.requireNonNull(input, "input");
        long suppliedNanos = validElapsed.toNanos();
        long acceptedNanos = Math.min(suppliedNanos, maximumFrameNanos);
        long droppedNanos = suppliedNanos - acceptedNanos;
        pendingInput = pendingInput.merge(validInput);
        long accumulated = Math.addExact(accumulatorNanos, acceptedNanos);
        if (accumulated > maximumAccumulatedNanos) {
            droppedNanos = Math.addExact(droppedNanos, accumulated - maximumAccumulatedNanos);
            accumulated = maximumAccumulatedNanos;
        }
        accumulatorNanos = accumulated;
        int fixedUpdateCount = runFixedUpdates();
        float interpolation = (float) accumulatorNanos / fixedNanos;
        frame = new FrameUpdate(
                Duration.ofNanos(acceptedNanos),
                Duration.ofNanos(simulationNanos),
                Duration.ofNanos(droppedNanos),
                interpolation,
                fixedUpdateCount,
                validInput);
        application.update(frame);
        return frame;
    }

    /** Renders using the current frame state. */
    public void render() {
        requireRunning();
        application.render(frame);
    }

    /**
     * Returns the immutable loop settings.
     *
     * @return loop settings
     */
    public GameLoopSettings settings() {
        return settings;
    }

    /**
     * Returns whether application startup completed.
     *
     * @return {@code true} after successful startup
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Returns whether terminal closure began.
     *
     * @return {@code true} once terminal closure starts
     */
    public boolean isClosed() {
        return closed;
    }

    /** Closes the owned application exactly once. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        application.close();
    }

    /** Performs every currently due fixed update. */
    private int runFixedUpdates() {
        int updateCount = 0;
        while (accumulatorNanos >= fixedNanos && updateCount < settings.maximumFixedUpdates()) {
            FixedUpdate update =
                    new FixedUpdate(tick, settings.fixedStep(), Duration.ofNanos(simulationNanos), pendingInput);
            application.fixedUpdate(update);
            pendingInput = pendingInput.heldOnly();
            accumulatorNanos -= fixedNanos;
            simulationNanos = Math.addExact(simulationNanos, fixedNanos);
            tick++;
            updateCount++;
        }
        return updateCount;
    }

    /** Requires a started, open runtime. */
    private void requireRunning() {
        requireOpen();
        if (!started) {
            throw new IllegalStateException("GameRuntime has not started");
        }
    }

    /** Requires a runtime before terminal closure. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("GameRuntime is closed");
        }
    }
}
