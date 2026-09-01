/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import io.github.glynch.jscene3d.animation.AnimationClip;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.scenes.Scene;
import io.github.glynch.jscene3d.textures.Texture;
import java.util.List;
import java.util.Objects;

/**
 * One loaded scene and the geometry, material, and texture resources that it owns.
 *
 * <p>Closing is terminal and idempotent. The scene graph itself is lightweight and is not closed;
 * its renderable resources become unavailable when this owner closes.
 */
public final class LoadedGltf implements AutoCloseable {
    private final Scene scene;
    private final List<AnimationClip> animations;
    private final List<BufferGeometry> geometries;
    private final List<Material> materials;
    private final List<Texture> textures;
    private boolean closed;

    /** Retains converted resources created exclusively for this loaded asset. */
    LoadedGltf(
            Scene scene,
            List<AnimationClip> animations,
            List<BufferGeometry> geometries,
            List<Material> materials,
            List<Texture> textures) {
        this.scene = Objects.requireNonNull(scene, "scene");
        this.animations = List.copyOf(Objects.requireNonNull(animations, "animations"));
        this.geometries = List.copyOf(Objects.requireNonNull(geometries, "geometries"));
        this.materials = List.copyOf(Objects.requireNonNull(materials, "materials"));
        this.textures = List.copyOf(Objects.requireNonNull(textures, "textures"));
    }

    /**
     * Returns the converted default scene.
     *
     * @return retained scene
     * @throws IllegalStateException if this asset is closed
     */
    public Scene scene() {
        requireOpen();
        return scene;
    }

    /**
     * Returns animation clips bound to nodes in the retained scene.
     *
     * <p>The immutable list and clips remain usable until this loaded asset closes. The clips are
     * renderer-independent and are advanced explicitly through a caller-owned animation mixer.
     *
     * @return immutable animation clips in source order
     * @throws IllegalStateException if this asset is closed
     */
    public List<AnimationClip> animations() {
        requireOpen();
        return animations;
    }

    /**
     * Returns whether this asset and all its owned render resources are closed.
     *
     * @return {@code true} after the first close
     */
    public boolean isClosed() {
        return closed;
    }

    /** Closes textures, materials, and geometries once. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        textures.forEach(Texture::close);
        materials.forEach(Material::close);
        geometries.forEach(BufferGeometry::close);
        closed = true;
    }

    /** Rejects access after terminal closure. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Loaded glTF is closed");
        }
    }
}
