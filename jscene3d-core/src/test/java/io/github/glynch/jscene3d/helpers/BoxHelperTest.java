/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.BufferUsage;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.LineSegments;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class BoxHelperTest {
    @Test
    void createsOwnedDynamicWorldBoundsWithTwelveEdges() {
        try (BufferGeometry geometry = BoxGeometry.create(2.0f, 4.0f, 6.0f);
                BasicMaterial material = new BasicMaterial();
                BoxHelper helper = new BoxHelper(positionedMesh(geometry, material, 1.0f, 2.0f, 3.0f))) {
            BufferAttribute positions = positionAttribute(helper);

            assertThat(helper).isInstanceOf(LineSegments.class);
            assertThat(helper.material().color()).isEqualTo(Color.YELLOW);
            assertThat(helper.material().usesVertexColors()).isFalse();
            assertThat(helper.renderOrder()).isOne();
            assertThat(positions.usage()).isEqualTo(BufferUsage.DYNAMIC);
            assertThat(positions.toArray())
                    .containsExactly(
                            0.0f, 0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 2.0f, 4.0f, 0.0f, 0.0f, 4.0f, 0.0f, 0.0f, 0.0f, 6.0f,
                            2.0f, 0.0f, 6.0f, 2.0f, 4.0f, 6.0f, 0.0f, 4.0f, 6.0f);
            assertThat(Objects.requireNonNull(helper.geometry().index()).toArray())
                    .containsExactly(0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7);
        }
    }

    @Test
    void aggregatesVisibleTransformedDescendantsAndIgnoresHelpers() {
        try (BufferGeometry firstGeometry = BoxGeometry.create(2.0f, 2.0f, 2.0f);
                BufferGeometry secondGeometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BufferGeometry hiddenGeometry = BoxGeometry.create(100.0f, 100.0f, 100.0f);
                BasicMaterial material = new BasicMaterial();
                AxesHelper axes = new AxesHelper(100.0f)) {
            Group target = new Group();
            target.setPosition(10.0f, 0.0f, 0.0f);
            target.add(positionedMesh(firstGeometry, material, 0.0f, 1.0f, 0.0f));
            target.add(positionedMesh(secondGeometry, material, 3.0f, -1.0f, 2.0f));
            Mesh hidden = positionedMesh(hiddenGeometry, material, 0.0f, 0.0f, 0.0f);
            hidden.setVisible(false);
            target.add(hidden);
            target.add(axes);

            try (BoxHelper helper = new BoxHelper(target, Color.CYAN)) {
                assertThat(helper.target()).isSameAs(target);
                assertThat(helper.material().color()).isEqualTo(Color.CYAN);
                assertBounds(positionAttribute(helper), 9.0f, -1.5f, -1.0f, 13.5f, 2.0f, 2.5f);
            }
        }
    }

    @Test
    void updatesChangedBoundsOnceAndKeepsStateWhenRefreshFails() {
        try (BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BasicMaterial material = new BasicMaterial()) {
            Mesh target = new Mesh(geometry, material);
            try (BoxHelper helper = new BoxHelper(target)) {
                BufferAttribute positions = positionAttribute(helper);
                long initialVersion = positions.version();

                helper.update();
                assertThat(positions.version()).isEqualTo(initialVersion);

                target.setPosition(2.0f, 0.0f, 0.0f);
                helper.update();
                assertThat(positions.version()).isEqualTo(initialVersion + 1L);
                assertBounds(positions, 1.5f, -0.5f, -0.5f, 2.5f, 0.5f, 0.5f);

                float[] currentPositions = positions.toArray();
                target.setVisible(false);
                assertThatIllegalStateException()
                        .isThrownBy(helper::update)
                        .withMessage("BoxHelper target contains no visible renderable geometry");
                assertThat(positions.toArray()).containsExactly(currentPositions);
            }
        }
    }

    @Test
    void retargetsToLineGeometryAndRejectsTargetsWithoutBoundsAtomically() {
        try (BufferGeometry meshGeometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BufferGeometry lineGeometry = BufferGeometry.builder()
                        .positions(-2.0f, 1.0f, 3.0f, 4.0f, 5.0f, -1.0f)
                        .build();
                BasicMaterial meshMaterial = new BasicMaterial();
                LineBasicMaterial lineMaterial = new LineBasicMaterial()) {
            Mesh initialTarget = new Mesh(meshGeometry, meshMaterial);
            Line lineTarget = new Line(lineGeometry, lineMaterial);
            lineTarget.setPosition(1.0f, 0.0f, 0.0f);
            Object3D emptyTarget = new Object3D();

            try (BoxHelper helper = new BoxHelper(initialTarget)) {
                helper.setTarget(lineTarget);
                assertThat(helper.target()).isSameAs(lineTarget);
                assertBounds(positionAttribute(helper), -1.0f, 1.0f, -1.0f, 5.0f, 5.0f, 3.0f);

                assertThatIllegalArgumentException()
                        .isThrownBy(() -> helper.setTarget(emptyTarget))
                        .withMessage("target must contain visible renderable geometry");
                assertThat(helper.target()).isSameAs(lineTarget);
                assertBounds(positionAttribute(helper), -1.0f, 1.0f, -1.0f, 5.0f, 5.0f, 3.0f);
            }
        }
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsInvalidConstructionArguments() {
        Object3D emptyTarget = new Object3D();
        assertThatNullPointerException().isThrownBy(() -> new BoxHelper(null)).withMessage("target");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BoxHelper(emptyTarget))
                .withMessage("target must contain visible renderable geometry");

        try (BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
                BasicMaterial material = new BasicMaterial()) {
            Mesh target = new Mesh(geometry, material);
            assertThatNullPointerException()
                    .isThrownBy(() -> new BoxHelper(target, null))
                    .withMessage("color");
        }
    }

    /** Creates a mesh at one local position. */
    private static Mesh positionedMesh(BufferGeometry geometry, Material material, float x, float y, float z) {
        Mesh mesh = new Mesh(geometry, material);
        mesh.setPosition(x, y, z);
        return mesh;
    }

    /** Returns the required generated corner attribute. */
    private static BufferAttribute positionAttribute(BoxHelper helper) {
        return Objects.requireNonNull(helper.geometry().attribute(BufferGeometry.POSITION));
    }

    /** Verifies the minimum and maximum generated corner values. */
    private static void assertBounds(
            BufferAttribute positions,
            float minimumX,
            float minimumY,
            float minimumZ,
            float maximumX,
            float maximumY,
            float maximumZ) {
        assertThat(positions.value(0, 0)).isEqualTo(minimumX);
        assertThat(positions.value(0, 1)).isEqualTo(minimumY);
        assertThat(positions.value(0, 2)).isEqualTo(minimumZ);
        assertThat(positions.value(6, 0)).isEqualTo(maximumX);
        assertThat(positions.value(6, 1)).isEqualTo(maximumY);
        assertThat(positions.value(6, 2)).isEqualTo(maximumZ);
    }
}
