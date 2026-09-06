/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/**
 * Category and mask bits controlling which authored collision shapes may interact.
 *
 * @param categoryBits categories occupied by the shape
 * @param maskBits categories with which the shape permits interaction
 */
public record CollisionFilter3d(int categoryBits, int maskBits) {
    /** Default filter: category one, interacting with every category. */
    public static final CollisionFilter3d DEFAULT = new CollisionFilter3d(1, -1);

    /**
     * Returns whether this filter and another filter mutually permit interaction.
     *
     * @param other filter to test
     * @return {@code true} when both masks permit the other category
     */
    public boolean matches(CollisionFilter3d other) {
        return (maskBits & other.categoryBits) != 0 && (other.maskBits & categoryBits) != 0;
    }
}
