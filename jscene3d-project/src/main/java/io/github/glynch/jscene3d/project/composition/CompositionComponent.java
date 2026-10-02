/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable effective component configuration on one expanded entity occurrence.
 *
 * @param owner owning entity occurrence
 * @param scope authored definition occurrence
 * @param authoredEntity local entity identity within the authored scope
 * @param definition original authored component definition
 * @param descriptor exact inert component descriptor
 * @param effectiveProperties descriptor-ordered effective properties
 * @param authoredSource source containing the component declaration
 * @param location diagnostic location within the root composition
 */
public record CompositionComponent(
        CompositionOccurrenceId owner,
        CompositionScope scope,
        EntityId authoredEntity,
        ComponentDefinition definition,
        ComponentTypeDescriptor descriptor,
        Map<PropertyId, CompositionValue> effectiveProperties,
        URI authoredSource,
        String location) {
    /** Copies one immutable component plan. */
    public CompositionComponent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(authoredEntity, "authoredEntity");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(descriptor, "descriptor");
        effectiveProperties = Collections.unmodifiableMap(new LinkedHashMap<>(effectiveProperties));
        Objects.requireNonNull(authoredSource, "authoredSource");
        Objects.requireNonNull(location, "location");
    }

    /**
     * Returns the portable effective property projection in descriptor order.
     *
     * @return immutable effective values
     */
    public Map<PropertyId, ProjectValue> values() {
        Map<PropertyId, ProjectValue> values = new LinkedHashMap<>();
        effectiveProperties.forEach((property, scoped) -> values.put(property, scoped.value()));
        return Collections.unmodifiableMap(values);
    }

    /**
     * Returns top-level effective resource references in descriptor order.
     *
     * @return immutable resource-reference projection
     */
    public Map<PropertyId, ResourceReference> resourceReferences() {
        Map<PropertyId, ResourceReference> references = new LinkedHashMap<>();
        effectiveProperties.forEach((property, scoped) -> {
            if (scoped.value() instanceof ProjectValue.ReferenceValue reference) {
                references.put(property, reference.reference());
            }
        });
        return Collections.unmodifiableMap(references);
    }
}
