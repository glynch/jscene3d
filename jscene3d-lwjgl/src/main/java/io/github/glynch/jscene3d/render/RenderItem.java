/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Mesh;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Reusable renderer-internal description of one mesh submission. */
final class RenderItem {
    private @Nullable Mesh mesh;
    private @Nullable BufferGeometry geometry;
    private @Nullable BasicMaterial material;
    private @Nullable Matrix4fc worldMatrix;
    private int elementCount;
    private int materialSortKey;
    private int geometrySortKey;
    private long traversalOrder;

    RenderItem() {
        // References are assigned when this pooled item participates in a frame.
    }

    static int compareOpaque(RenderItem first, RenderItem second) {
        int comparison = Integer.compare(first.materialSortKey, second.materialSortKey);
        if (comparison == 0) {
            comparison = Integer.compare(first.geometrySortKey, second.geometrySortKey);
        }
        if (comparison == 0) {
            comparison = Long.compare(first.traversalOrder, second.traversalOrder);
        }
        return comparison;
    }

    void assign(
            Mesh mesh,
            BufferGeometry geometry,
            BasicMaterial material,
            Matrix4fc worldMatrix,
            int elementCount,
            long traversalOrder) {
        this.mesh = mesh;
        this.geometry = geometry;
        this.material = material;
        this.worldMatrix = worldMatrix;
        this.elementCount = elementCount;
        materialSortKey = System.identityHashCode(material);
        geometrySortKey = System.identityHashCode(geometry);
        this.traversalOrder = traversalOrder;
    }

    Mesh mesh() {
        return Objects.requireNonNull(mesh, "Inactive render item has no mesh");
    }

    BufferGeometry geometry() {
        return Objects.requireNonNull(geometry, "Inactive render item has no geometry");
    }

    BasicMaterial material() {
        return Objects.requireNonNull(material, "Inactive render item has no material");
    }

    Matrix4fc worldMatrix() {
        return Objects.requireNonNull(worldMatrix, "Inactive render item has no world matrix");
    }

    int elementCount() {
        return elementCount;
    }

    void release() {
        mesh = null;
        geometry = null;
        material = null;
        worldMatrix = null;
        elementCount = 0;
        materialSortKey = 0;
        geometrySortKey = 0;
        traversalOrder = 0L;
    }
}
