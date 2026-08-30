/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static io.github.glynch.jscene3d.core.JomlAssertions.EPSILON;
import static io.github.glynch.jscene3d.core.JomlAssertions.assertQuaternion;
import static io.github.glynch.jscene3d.core.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;
import static org.joml.Math.PI_OVER_2_f;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

final class Object3DTransformTest {
    @Test
    void startsWithStableIdentityTransformViews() {
        Object3D object = new Object3D();
        Vector3fc position = object.position();
        Quaternionfc quaternion = object.quaternion();
        Vector3fc scale = object.scale();
        Matrix4fc matrix = object.matrix();
        Matrix4fc matrixWorld = object.matrixWorld();

        assertThat(position).isSameAs(object.position());
        assertThat(quaternion).isSameAs(object.quaternion());
        assertThat(scale).isSameAs(object.scale());
        assertThat(matrix).isSameAs(object.matrix());
        assertThat(matrixWorld).isSameAs(object.matrixWorld());
        assertVector(position, 0.0f, 0.0f, 0.0f);
        assertQuaternion(quaternion, 0.0f, 0.0f, 0.0f, 1.0f);
        assertVector(scale, 1.0f, 1.0f, 1.0f);
        Matrix4f identity = new Matrix4f();
        assertThat(matrix.equals(identity, EPSILON)).isTrue();
        assertThat(matrixWorld.equals(identity, EPSILON)).isTrue();
    }

    @Test
    void acceptsScalarOrExistingValuesWithoutRetainingMutableInputs() {
        Object3D object = new Object3D();
        Vector3f position = new Vector3f(1.0f, 2.0f, 3.0f);
        Quaternionf quaternion = new Quaternionf().rotationY(0.5f);
        Vector3f scale = new Vector3f(2.0f, 3.0f, 4.0f);

        object.setPosition(position);
        object.setQuaternion(quaternion);
        object.setScale(scale);
        position.zero();
        quaternion.identity();
        scale.zero();

        assertVector(object.position(), 1.0f, 2.0f, 3.0f);
        assertThat(object.quaternion().equals(new Quaternionf().rotationY(0.5f), EPSILON))
                .isTrue();
        assertVector(object.scale(), 2.0f, 3.0f, 4.0f);

        object.setPosition(5.0f, 6.0f, 7.0f);
        object.setQuaternion(0.0f, 0.0f, 0.0f, 2.0f);
        object.setScale(8.0f, 9.0f, 10.0f);

        assertVector(object.position(), 5.0f, 6.0f, 7.0f);
        assertQuaternion(object.quaternion(), 0.0f, 0.0f, 0.0f, 1.0f);
        assertVector(object.scale(), 8.0f, 9.0f, 10.0f);
    }

    @Test
    void composesLocalTransformsAsTranslationRotationScale() {
        Object3D object = new Object3D();
        object.setPosition(10.0f, 20.0f, 30.0f);
        object.rotateZ(PI_OVER_2_f);
        object.setScale(2.0f, 3.0f, 4.0f);
        Vector3f transformed = new Vector3f();

        object.matrix().transformPosition(new Vector3f(1.0f, 0.0f, 0.0f), transformed);

        assertVector(transformed, 10.0f, 22.0f, 30.0f);
    }

    @Test
    void updatesWorldTransformsAfterAncestorChangesAndReparenting() {
        Object3D firstParent = new Object3D();
        Object3D secondParent = new Object3D();
        Object3D child = new Object3D();
        firstParent.setPosition(5.0f, 0.0f, 0.0f);
        firstParent.rotateZ(PI_OVER_2_f);
        child.setPosition(1.0f, 0.0f, 0.0f);
        firstParent.add(child);
        Vector3f worldPosition = new Vector3f();

        child.worldPosition(worldPosition);
        assertVector(worldPosition, 5.0f, 1.0f, 0.0f);

        firstParent.setPosition(10.0f, 0.0f, 0.0f);
        child.worldPosition(worldPosition);
        assertVector(worldPosition, 10.0f, 1.0f, 0.0f);

        secondParent.setPosition(-2.0f, 0.0f, 0.0f);
        secondParent.add(child);
        child.worldPosition(worldPosition);
        assertVector(worldPosition, -1.0f, 0.0f, 0.0f);
    }

