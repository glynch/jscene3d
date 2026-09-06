/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/**
 * World-scoped physics participation invoked between the fixed before-physics and after-physics phases.
 *
 * <p>A host binds a backend-specific interface which extends this seam. The world discovers participation from this
 * world-module capability while component scheduling remains controlled exclusively by component descriptors. More
 * than one physics module may participate; invocation follows host binding order. Implementations run on the world's
 * logical simulation thread and must not perform blocking resource I/O.
 */
public interface PhysicsStepWorldModule extends WorldModule {
    /**
     * Advances physics and delivers any resulting physics signals for one fixed step.
     *
     * @param update immutable world-owned fixed-step context
     */
    void stepPhysics(FixedUpdateContext update);
}
