/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.application;

import io.github.glynch.jscene3d.project.runtime.WorldModule;

/** World-scoped request seam between authored application behavior and its host. */
public interface ApplicationControl extends WorldModule {
    /**
     * Returns whether the host currently retains a gameplay world which can be resumed.
     *
     * @return whether Resume is meaningful
     */
    boolean canResume();

    /**
     * Requests one application transition after the current world update completes.
     *
     * @param command requested transition
     */
    void request(ApplicationCommand command);
}
