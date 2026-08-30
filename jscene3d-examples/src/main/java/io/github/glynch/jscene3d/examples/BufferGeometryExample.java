/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BoxGeometry;
import io.github.glynch.jscene3d.core.BufferAttribute;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.IndexBuffer;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.PlaneGeometry;
import io.github.glynch.jscene3d.core.SphereGeometry;
import java.util.Objects;

/** Demonstrates buffer geometry, scoped edits, built-in factories, and mesh binding. */
public final class BufferGeometryExample {
    /** Prevents instantiation of this example entry point. */
    private BufferGeometryExample() {
        throw new AssertionError("BufferGeometryExample cannot be instantiated");
    }

    /**
     * Builds a custom triangle and the initial built-in geometry values.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        try (BufferGeometry triangle = createTriangle();
                BufferGeometry plane = PlaneGeometry.create(4.0f, 2.0f);
                BufferGeometry box = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BufferGeometry sphere = SphereGeometry.create(1.0f);
                BasicMaterial material = new BasicMaterial(Color.BLUE)) {
            Mesh triangleMesh = new Mesh(triangle, material);
            BufferAttribute positions = Objects.requireNonNull(triangle.attribute(BufferGeometry.POSITION));
            positions.edit(editor -> {
                editor.setXYZ(0, -1.0f, -0.5f, 0.0f);
                editor.setXYZ(1, 1.0f, -0.5f, 0.0f);
                editor.setXYZ(2, 0.0f, 1.0f, 0.0f);
            });
            triangle.computeBoundingBox();
            triangle.computeBoundingSphere();

            if (positions.version() != 1L
                    || triangleMesh.geometry() != triangle
                    || triangleMesh.material() != material
                    || plane.vertexCount() != 4
                    || box.vertexCount() != 24
                    || sphere.vertexCount() != 561) {
                throw new IllegalStateException("Unexpected geometry data");
            }
        }
    }

    /** Creates one indexed triangle with per-vertex colors using the low-level builder. */
    private static BufferGeometry createTriangle() {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(
                BufferGeometry.POSITION,
                BufferAttribute.of(new float[] {-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f}, 3));
        geometry.setIndex(IndexBuffer.of(new int[] {0, 1, 2}));
        return geometry;
    }
}
