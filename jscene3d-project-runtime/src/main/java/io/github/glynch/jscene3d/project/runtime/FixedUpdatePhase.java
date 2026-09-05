/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Deterministic stage within one project-runtime fixed update. */
public enum FixedUpdatePhase {
    /** Applies game decisions, controls, forces, and requested body motion. */
    BEFORE_PHYSICS,

    /** Advances physics worlds and resolves contacts and overlaps. */
    PHYSICS,

    /** Synchronizes simulation results and publishes resulting events. */
    AFTER_PHYSICS
}
