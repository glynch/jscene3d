/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.core.BasicMaterial;
import io.github.glynch.jscene3d.core.BufferGeometry;
import io.github.glynch.jscene3d.core.Color;
import io.github.glynch.jscene3d.core.Group;
import io.github.glynch.jscene3d.core.Mesh;
import io.github.glynch.jscene3d.core.Scene;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

final class RenderListTest {
    @Test
    void partitionsOpaqueAndStablySortsTransparentItemsBackToFront() {
        try (BufferGeometry geometry = createTriangle();
                BasicMaterial opaqueMaterial = new BasicMaterial(Color.RED);
                BasicMaterial transparentMaterial = new BasicMaterial(Color.BLUE)) {
            transparentMaterial.setTransparent(true);
            Mesh firstTransparent = new Mesh(geometry, transparentMaterial);
            firstTransparent.setPosition(0.0f, 0.0f, -1.0f);
            firstTransparent.setFrustumCullingEnabled(false);
            Mesh opaque = new Mesh(geometry, opaqueMaterial);
            Mesh secondTransparent = new Mesh(geometry, transparentMaterial);
            secondTransparent.setPosition(0.0f, 0.0f, -3.0f);
            secondTransparent.setFrustumCullingEnabled(false);
            Mesh equalDepthTransparent = new Mesh(geometry, transparentMaterial);
            equalDepthTransparent.setPosition(1.0f, 0.0f, -3.0f);
            equalDepthTransparent.setFrustumCullingEnabled(false);
            Scene scene = new Scene();
            scene.add(firstTransparent);
            scene.add(opaque);
            scene.add(secondTransparent);
            scene.add(equalDepthTransparent);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            build(renderList, scene);

            assertThat(renderList.opaqueCount()).isEqualTo(1);
            assertThat(renderList.opaqueItem(0).mesh()).isSameAs(opaque);
            assertThat(renderList.transparentCount()).isEqualTo(3);
            assertThat(renderList.transparentItem(0).mesh()).isSameAs(secondTransparent);
            assertThat(renderList.transparentItem(1).mesh()).isSameAs(equalDepthTransparent);
            assertThat(renderList.transparentItem(2).mesh()).isSameAs(firstTransparent);
            renderList.clear();
        }
    }

    @Test
    void sortsOpaqueItemsAndReusesTheirPooledDescriptions() {
        try (BufferGeometry firstGeometry = createTriangle();
                BufferGeometry secondGeometry = createTriangle();
                BasicMaterial firstMaterial = new BasicMaterial(Color.RED);
                BasicMaterial secondMaterial = new BasicMaterial(Color.BLUE)) {
            Mesh firstMesh = new Mesh(secondGeometry, firstMaterial);
            Mesh secondMesh = new Mesh(firstGeometry, secondMaterial);
            Mesh thirdMesh = new Mesh(firstGeometry, firstMaterial);
            Scene scene = new Scene();
            scene.add(firstMesh);
            scene.add(secondMesh);
            scene.add(thirdMesh);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            build(renderList, scene);
            RenderItem firstMeshItem = findOpaqueItem(renderList, firstMesh);
            List<RenderItem> initialOrder = opaqueItems(renderList);

            assertThat(initialOrder).isSortedAccordingTo(RenderItem::compareOpaque);

            renderList.clear();
            build(renderList, scene);

            assertThat(findOpaqueItem(renderList, firstMesh)).isSameAs(firstMeshItem);
            assertThat(opaqueItems(renderList)).containsExactlyElementsOf(initialOrder);
            renderList.clear();
        }
    }

    @Test
    void skipsInvisibleSubtreesAndMaterials() {
        try (BufferGeometry geometry = createTriangle();
                BasicMaterial visibleMaterial = new BasicMaterial(Color.RED);
                BasicMaterial invisibleMaterial = new BasicMaterial(Color.BLUE)) {
            Group hiddenGroup = new Group();
            hiddenGroup.setVisible(false);
            hiddenGroup.add(new Mesh(geometry, visibleMaterial));
            invisibleMaterial.setVisible(false);
            Mesh invisibleMaterialMesh = new Mesh(geometry, invisibleMaterial);
            Scene scene = new Scene();
            scene.add(hiddenGroup);
            scene.add(invisibleMaterialMesh);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            build(renderList, scene);

            assertThat(renderList.opaqueCount()).isZero();
            assertThat(renderList.transparentCount()).isZero();
            renderList.clear();
        }
    }

    @Test
    void cullsOutsideMeshesUnlessCullingIsDisabled() {
        try (BufferGeometry geometry = createTriangle();
                BasicMaterial material = new BasicMaterial(Color.RED)) {
            Mesh mesh = new Mesh(geometry, material);
            mesh.setPosition(3.0f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(mesh);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            RenderStatistics culledStatistics = build(renderList, scene);

            assertThat(renderList.opaqueCount()).isZero();
            assertThat(culledStatistics.culledMeshes()).isEqualTo(1);
            assertThat(geometry.boundingSphere()).isNotNull();

            geometry.clearBoundingSphere();
            mesh.setFrustumCullingEnabled(false);
            RenderStatistics uncullableStatistics = build(renderList, scene);

            assertThat(renderList.opaqueCount()).isEqualTo(1);
            assertThat(uncullableStatistics.culledMeshes()).isZero();
            assertThat(geometry.boundingSphere()).isNull();
            renderList.clear();
        }
    }

    private static BufferGeometry createTriangle() {
        return BufferGeometry.builder()
                .positions(-0.2f, -0.2f, 0.0f, 0.2f, -0.2f, 0.0f, 0.0f, 0.2f, 0.0f)
                .build();
    }

    private static RenderStatistics build(RenderList renderList, Scene scene) {
        Frustum frustum = new Frustum();
        frustum.update(new Matrix4f(), new Matrix4f());
        RenderStatistics statistics = new RenderStatistics();
        statistics.beginFrame();
        renderList.build(scene, new Matrix4f(), frustum, statistics);
        return statistics;
    }

    private static RenderItem findOpaqueItem(RenderList renderList, Mesh mesh) {
        for (int index = 0; index < renderList.opaqueCount(); index++) {
            RenderItem item = renderList.opaqueItem(index);
            if (item.mesh() == mesh) {
                return item;
            }
        }
        throw new AssertionError("Mesh is not present in the opaque render list");
    }

    private static List<RenderItem> opaqueItems(RenderList renderList) {
        List<RenderItem> items = new ArrayList<>(renderList.opaqueCount());
        for (int index = 0; index < renderList.opaqueCount(); index++) {
            items.add(renderList.opaqueItem(index));
        }
        return items;
    }
}
