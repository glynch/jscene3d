/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

/** Reusable assertions for JOML values. */
final class JomlAssertions {
    static final float EPSILON = 1.0e-5f;

    private JomlAssertions() {
        throw new AssertionError("JomlAssertions cannot be instantiated");
    }

    static void assertVector(Vector3fc actual, float expectedX, float expectedY, float expectedZ) {
        assertThat(actual.x()).isCloseTo(expectedX, within(EPSILON));
        assertThat(actual.y()).isCloseTo(expectedY, within(EPSILON));
        assertThat(actual.z()).isCloseTo(expectedZ, within(EPSILON));
    }

    static void assertQuaternion(
            Quaternionfc actual, float expectedX, float expectedY, float expectedZ, float expectedW) {
        assertThat(actual.x()).isCloseTo(expectedX, within(EPSILON));
        assertThat(actual.y()).isCloseTo(expectedY, within(EPSILON));
        assertThat(actual.z()).isCloseTo(expectedZ, within(EPSILON));
        assertThat(actual.w()).isCloseTo(expectedW, within(EPSILON));
    }
}
