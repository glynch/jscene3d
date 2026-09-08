/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.materials.Material;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Shared immutable-use runtime material resource.
 *
 * <p>{@link #owning(Material)} transfers exclusive ownership of the supplied open material. The caller must not retain
 * or mutate that material afterwards. Per-instance changes belong on presentation components rather than this shared
 * value.
 */
public final class Material3dResource implements AutoCloseable {
    private final Material material;
    private final List<? extends AutoCloseable> dependencies;

    /** Terminal resource state. */
    private boolean closed;

    /** Takes ownership of one validated material. */
    private Material3dResource(Material material, List<? extends AutoCloseable> dependencies) {
        this.material = Objects.requireNonNull(material, "material");
        this.dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
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
        return new Material3dResource(material, List.of());
    }

    /** Takes ownership of one material and the nested resource leases which keep its dependencies alive. */
    static Material3dResource owning(Material material, List<? extends AutoCloseable> dependencies) {
        return new Material3dResource(material, dependencies);
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
        @Nullable RuntimeException failure = null;
        try {
            material.close();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        for (int index = dependencies.size() - 1; index >= 0; index--) {
            try {
                dependencies.get(index).close();
            } catch (RuntimeException exception) {
                failure = retainFailure(failure, exception);
            } catch (Exception exception) {
                failure = retainFailure(
                        failure, new IllegalStateException("Unable to close material dependency", exception));
            }
        }
        if (failure != null) {
            throw failure;
        }
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

    /** Retains the first cleanup failure and suppresses every later failure. */
    private static RuntimeException retainFailure(@Nullable RuntimeException retained, RuntimeException next) {
        if (retained == null) {
            return next;
        }
        retained.addSuppressed(next);
        return retained;
    }
}
