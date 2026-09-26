/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import io.github.glynch.jscene3d.project.entity.ComponentTarget;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable semantic Inspector value independent of JSON and concrete controls. */
public sealed interface InspectorValue
        permits InspectorValue.NullValue,
                InspectorValue.BooleanValue,
                InspectorValue.NumberValue,
                InspectorValue.TextValue,
                InspectorValue.ArrayValue,
                InspectorValue.ObjectValue,
                InspectorValue.ReferenceValue,
                InspectorValue.EntityTargetValue,
                InspectorValue.ComponentTargetValue {
    /** Explicit null. */
    enum NullValue implements InspectorValue {
        /** The only null value. */
        INSTANCE
    }

    /** Boolean value.
     *
     * @param value stored value
     */
    record BooleanValue(boolean value) implements InspectorValue {}

    /** Exact arbitrary-precision decimal value.
     *
     * @param value stored decimal
     */
    record NumberValue(BigDecimal value) implements InspectorValue {
        /** Validates the exact number. */
        public NumberValue {
            Objects.requireNonNull(value, "value");
        }
    }

    /** Text value.
     *
     * @param value stored text
     */
    record TextValue(String value) implements InspectorValue {
        /** Validates the stored text. */
        public TextValue {
            Objects.requireNonNull(value, "value");
        }
    }

    /** Ordered values.
     *
     * @param values immutable values
     */
    record ArrayValue(List<InspectorValue> values) implements InspectorValue {
        /** Copies the ordered values. */
        public ArrayValue {
            values = List.copyOf(values);
        }
    }

    /** Ordered named values without an implied member schema.
     *
     * @param values immutable values in source order
     */
    record ObjectValue(Map<String, InspectorValue> values) implements InspectorValue {
        /** Copies entries while preserving their source order. */
        public ObjectValue {
            Objects.requireNonNull(values, "values");
            LinkedHashMap<String, InspectorValue> copied = new LinkedHashMap<>();
            values.forEach((key, value) -> copied.put(
                    Objects.requireNonNull(key, "object key"), Objects.requireNonNull(value, "object value")));
            values = Collections.unmodifiableMap(copied);
        }
    }

    /** Semantic resource reference with current resolution presentation.
     *
     * @param reference portable reference identity
     * @param label semantic display label or portable fallback
     * @param resolution current resolution state
     * @param revealUri optional source that can be revealed
     */
    record ReferenceValue(
            ResourceReference reference, AuthoringText label, Resolution resolution, Optional<URI> revealUri)
            implements InspectorValue {
        /** Validates resolved reference presentation. */
        public ReferenceValue {
            Objects.requireNonNull(reference, "reference");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(resolution, "resolution");
            Objects.requireNonNull(revealUri, "revealUri");
        }
    }

    /** Semantic entity target with optional Hierarchy reveal identity.
     *
     * @param entity entity identity
     * @param label authored or semantic display label
     * @param resolution current resolution state
     * @param occurrence optional occurrence in the containing definition
     */
    record EntityTargetValue(
            EntityId entity, AuthoringText label, Resolution resolution, Optional<HierarchyOccurrenceId> occurrence)
            implements InspectorValue {
        /** Validates resolved entity-target presentation. */
        public EntityTargetValue {
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(resolution, "resolution");
            Objects.requireNonNull(occurrence, "occurrence");
        }
    }

    /** Semantic component target with its owning entity context.
     *
     * @param target component identity
     * @param entityLabel owning entity label
     * @param componentLabel component label or identity fallback
     * @param componentType exact component type when resolved
     * @param resolution current resolution state
     * @param occurrence optional owning-entity occurrence for reveal
     */
    record ComponentTargetValue(
            ComponentTarget target,
            AuthoringText entityLabel,
            AuthoringText componentLabel,
            Optional<ComponentType> componentType,
            Resolution resolution,
            Optional<HierarchyOccurrenceId> occurrence)
            implements InspectorValue {
        /** Validates resolved component-target presentation. */
        public ComponentTargetValue {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(entityLabel, "entityLabel");
            Objects.requireNonNull(componentLabel, "componentLabel");
            Objects.requireNonNull(componentType, "componentType");
            Objects.requireNonNull(resolution, "resolution");
            Objects.requireNonNull(occurrence, "occurrence");
        }
    }

    /** Current resolution status for a semantic reference or authored target. */
    enum Resolution {
        /** The referenced semantic target is currently available. */
        RESOLVED,
        /** The portable identity cannot currently be resolved. */
        BROKEN
    }
}
