/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import io.github.glynch.jscene3d.internal.Preconditions;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Named position and optional normal displacement data for one geometry shape.
 *
 * <p>Target attributes contain three-component displacements with one item per base vertex. The
 * attributes remain application-owned and mutable; a geometry retains their identities without
 * taking lifecycle ownership.
 */
public final class MorphTarget {
    private final String name;
    private final BufferAttribute positions;
    private final @Nullable BufferAttribute normals;

    /**
     * Creates a position-only displacement target.
     *
     * @param name non-blank display and lookup name
     * @param positions three-component position displacements
     */
    public MorphTarget(String name, BufferAttribute positions) {
        this(name, positions, null);
    }

    /**
     * Creates one displacement target.
     *
     * @param name non-blank display and lookup name
     * @param positions three-component position displacements
     * @param normals optional three-component normal displacements
     * @throws NullPointerException if {@code name} or {@code positions} is {@code null}
     * @throws IllegalArgumentException if the name is blank, an attribute does not have three
     *     components, or the attribute counts differ
     */
    public MorphTarget(String name, BufferAttribute positions, @Nullable BufferAttribute normals) {
        this.name = Preconditions.requireNonBlank(name, "name");
        this.positions = requireThreeComponents(Objects.requireNonNull(positions, "positions"), "positions");
        this.normals = normals == null ? null : requireThreeComponents(normals, "normals");
        if (normals != null && normals.count() != positions.count()) {
            throw new IllegalArgumentException(
                    "morph normal count must equal position count: " + normals.count() + " != " + positions.count());
        }
    }

    /**
     * Returns the target name.
     *
     * @return non-blank name
     */
    public String name() {
        return name;
    }

    /**
     * Returns position displacements.
     *
     * @return retained three-component attribute
     */
    public BufferAttribute positions() {
        return positions;
    }

    /**
     * Returns optional normal displacements.
     *
     * @return retained normal attribute, or an empty value
     */
    public Optional<BufferAttribute> normals() {
        return Optional.ofNullable(normals);
    }

    /** Validates one displacement attribute. */
    private static BufferAttribute requireThreeComponents(BufferAttribute attribute, String label) {
        if (attribute.itemSize() != 3) {
            throw new IllegalArgumentException(label + " itemSize must be 3: " + attribute.itemSize());
        }
        return attribute;
    }
}
