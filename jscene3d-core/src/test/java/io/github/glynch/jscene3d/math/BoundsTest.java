/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.math;

import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class BoundsTest {
    @Test
    void exposesImmutableValueSemantics() {
        BoundingBox box = new BoundingBox(-1.0f, -2.0f, -3.0f, 4.0f, 5.0f, 6.0f);
        BoundingSphere sphere = new BoundingSphere(1.0f, 2.0f, 3.0f, 4.0f);

        assertVector(box.minimum(), -1.0f, -2.0f, -3.0f);
        assertVector(box.maximum(), 4.0f, 5.0f, 6.0f);
        assertVector(sphere.center(), 1.0f, 2.0f, 3.0f);
        assertThat(sphere.radius()).isEqualTo(4.0f);
        assertThat(box).isEqualTo(new BoundingBox(-1.0f, -2.0f, -3.0f, 4.0f, 5.0f, 6.0f));
        assertThat(sphere).isEqualTo(new BoundingSphere(1.0f, 2.0f, 3.0f, 4.0f));
    }

    @Test
    void rejectsInvalidBounds() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BoundingBox(1.0f, 0.0f, 0.0f, -1.0f, 1.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new BoundingBox(Float.NaN, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new BoundingSphere(0.0f, 0.0f, 0.0f, -1.0f));
    }
}
