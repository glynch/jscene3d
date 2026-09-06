/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

/** Component-visible update phases in the deterministic world schedule. */
public enum ComponentUpdatePhase {
    /** Fixed-step behavior before the engine-owned physics phase. */
    BEFORE_PHYSICS,
    /** Fixed-step behavior after physics synchronization and signal delivery. */
    AFTER_PHYSICS,
    /** Presentation behavior once per rendered frame. */
    FRAME_UPDATE
}
