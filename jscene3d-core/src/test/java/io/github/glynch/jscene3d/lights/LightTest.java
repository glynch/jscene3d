/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.joml.Math.toRadians;

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
        DirectionalLight directionalLight = new DirectionalLight();
        PointLight pointLight = new PointLight();
        Vector3f target = new Vector3f();
        Vector3f position = new Vector3f();

        assertThat(ambientLight.color()).isEqualTo(Color.WHITE);
        assertThat(ambientLight.intensity()).isEqualTo(1.0f);
        assertThat(directionalLight.color()).isEqualTo(Color.WHITE);
        assertThat(directionalLight.intensity()).isEqualTo(1.0f);
        assertVector(directionalLight.worldPosition(position), 0.0f, 1.0f, 0.0f);
        assertVector(directionalLight.target(target), 0.0f, 0.0f, 0.0f);
        assertThat(pointLight.color()).isEqualTo(Color.WHITE);
        assertThat(pointLight.intensity()).isEqualTo(1.0f);
        assertThat(pointLight.distance()).isZero();
        assertThat(pointLight.decay()).isEqualTo(2.0f);
    }

    @Test
    void supportsColorAndIntensityConvenienceConstructors() {
        AmbientLight ambientLight = new AmbientLight(Color.BLUE);
        DirectionalLight directionalLight = new DirectionalLight(Color.GREEN, 0.5f);
        PointLight pointLight = new PointLight(Color.RED, 12.0f);

        assertThat(ambientLight.color()).isEqualTo(Color.BLUE);
        assertThat(ambientLight.intensity()).isEqualTo(1.0f);
        assertThat(directionalLight.color()).isEqualTo(Color.GREEN);
        assertThat(directionalLight.intensity()).isEqualTo(0.5f);
        assertThat(pointLight.color()).isEqualTo(Color.RED);
        assertThat(pointLight.intensity()).isEqualTo(12.0f);
    }

    @Test
    void providesDocumentedHemisphereDefaultsAndMutation() {
        HemisphereLight light = new HemisphereLight();
        Vector3f position = new Vector3f();

        assertThat(light.color()).isEqualTo(Color.WHITE);
        assertThat(light.groundColor()).isEqualTo(Color.WHITE);
        assertThat(light.intensity()).isEqualTo(1.0f);
        assertVector(light.worldPosition(position), 0.0f, 1.0f, 0.0f);

        light.setColor(Color.BLUE);
        light.setGroundColor(Color.RED);
        light.setIntensity(0.5f);

        assertThat(light.color()).isEqualTo(Color.BLUE);
        assertThat(light.groundColor()).isEqualTo(Color.RED);
        assertThat(light.intensity()).isEqualTo(0.5f);
    }

    @Test
    void providesDocumentedSpotlightDefaultsAndMutation() {
        SpotLight light = new SpotLight();
        Vector3f target = new Vector3f();
        Vector3f position = new Vector3f();

        assertThat(light.color()).isEqualTo(Color.WHITE);
        assertThat(light.intensity()).isEqualTo(1.0f);
        assertThat(light.distance()).isZero();
        assertThat(light.decay()).isEqualTo(2.0f);
        assertThat(light.angle()).isEqualTo(toRadians(60.0f));
        assertThat(light.penumbra()).isZero();
        assertVector(light.worldPosition(position), 0.0f, 1.0f, 0.0f);
        assertVector(light.target(target), 0.0f, 0.0f, 0.0f);

        light.setDistance(12.0f);
        light.setDecay(1.0f);
        light.setAngle(toRadians(30.0f));
        light.setPenumbra(0.5f);
        light.setTarget(1.0f, 2.0f, 3.0f);

        assertThat(light.distance()).isEqualTo(12.0f);
        assertThat(light.decay()).isEqualTo(1.0f);
        assertThat(light.angle()).isEqualTo(toRadians(30.0f));
        assertThat(light.penumbra()).isEqualTo(0.5f);
        assertVector(light.target(target), 1.0f, 2.0f, 3.0f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidHemisphereAndSpotlightProperties() {
        HemisphereLight hemisphereLight = new HemisphereLight();
        SpotLight spotLight = new SpotLight();

        assertThatNullPointerException().isThrownBy(() -> new HemisphereLight(null, Color.WHITE));
        assertThatNullPointerException().isThrownBy(() -> new HemisphereLight(Color.WHITE, null));
        assertThatNullPointerException().isThrownBy(() -> hemisphereLight.setGroundColor(null));
        assertThatNullPointerException().isThrownBy(() -> new SpotLight(null));
        assertThatNullPointerException().isThrownBy(() -> spotLight.setTarget(null));
        assertThatNullPointerException().isThrownBy(() -> spotLight.target(null));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setDistance(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setDecay(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setAngle(0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setAngle(toRadians(91.0f)));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setPenumbra(-0.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setPenumbra(1.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> spotLight.setTarget(0.0f, Float.NaN, 0.0f));
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
    void copiesDirectionalTargetValuesInAndOut() {
        DirectionalLight light = new DirectionalLight();
        Vector3f suppliedTarget = new Vector3f(1.0f, 2.0f, 3.0f);
        Vector3f copiedTarget = new Vector3f();

        light.setTarget(suppliedTarget);
        suppliedTarget.set(9.0f, 9.0f, 9.0f);

        assertThat(light.target(copiedTarget)).isSameAs(copiedTarget);
        assertVector(copiedTarget, 1.0f, 2.0f, 3.0f);

        light.setTarget(-1.0f, -2.0f, -3.0f);
        light.target(copiedTarget);
        assertVector(copiedTarget, -1.0f, -2.0f, -3.0f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidLightProperties() {
        PointLight light = new PointLight();
        DirectionalLight directionalLight = new DirectionalLight();

        assertThatNullPointerException().isThrownBy(() -> new AmbientLight(null));
        assertThatNullPointerException().isThrownBy(() -> new PointLight(null, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> new DirectionalLight(null));
        assertThatNullPointerException().isThrownBy(() -> light.setColor(null));
        assertThatNullPointerException().isThrownBy(() -> directionalLight.setTarget(null));
        assertThatNullPointerException().isThrownBy(() -> directionalLight.target(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new AmbientLight(Color.WHITE, -1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setIntensity(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setIntensity(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDistance(Float.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDistance(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDecay(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> light.setDecay(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> directionalLight.setTarget(Float.NaN, 0.0f, 0.0f));
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
        DirectionalLight directionalLight = new DirectionalLight();
        PointLight pointLight = new PointLight();
        HemisphereLight hemisphereLight = new HemisphereLight();
        SpotLight spotLight = new SpotLight();
        root.add(ambientLight);
        root.add(directionalLight);
        root.add(hemisphereLight);
        root.add(pointLight);
        root.add(spotLight);
        List<Object3D> visited = new ArrayList<>();

        root.traverseVisible(visited::add);
        assertThat(visited)
                .containsExactly(root, ambientLight, directionalLight, hemisphereLight, pointLight, spotLight);

        ambientLight.setVisible(false);
        visited.clear();
        root.traverseVisible(visited::add);
        assertThat(visited).containsExactly(root, directionalLight, hemisphereLight, pointLight, spotLight);
    }
}
