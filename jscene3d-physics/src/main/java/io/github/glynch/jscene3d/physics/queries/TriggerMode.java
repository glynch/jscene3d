/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.queries;

/** Controls whether a query considers trigger colliders. */
public enum TriggerMode {
    /** Ignore trigger colliders. */
    EXCLUDE,
    /** Include both solid and trigger colliders. */
    INCLUDE,
    /** Consider only trigger colliders. */
    ONLY
}
