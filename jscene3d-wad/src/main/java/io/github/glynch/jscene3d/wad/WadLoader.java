/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;
import io.github.glynch.jscene3d.wad.internal.WadDecoder;
import java.nio.file.Path;

/** Entry point for validating and indexing WAD archives without interpreting lump content. */
public final class WadLoader {
    /** Prevents instantiation of this stateless loader. */
    private WadLoader() {
        throw new AssertionError("WadLoader cannot be instantiated");
    }

    /**
     * Validates and indexes one WAD while recording its complete source fingerprint.
     *
     * @param source source WAD path
     * @return immutable archive or ordered diagnostics
     */
    public static WadLoadResult load(Path source) {
        return WadDecoder.load(source);
    }

    /**
     * Validates and indexes one WAD only when its complete source fingerprint matches.
     *
     * @param source source WAD path
     * @param expectedSha256 required hexadecimal SHA-256 fingerprint
     * @return immutable archive or ordered diagnostics
     */
    public static WadLoadResult load(Path source, String expectedSha256) {
        return WadDecoder.load(source, Preconditions.requireSha256(expectedSha256, "expectedSha256"));
    }
}
