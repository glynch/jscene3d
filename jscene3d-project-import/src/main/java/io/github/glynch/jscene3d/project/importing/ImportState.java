/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

/** Persistent state of one import definition relative to its published cache. */
public enum ImportState {
    /** A complete published generation matches every current input. */
    CURRENT,
    /** No complete published generation exists. */
    MISSING,
    /** A prior complete generation exists but its inputs no longer match. */
    STALE,
    /** Current state cannot be evaluated because required input or implementation is unavailable. */
    BLOCKED
}
