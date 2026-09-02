/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game;

/**
 * Caller-defined game behavior coordinated by a {@link GameRuntime}.
 *
 * <p>Callbacks run serially on the thread that controls the runtime. The application owns its
 * game-specific resources and releases them from {@link #close()}.
 */
public interface GameApplication extends AutoCloseable {
    /** Initializes application state before its first update or render. */
    void start();

    /**
     * Advances deterministic simulation by exactly one configured step.
     *
     * @param update immutable fixed-update state
     */
    void fixedUpdate(FixedUpdate update);

    /**
     * Advances frame-rate-dependent presentation state after all fixed updates.
     *
     * @param update immutable current-frame state
     */
    void update(FrameUpdate update);

    /**
     * Renders one frame using the most recently completed update state.
     *
     * @param update immutable current-frame state
     */
    void render(FrameUpdate update);

    /** Releases application-owned resources; implementations must be idempotent. */
    @Override
    void close();
}
