/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import java.util.Objects;

/** Renderer-produced authoring intent for one transform drag lifecycle phase. */
record SceneTransformEvent(
        Phase phase, String editId, ComponentIdentity identity, String propertyId, Vector3 value, long revision) {
    SceneTransformEvent {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(editId, "editId");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(propertyId, "propertyId");
        Objects.requireNonNull(value, "value");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    enum Phase {
        BEGIN,
        UPDATE,
        COMMIT,
        CANCEL,
        BLOCKED
    }
}
