/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.scenes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.Object3D;
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
    @SuppressWarnings("NullAway")
    void sceneRejectsANullBackgroundAssignment() {
        Scene scene = new Scene();

        assertThatNullPointerException()
                .isThrownBy(() -> scene.setBackground(null))
                .withMessage("background");
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
