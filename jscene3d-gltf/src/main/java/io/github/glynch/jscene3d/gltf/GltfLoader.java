/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads glTF 2.0 JSON and GLB assets into JScene3D-owned scene resources.
 *
 * <p>The initial capability profile supports the selected scene, node TRS transforms, triangle
 * primitives, indices, positions, normals, primary texture coordinates, RGB/RGBA vertex colours,
 * metallic-roughness materials, PNG/JPEG images, core glTF sampler state, skeletal skinning,
 * {@code KHR_draco_mesh_compression}, and node translation, rotation, and scale animation using
 * step, linear, or cubic-spline interpolation. Other required extensions, morph targets, cameras,
 * secondary texture-coordinate selection, and non-triangle primitives fail with a diagnostic
 * {@link GltfLoadException}.
 */
public final class GltfLoader {
    /** Prevents instantiation of this stateless loader. */
    private GltfLoader() {
        throw new AssertionError("GltfLoader cannot be instantiated");
    }

    /**
     * Loads a glTF 2.0 JSON or GLB file and all referenced resources synchronously.
     *
     * @param source glTF or GLB source path
     * @return closeable owner of the converted default scene and its resources
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws GltfLoadException if reading or conversion fails
     */
    public static LoadedGltf load(Path source) {
        Path validSource = Objects.requireNonNull(source, "source");
        try {
            return GltfConverter.load(validSource);
        } catch (GltfLoadException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new GltfLoadException(validSource, "Failed to load glTF: " + validSource, exception);
        }
    }
}
