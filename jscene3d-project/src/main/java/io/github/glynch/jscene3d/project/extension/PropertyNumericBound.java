/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension;

import java.math.BigDecimal;
import java.util.Objects;

/** One inclusive or exclusive numeric endpoint used by editor presentation. */
public final class PropertyNumericBound {
    private final BigDecimal value;
    private final boolean inclusive;

    /**
     * Creates one exact decimal bound.
     *
     * @param value exact decimal endpoint
     * @param inclusive whether the endpoint itself is accepted
     */
    public PropertyNumericBound(BigDecimal value, boolean inclusive) {
        this.value = Objects.requireNonNull(value, "value");
        this.inclusive = inclusive;
    }

    /**
     * Returns the exact decimal endpoint.
     *
     * @return bound value
     */
    public BigDecimal value() {
        return value;
    }

    /**
     * Returns whether the endpoint itself is accepted.
     *
     * @return whether the bound is inclusive
     */
    public boolean isInclusive() {
        return inclusive;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PropertyNumericBound bound && inclusive == bound.inclusive && value.equals(bound.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, inclusive);
    }

    @Override
    public String toString() {
        return "PropertyNumericBound[value=" + value + ", inclusive=" + inclusive + ']';
    }
}
