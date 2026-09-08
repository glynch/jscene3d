/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.material;

/** Malformed Doom patch signal translated by the consuming importer. */
public final class DoomPatchDataException extends RuntimeException {
    /**
     * Creates a patch-data failure with a user-facing explanation.
     *
     * @param message failure explanation
     */
    public DoomPatchDataException(String message) {
        super(message);
    }
}
