/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;

/**
 * One opaque entry in a WAD directory; duplicate names remain distinct by index.
 *
 * @param index zero-based directory index
 * @param name normalized printable ASCII name of at most eight characters
 * @param offset unsigned source offset represented as a non-negative long
 * @param size entry size in bytes
 */
public record WadLump(int index, String name, long offset, int size) {
    /** Creates validated opaque directory metadata. */
    public WadLump {
        index = Preconditions.requireNonNegative(index, "index");
        name = Preconditions.requireLumpName(name, "name");
        offset = Preconditions.requireNonNegative(offset, "offset");
        size = Preconditions.requireNonNegative(size, "size");
    }
}
