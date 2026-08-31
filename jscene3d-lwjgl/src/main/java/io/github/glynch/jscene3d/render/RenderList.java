/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BoundingSphere;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Material;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.Object3D;
import io.github.glynch.jscene3d.core.Scene;
import io.github.glynch.jscene3d.core.ShaderMaterial;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4fc;

/** Reusable renderer-internal collection of opaque and transparent submissions. */
final class RenderList {
    private final ArrayDeque<Object3D> pendingObjects;
    private final ArrayList<RenderItem> itemPool;
    private final ArrayList<RenderItem> opaqueItems;
    private final ArrayList<RenderItem> transparentItems;

    private int activeItemCount;
    private long traversalOrder;

    /** Creates reusable traversal, pooling, and submission collections. */
    RenderList() {
        pendingObjects = new ArrayDeque<>();
        itemPool = new ArrayList<>();
        opaqueItems = new ArrayList<>();
        transparentItems = new ArrayList<>();
    }

    /** Rebuilds opaque and transparent submissions from a scene hierarchy. */
    void build(Scene scene, Matrix4fc viewMatrix, Frustum frustum, RenderStatistics statistics) {
        clear();
        pendingObjects.push(scene);
        try {
            while (!pendingObjects.isEmpty()) {
                Object3D object = pendingObjects.pop();
                if (!object.isVisible()) {
                    continue;
                }
                if (object instanceof Mesh mesh) {
                    collect(mesh, viewMatrix, frustum, statistics);
                }
                List<Object3D> children = object.children();
                for (int index = children.size() - 1; index >= 0; index--) {
                    pendingObjects.push(children.get(index));
                }
            }
            opaqueItems.sort(RenderItem::compareOpaque);
            transparentItems.sort(RenderItem::compareTransparent);
        } catch (RuntimeException exception) {
            clear();
            throw exception;
        }
    }

    /** Returns the number of active opaque submissions. */
    int opaqueCount() {
        return opaqueItems.size();
    }

    /** Returns an opaque submission by sorted position. */
    RenderItem opaqueItem(int index) {
        return opaqueItems.get(index);
    }

    /** Returns the number of active transparent submissions. */
    int transparentCount() {
        return transparentItems.size();
    }

    /** Returns a transparent submission by back-to-front sorted position. */
    RenderItem transparentItem(int index) {
        return transparentItems.get(index);
    }

    /** Releases active submissions while retaining allocated pooling capacity. */
    void clear() {
        pendingObjects.clear();
        opaqueItems.clear();
        transparentItems.clear();
        for (int index = 0; index < activeItemCount; index++) {
            itemPool.get(index).release();
        }
        activeItemCount = 0;
        traversalOrder = 0L;
    }

    /** Validates and classifies one visible mesh, including optional frustum rejection. */
    private void collect(Mesh mesh, Matrix4fc viewMatrix, Frustum frustum, RenderStatistics statistics) {
        BufferGeometry geometry = mesh.geometry();
        Material material = mesh.material();
        if (!material.visible()) {
            return;
        }
        int elementCount = geometry.drawRangeCount();
        if (elementCount == 0) {
            return;
        }
        if (!(material instanceof BasicMaterial) && !(material instanceof ShaderMaterial)) {
            throw new IllegalStateException(
                    "Unsupported material type: " + material.getClass().getName());
        }

        Matrix4fc worldMatrix = mesh.matrixWorld();
        if (mesh.isFrustumCullingEnabled()) {
            BoundingSphere boundingSphere = geometry.boundingSphere();
            if (boundingSphere == null) {
                boundingSphere = geometry.computeBoundingSphere();
            }
            if (!frustum.intersects(boundingSphere, worldMatrix)) {
                statistics.recordCulledMesh();
                return;
            }
        }

        RenderItem item = acquireItem();
        float worldX = worldMatrix.m30();
        float worldY = worldMatrix.m31();
        float worldZ = worldMatrix.m32();
        float cameraDepth =
                viewMatrix.m02() * worldX + viewMatrix.m12() * worldY + viewMatrix.m22() * worldZ + viewMatrix.m32();
        item.assign(mesh, geometry, material, worldMatrix, elementCount, cameraDepth, traversalOrder++);
        if (material.transparent()) {
            transparentItems.add(item);
        } else {
            opaqueItems.add(item);
        }
    }

    /** Acquires one active item while growing retained pool capacity only when necessary. */
    private RenderItem acquireItem() {
        if (activeItemCount == itemPool.size()) {
            itemPool.add(new RenderItem());
        }
        return itemPool.get(activeItemCount++);
    }
}
