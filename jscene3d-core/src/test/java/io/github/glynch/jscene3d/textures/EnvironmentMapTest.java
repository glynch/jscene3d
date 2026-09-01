/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.textures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

final class EnvironmentMapTest {
    @Test
    void ownsOneDefensiveLinearRgbCopy() {
        float[] source = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f};
        try (EnvironmentMap environmentMap = EnvironmentMap.equirectangular(2, 1, source)) {
            source[0] = 99.0f;
            FloatBuffer destination = FloatBuffer.allocate(6);

            environmentMap.copyPixelsTo(destination);

            assertThat(environmentMap.width()).isEqualTo(2);
            assertThat(environmentMap.height()).isOne();
            assertThat(environmentMap.pixelComponentCount()).isEqualTo(6);
            assertThat(destination.array()).containsExactly(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        }
    }

    @Test
    void rejectsInvalidImages() {
        assertThatIllegalArgumentException().isThrownBy(() -> EnvironmentMap.equirectangular(2, 1, new float[3]));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EnvironmentMap.equirectangular(1, 1, new float[] {1.0f, -1.0f, 1.0f}));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EnvironmentMap.equirectangular(1, 1, new float[] {1.0f, Float.NaN, 1.0f}));
    }

    @Test
    void closesTerminally() {
        EnvironmentMap environmentMap = EnvironmentMap.equirectangular(1, 1, new float[3]);

        environmentMap.close();
        environmentMap.close();

        assertThat(environmentMap.isClosed()).isTrue();
        assertThatIllegalStateException().isThrownBy(environmentMap::width);
    }
}
