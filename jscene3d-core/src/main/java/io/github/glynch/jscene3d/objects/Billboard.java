/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.PlaneGeometry;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * An unlit rectangular scene object oriented toward the active camera by the renderer.
 *
 * <p>The generated unit quad lies in the local XY plane. Inherited X and Y scale therefore define
 * its world-space width and height. Its inherited orientation is intentionally ignored because
 * {@link #alignment()} determines the rendered orientation. The material is shared and remains
 * caller-owned; this object owns only its generated geometry.
 */
public final class Billboard extends RenderableObject implements AutoCloseable {
    private final BufferGeometry geometry;
    private final Vector2f anchor;

    private BasicMaterial material;
    private BillboardAlignment alignment;

    /**
     * Creates a centred spherical billboard retaining a shared unlit material.
     *
     * @param material open material retained without taking ownership
     * @throws NullPointerException if {@code material} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     */
    public Billboard(BasicMaterial material) {
        this.material = Preconditions.requireOpen(material, "material");
        geometry = PlaneGeometry.create(1.0f, 1.0f);
        anchor = new Vector2f(0.5f);
        alignment = BillboardAlignment.SPHERICAL;
    }

    /**
     * Returns the generated unit-quad geometry used for this billboard's draw.
     *
     * <p>The geometry is owned by this billboard and must not be closed independently.
     *
     * @return open generated geometry
     * @throws IllegalStateException if this billboard is closed
     */
    public BufferGeometry geometry() {
        if (geometry.isClosed()) {
            throw new IllegalStateException("Billboard is closed");
        }
        return geometry;
    }

    /**
     * Returns the shared unlit material.
     *
     * @return retained material
     * @throws IllegalStateException if this billboard or its material is closed
     */
    public BasicMaterial material() {
        geometry();
        if (material.isClosed()) {
            throw new IllegalStateException("Billboard material is closed");
        }
        return material;
    }

    /**
     * Replaces the shared unlit material without transferring ownership.
     *
     * @param material open replacement material
     * @throws NullPointerException if {@code material} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     * @throws IllegalStateException if this billboard is closed
     */
    public void setMaterial(BasicMaterial material) {
        geometry();
        this.material = Preconditions.requireOpen(material, "material");
    }

    /**
     * Returns how this billboard faces the active camera.
     *
     * @return current alignment, initially {@link BillboardAlignment#SPHERICAL}
     * @throws IllegalStateException if this billboard is closed
     */
    public BillboardAlignment alignment() {
        geometry();
        return alignment;
    }

    /**
     * Changes how this billboard faces the active camera.
     *
     * @param alignment replacement alignment
     * @throws NullPointerException if {@code alignment} is {@code null}
     * @throws IllegalStateException if this billboard is closed
     */
    public void setAlignment(BillboardAlignment alignment) {
        geometry();
        this.alignment = Objects.requireNonNull(alignment, "alignment");
    }

    /**
     * Returns the stable live view of the local anchor coordinates.
     *
     * <p>An anchor of {@code (0, 0)} selects the lower-left corner, {@code (0.5, 0.5)} selects the
     * centre, and {@code (1, 1)} selects the upper-right corner.
     *
     * @return current anchor, initially {@code (0.5, 0.5)}
     * @throws IllegalStateException if this billboard is closed
     */
    public Vector2fc anchor() {
        geometry();
        return anchor;
    }

    /**
     * Changes the local point held at the billboard's world position.
     *
     * @param x finite horizontal anchor coordinate
     * @param y finite vertical anchor coordinate
     * @throws IllegalArgumentException if either coordinate is not finite
     * @throws IllegalStateException if this billboard is closed
     */
    public void setAnchor(float x, float y) {
        geometry();
        anchor.set(Preconditions.requireFinite(x, "x"), Preconditions.requireFinite(y, "y"));
    }

    /**
     * Copies a local anchor point.
     *
     * @param anchor finite anchor to copy
     * @throws NullPointerException if {@code anchor} is {@code null}
     * @throws IllegalArgumentException if either coordinate is not finite
     * @throws IllegalStateException if this billboard is closed
     */
    public void setAnchor(Vector2fc anchor) {
        Vector2fc validAnchor = Preconditions.requireFinite(anchor, "anchor");
        setAnchor(validAnchor.x(), validAnchor.y());
    }

    /**
     * Returns whether this billboard's generated geometry has been closed.
     *
     * @return whether terminal closure has occurred
     */
    public boolean isClosed() {
        return geometry.isClosed();
    }

    /** Closes the generated geometry without closing the shared material. */
    @Override
    public void close() {
        geometry.close();
    }
}
