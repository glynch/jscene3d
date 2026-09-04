/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.FixedUpdate;

/** Runtime object opting into deterministic fixed updates. */
public interface FixedUpdateParticipant {
    /**
     * Advances deterministic state by one fixed step.
     *
     * @param update immutable fixed-step timing state
     */
    void fixedUpdate(FixedUpdate update);
}
