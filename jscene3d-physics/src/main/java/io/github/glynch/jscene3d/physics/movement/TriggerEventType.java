/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

/** Lifecycle transition for a trigger overlap observed after an explicit move. */
public enum TriggerEventType {
    /** The moving collider newly overlaps the trigger. */
    ENTER,
    /** The moving collider continues to overlap the trigger. */
    STAY,
    /** The moving collider no longer overlaps the trigger. */
    EXIT
}
