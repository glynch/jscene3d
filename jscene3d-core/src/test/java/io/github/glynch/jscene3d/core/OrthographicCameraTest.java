/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static io.github.glynch.jscene3d.core.JomlAssertions.EPSILON;
import static io.github.glynch.jscene3d.core.JomlAssertions.assertNdc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.junit.jupiter.api.Test;

final class OrthographicCameraTest {
    @Test
    void exposesValidatedInitialProperties() {
        OrthographicCamera camera = new OrthographicCamera(-2.0f, 2.0f, 3.0f, -1.0f, 0.0f, 100.0f);

        assertThat(camera.left()).isEqualTo(-2.0f);
        assertThat(camera.right()).isEqualTo(2.0f);
        assertThat(camera.top()).isEqualTo(3.0f);
        assertThat(camera.bottom()).isEqualTo(-1.0f);
        assertThat(camera.near()).isZero();
        assertThat(camera.far()).isEqualTo(100.0f);
        assertThat(camera.zoom()).isEqualTo(1.0f);
    }

    @Test
    void projectsRepresentativePointsToOpenGlNdc() {
        OrthographicCamera camera = new OrthographicCamera(-2.0f, 2.0f, 3.0f, -1.0f, 1.0f, 11.0f);

        assertNdc(camera.projectionMatrix(), 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, -1.0f);
        assertNdc(camera.projectionMatrix(), -2.0f, 3.0f, -1.0f, -1.0f, 1.0f, -1.0f);
        assertNdc(camera.projectionMatrix(), 2.0f, -1.0f, -11.0f, 1.0f, -1.0f, 1.0f);
    }

    @Test
    void exposesStableMutuallyInverseProjectionMatrices() {
        OrthographicCamera camera = new OrthographicCamera(-2.0f, 2.0f, 3.0f, -1.0f, 0.0f, 100.0f);
        Matrix4fc projection = camera.projectionMatrix();
        Matrix4fc inverse = camera.inverseProjectionMatrix();

        assertThat(projection).isSameAs(camera.projectionMatrix());
        assertThat(inverse).isSameAs(camera.inverseProjectionMatrix());
        assertThat(new Matrix4f(projection).mul(inverse).equals(new Matrix4f(), EPSILON))
                .isTrue();
    }

    @Test
    void atomicallyChangesBoundsAndClippingPlanes() {
        OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.0f, 100.0f);
        long initialVersion = camera.projectionVersion();

        camera.setBounds(-2.0f, 2.0f, 3.0f, -3.0f);
        camera.setClippingPlanes(1.0f, 200.0f);

        assertThat(camera.left()).isEqualTo(-2.0f);
        assertThat(camera.right()).isEqualTo(2.0f);
        assertThat(camera.top()).isEqualTo(3.0f);
        assertThat(camera.bottom()).isEqualTo(-3.0f);
        assertThat(camera.near()).isEqualTo(1.0f);
        assertThat(camera.far()).isEqualTo(200.0f);
        assertThat(camera.projectionVersion()).isEqualTo(initialVersion + 2L);

        camera.setBounds(-2.0f, 2.0f, 3.0f, -3.0f);
        camera.setClippingPlanes(1.0f, 200.0f);
        assertThat(camera.projectionVersion()).isEqualTo(initialVersion + 2L);
    }

    @Test
    void changesZoomAndProjectionAroundTheBoundsCenter() {
        OrthographicCamera camera = new OrthographicCamera(-2.0f, 6.0f, 5.0f, -3.0f, 1.0f, 11.0f);
        long initialVersion = camera.projectionVersion();

        camera.setZoom(2.0f);

        assertThat(camera.zoom()).isEqualTo(2.0f);
        assertThat(camera.projectionVersion()).isEqualTo(initialVersion + 1L);
        assertNdc(camera.projectionMatrix(), 0.0f, 3.0f, -1.0f, -1.0f, 1.0f, -1.0f);
        assertNdc(camera.projectionMatrix(), 4.0f, -1.0f, -11.0f, 1.0f, -1.0f, 1.0f);

        camera.setZoom(2.0f);
        assertThat(camera.projectionVersion()).isEqualTo(initialVersion + 1L);
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrthographicCamera(1.0f, 1.0f, 1.0f, -1.0f, 0.0f, 100.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrthographicCamera(-1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 100.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 100.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 100.0f, 100.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrthographicCamera(Float.NaN, 1.0f, 1.0f, -1.0f, 0.0f, 100.0f));
    }

    @Test
    void rejectsInvalidMutationWithoutChangingState() {
        OrthographicCamera camera = new OrthographicCamera(-1.0f, 1.0f, 1.0f, -1.0f, 0.0f, 100.0f);

        assertThatIllegalArgumentException().isThrownBy(() -> camera.setBounds(2.0f, 1.0f, 1.0f, -1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.setBounds(-1.0f, 1.0f, -2.0f, -1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.setClippingPlanes(Float.NaN, 100.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> camera.setZoom(0.0f));

        assertThat(camera.left()).isEqualTo(-1.0f);
        assertThat(camera.right()).isEqualTo(1.0f);
        assertThat(camera.top()).isEqualTo(1.0f);
        assertThat(camera.bottom()).isEqualTo(-1.0f);
        assertThat(camera.near()).isZero();
        assertThat(camera.far()).isEqualTo(100.0f);
        assertThat(camera.zoom()).isEqualTo(1.0f);
    }
}
