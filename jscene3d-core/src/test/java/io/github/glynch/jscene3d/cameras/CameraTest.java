/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.cameras;

import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.objects.Group;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class CameraTest {
    @Test
    void derivesAStableViewMatrixFromWorldTransform() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 5.0f);

        Matrix4fc viewMatrix = camera.viewMatrix();
        Vector3f viewedOrigin = viewMatrix.transformPosition(new Vector3f());

        assertThat(viewMatrix).isSameAs(camera.viewMatrix());
        assertVector(viewedOrigin, 0.0f, 0.0f, -5.0f);

        camera.setPosition(1.0f, 2.0f, 3.0f);
        camera.viewMatrix().transformPosition(new Vector3f(1.0f, 2.0f, 2.0f), viewedOrigin);
        assertVector(viewedOrigin, 0.0f, 0.0f, -1.0f);
    }

    @Test
    void derivesViewMatrixThroughParentTransforms() {
        Group parent = new Group();
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        parent.setPosition(10.0f, 0.0f, 0.0f);
        camera.setPosition(0.0f, 0.0f, 5.0f);
        parent.add(camera);
        Vector3f viewedCameraPosition = new Vector3f(10.0f, 0.0f, 5.0f);

        camera.viewMatrix().transformPosition(viewedCameraPosition);

        assertVector(viewedCameraPosition, 0.0f, 0.0f, 0.0f);
    }

    @Test
    void aimsLocalNegativeZAtAWorldTarget() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        camera.setPosition(5.0f, 0.0f, 0.0f);

        camera.lookAt(0.0f, 0.0f, 0.0f);

        Vector3f worldForward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(camera.worldQuaternion(new Quaternionf()));
        assertVector(worldForward, -1.0f, 0.0f, 0.0f);
    }

    @Test
    void compensatesForParentRotationWhenAimingAtAWorldTarget() {
        Group parent = new Group();
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        parent.rotateY(0.7f);
        camera.setPosition(0.0f, 0.0f, 5.0f);
        parent.add(camera);
        Vector3f cameraWorldPosition = camera.worldPosition(new Vector3f());
        Vector3f expectedForward = new Vector3f(cameraWorldPosition).negate().normalize();

        camera.lookAt(new Vector3f());

        Vector3f worldForward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(camera.worldQuaternion(new Quaternionf()));
        assertVector(worldForward, expectedForward.x, expectedForward.y, expectedForward.z);
    }

    @Test
    void aimsAtWorldTargetThroughNonUniformParentScale() {
        Group parent = new Group();
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        parent.setScale(2.0f, 3.0f, 4.0f);
        parent.rotateY(0.4f);
        camera.setPosition(1.0f, 2.0f, 3.0f);
        parent.add(camera);
        Vector3f cameraWorldPosition = camera.worldPosition(new Vector3f());
        Vector3f expectedForward = new Vector3f(cameraWorldPosition).negate().normalize();

        camera.lookAt(0.0f, 0.0f, 0.0f);

        Vector3f worldForward = camera.matrixWorld()
                .transformDirection(new Vector3f(0.0f, 0.0f, -1.0f))
                .normalize();
        assertVector(worldForward, expectedForward.x, expectedForward.y, expectedForward.z);
    }

    @Test
    void resolvesUndefinedRollWhenLookingAlongWorldY() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);

        camera.lookAt(0.0f, 1.0f, 0.0f);

        Vector3f worldForward = new Vector3f(0.0f, 0.0f, -1.0f).rotate(camera.worldQuaternion(new Quaternionf()));
        assertVector(worldForward, 0.0f, 1.0f, 0.0f);
    }

    @Test
    void rejectsUndefinedOrInvalidLookAtTargets() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        Vector3f nonFiniteTarget = new Vector3f(Float.NaN, 0.0f, 0.0f);

        assertThatIllegalArgumentException().isThrownBy(() -> camera.lookAt(0.0f, 0.0f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.lookAt(Float.POSITIVE_INFINITY, 0.0f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.lookAt(nonFiniteTarget));
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullLookAtTarget() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);

        assertThatNullPointerException().isThrownBy(() -> camera.lookAt(null)).withMessage("target");
    }

    @Test
    void rejectsSingularCameraWorldTransform() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        camera.setScale(1.0f, 0.0f, 1.0f);

        assertThatIllegalStateException().isThrownBy(camera::viewMatrix);
    }

    @Test
    void rejectsLookAtThroughSingularParentTransform() {
        Group parent = new Group();
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);
        parent.setScale(1.0f, 0.0f, 1.0f);
        parent.add(camera);

        assertThatIllegalStateException().isThrownBy(() -> camera.lookAt(0.0f, 0.0f, -1.0f));
    }
}
