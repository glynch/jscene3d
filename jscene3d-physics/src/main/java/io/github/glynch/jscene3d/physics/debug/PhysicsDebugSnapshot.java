/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.debug;

import java.util.List;

/** Immutable line representation of the colliders in a physics world at one instant. */
public final class PhysicsDebugSnapshot {
    private final List<PhysicsDebugLine> lines;

    /** Creates a snapshot by copying the line list.
     * @param lines line segments to retain
     */
    public PhysicsDebugSnapshot(List<PhysicsDebugLine> lines) {
        this.lines = List.copyOf(lines);
    }

    /** Returns deterministic shape lines ordered by collider identifier.
     * @return immutable line list
     */
    public List<PhysicsDebugLine> lines() {
        return lines;
    }
}
