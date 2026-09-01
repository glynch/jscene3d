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

final class ExponentialSquaredFogTest {
    @Test
    void retainsMutableColorAndDensityConfiguration() {
        ExponentialSquaredFog fog = new ExponentialSquaredFog(Color.GRAY, 0.02f);

        fog.setColor(Color.BLUE);
        fog.setDensity(0.05f);

        assertThat(fog.color()).isSameAs(Color.BLUE);
        assertThat(fog.density()).isEqualTo(0.05f);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidConfiguration() {
        assertThatNullPointerException().isThrownBy(() -> new ExponentialSquaredFog(null, 0.01f));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExponentialSquaredFog(Color.WHITE, -0.01f));
        assertThatIllegalArgumentException().isThrownBy(() -> new ExponentialSquaredFog(Color.WHITE, Float.NaN));

        ExponentialSquaredFog fog = new ExponentialSquaredFog(Color.WHITE, 0.01f);
        assertThatNullPointerException().isThrownBy(() -> fog.setColor(null));
        assertThatIllegalArgumentException().isThrownBy(() -> fog.setDensity(-0.01f));
    }
}
