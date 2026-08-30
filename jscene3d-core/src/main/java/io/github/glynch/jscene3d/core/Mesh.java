/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import java.util.Objects;

/** A triangular scene object that binds one buffer geometry to one material. */
public final class Mesh extends Object3D {
    private BufferGeometry geometry;
    private Material material;

    /**
     * Creates a mesh retaining shared geometry and material references.
     *
     * @param geometry open triangle geometry
     * @param material open surface material
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is already closed
     */
    public Mesh(BufferGeometry geometry, Material material) {
        BufferGeometry validGeometry = Objects.requireNonNull(geometry, "geometry");
        Material validMaterial = Objects.requireNonNull(material, "material");
        if (validGeometry.isClosed()) {
            throw new IllegalArgumentException("geometry must be open");
        }
        if (validMaterial.isClosed()) {
            throw new IllegalArgumentException("material must be open");
        }
        this.geometry = validGeometry;
        this.material = validMaterial;
    }

    /**
     * Returns the shared geometry.
     *
     * @return the retained geometry
     * @throws IllegalStateException if the retained geometry is closed
     */
    public BufferGeometry geometry() {
        if (geometry.isClosed()) {
            throw new IllegalStateException("Mesh geometry is closed");
        }
        return geometry;
    }

    /**
     * Replaces the shared geometry reference.
     *
     * @param geometry open triangle geometry
     * @throws NullPointerException if {@code geometry} is {@code null}
     * @throws IllegalArgumentException if {@code geometry} is closed
     */
    public void setGeometry(BufferGeometry geometry) {
        BufferGeometry validGeometry = Objects.requireNonNull(geometry, "geometry");
        if (validGeometry.isClosed()) {
            throw new IllegalArgumentException("geometry must be open");
        }
        this.geometry = validGeometry;
    }

    /**
     * Returns the shared material.
     *
     * @return the retained material
     * @throws IllegalStateException if the retained material is closed
     */
    public Material material() {
        if (material.isClosed()) {
            throw new IllegalStateException("Mesh material is closed");
        }
        return material;
    }

    /**
     * Replaces the shared material reference.
     *
     * @param material open surface material
     * @throws NullPointerException if {@code material} is {@code null}
     * @throws IllegalArgumentException if {@code material} is closed
     */
    public void setMaterial(Material material) {
        Material validMaterial = Objects.requireNonNull(material, "material");
        if (validMaterial.isClosed()) {
            throw new IllegalArgumentException("material must be open");
        }
        this.material = validMaterial;
    }
}
