/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

import org.joml.Vector3fc;

/** World-positioned sound which can be moved and retriggered independently. */
public interface PositionalSound extends AutoCloseable {
    /**
     * Moves the source to the supplied world position and restarts playback from the beginning.
     *
     * @param position finite listener-compatible world position
     */
    void restart(Vector3fc position);

    /** Releases this sound's world-owned playback source. Repeated closure is harmless. */
    @Override
    void close();
}
