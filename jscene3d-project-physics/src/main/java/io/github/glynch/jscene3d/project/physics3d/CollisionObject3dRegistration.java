/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/** World-module-owned backend registration controlled by one collision-object component lifecycle. */
public interface CollisionObject3dRegistration extends AutoCloseable {
    /**
     * Enables or disables physics participation.
     *
     * @param enabled desired participation
     * @throws IllegalStateException if this registration is closed
     */
    void setEnabled(boolean enabled);

    /**
     * Returns whether this registration currently participates.
     *
     * @return whether this registration currently participates
     */
    boolean isEnabled();

    /**
     * Returns whether this registration has been released.
     *
     * @return whether this registration has been released
     */
    boolean isClosed();

    /** Removes this collision object and all of its shapes exactly once. */
    @Override
    void close();
}
