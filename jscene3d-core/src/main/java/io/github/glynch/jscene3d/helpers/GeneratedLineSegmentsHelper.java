/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.helpers;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.objects.LineSegments;

/** Shared owned-resource implementation for fixed generated line-segment helpers. */
abstract class GeneratedLineSegmentsHelper extends LineSegments implements AutoCloseable {
    private final BufferGeometry ownedGeometry;
    private final LineBasicMaterial ownedMaterial;

    /** Retains generated geometry and creates its owned vertex-color material. */
    GeneratedLineSegmentsHelper(BufferGeometry geometry) {
        this(geometry, createVertexColorMaterial());
    }

    /** Retains generated geometry and a supplied owned material. */
    GeneratedLineSegmentsHelper(BufferGeometry geometry, LineBasicMaterial material) {
        super(geometry, material, false);
        ownedGeometry = geometry;
        ownedMaterial = material;
    }

    /** Retains a generated owned-resource pair assembled atomically by a specialization. */
    GeneratedLineSegmentsHelper(OwnedResources resources) {
        this(resources.geometry(), resources.material());
    }

    /**
     * Returns whether either owned resource has been closed.
     *
     * @return {@code true} when this helper is no longer renderable
     */
    public final boolean isClosed() {
        return ownedGeometry.isClosed() || ownedMaterial.isClosed();
    }

    /** Closes the generated geometry and material. Repeated closure is a no-op. */
    @Override
    public final void close() {
        ownedGeometry.close();
        ownedMaterial.close();
    }

    /** Creates the common white material that reveals generated vertex colors. */
    private static LineBasicMaterial createVertexColorMaterial() {
        LineBasicMaterial material = new LineBasicMaterial();
        material.setUsesVertexColors(true);
        return material;
    }

    /** Geometry and material created together after specialization-specific validation. */
    record OwnedResources(BufferGeometry geometry, LineBasicMaterial material) {}
}
