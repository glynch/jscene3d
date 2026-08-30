/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static io.github.glynch.jscene3d.core.JomlAssertions.EPSILON;
import static io.github.glynch.jscene3d.core.JomlAssertions.assertNdc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.joml.Math.PI_OVER_2_f;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.junit.jupiter.api.Test;

final class PerspectiveCameraTest {
    @Test
    void exposesValidatedInitialProperties() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_2_f, 2.0f, 0.1f, 100.0f);

        assertThat(camera.fieldOfView()).isEqualTo(PI_OVER_2_f);
        assertThat(camera.aspectRatio()).isEqualTo(2.0f);
        assertThat(camera.near()).isEqualTo(0.1f);
        assertThat(camera.far()).isEqualTo(100.0f);
    }

    @Test
    void projectsRepresentativePointsToOpenGlNdc() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_2_f, 1.0f, 1.0f, 10.0f);

        assertNdc(camera.projectionMatrix(), 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f);
        assertNdc(camera.projectionMatrix(), 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f);
        assertNdc(camera.projectionMatrix(), 0.0f, 0.0f, -10.0f, 0.0f, 0.0f, 1.0f);
    }

    @Test
    void exposesStableMutuallyInverseProjectionMatrices() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_2_f, 1.5f, 0.1f, 100.0f);
        Matrix4fc projection = camera.projectionMatrix();
        Matrix4fc inverse = camera.inverseProjectionMatrix();

        assertThat(projection).isSameAs(camera.projectionMatrix());
        assertThat(inverse).isSameAs(camera.inverseProjectionMatrix());
        assertThat(new Matrix4f(projection).mul(inverse).equals(new Matrix4f(), EPSILON))
                .isTrue();
    }

    @Test
    void updatesProjectionLazilyAndVersionsOnlyRealChanges() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_2_f, 1.0f, 0.1f, 100.0f);
        Matrix4fc projection = camera.projectionMatrix();
        float initialHorizontalScale = projection.m00();
        long initialVersion = camera.projectionVersion();

        camera.setAspectRatio(2.0f);

        assertThat(camera.projectionVersion()).isEqualTo(initialVersion + 1L);
        assertThat(camera.projectionMatrix()).isSameAs(projection);
        assertThat(projection.m00()).isNotEqualTo(initialHorizontalScale);

        camera.setAspectRatio(2.0f);
        assertThat(camera.projectionVersion()).isEqualTo(initialVersion + 1L);
    }

    @Test
    void atomicallyChangesFieldOfViewAndClippingPlanes() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_2_f, 1.0f, 0.1f, 100.0f);

        camera.setFieldOfView(1.0f);
        camera.setClippingPlanes(0.5f, 500.0f);

        assertThat(camera.fieldOfView()).isEqualTo(1.0f);
        assertThat(camera.near()).isEqualTo(0.5f);
        assertThat(camera.far()).isEqualTo(500.0f);
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatIllegalArgumentException().isThrownBy(() -> new PerspectiveCamera(0.0f, 1.0f, 0.1f, 100.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PerspectiveCamera((float) Math.PI, 1.0f, 0.1f, 100.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new PerspectiveCamera(1.0f, 0.0f, 0.1f, 100.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new PerspectiveCamera(1.0f, 1.0f, 0.0f, 100.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new PerspectiveCamera(1.0f, 1.0f, 1.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new PerspectiveCamera(Float.NaN, 1.0f, 0.1f, 100.0f));
    }

    @Test
    void rejectsInvalidMutationWithoutChangingState() {
        PerspectiveCamera camera = new PerspectiveCamera(1.0f, 1.0f, 0.1f, 100.0f);

        assertThatIllegalArgumentException().isThrownBy(() -> camera.setFieldOfView(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.setAspectRatio(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.setClippingPlanes(10.0f, 1.0f));

        assertThat(camera.fieldOfView()).isEqualTo(1.0f);
        assertThat(camera.aspectRatio()).isEqualTo(1.0f);
        assertThat(camera.near()).isEqualTo(0.1f);
        assertThat(camera.far()).isEqualTo(100.0f);
    }
}
