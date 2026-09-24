/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.Objects;

/** Stable typed target of an explicit authoring mutation. */
public sealed interface InspectorMutationTarget
        permits InspectorMutationTarget.EntityEnabled, InspectorMutationTarget.ComponentProperty {
    /**
     * Returns the hierarchy occurrence against which editability is checked.
     *
     * @return hierarchy occurrence
     */
    HierarchyOccurrenceId occurrence();

    /**
     * Targets the enabled state of one locally authored entity or placement.
     *
     * @param occurrence editable hierarchy occurrence
     * @param entity authored entity or placement identity
     */
    record EntityEnabled(HierarchyOccurrenceId occurrence, EntityId entity) implements InspectorMutationTarget {
        /** Validates the mutation identity. */
        public EntityEnabled {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(entity, "entity");
        }
    }

    /**
     * Targets one property on a component of a locally authored entity.
     *
     * @param occurrence editable local-entity occurrence
     * @param entity entity identity
     * @param component component identity
     * @param property property identity
     */
    record ComponentProperty(
            HierarchyOccurrenceId occurrence, EntityId entity, ComponentId component, PropertyId property)
            implements InspectorMutationTarget {
        /** Validates the mutation identity. */
        public ComponentProperty {
            Objects.requireNonNull(occurrence, "occurrence");
            Objects.requireNonNull(entity, "entity");
            Objects.requireNonNull(component, "component");
            Objects.requireNonNull(property, "property");
        }
    }
}
