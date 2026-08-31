/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.render.Renderer;
import io.github.glynch.jscene3d.scenes.Scene;
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
            assertThat(renderList.opaqueItem(0).object()).isSameAs(opaque);
            assertThat(renderList.transparentCount()).isEqualTo(3);
            assertThat(renderList.transparentItem(0).object()).isSameAs(secondTransparent);
            assertThat(renderList.transparentItem(1).object()).isSameAs(equalDepthTransparent);
            assertThat(renderList.transparentItem(2).object()).isSameAs(firstTransparent);
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
    void appliesRenderOrderBeforeOpaqueBatchingAndTransparentDepth() {
        try (BufferGeometry geometry = createTriangle();
                BasicMaterial firstMaterial = new BasicMaterial(Color.RED);
                BasicMaterial secondMaterial = new BasicMaterial(Color.BLUE)) {
            Mesh laterOpaque = new Mesh(geometry, firstMaterial);
            laterOpaque.setRenderOrder(4);
            Mesh earlierOpaque = new Mesh(geometry, secondMaterial);
            earlierOpaque.setRenderOrder(-2);
            Mesh laterTransparent = new Mesh(geometry, firstMaterial);
            laterTransparent.setRenderOrder(3);
            laterTransparent.setPosition(0.0f, 0.0f, -10.0f);
            laterTransparent.setFrustumCullingEnabled(false);
            Mesh earlierTransparent = new Mesh(geometry, secondMaterial);
            earlierTransparent.setRenderOrder(-1);
            earlierTransparent.setPosition(0.0f, 0.0f, -1.0f);
            earlierTransparent.setFrustumCullingEnabled(false);
            firstMaterial.setTransparent(true);
            secondMaterial.setTransparent(true);
            Scene transparentScene = new Scene();
            transparentScene.add(laterTransparent);
            transparentScene.add(earlierTransparent);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            Scene opaqueScene = new Scene();
            firstMaterial.setTransparent(false);
            secondMaterial.setTransparent(false);
            opaqueScene.add(laterOpaque);
            opaqueScene.add(earlierOpaque);
            build(renderList, opaqueScene);
            assertThat(renderList.opaqueItem(0).object()).isSameAs(earlierOpaque);
            assertThat(renderList.opaqueItem(1).object()).isSameAs(laterOpaque);

            renderList.clear();
            firstMaterial.setTransparent(true);
            secondMaterial.setTransparent(true);
            build(renderList, transparentScene);
            assertThat(renderList.transparentItem(0).object()).isSameAs(earlierTransparent);
            assertThat(renderList.transparentItem(1).object()).isSameAs(laterTransparent);
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

            build(renderList, scene);

            assertThat(renderList.opaqueCount()).isZero();
            assertThat(renderList.culledMeshes()).isEqualTo(1);
            assertThat(geometry.boundingSphere()).isNotNull();

            geometry.clearBoundingSphere();
            mesh.setFrustumCullingEnabled(false);
            build(renderList, scene);

            assertThat(renderList.opaqueCount()).isEqualTo(1);
            assertThat(renderList.culledMeshes()).isZero();
            assertThat(geometry.boundingSphere()).isNull();
            renderList.clear();
        }
    }

    @Test
    void collectsLineStripsAndSegmentsWithTheirPrimitiveTopologies() {
        try (BufferGeometry stripGeometry = createLineStrip();
                BufferGeometry segmentsGeometry = createLineSegments();
                LineBasicMaterial material = new LineBasicMaterial(Color.RED)) {
            Line strip = new Line(stripGeometry, material);
            LineSegments segments = new LineSegments(segmentsGeometry, material);
            Scene scene = new Scene();
            scene.add(strip);
            scene.add(segments);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            build(renderList, scene);

            assertThat(renderList.opaqueCount()).isEqualTo(2);
            RenderItem stripItem = findOpaqueItem(renderList, strip);
            RenderItem segmentsItem = findOpaqueItem(renderList, segments);
            assertThat(stripItem.topology()).isEqualTo(PrimitiveTopology.LINE_STRIP);
            assertThat(stripItem.elementCount()).isEqualTo(3);
            assertThat(segmentsItem.topology()).isEqualTo(PrimitiveTopology.LINE_SEGMENTS);
            assertThat(segmentsItem.elementCount()).isEqualTo(4);
            renderList.clear();
        }
    }

    @Test
    void rejectsUnpairedLineSegmentElements() {
        try (BufferGeometry geometry = createLineStrip();
                LineBasicMaterial material = new LineBasicMaterial()) {
            Scene scene = new Scene();
            scene.add(new LineSegments(geometry, material));
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            assertThatIllegalStateException()
                    .isThrownBy(() -> build(renderList, scene))
                    .withMessage("LineSegments draw range must contain an even number of elements: 3");
        }
    }

    @Test
    void cullsOutsideLinesAndReportsLineStatistics() {
        try (BufferGeometry geometry = createLineSegments();
                LineBasicMaterial material = new LineBasicMaterial()) {
            Line line = new Line(geometry, material);
            line.setPosition(3.0f, 0.0f, 0.0f);
            Scene scene = new Scene();
            scene.add(line);
            RenderList renderList = new RenderList(Renderer.MAX_POINT_LIGHTS);

            build(renderList, scene);

            assertThat(renderList.opaqueCount()).isZero();
            assertThat(renderList.culledLines()).isOne();
            assertThat(renderList.culledMeshes()).isZero();
        }
    }

    private static BufferGeometry createTriangle() {
        return BufferGeometry.builder()
                .positions(-0.2f, -0.2f, 0.0f, 0.2f, -0.2f, 0.0f, 0.0f, 0.2f, 0.0f)
                .build();
    }

    private static BufferGeometry createLineStrip() {
        return BufferGeometry.builder()
                .positions(-0.5f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 0.0f, 0.0f)
                .build();
    }

    private static BufferGeometry createLineSegments() {
        return BufferGeometry.builder()
                .positions(-0.5f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 0.0f, 0.0f)
                .build();
    }

    private static void build(RenderList renderList, Scene scene) {
        Frustum frustum = new Frustum();
        frustum.update(new Matrix4f(), new Matrix4f());
        renderList.build(scene, new Matrix4f(), frustum);
    }

    private static RenderItem findOpaqueItem(RenderList renderList, Mesh mesh) {
        for (int index = 0; index < renderList.opaqueCount(); index++) {
            RenderItem item = renderList.opaqueItem(index);
            if (item.object() == mesh) {
                return item;
            }
        }
        throw new AssertionError("Mesh is not present in the opaque render list");
    }

    private static RenderItem findOpaqueItem(RenderList renderList, Line line) {
        for (int index = 0; index < renderList.opaqueCount(); index++) {
            RenderItem item = renderList.opaqueItem(index);
            if (item.object() == line) {
                return item;
            }
        }
        throw new AssertionError("Line is not present in the opaque render list");
    }

    private static List<RenderItem> opaqueItems(RenderList renderList) {
        List<RenderItem> items = new ArrayList<>(renderList.opaqueCount());
        for (int index = 0; index < renderList.opaqueCount(); index++) {
            items.add(renderList.opaqueItem(index));
        }
        return items;
    }
}
