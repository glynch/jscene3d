/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

/** Application callback invoked immediately before or after one renderable-object draw. */
@FunctionalInterface
public interface RenderCallback {
    /**
     * Handles one object draw at its documented lifecycle point.
     *
     * <p>Callback exceptions propagate from the renderer and abort the active frame. Callbacks run
     * on the caller's render thread and must not attempt to render recursively.
     *
     * @param context immutable description of the selected draw
     */
    void invoke(RenderContext context);
}
