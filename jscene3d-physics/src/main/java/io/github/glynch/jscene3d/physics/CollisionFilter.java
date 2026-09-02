/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

/**
 * Category and mask bits controlling which colliders may interact.
 *
 * @param categoryBits categories occupied by the collider
 * @param maskBits categories with which the collider permits interaction
 */
public record CollisionFilter(int categoryBits, int maskBits) {
    /** Default filter: category one, interacting with every category. */
    public static final CollisionFilter DEFAULT = new CollisionFilter(1, -1);

    /**
     * Returns whether this filter and another filter mutually permit interaction.
     *
     * @param other filter to test
     * @return {@code true} when both masks permit the other category
     */
    public boolean matches(CollisionFilter other) {
        return (maskBits & other.categoryBits) != 0 && (other.maskBits & categoryBits) != 0;
    }
}
