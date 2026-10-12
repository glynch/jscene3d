/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

/** Active Scene View entity tool understood by the renderer-owned transform gizmo. */
enum SceneTransformTool {
    /** Selection only; no transform overlay. */
    SELECT,
    /** Position editing. */
    MOVE,
    /** Euler-orientation editing. */
    ROTATE,
    /** Local-scale editing. */
    SCALE
}
