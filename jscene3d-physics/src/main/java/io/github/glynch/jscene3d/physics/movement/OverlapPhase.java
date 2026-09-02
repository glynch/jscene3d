/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.movement;

/** The relationship between a moving body and a collision sensor after one move. */
public enum OverlapPhase {
    /** The body began overlapping the sensor. */
    ENTER,
    /** The body remains inside the sensor. */
    STAY,
    /** The body stopped overlapping the sensor. */
    EXIT
}
