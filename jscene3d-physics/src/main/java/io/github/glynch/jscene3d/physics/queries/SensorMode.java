/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

/** Controls whether a spatial query considers collision sensors. */
public enum SensorMode {
    /** Ignore collision sensors. */
    EXCLUDE,
    /** Include both solid bodies and collision sensors. */
    INCLUDE,
    /** Consider only collision sensors. */
    ONLY
}
