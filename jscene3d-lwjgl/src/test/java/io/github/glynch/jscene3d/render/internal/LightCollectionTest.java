/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.lights.PointLight;
import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class LightCollectionTest {
    @Test
    void combinesAmbientLightsAndRetainsPointLightOrder() {
        LightCollection lights = new LightCollection(2, 2);
        DirectionalLight firstDirectional = new DirectionalLight(Color.GREEN, 0.75f);
        DirectionalLight secondDirectional = new DirectionalLight(Color.WHITE, 0.5f);
        PointLight firstPoint = new PointLight(Color.RED, 2.0f);
        PointLight secondPoint = new PointLight(Color.BLUE, 3.0f);

        lights.add(new AmbientLight(Color.RED, 0.25f));
        lights.add(firstDirectional);
        lights.add(firstPoint);
        lights.add(new AmbientLight(Color.BLUE, 0.5f));
        lights.add(secondDirectional);
        lights.add(secondPoint);

        assertThat(lights.ambientRed()).isEqualTo(0.25f);
        assertThat(lights.ambientGreen()).isZero();
        assertThat(lights.ambientBlue()).isEqualTo(0.5f);
        assertThat(lights.pointLightCount()).isEqualTo(2);
        assertThat(lights.pointLight(0)).isSameAs(firstPoint);
        assertThat(lights.pointLight(1)).isSameAs(secondPoint);
        assertThat(lights.directionalLightCount()).isEqualTo(2);
        assertThat(lights.directionalLight(0)).isSameAs(firstDirectional);
        assertThat(lights.directionalLight(1)).isSameAs(secondDirectional);
    }

    @Test
    void enforcesPointLightCapacityAndClearsForReuse() {
        LightCollection lights = new LightCollection(1, 1);
        PointLight pointLight = new PointLight();
        DirectionalLight directionalLight = new DirectionalLight();
        lights.add(pointLight);
        lights.add(directionalLight);

        assertThatIllegalStateException()
                .isThrownBy(() -> lights.add(new PointLight()))
                .withMessage("Scene has more visible point lights than Renderer supports: 2 > 1");
        assertThatIllegalStateException()
                .isThrownBy(() -> lights.add(new DirectionalLight()))
                .withMessage("Scene has more visible directional lights than Renderer supports: 2 > 1");

        lights.clear();
        lights.add(new AmbientLight(Color.GREEN, 0.5f));

        assertThat(lights.pointLightCount()).isZero();
        assertThat(lights.directionalLightCount()).isZero();
        assertThat(lights.ambientRed()).isZero();
        assertThat(lights.ambientGreen()).isEqualTo(0.5f);
        assertThat(lights.ambientBlue()).isZero();
    }

    @Test
    void rejectsAmbientAccumulationOverflow() {
        LightCollection lights = new LightCollection(1, 1);
        lights.add(new AmbientLight(Color.WHITE, Float.MAX_VALUE));

        assertThatIllegalStateException()
                .isThrownBy(() -> lights.add(new AmbientLight(Color.WHITE, Float.MAX_VALUE)))
                .withMessage("Combined AmbientLight red contribution is not finite");
    }
}
