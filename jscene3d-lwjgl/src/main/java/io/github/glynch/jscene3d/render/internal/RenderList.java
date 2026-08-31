/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.lights.Light;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.materials.NormalMaterial;
import io.github.glynch.jscene3d.materials.PhongMaterial;
import io.github.glynch.jscene3d.materials.ShaderMaterial;
import io.github.glynch.jscene3d.materials.StandardMaterial;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4fc;

/** Reusable renderer-internal collection of opaque and transparent submissions. */
public final class RenderList {
    private final ArrayDeque<Object3D> pendingObjects;
    private final ArrayList<RenderItem> itemPool;
    private final ArrayList<RenderItem> opaqueItems;
    private final ArrayList<RenderItem> transparentItems;
    private final LightCollection lights;

    private int activeItemCount;
    private int culledMeshes;
    private int culledLines;
    private long traversalOrder;

    /**
     * Creates reusable traversal, pooling, and submission collections.
     *
     * @param maximumPointLights maximum accepted visible point lights
     * @param maximumDirectionalLights maximum accepted visible directional lights
     * @param maximumSpotLights maximum accepted visible spotlights
     * @param maximumHemisphereLights maximum accepted visible hemisphere lights
     */
    public RenderList(
            int maximumPointLights, int maximumDirectionalLights, int maximumSpotLights, int maximumHemisphereLights) {
        pendingObjects = new ArrayDeque<>();
        itemPool = new ArrayList<>();
        opaqueItems = new ArrayList<>();
        transparentItems = new ArrayList<>();
        lights = new LightCollection(
                maximumPointLights, maximumDirectionalLights, maximumSpotLights, maximumHemisphereLights);
    }

    /**
     * Rebuilds opaque and transparent submissions from a scene hierarchy.
     *
     * @param scene scene hierarchy to traverse
     * @param viewMatrix current view matrix
     * @param frustum current camera frustum
     */
    public void build(Scene scene, Matrix4fc viewMatrix, Frustum frustum) {
        clear();
        pendingObjects.push(scene);
        try {
            while (!pendingObjects.isEmpty()) {
                Object3D object = pendingObjects.pop();
                if (!object.isVisible()) {
                    continue;
                }
                if (object instanceof Mesh mesh) {
                    collectMesh(mesh, viewMatrix, frustum);
                } else if (object instanceof Line line) {
                    collectLine(line, viewMatrix, frustum);
                }
                if (object instanceof Light light) {
                    lights.add(light);
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

    /**
     * Returns the number of active opaque submissions.
     *
     * @return opaque submission count
     */
    public int opaqueCount() {
        return opaqueItems.size();
    }

    /**
     * Returns an opaque submission by sorted position.
     *
     * @param index zero-based sorted position
     * @return opaque render item
     */
    public RenderItem opaqueItem(int index) {
        return opaqueItems.get(index);
    }

    /**
     * Returns the number of active transparent submissions.
     *
     * @return transparent submission count
     */
    public int transparentCount() {
        return transparentItems.size();
    }

    /**
     * Returns a transparent submission by back-to-front sorted position.
     *
     * @param index zero-based sorted position
     * @return transparent render item
     */
    public RenderItem transparentItem(int index) {
        return transparentItems.get(index);
    }

    /**
     * Returns the stable visible-light collection for the active frame.
     *
     * @return visible-light collection
     */
    public LightCollection lights() {
        return lights;
    }

    /**
     * Returns meshes rejected during the active build.
     *
     * @return culled mesh count
     */
    public int culledMeshes() {
        return culledMeshes;
    }

    /**
     * Returns line objects rejected during the active build.
     *
     * @return culled line-object count
     */
    public int culledLines() {
        return culledLines;
    }

    /** Releases active submissions while retaining allocated pooling capacity. */
    public void clear() {
        pendingObjects.clear();
        opaqueItems.clear();
        transparentItems.clear();
        lights.clear();
        for (int index = 0; index < activeItemCount; index++) {
            itemPool.get(index).release();
        }
        activeItemCount = 0;
        culledMeshes = 0;
        culledLines = 0;
        traversalOrder = 0L;
    }

    /** Validates and classifies one visible mesh. */
    private void collectMesh(Mesh mesh, Matrix4fc viewMatrix, Frustum frustum) {
        BufferGeometry geometry = mesh.geometry();
        Material material = mesh.material();
        boolean supported =
                switch (material) {
                    case BasicMaterial ignored -> true;
                    case LambertMaterial ignored -> true;
                    case LineBasicMaterial ignored -> false;
                    case NormalMaterial ignored -> true;
                    case PhongMaterial ignored -> true;
                    case ShaderMaterial ignored -> true;
                    case StandardMaterial ignored -> true;
                };
        if (!supported) {
            throw new IllegalStateException(
                    "Unsupported material type: " + material.getClass().getName());
        }
        collect(mesh, geometry, material, PrimitiveTopology.TRIANGLES, viewMatrix, frustum);
    }

    /** Validates and classifies one visible line object. */
    private void collectLine(Line line, Matrix4fc viewMatrix, Frustum frustum) {
        BufferGeometry geometry = line.geometry();
        LineBasicMaterial material = line.material();
        PrimitiveTopology topology =
                line instanceof LineSegments ? PrimitiveTopology.LINE_SEGMENTS : PrimitiveTopology.LINE_STRIP;
        collect(line, geometry, material, topology, viewMatrix, frustum);
    }

    /** Classifies one visible renderable scene object, including optional frustum rejection. */
    private void collect(
            Object3D object,
            BufferGeometry geometry,
            Material material,
            PrimitiveTopology topology,
            Matrix4fc viewMatrix,
            Frustum frustum) {
        if (!material.visible()) {
            return;
        }
        int elementCount = geometry.drawRangeCount();
        topology.validateElementCount(elementCount);
        if (!topology.hasPrimitives(elementCount)) {
            return;
        }

        Matrix4fc worldMatrix = object.matrixWorld();
        if (object.isFrustumCullingEnabled()) {
            BoundingSphere boundingSphere = geometry.boundingSphere();
            if (boundingSphere == null) {
                boundingSphere = geometry.computeBoundingSphere();
            }
            if (!frustum.intersects(boundingSphere, worldMatrix)) {
                if (topology.isLine()) {
                    culledLines++;
                } else {
                    culledMeshes++;
                }
                return;
            }
        }

        RenderItem item = acquireItem();
        item.assign(object, geometry, material, topology, elementCount, viewMatrix, traversalOrder++);
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
