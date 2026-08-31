/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Objects;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** Reusable renderer-internal description of one scene-object submission. */
public final class RenderItem {
    private @Nullable Object3D object;
    private @Nullable BufferGeometry geometry;
    private @Nullable Material material;
    private @Nullable Matrix4fc worldMatrix;
    private @Nullable PrimitiveTopology topology;
    private int elementCount;
    private int materialSortKey;
    private int geometrySortKey;
    private int renderOrder;
    private float cameraDepth;
    private long traversalOrder;

    /** Creates an inactive item ready for pooled assignment. */
    public RenderItem() {
        // References are assigned when this pooled item participates in a frame.
    }

    /**
     * Orders opaque items by explicit order, material, geometry, and stable traversal order.
     *
     * @param first first item
     * @param second second item
     * @return comparator result
     */
    public static int compareOpaque(RenderItem first, RenderItem second) {
        int comparison = Integer.compare(first.renderOrder, second.renderOrder);
        if (comparison == 0) {
            comparison = Integer.compare(first.materialSortKey, second.materialSortKey);
        }
        if (comparison == 0) {
            comparison = Integer.compare(first.geometrySortKey, second.geometrySortKey);
        }
        if (comparison == 0) {
            comparison = Long.compare(first.traversalOrder, second.traversalOrder);
        }
        return comparison;
    }

    /**
     * Orders transparent items by explicit order, then back-to-front with stable ties.
     *
     * @param first first item
     * @param second second item
     * @return comparator result
     */
    public static int compareTransparent(RenderItem first, RenderItem second) {
        int comparison = Integer.compare(first.renderOrder, second.renderOrder);
        if (comparison == 0) {
            comparison = Float.compare(first.cameraDepth, second.cameraDepth);
        }
        return comparison == 0 ? Long.compare(first.traversalOrder, second.traversalOrder) : comparison;
    }

    /**
     * Assigns one active scene-object submission to this pooled item.
     *
     * @param object submitted scene object
     * @param geometry submitted geometry
     * @param material submitted material
     * @param topology primitive topology
     * @param elementCount selected element count
     * @param viewMatrix current view matrix
     * @param traversalOrder stable scene traversal position
     */
    public void assign(
            Object3D object,
            BufferGeometry geometry,
            Material material,
            PrimitiveTopology topology,
            int elementCount,
            Matrix4fc viewMatrix,
            long traversalOrder) {
        Matrix4fc assignedWorldMatrix = object.matrixWorld();
        this.object = object;
        this.geometry = geometry;
        this.material = material;
        worldMatrix = assignedWorldMatrix;
        this.topology = topology;
        this.elementCount = elementCount;
        float worldX = assignedWorldMatrix.m30();
        float worldY = assignedWorldMatrix.m31();
        float worldZ = assignedWorldMatrix.m32();
        cameraDepth =
                viewMatrix.m02() * worldX + viewMatrix.m12() * worldY + viewMatrix.m22() * worldZ + viewMatrix.m32();
        materialSortKey = System.identityHashCode(material);
        geometrySortKey = System.identityHashCode(geometry);
        renderOrder = object.renderOrder();
        this.traversalOrder = traversalOrder;
    }

    /**
     * Returns the active scene object.
     *
     * @return active scene object
     */
    public Object3D object() {
        return Objects.requireNonNull(object, "Inactive render item has no scene object");
    }

    /**
     * Returns the active geometry.
     *
     * @return active geometry
     */
    public BufferGeometry geometry() {
        return Objects.requireNonNull(geometry, "Inactive render item has no geometry");
    }

    /**
     * Returns the active material.
     *
     * @return active material
     */
    public Material material() {
        return Objects.requireNonNull(material, "Inactive render item has no material");
    }

    /**
     * Returns the stable world-matrix view captured for the active submission.
     *
     * @return active world matrix
     */
    public Matrix4fc worldMatrix() {
        return Objects.requireNonNull(worldMatrix, "Inactive render item has no world matrix");
    }

    /**
     * Returns how successive geometry elements form primitives.
     *
     * @return active primitive topology
     */
    public PrimitiveTopology topology() {
        return Objects.requireNonNull(topology, "Inactive render item has no topology");
    }

    /**
     * Returns the number of indexed or non-indexed elements to draw.
     *
     * @return selected element count
     */
    public int elementCount() {
        return elementCount;
    }

    /** Clears references and scalar state before returning this item to the pool. */
    public void release() {
        object = null;
        geometry = null;
        material = null;
        worldMatrix = null;
        topology = null;
        elementCount = 0;
        materialSortKey = 0;
        geometrySortKey = 0;
        renderOrder = 0;
        cameraDepth = 0.0f;
        traversalOrder = 0L;
    }
}
