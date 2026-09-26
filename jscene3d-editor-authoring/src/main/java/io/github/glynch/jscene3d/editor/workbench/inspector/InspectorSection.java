/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.ComponentType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable target-scoped group of Inspector properties.
 *
 * @param identity stable section identity within the projection
 * @param kind semantic navigator target kind
 * @param label author-facing section label
 * @param description optional author-facing description
 * @param componentId component instance identity for component groups
 * @param componentType exact component type for component sections
 * @param metadataAvailable whether exact descriptor metadata was resolved
 * @param editable whether any future mutation may target this group
 * @param properties typed properties in declaration order
 */
public record InspectorSection(
        String identity,
        Kind kind,
        AuthoringText label,
        Optional<AuthoringText> description,
        Optional<ComponentId> componentId,
        Optional<ComponentType> componentType,
        boolean metadataAvailable,
        boolean editable,
        List<InspectorProperty> properties) {
    /** Copies and validates section data. */
    public InspectorSection {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(componentType, "componentType");
        properties = List.copyOf(properties);
        if ((kind == Kind.COMPONENT) != componentId.isPresent()
                || (kind == Kind.COMPONENT) != componentType.isPresent()) {
            throw new IllegalArgumentException("only component groups have component identity and type");
        }
    }

    /** Selectable target kinds shown by the Inspector navigator. */
    public enum Kind {
        /** Entity-level authored state. */
        ENTITY,
        /** One component instance. */
        COMPONENT,
        /** Reusable-definition placement context. */
        PLACEMENT
    }
}
