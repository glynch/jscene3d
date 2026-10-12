/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import java.util.Objects;
import java.util.Optional;

/** One renderer-owned pointer decision and its optional authoritative edit intent. */
record SceneTransformPointerOutcome(boolean claimed, Optional<SceneTransformEvent> event) {
    SceneTransformPointerOutcome {
        Objects.requireNonNull(event, "event");
    }

    static SceneTransformPointerOutcome unclaimed() {
        return new SceneTransformPointerOutcome(false, Optional.empty());
    }

    static SceneTransformPointerOutcome claimed(Optional<SceneTransformEvent> event) {
        return new SceneTransformPointerOutcome(true, event);
    }
}
