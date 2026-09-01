/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class LightShadowTest {
    @Test
    void providesStableOwnedDescriptionsWithDocumentedDefaults() {
        DirectionalLight directional = new DirectionalLight();
        SpotLight spot = new SpotLight();
        PointLight point = new PointLight();

        assertThat(directional.shadow()).isSameAs(directional.shadow());
        assertThat(spot.shadow()).isSameAs(spot.shadow());
        assertThat(point.shadow()).isSameAs(point.shadow());
        assertCommonDefaults(directional, directional.shadow());
        assertCommonDefaults(spot, spot.shadow());
        assertCommonDefaults(point, point.shadow());
        assertThat(directional.shadow().cameraLeft()).isEqualTo(-5.0f);
        assertThat(directional.shadow().cameraRight()).isEqualTo(5.0f);
        assertThat(directional.shadow().cameraBottom()).isEqualTo(-5.0f);
        assertThat(directional.shadow().cameraTop()).isEqualTo(5.0f);
    }

    @Test
    void mutatesCommonAndDirectionalConfiguration() {
        DirectionalLight light = new DirectionalLight();
        DirectionalLightShadow shadow = light.shadow();

        light.setShadowCastingEnabled(true);
        shadow.setMapSize(1024, 2048);
        shadow.setBias(-0.0005f);
        shadow.setNormalBias(0.025f);
        shadow.setCameraRange(0.1f, 80.0f);
        shadow.setCameraBounds(-12.0f, 8.0f, -6.0f, 14.0f);

        assertThat(light.isShadowCastingEnabled()).isTrue();
        assertThat(shadow.mapWidth()).isEqualTo(1024);
        assertThat(shadow.mapHeight()).isEqualTo(2048);
        assertThat(shadow.bias()).isEqualTo(-0.0005f);
        assertThat(shadow.normalBias()).isEqualTo(0.025f);
        assertThat(shadow.cameraNear()).isEqualTo(0.1f);
        assertThat(shadow.cameraFar()).isEqualTo(80.0f);
        assertThat(shadow.cameraLeft()).isEqualTo(-12.0f);
        assertThat(shadow.cameraRight()).isEqualTo(8.0f);
        assertThat(shadow.cameraBottom()).isEqualTo(-6.0f);
        assertThat(shadow.cameraTop()).isEqualTo(14.0f);
    }

    @Test
    void acceptsSquarePointShadowMaps() {
        PointLightShadow shadow = new PointLight().shadow();

        shadow.setMapSize(256, 256);

        assertThat(shadow.mapWidth()).isEqualTo(256);
        assertThat(shadow.mapHeight()).isEqualTo(256);
    }

    @Test
    void rejectsInvalidConfigurationWithoutPartialMutation() {
        DirectionalLightShadow directional = new DirectionalLight().shadow();
        PointLightShadow point = new PointLight().shadow();

        assertThatIllegalArgumentException().isThrownBy(() -> directional.setMapSize(0, 512));
        assertThatIllegalArgumentException().isThrownBy(() -> point.setMapSize(256, 512));
        assertThatIllegalArgumentException().isThrownBy(() -> directional.setBias(Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> directional.setNormalBias(Float.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(() -> directional.setCameraRange(0.0f, 10.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> directional.setCameraRange(10.0f, 10.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> directional.setCameraBounds(1.0f, 1.0f, -1.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> directional.setCameraBounds(-1.0f, 1.0f, 2.0f, 1.0f));

        assertThat(directional.mapWidth()).isEqualTo(LightShadow.DEFAULT_MAP_SIZE);
        assertThat(directional.mapHeight()).isEqualTo(LightShadow.DEFAULT_MAP_SIZE);
        assertThat(point.mapWidth()).isEqualTo(LightShadow.DEFAULT_MAP_SIZE);
        assertThat(point.mapHeight()).isEqualTo(LightShadow.DEFAULT_MAP_SIZE);
        assertThat(directional.cameraNear()).isEqualTo(0.5f);
        assertThat(directional.cameraFar()).isEqualTo(500.0f);
    }

    /** Asserts defaults shared by each shadow-capable light kind. */
    private static void assertCommonDefaults(ShadowCastingLight light, LightShadow shadow) {
        assertThat(light.isShadowCastingEnabled()).isFalse();
        assertThat(shadow.mapWidth()).isEqualTo(LightShadow.DEFAULT_MAP_SIZE);
        assertThat(shadow.mapHeight()).isEqualTo(LightShadow.DEFAULT_MAP_SIZE);
        assertThat(shadow.bias()).isZero();
        assertThat(shadow.normalBias()).isZero();
        assertThat(shadow.cameraNear()).isEqualTo(0.5f);
        assertThat(shadow.cameraFar()).isEqualTo(500.0f);
    }
}
