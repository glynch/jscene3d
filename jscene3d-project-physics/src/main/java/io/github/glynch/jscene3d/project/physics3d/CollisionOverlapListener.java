/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/** Synchronous sink used by a physics adapter to deliver sensor overlap transitions. */
public interface CollisionOverlapListener {
    /**
     * Delivers one newly established precise shape overlap.
     *
     * @param overlap overlap relationship
     */
    void onEntered(CollisionOverlap3d overlap);

    /**
     * Delivers one precise shape overlap which ended.
     *
     * @param overlap previous overlap relationship
     */
    void onExited(CollisionOverlap3d overlap);
}
