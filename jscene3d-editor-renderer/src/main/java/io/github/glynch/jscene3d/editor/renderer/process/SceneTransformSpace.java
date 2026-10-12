/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

/** Coordinate frame used by the Scene View transform gizmo. */
enum SceneTransformSpace {
    /** World-aligned transform axes. */
    GLOBAL,
    /** Selected-entity orientation axes. */
    LOCAL
}
