/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.materials.Material;
import java.util.Objects;

/**
 * Shared immutable-use runtime material resource.
 *
 * <p>{@link #owning(Material)} transfers exclusive ownership of the supplied open material. The caller must not retain
 * or mutate that material afterwards. Per-instance changes belong on presentation components rather than this shared
 * value.
 */
public final class Material3dResource implements AutoCloseable {
    private final Material material;

    /** Terminal resource state. */
    private boolean closed;

    /** Takes ownership of one validated material. */
    private Material3dResource(Material material) {
        this.material = Objects.requireNonNull(material, "material");
        if (material.isClosed()) {
            throw new IllegalArgumentException("material must be open");
        }
    }

    /**
     * Takes exclusive ownership of an open material as an immutable-use runtime resource.
     *
     * @param material material whose ownership transfers
     * @return new resource
     * @throws NullPointerException if {@code material} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     */
    public static Material3dResource owning(Material material) {
        return new Material3dResource(material);
    }

    /**
     * Returns whether this resource and its owned material are closed.
     *
     * @return {@code true} after terminal closure
     */
    public boolean isClosed() {
        return closed;
    }

    /** Closes the owned material exactly once; repeated calls have no effect. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        material.close();
    }

    /** Returns the internal material while this resource is open. */
    Material material() {
        requireOpen();
        return material;
    }

    /** Rejects internal access after terminal closure. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Material3dResource is closed");
        }
    }
}
