/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Portable value stored in scene properties, controller properties, or instance overrides.
 *
 * <p>The closed value family keeps JSON-library types out of the public project interface and
 * preserves resource references as explicit values rather than magic strings.
 */
public sealed interface ProjectValue
        permits ProjectValue.NullValue,
                ProjectValue.BooleanValue,
                ProjectValue.NumberValue,
                ProjectValue.TextValue,
                ProjectValue.ArrayValue,
                ProjectValue.ObjectValue,
                ProjectValue.ReferenceValue {
    /** Explicit JSON null. */
    enum NullValue implements ProjectValue {
        /** The only null-value instance. */
        INSTANCE
    }

    /** Boolean property value.
     *
     * @param value stored boolean
     */
    record BooleanValue(boolean value) implements ProjectValue {}

    /** Arbitrary-precision numeric property value.
     *
     * @param value stored number
     */
    record NumberValue(BigDecimal value) implements ProjectValue {
        /** Validates the stored number. */
        public NumberValue {
            Objects.requireNonNull(value, "value");
        }
    }

    /** Text property value.
     *
     * @param value stored text, which may be empty
     */
    record TextValue(String value) implements ProjectValue {
        /** Validates the stored text. */
        public TextValue {
            Objects.requireNonNull(value, "value");
        }
    }

    /** Ordered array property value.
     *
     * @param values immutable values
     */
    record ArrayValue(List<ProjectValue> values) implements ProjectValue {
        /** Copies array values. */
        public ArrayValue {
            values = List.copyOf(values);
        }
    }

    /** Ordered object property value.
     *
     * @param values immutable properties in source order
     */
    record ObjectValue(Map<String, ProjectValue> values) implements ProjectValue {
        /** Copies object values while preserving source order. */
        public ObjectValue {
            Objects.requireNonNull(values, "values");
            Map<String, ProjectValue> copied = new LinkedHashMap<>();
            values.forEach((key, value) -> copied.put(
                    Objects.requireNonNull(key, "object key"), Objects.requireNonNull(value, "object value")));
            values = Collections.unmodifiableMap(copied);
        }
    }

    /** Explicit resource-reference property value.
     *
     * @param reference validated reference
     */
    record ReferenceValue(ResourceReference reference) implements ProjectValue {
        /** Validates the resource reference. */
        public ReferenceValue {
            Objects.requireNonNull(reference, "reference");
        }
    }
}
