/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.objects.Billboard;
import io.github.glynch.jscene3d.objects.BillboardAlignment;
import org.assertj.core.data.Offset;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class BillboardTransformTest {
    private static final Offset<Float> TOLERANCE = Offset.offset(1.0E-5F);

    @Test
    void copiesCameraOrientationForSphericalAlignmentWithoutMutatingSceneState() {
        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            billboard.setPosition(1.0f, 2.0f, 3.0f);
            billboard.setScale(2.0f, 4.0f, 1.0f);
            billboard.rotateX(0.4f);
            Matrix4f originalWorldMatrix = new Matrix4f(billboard.matrixWorld());
            Matrix4f cameraWorldMatrix = new Matrix4f()
                    .translationRotateScale(
                            new Vector3f(8.0f, 5.0f, 4.0f), new Quaternionf().rotateY(PI_OVER_TWO), new Vector3f(1.0f));

            Matrix4f result = new BillboardTransform().resolve(billboard, cameraWorldMatrix, new Matrix4f());

            Vector3f centre = result.transformPosition(new Vector3f());
            Vector3f facing =
                    result.transformDirection(new Vector3f(0.0f, 0.0f, 1.0f)).normalize();
            assertVector(centre, 1.0f, 2.0f, 3.0f);
            assertVector(facing, 1.0f, 0.0f, 0.0f);
            assertVector(result.getScale(new Vector3f()), 2.0f, 4.0f, 1.0f);
            assertThat(billboard.matrixWorld()).isEqualTo(originalWorldMatrix);
        }
    }

    @Test
    void keepsCylindricalAlignmentUprightWhileFacingTheCameraHorizontally() {
        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            billboard.setAlignment(BillboardAlignment.CYLINDRICAL);
            billboard.setScale(2.0f, 5.0f, 1.0f);
            Matrix4f cameraWorldMatrix = new Matrix4f().translation(10.0f, 7.0f, 0.0f);

            Matrix4f result = new BillboardTransform().resolve(billboard, cameraWorldMatrix, new Matrix4f());

            Vector3f facing =
                    result.transformDirection(new Vector3f(0.0f, 0.0f, 1.0f)).normalize();
            Vector3f upright =
                    result.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).normalize();
            assertVector(facing, 1.0f, 0.0f, 0.0f);
            assertVector(upright, 0.0f, 1.0f, 0.0f);
        }
    }

    @Test
    void offsetsGeometrySoTheAnchorRemainsAtTheWorldPosition() {
        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            billboard.setAlignment(BillboardAlignment.CYLINDRICAL);
            billboard.setPosition(2.0f, 3.0f, 4.0f);
            billboard.setScale(2.0f, 6.0f, 1.0f);
            billboard.setAnchor(0.5f, 0.0f);

            Matrix4f result = new BillboardTransform()
                    .resolve(billboard, new Matrix4f().translation(2.0f, 3.0f, 9.0f), new Matrix4f());

            Vector3f quadCentre = result.transformPosition(new Vector3f());
            Vector3f lowerCentre = result.transformPosition(new Vector3f(0.0f, -0.5f, 0.0f));
            assertVector(quadCentre, 2.0f, 6.0f, 4.0f);
            assertVector(lowerCentre, 2.0f, 3.0f, 4.0f);
        }
    }

    @Test
    void usesADeterministicDirectionWhenTheCameraIsOnTheCylindricalAxis() {
        try (BasicMaterial material = new BasicMaterial();
                Billboard billboard = new Billboard(material)) {
            billboard.setAlignment(BillboardAlignment.CYLINDRICAL);

            Matrix4f result = new BillboardTransform()
                    .resolve(billboard, new Matrix4f().translation(0.0f, 10.0f, 0.0f), new Matrix4f());

            Vector3f facing =
                    result.transformDirection(new Vector3f(0.0f, 0.0f, 1.0f)).normalize();
            assertVector(facing, 0.0f, 0.0f, 1.0f);
        }
    }

    /** Asserts vector components independently with floating-point tolerance. */
    private static void assertVector(Vector3f vector, float x, float y, float z) {
        assertThat(vector.x).isCloseTo(x, TOLERANCE);
        assertThat(vector.y).isCloseTo(y, TOLERANCE);
        assertThat(vector.z).isCloseTo(z, TOLERANCE);
    }
}
