/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

/** Container identity declared by a WAD header without assigning gameplay meaning. */
public enum WadKind {
    /** A complete independently identified archive. */
    IWAD,

    /** An archive conventionally applied after another archive. */
    PWAD
}
