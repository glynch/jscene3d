/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

/** One example whose lifecycle can be hosted standalone or inside the native example browser. */
public interface HostedExample extends AutoCloseable {
    /** Responds after the host changes the drawable content area's dimensions. */
    void resize();

    /**
     * Advances interaction and animation for one event-polling cycle.
     *
     * @param frame immutable current-frame state
     */
    void update(ExampleFrame frame);

    /** Draws the current frame without swapping the owning window's buffers. */
    void render();

    /**
     * Draws the representative scene used for catalogue thumbnail capture.
     *
     * <p>The default preserves the complete rendered example. Implementations with large
     * interactive overlays may omit those overlays while retaining the same scene state.
     */
    default void renderThumbnail() {
        render();
    }

    /** Releases every geometry, material, texture, helper, and other owned resource. */
    @Override
    void close();
}
