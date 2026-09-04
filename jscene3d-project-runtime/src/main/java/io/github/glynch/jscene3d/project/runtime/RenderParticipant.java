/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.FrameUpdate;

/** Runtime object opting into rendering callbacks. */
public interface RenderParticipant {
    /**
     * Renders the current frame.
     *
     * @param update immutable current-frame timing state
     */
    void render(FrameUpdate update);
}
