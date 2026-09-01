/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

final class EnvironmentPrefilterTest {
    @Test
    void preservesConstantRadianceAcrossDiffuseAndReflectionConvolution() {
        EnvironmentPrefilter prefilter =
                new EnvironmentPrefilter(2, 1, new float[] {1.0f, 2.0f, 4.0f, 1.0f, 2.0f, 4.0f});

        float[] irradiance = prefilter.irradiance();
        List<EnvironmentPrefilter.Level> reflections = prefilter.reflections();

        assertTriples(irradiance, 1.0f, 2.0f, 4.0f);
        assertThat(reflections).hasSize(9);
        for (EnvironmentPrefilter.Level level : reflections) {
            assertTriples(level.pixels(), 1.0f, 2.0f, 4.0f);
        }
    }

    /** Asserts every consecutive RGB triple within a small floating-point tolerance. */
    private static void assertTriples(float[] values, float red, float green, float blue) {
        for (int index = 0; index < values.length; index += 3) {
            assertThat(values[index]).isCloseTo(red, within());
            assertThat(values[index + 1]).isCloseTo(green, within());
            assertThat(values[index + 2]).isCloseTo(blue, within());
        }
    }

    /** Returns a shared assertion tolerance without static-import ambiguity. */
    private static Offset<Float> within() {
        return offset(0.0001f);
    }
}
