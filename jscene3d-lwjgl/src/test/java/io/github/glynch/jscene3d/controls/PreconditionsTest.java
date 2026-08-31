/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.controls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class PreconditionsTest {
    @Test
    void acceptsValidControlArguments() {
        assertThat(Preconditions.requireNonNegative(0.0f, "value")).isZero();
        assertThat(Preconditions.requirePositive(1.0f, "value")).isOne();
        assertThat(Preconditions.requireFinite(-1.0f, "value")).isEqualTo(-1.0f);
        assertThat(Preconditions.requireInRange(1.0f, "value", 0.0f, 1.0f)).isOne();
        Preconditions.requireOrdered(1.0f, "minimum", 1.0f, "maximum");
        assertThat(Preconditions.requireGreaterThan(2.0f, "value", 1.0f)).isEqualTo(2.0f);
        Preconditions.requireSpanLessThan(0.0f, "minimum", 1.0f, "maximum", 2.0f);
    }

    @Test
    void rejectsInvalidControlArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireNonNegative(-1.0f, "value"));
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireNonNegative(Float.NaN, "value"));
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requirePositive(0.0f, "value"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requirePositive(Float.POSITIVE_INFINITY, "value"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireFinite(Float.NEGATIVE_INFINITY, "value"));
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireInRange(-1.0f, "value", 0.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireInRange(2.0f, "value", 0.0f, 1.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireOrdered(2.0f, "minimum", 1.0f, "maximum"));
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireGreaterThan(1.0f, "value", 1.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireSpanLessThan(0.0f, "minimum", 2.0f, "maximum", 2.0f));
    }
}
