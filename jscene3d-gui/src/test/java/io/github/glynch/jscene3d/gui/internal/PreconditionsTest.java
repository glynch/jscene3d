/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

final class PreconditionsTest {
    @Test
    void acceptsValidArguments() {
        assertThat(Preconditions.requirePositive(1, "count")).isOne();
        assertThat(Preconditions.requireFinite(1.0f, "value")).isOne();
        Preconditions.requireOrdered(1.0f, "minimum", 1.0f, "maximum");
        assertThat(Preconditions.requireNonBlank("value", "name")).isEqualTo("value");
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsInvalidArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requirePositive(0, "count"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireFinite(Float.POSITIVE_INFINITY, "value"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireOrdered(Float.NaN, "minimum", 1.0f, "maximum"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireOrdered(0.0f, "minimum", Float.NaN, "maximum"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Preconditions.requireOrdered(2.0f, "minimum", 1.0f, "maximum"));
        assertThatIllegalArgumentException().isThrownBy(() -> Preconditions.requireNonBlank(" ", "name"));
        assertThatNullPointerException().isThrownBy(() -> Preconditions.requireNonBlank(null, "name"));
    }
}
