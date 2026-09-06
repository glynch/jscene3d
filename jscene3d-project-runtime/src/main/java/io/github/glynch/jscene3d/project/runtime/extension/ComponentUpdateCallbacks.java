/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.runtime.FixedUpdateContext;
import io.github.glynch.jscene3d.project.runtime.FrameUpdateContext;

/**
 * Optional deterministic update callbacks implemented by an ordinary runtime component.
 *
 * <p>The component descriptor remains authoritative. The world invokes only phases declared by the exact descriptor,
 * even when the runtime value overrides additional methods. Callbacks run synchronously on the caller-owned logical
 * simulation thread and must not recursively advance or close their world.
 */
public interface ComponentUpdateCallbacks {
    /**
     * Runs fixed-step behavior before the engine-owned physics phase.
     *
     * @param update current fixed-update timing
     */
    default void onBeforePhysics(FixedUpdateContext update) {}

    /**
     * Runs fixed-step behavior after physics state and collision signals are current.
     *
     * @param update current fixed-update timing
     */
    default void onAfterPhysics(FixedUpdateContext update) {}

    /**
     * Runs presentation-only behavior once for a rendered frame.
     *
     * @param update current presentation timing
     */
    default void onFrameUpdate(FrameUpdateContext update) {}
}
