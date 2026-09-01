/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.objects;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.internal.Preconditions;
import io.github.glynch.jscene3d.materials.Material;

/**
 * A triangular scene object that binds one buffer geometry to one material.
 *
 * <p>The class is extensible only for scene-object specializations that preserve this resource
 * binding contract, such as {@link SkinnedMesh}.
 */
public class Mesh extends Object3D {
    private BufferGeometry geometry;
    private Material material;
    private boolean shadowCastingEnabled;
    private boolean shadowReceivingEnabled;

    /**
     * Creates a mesh retaining shared geometry and material references.
     *
     * @param geometry open triangle geometry
     * @param material open surface material
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if an argument is already closed
     */
    public Mesh(BufferGeometry geometry, Material material) {
        this.geometry = Preconditions.requireOpen(geometry, "geometry");
        this.material = Preconditions.requireOpen(material, "material");
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
        this.geometry = Preconditions.requireOpen(geometry, "geometry");
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
        this.material = Preconditions.requireOpen(material, "material");
    }

    /**
     * Returns whether this mesh participates in shadow-map depth passes.
     *
     * @return {@code false} by default
     */
    public boolean isShadowCastingEnabled() {
        return shadowCastingEnabled;
    }

    /**
     * Changes whether this mesh participates in shadow-map depth passes.
     *
     * @param enabled whether this mesh casts shadows from shadow-enabled lights
     */
    public void setShadowCastingEnabled(boolean enabled) {
        shadowCastingEnabled = enabled;
    }

    /**
     * Returns whether lit materials on this mesh sample generated shadow maps.
     *
     * @return {@code false} by default
     */
    public boolean isShadowReceivingEnabled() {
        return shadowReceivingEnabled;
    }

    /**
     * Changes whether lit materials on this mesh sample generated shadow maps.
     *
     * @param enabled whether this mesh receives shadows from shadow-enabled lights
     */
    public void setShadowReceivingEnabled(boolean enabled) {
        shadowReceivingEnabled = enabled;
    }
}
