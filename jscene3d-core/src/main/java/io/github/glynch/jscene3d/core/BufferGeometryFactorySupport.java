/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

/** Shared assembly for built-in indexed triangle geometry factories. */
final class BufferGeometryFactorySupport {
    private BufferGeometryFactorySupport() {
        throw new AssertionError("BufferGeometryFactorySupport cannot be instantiated");
    }

    static BufferGeometry create(float[] positions, float[] normals, float[] textureCoordinates, int[] indices) {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(positions, 3));
        geometry.setAttribute(BufferGeometry.NORMAL, BufferAttribute.of(normals, 3));
        geometry.setAttribute(BufferGeometry.UV, BufferAttribute.of(textureCoordinates, 2));
        geometry.setIndex(IndexBuffer.of(indices));
        geometry.computeBoundingBox();
        geometry.computeBoundingSphere();
        return geometry;
    }
}
