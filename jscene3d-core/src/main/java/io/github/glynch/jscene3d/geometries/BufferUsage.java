/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

/** Describes the expected mutation frequency of geometry data. */
public enum BufferUsage {
    /** Data is expected to remain unchanged after initial upload. */
    STATIC,

    /** Data is expected to change repeatedly. */
    DYNAMIC,

    /** Data is expected to change for nearly every use. */
    STREAM
}
