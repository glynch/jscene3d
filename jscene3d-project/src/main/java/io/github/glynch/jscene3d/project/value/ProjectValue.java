/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.value;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;

import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.math.BigDecimal;
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
                ProjectValue.ReferenceValue,
                ProjectValue.EntityTargetValue,
                ProjectValue.ComponentTargetValue {
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
            values = immutableProjectValues(values, "values");
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

    /** Stable target of an entity in the authored scope containing this value.
     *
     * @param entity local entity identity
     */
    record EntityTargetValue(EntityId entity) implements ProjectValue {
        /** Validates the target identity. */
        public EntityTargetValue {
            Objects.requireNonNull(entity, "entity");
        }
    }

    /** Stable target of a component in the authored scope containing this value.
     *
     * @param target local entity and component identities
     */
    record ComponentTargetValue(ComponentTarget target) implements ProjectValue {
        /** Validates the component target. */
        public ComponentTargetValue {
            Objects.requireNonNull(target, "target");
        }
    }
}
