/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Shared owned-resource implementation for fixed generated line-segment helpers. */
abstract class GeneratedLineSegmentsHelper extends LineSegments implements AutoCloseable {
    private final BufferGeometry ownedGeometry;
    private final LineBasicMaterial ownedMaterial;

    /** Retains generated geometry and creates its owned vertex-color material. */
    GeneratedLineSegmentsHelper(BufferGeometry geometry) {
        super(geometry, createMaterial(), false);
        ownedGeometry = geometry;
        ownedMaterial = material();
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
    private static LineBasicMaterial createMaterial() {
        LineBasicMaterial material = new LineBasicMaterial();
        material.setUsesVertexColors(true);
        return material;
    }
}
