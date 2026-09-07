/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import io.github.glynch.jscene3d.project.runtime.WorldModule;

/** Read-only semantic input available to components through their world. */
public interface InputWorldModule extends WorldModule {
    /** Returns the semantic input snapshot published for the current update.
     *
     * @return current immutable semantic snapshot
     */
    ActionSnapshot snapshot();
}