    @Test
    void reportsInheritedWorldRotationAndScaleIntoCallerDestinations() {
        Object3D parent = new Object3D();
        Object3D child = new Object3D();
        parent.setScale(2.0f, 3.0f, 4.0f);
        parent.rotateY(0.5f);
        parent.add(child);
        Vector3f worldScale = new Vector3f();
        Quaternionf worldQuaternion = new Quaternionf();

        assertThat(child.worldScale(worldScale)).isSameAs(worldScale);
        assertThat(child.worldQuaternion(worldQuaternion)).isSameAs(worldQuaternion);
        assertVector(worldScale, 2.0f, 3.0f, 4.0f);
        assertThat(worldQuaternion.equals(parent.quaternion(), EPSILON)).isTrue();

        parent.setScale(-2.0f, 3.0f, 4.0f);
        child.worldScale(worldScale);
        assertVector(worldScale, -2.0f, 3.0f, 4.0f);
    }

    @Test
    void supportsEveryEulerRotationOrderAndIncrementalAxisRotations() {
        for (RotationOrder order : RotationOrder.values()) {
            Object3D object = new Object3D();
            object.setRotationFromEuler(0.2f, 0.3f, 0.4f, order);
            assertThat(object.quaternion().lengthSquared()).isCloseTo(1.0f, within(EPSILON));
        }

        Object3D object = new Object3D();
        object.rotateX(0.2f);
        object.rotateY(0.3f);
        object.rotateZ(0.4f);
        Quaternionf expected = new Quaternionf().rotationXYZ(0.2f, 0.3f, 0.4f);

        assertThat(object.quaternion().equals(expected, EPSILON)).isTrue();
    }

    @Test
    void rejectsInvalidTransformValues() {
        Object3D object = new Object3D();
        Vector3f nonFiniteVector = new Vector3f(Float.NaN, 0.0f, 0.0f);
        Quaternionf nonFiniteQuaternion = new Quaternionf(Float.POSITIVE_INFINITY, 0.0f, 0.0f, 1.0f);
        Quaternionf zeroQuaternion = new Quaternionf(0.0f, 0.0f, 0.0f, 0.0f);

        assertThatIllegalArgumentException().isThrownBy(() -> object.setPosition(Float.NaN, 0.0f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> object.setPosition(nonFiniteVector));
        assertThatIllegalArgumentException().isThrownBy(() -> object.setScale(1.0f, Float.POSITIVE_INFINITY, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> object.setQuaternion(nonFiniteQuaternion));
        assertThatIllegalArgumentException().isThrownBy(() -> object.setQuaternion(zeroQuaternion));
        assertThatIllegalArgumentException().isThrownBy(() -> object.rotateX(Float.NEGATIVE_INFINITY));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> object.setRotationFromEuler(0.0f, Float.NaN, 0.0f, RotationOrder.XYZ));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullTransformArguments() {
        Object3D object = new Object3D();

        assertThatNullPointerException()
                .isThrownBy(() -> object.setPosition(null))
                .withMessage("position");
        assertThatNullPointerException()
                .isThrownBy(() -> object.setQuaternion(null))
                .withMessage("quaternion");
        assertThatNullPointerException().isThrownBy(() -> object.setScale(null)).withMessage("scale");
        assertThatNullPointerException()
                .isThrownBy(() -> object.setRotationFromEuler(0.0f, 0.0f, 0.0f, null))
                .withMessage("order");
        assertThatNullPointerException()
                .isThrownBy(() -> object.worldPosition(null))
                .withMessage("destination");
        assertThatNullPointerException()
                .isThrownBy(() -> object.worldQuaternion(null))
                .withMessage("destination");
        assertThatNullPointerException()
                .isThrownBy(() -> object.worldScale(null))
                .withMessage("destination");
    }

    @Test
    void resolvesVeryDeepWorldTransformsWithoutUsingTheCallStack() {
        Object3D leaf = new Object3D();
        leaf.setPosition(1.0f, 0.0f, 0.0f);
        Object3D root = leaf;
        int hierarchyDepth = 25_000;
        for (int index = 1; index < hierarchyDepth; index++) {
            Object3D newRoot = new Object3D();
            newRoot.setPosition(1.0f, 0.0f, 0.0f);
            newRoot.add(root);
            root = newRoot;
        }
        Vector3f worldPosition = new Vector3f();

        leaf.worldPosition(worldPosition);

        assertVector(worldPosition, hierarchyDepth, 0.0f, 0.0f);
    }
}
