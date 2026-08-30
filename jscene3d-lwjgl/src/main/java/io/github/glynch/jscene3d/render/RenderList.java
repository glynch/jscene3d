/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Material;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.Object3D;
import io.github.glynch.jscene3d.core.Scene;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Reusable renderer-internal collection of opaque and transparent submissions. */
final class RenderList {
    private final ArrayDeque<Object3D> pendingObjects;
    private final ArrayList<RenderItem> itemPool;
    private final ArrayList<RenderItem> opaqueItems;
    private final ArrayList<RenderItem> transparentItems;

    private int activeItemCount;
    private long traversalOrder;

    RenderList() {
        pendingObjects = new ArrayDeque<>();
        itemPool = new ArrayList<>();
        opaqueItems = new ArrayList<>();
        transparentItems = new ArrayList<>();
    }

    void build(Scene scene) {
        clear();
        pendingObjects.push(scene);
        try {
            while (!pendingObjects.isEmpty()) {
                Object3D object = pendingObjects.pop();
                if (!object.isVisible()) {
                    continue;
                }
                if (object instanceof Mesh mesh) {
                    collect(mesh);
                }
                List<Object3D> children = object.children();
                for (int index = children.size() - 1; index >= 0; index--) {
                    pendingObjects.push(children.get(index));
                }
            }
            opaqueItems.sort(RenderItem::compareOpaque);
        } catch (RuntimeException exception) {
            clear();
            throw exception;
        }
    }

    int opaqueCount() {
        return opaqueItems.size();
    }

    RenderItem opaqueItem(int index) {
        return opaqueItems.get(index);
    }

    int transparentCount() {
        return transparentItems.size();
    }

    RenderItem transparentItem(int index) {
        return transparentItems.get(index);
    }

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

    private void collect(Mesh mesh) {
        BufferGeometry geometry = mesh.geometry();
        Material material = mesh.material();
        if (!material.visible()) {
            return;
        }
        int elementCount = geometry.drawRangeCount();
        if (elementCount == 0) {
            return;
        }
        if (!(material instanceof BasicMaterial basicMaterial)) {
            throw new IllegalStateException(
                    "Unsupported material type: " + material.getClass().getName());
        }

        RenderItem item = acquireItem();
        item.assign(mesh, geometry, basicMaterial, elementCount, traversalOrder++);
        if (basicMaterial.transparent()) {
            transparentItems.add(item);
        } else {
            opaqueItems.add(item);
        }
    }

    private RenderItem acquireItem() {
        if (activeItemCount == itemPool.size()) {
            itemPool.add(new RenderItem());
        }
        return itemPool.get(activeItemCount++);
    }
}
