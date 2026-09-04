/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.FrameUpdate;

/** Runtime object opting into frame-rate-dependent updates. */
public interface FrameUpdateParticipant {
    /**
     * Advances frame-rate-dependent state.
     *
     * @param update immutable current-frame timing state
     */
    void update(FrameUpdate update);
}
