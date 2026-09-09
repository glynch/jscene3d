/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.presentation;

/** World-owned listener-relative sound which can be retriggered from its beginning. */
public interface LocalSound extends AutoCloseable {
    /** Restarts playback from the beginning, including while an earlier playback remains active. */
    void restart();

    /** Releases this sound's world-owned playback source. Repeated closure is harmless. */
    @Override
    void close();
}
