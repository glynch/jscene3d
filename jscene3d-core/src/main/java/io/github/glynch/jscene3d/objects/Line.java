/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;

/** A connected line strip that binds shared geometry to one line material. */
public class Line extends RenderableObject {
    private BufferGeometry geometry;
    private LineBasicMaterial material;
    private final boolean resourceReplacementAllowed;

    /**
     * Creates a connected line strip retaining shared geometry and material references.
     *
     * <p>Successive geometry elements form connected segments. Zero or one selected element draws
     * nothing.
     *
     * @param geometry open line geometry
     * @param material open line material
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is already closed
     */
    public Line(BufferGeometry geometry, LineBasicMaterial material) {
        this(geometry, material, true);
    }

    /** Retains shared line resources with a fixed replacement policy for library specializations. */
    Line(BufferGeometry geometry, LineBasicMaterial material, boolean resourceReplacementAllowed) {
        this.geometry = Preconditions.requireOpen(geometry, "geometry");
        this.material = Preconditions.requireOpen(material, "material");
        this.resourceReplacementAllowed = resourceReplacementAllowed;
    }

    /**
     * Returns the shared geometry.
     *
     * @return retained geometry
     * @throws IllegalStateException if the retained geometry is closed
     */
    public final BufferGeometry geometry() {
        if (geometry.isClosed()) {
            throw new IllegalStateException("Line geometry is closed");
        }
        return geometry;
    }

    /**
     * Replaces the shared geometry reference.
     *
     * @param geometry open line geometry
     * @throws NullPointerException if {@code geometry} is {@code null}
     * @throws IllegalArgumentException if {@code geometry} is closed
     * @throws UnsupportedOperationException if this line specialization owns its geometry
     */
    public final void setGeometry(BufferGeometry geometry) {
        requireResourceReplacementAllowed("geometry");
        this.geometry = Preconditions.requireOpen(geometry, "geometry");
    }

    /**
     * Returns the shared line material.
     *
     * @return retained line material
     * @throws IllegalStateException if the retained material is closed
     */
    public final LineBasicMaterial material() {
        if (material.isClosed()) {
            throw new IllegalStateException("Line material is closed");
        }
        return material;
    }

    /**
     * Replaces the shared line material reference.
     *
     * @param material open line material
     * @throws NullPointerException if {@code material} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     * @throws UnsupportedOperationException if this line specialization owns its material
     */
    public final void setMaterial(LineBasicMaterial material) {
        requireResourceReplacementAllowed("material");
        this.material = Preconditions.requireOpen(material, "material");
    }

    /** Rejects replacement for specialized line objects that own their generated resources. */
    private void requireResourceReplacementAllowed(String resourceName) {
        if (!resourceReplacementAllowed) {
            throw new UnsupportedOperationException(
                    getClass().getSimpleName() + " owns its " + resourceName + "; replacement is unsupported");
        }
    }
}
