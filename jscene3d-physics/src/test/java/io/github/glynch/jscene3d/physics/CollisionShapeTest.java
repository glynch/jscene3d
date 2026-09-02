/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.physics.shapes.BoxShape;
import io.github.glynch.jscene3d.physics.shapes.CapsuleShape;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import org.junit.jupiter.api.Test;

final class CollisionShapeTest {
    @Test
    void rejectsInvalidSphereDimensions() {
        assertThatThrownBy(() -> new SphereShape(0.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SphereShape(Float.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidBoxDimensions() {
        assertThatThrownBy(() -> new BoxShape(-1.0F, 1.0F, 1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoxShape(Float.NaN, 1.0F, 1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoxShape(1.0F, Float.POSITIVE_INFINITY, 1.0F))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoxShape(1.0F, 0.0F, 1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoxShape(1.0F, 1.0F, -1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoxShape(1.0F, 1.0F, Float.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void permitsSphereLikeCapsuleAndRejectsInvalidDimensions() {
        new CapsuleShape(1.0F, 0.0F);

        assertThatThrownBy(() -> new CapsuleShape(0.0F, 1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CapsuleShape(Float.NaN, 1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CapsuleShape(1.0F, -1.0F)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CapsuleShape(1.0F, Float.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
