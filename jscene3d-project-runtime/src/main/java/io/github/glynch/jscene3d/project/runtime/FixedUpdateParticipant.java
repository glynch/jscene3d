/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.game.FixedUpdate;

/** Runtime object participating in one deterministic fixed-update phase. */
public interface FixedUpdateParticipant {
    /**
     * Returns the fixed-update phase selected during runtime composition.
     *
     * <p>The phase must remain constant for the lifetime of the runtime object.
     *
     * @return fixed-update phase
     */
    FixedUpdatePhase fixedUpdatePhase();

    /**
     * Advances deterministic state by one fixed step.
     *
     * @param update immutable fixed-step timing state
     */
    void fixedUpdate(FixedUpdate update);
}
