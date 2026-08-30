/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

final class AnglesTest {
    private static final float EPSILON = 1.0e-6f;

    @Test
    void exposesCommonAnglesInRadians() {
        assertThat(Angles.PI_OVER_SIX).isCloseTo((float) Math.toRadians(30.0), within(EPSILON));
        assertThat(Angles.PI_OVER_FOUR).isCloseTo((float) Math.toRadians(45.0), within(EPSILON));
        assertThat(Angles.PI_OVER_THREE).isCloseTo((float) Math.toRadians(60.0), within(EPSILON));
        assertThat(Angles.PI_OVER_TWO).isCloseTo((float) Math.toRadians(90.0), within(EPSILON));
        assertThat(Angles.PI).isCloseTo((float) Math.toRadians(180.0), within(EPSILON));
        assertThat(Angles.TWO_PI).isCloseTo((float) Math.toRadians(360.0), within(EPSILON));
    }
}
