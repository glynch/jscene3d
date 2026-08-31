/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.raycasting;

import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;
import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class RaycasterTest {
    @Test
    void copiesAndRobustlyNormalizesExplicitRayValues() {
        Vector3f origin = new Vector3f(1.0f, 2.0f, 3.0f);
        Vector3f direction = new Vector3f(Float.MAX_VALUE, 0.0f, -Float.MAX_VALUE);
        Raycaster raycaster = new Raycaster(origin, direction);
        origin.zero();
        direction.zero();

        assertVector(raycaster.origin(new Vector3f()), 1.0f, 2.0f, 3.0f);
        float inverseSquareRootTwo = (float) (1.0 / Math.sqrt(2.0));
        assertVector(raycaster.direction(new Vector3f()), inverseSquareRootTwo, 0.0f, -inverseSquareRootTwo);

        raycaster.setRay(4.0f, 5.0f, 6.0f, 0.0f, 3.0f, 0.0f);
        assertVector(raycaster.origin(new Vector3f()), 4.0f, 5.0f, 6.0f);
        assertVector(raycaster.direction(new Vector3f()), 0.0f, 1.0f, 0.0f);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsInvalidRayValuesAndDestinations() {
        Raycaster raycaster = new Raycaster();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> raycaster.setRay(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
                .withMessage("direction must not have zero length");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> raycaster.setRay(Float.NaN, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> raycaster.setRay(0.0f, 0.0f, 0.0f, Float.POSITIVE_INFINITY, 0.0f, -1.0f));
        assertThatNullPointerException().isThrownBy(() -> raycaster.setRay(null, new Vector3f(0.0f, 0.0f, -1.0f)));
        assertThatNullPointerException().isThrownBy(() -> raycaster.setRay(new Vector3f(), null));
        assertThatNullPointerException().isThrownBy(() -> raycaster.origin(null));
        assertThatNullPointerException().isThrownBy(() -> raycaster.direction(null));
    }

    @Test
    void createsPerspectiveRaysFromNormalizedCameraCoordinates() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_TWO, 1.0f, 0.1f, 100.0f);
        camera.setPosition(0.0f, 0.0f, 2.0f);
        Raycaster raycaster = new Raycaster();

        raycaster.setFromCamera(0.0f, 0.0f, camera);
        assertVector(raycaster.origin(new Vector3f()), 0.0f, 0.0f, 2.0f);
        assertVector(raycaster.direction(new Vector3f()), 0.0f, 0.0f, -1.0f);

        raycaster.setFromCamera(1.0f, 0.0f, camera);
        float inverseSquareRootTwo = (float) (1.0 / Math.sqrt(2.0));
        assertVector(raycaster.direction(new Vector3f()), inverseSquareRootTwo, 0.0f, -inverseSquareRootTwo);
    }

    @Test
    void createsParallelOrthographicRaysFromTheCameraPlane() {
        OrthographicCamera camera = new OrthographicCamera(-2.0f, 2.0f, 2.0f, -2.0f, 0.1f, 100.0f);
        camera.setZoom(2.0f);
        camera.setPosition(0.0f, 0.0f, 2.0f);
        Raycaster raycaster = new Raycaster();

        raycaster.setFromCamera(1.0f, -1.0f, camera);

        assertVector(raycaster.origin(new Vector3f()), 1.0f, -1.0f, 2.0f);
        assertVector(raycaster.direction(new Vector3f()), 0.0f, 0.0f, -1.0f);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void validatesCameraRayInputsAndTransforms() {
        PerspectiveCamera camera = new PerspectiveCamera(PI_OVER_TWO, 1.0f, 0.1f, 100.0f);
        Raycaster raycaster = new Raycaster();

        assertThatIllegalArgumentException().isThrownBy(() -> raycaster.setFromCamera(Float.NaN, 0.0f, camera));
        assertThatNullPointerException().isThrownBy(() -> raycaster.setFromCamera(0.0f, 0.0f, null));

        camera.setScale(1.0f, 0.0f, 1.0f);
        assertThatIllegalStateException().isThrownBy(() -> raycaster.setFromCamera(0.0f, 0.0f, camera));
    }
}
