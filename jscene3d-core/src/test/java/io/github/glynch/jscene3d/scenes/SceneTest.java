/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.scenes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.fogs.LinearFog;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.textures.EnvironmentMap;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class SceneTest {
    @Test
    void sceneOwnsAnOptionalSolidBackground() {
        Scene scene = new Scene();

        assertThat(scene.background()).isNull();

        scene.setBackground(Color.GRAY);
        assertThat(scene.background()).isSameAs(Color.GRAY);

        scene.clearBackground();
        assertThat(scene.background()).isNull();
    }

    @Test
    void sceneSharesOptionalFog() {
        Scene scene = new Scene();
        LinearFog fog = new LinearFog(Color.GRAY, 2.0f, 20.0f);

        assertThat(scene.fog()).isNull();

        scene.setFog(fog);
        assertThat(scene.fog()).isSameAs(fog);

        scene.clearFog();
        assertThat(scene.fog()).isNull();
    }

    @Test
    @SuppressWarnings("NullAway")
    void sceneRejectsANullFogAssignment() {
        Scene scene = new Scene();

        assertThatNullPointerException().isThrownBy(() -> scene.setFog(null)).withMessage("fog");
    }

    @Test
    @SuppressWarnings("NullAway")
    void sceneRejectsANullBackgroundAssignment() {
        Scene scene = new Scene();

        assertThatNullPointerException()
                .isThrownBy(() -> scene.setBackground(null))
                .withMessage("background");
    }

    @Test
    void keepsVisibleAndLightingEnvironmentsIndependent() {
        try (EnvironmentMap background = EnvironmentMap.equirectangular(1, 1, new float[3]);
                EnvironmentMap lighting = EnvironmentMap.equirectangular(1, 1, new float[3])) {
            Scene scene = new Scene();

            scene.setBackgroundEnvironment(background);
            scene.setEnvironment(lighting);
            scene.setBackgroundIntensity(0.75f);
            scene.setEnvironmentIntensity(1.5f);
            scene.setEnvironmentRotation(0.0f, 0.5f, 0.0f);

            assertThat(scene.background()).isNull();
            assertThat(scene.backgroundEnvironment()).isSameAs(background);
            assertThat(scene.environment()).isSameAs(lighting);
            assertThat(scene.backgroundIntensity()).isEqualTo(0.75f);
            assertThat(scene.environmentIntensity()).isEqualTo(1.5f);
            assertThat(scene.environmentRotation(new Quaternionf())).isEqualTo(new Quaternionf().rotationY(0.5f));

            scene.setBackground(Color.BLUE);
            scene.clearEnvironment();
            assertThat(scene.background()).isSameAs(Color.BLUE);
            assertThat(scene.backgroundEnvironment()).isNull();
            assertThat(scene.environment()).isNull();
        }
    }

    @Test
    void rejectsInvalidEnvironmentState() {
        Scene scene = new Scene();
        EnvironmentMap closed = EnvironmentMap.equirectangular(1, 1, new float[3]);
        closed.close();

        assertThatIllegalArgumentException().isThrownBy(() -> scene.setEnvironment(closed));
        assertThatIllegalArgumentException().isThrownBy(() -> scene.setBackgroundEnvironment(closed));
        assertThatIllegalArgumentException().isThrownBy(() -> scene.setEnvironmentIntensity(-0.1f));
        assertThatIllegalArgumentException().isThrownBy(() -> scene.setBackgroundIntensity(Float.NaN));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> scene.setEnvironmentRotation(new Quaternionf(0.0f, 0.0f, 0.0f, 0.0f)));
    }

    @Test
    void sceneAndGroupRetainObject3DBehavior() {
        Scene scene = new Scene();
        Group group = new Group();
        Object3D child = new Object3D();
        scene.setPosition(1.0f, 0.0f, 0.0f);
        group.setPosition(2.0f, 0.0f, 0.0f);

        scene.add(group);
        group.add(child);

        assertThat(scene.children()).containsExactly(group);
        assertThat(group.children()).containsExactly(child);
        assertThat(child.worldPosition(new Vector3f()).x()).isEqualTo(3.0f);
    }
}
