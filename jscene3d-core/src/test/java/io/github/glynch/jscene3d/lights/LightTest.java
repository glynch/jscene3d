/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class LightTest {
    @Test
    void providesDocumentedDefaults() {
        AmbientLight ambientLight = new AmbientLight();
        PointLight pointLight = new PointLight();

        assertThat(ambientLight.color()).isEqualTo(Color.WHITE);
        assertThat(ambientLight.intensity()).isEqualTo(1.0f);
        assertThat(pointLight.color()).isEqualTo(Color.WHITE);
        assertThat(pointLight.intensity()).isEqualTo(1.0f);
        assertThat(pointLight.distance()).isZero();
        assertThat(pointLight.decay()).isEqualTo(2.0f);
    }

    @Test
    void supportsColorAndIntensityConvenienceConstructors() {
        AmbientLight ambientLight = new AmbientLight(Color.BLUE);
        PointLight pointLight = new PointLight(Color.RED, 12.0f);

        assertThat(ambientLight.color()).isEqualTo(Color.BLUE);
        assertThat(ambientLight.intensity()).isEqualTo(1.0f);
        assertThat(pointLight.color()).isEqualTo(Color.RED);
        assertThat(pointLight.intensity()).isEqualTo(12.0f);
    }

    @Test
    void mutatesValidatedLightProperties() {
        PointLight light = new PointLight();

        light.setColor(Color.CYAN);
        light.setIntensity(20.0f);
        light.setDistance(100.0f);
        light.setDecay(1.5f);

        assertThat(light.color()).isEqualTo(Color.CYAN);
        assertThat(light.intensity()).isEqualTo(20.0f);
        assertThat(light.distance()).isEqualTo(100.0f);
        assertThat(light.decay()).isEqualTo(1.5f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidLightProperties() {
        PointLight light = new PointLight();

        assertThatNullPointerException().isThrownBy(() -> new AmbientLight(null));
        assertThatNullPointerException().isThrownBy(() -> new PointLight(null, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> light.setColor(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new AmbientLight(Color.WHITE, -1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setIntensity(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setIntensity(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDistance(Float.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDistance(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDecay(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDecay(-1.0f));
    }

    @Test
    void derivesPointLightWorldPositionFromItsHierarchy() {
        Object3D parent = new Object3D();
        PointLight light = new PointLight();
        Vector3f worldPosition = new Vector3f();
        parent.setPosition(10.0f, 20.0f, 30.0f);
        light.setPosition(1.0f, 2.0f, 3.0f);
        parent.add(light);

        assertThat(light.worldPosition(worldPosition)).isSameAs(worldPosition);
        assertVector(worldPosition, 11.0f, 22.0f, 33.0f);

        parent.setPosition(-5.0f, 0.0f, 5.0f);
        light.worldPosition(worldPosition);
        assertVector(worldPosition, -4.0f, 2.0f, 8.0f);
    }

    @Test
    void ambientTransformDoesNotChangeItsIlluminationProperties() {
        AmbientLight light = new AmbientLight(Color.YELLOW, 0.25f);

        light.setPosition(10.0f, 20.0f, 30.0f);
        light.rotateX(1.0f);
        light.setScale(2.0f, 3.0f, 4.0f);

        assertThat(light.color()).isEqualTo(Color.YELLOW);
        assertThat(light.intensity()).isEqualTo(0.25f);
    }

    @Test
    void participatesInHierarchyAndVisibleTraversal() {
        Object3D root = new Object3D();
        AmbientLight ambientLight = new AmbientLight();
        PointLight pointLight = new PointLight();
        root.add(ambientLight);
        root.add(pointLight);
        List<Object3D> visited = new ArrayList<>();

        root.traverseVisible(visited::add);
        assertThat(visited).containsExactly(root, ambientLight, pointLight);

        ambientLight.setVisible(false);
        visited.clear();
        root.traverseVisible(visited::add);
        assertThat(visited).containsExactly(root, pointLight);
    }
}
