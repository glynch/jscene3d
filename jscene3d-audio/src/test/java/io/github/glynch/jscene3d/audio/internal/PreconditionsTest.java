/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Verifies shared audio value validation independently from a native device. */
final class PreconditionsTest {
    /** Accepts boundary gains and rejects non-finite or out-of-range values. */
    @Test
    void validatesUnitIntervals() {
        assertThat(Preconditions.requireUnitInterval(0.0F, "gain")).isZero();
        assertThat(Preconditions.requireUnitInterval(1.0F, "gain")).isOne();

        assertThatThrownBy(() -> Preconditions.requireUnitInterval(-0.1F, "gain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gain");
        assertThatThrownBy(() -> Preconditions.requireUnitInterval(Float.NaN, "gain"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireUnitInterval(1.1F, "gain"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Distinguishes non-negative and strictly positive finite values. */
    @Test
    void validatesScalarRanges() {
        assertThat(Preconditions.requireNonNegative(0.0F, "rolloff")).isZero();
        assertThat(Preconditions.requirePositive(0.1F, "pitch")).isEqualTo(0.1F);

        assertThatThrownBy(() -> Preconditions.requireNonNegative(-0.1F, "rolloff"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requirePositive(0.0F, "pitch"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requirePositive(Float.POSITIVE_INFINITY, "pitch"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Copies finite vectors and normalizes only valid non-zero directions. */
    @Test
    void validatesVectorsAndDirections() {
        Vector3f source = new Vector3f(2.0F, 0.0F, 0.0F);

        assertThat(Preconditions.requireFinite(source, "position"))
                .isEqualTo(source)
                .isNotSameAs(source);
        assertThat(Preconditions.requireDirection(source, "forward")).isEqualTo(new Vector3f(1.0F, 0.0F, 0.0F));

        Vector3f nonFinite = new Vector3f(Float.NaN, 0.0F, 0.0F);
        Vector3f zero = new Vector3f();
        assertThatThrownBy(() -> Preconditions.requireFinite(nonFinite, "position"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Preconditions.requireDirection(zero, "forward"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
