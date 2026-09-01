/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.fogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class LinearFogTest {
    @Test
    void retainsMutableColorAndDistanceConfiguration() {
        LinearFog fog = new LinearFog(Color.GRAY, 4.0f, 20.0f);

        fog.setColor(Color.BLUE);
        fog.setNearDistance(5.0f);
        fog.setFarDistance(24.0f);

        assertThat(fog.color()).isSameAs(Color.BLUE);
        assertThat(fog.nearDistance()).isEqualTo(5.0f);
        assertThat(fog.farDistance()).isEqualTo(24.0f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidConfiguration() {
        assertThatNullPointerException().isThrownBy(() -> new LinearFog(null, 1.0f, 2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new LinearFog(Color.WHITE, -1.0f, 2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new LinearFog(Color.WHITE, 2.0f, 2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new LinearFog(Color.WHITE, 3.0f, 2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> new LinearFog(Color.WHITE, 1.0f, Float.NaN));

        LinearFog fog = new LinearFog(Color.WHITE, 1.0f, 3.0f);
        assertThatNullPointerException().isThrownBy(() -> fog.setColor(null));
        assertThatIllegalArgumentException().isThrownBy(() -> fog.setNearDistance(3.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> fog.setFarDistance(1.0f));
    }
}
