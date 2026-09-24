/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.inspector;

import io.github.glynch.jscene3d.project.component.ComponentType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable target-scoped group of Inspector properties.
 *
 * @param identity stable section identity within the projection
 * @param label author-facing section label
 * @param description optional author-facing description
 * @param componentType exact component type for component sections
 * @param metadataAvailable whether exact descriptor metadata was resolved
 * @param properties typed properties in declaration order
 */
public record InspectorSection(
        String identity,
        String label,
        Optional<String> description,
        Optional<ComponentType> componentType,
        boolean metadataAvailable,
        List<InspectorProperty> properties) {
    /** Copies and validates section data. */
    public InspectorSection {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(componentType, "componentType");
        properties = List.copyOf(properties);
    }
}
