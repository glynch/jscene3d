/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable editor-owned data rendered by the read-only Inspector. */
record EditorInspectorView(
        String title, String kind, String source, String identity, boolean generated, List<Section> sections) {
    /** Copies and validates one complete Inspector projection. */
    EditorInspectorView {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        sections = List.copyOf(sections);
    }

    /** One collapsible Inspector section, usually representing a component descriptor. */
    record Section(String title, Optional<String> description, boolean metadataAvailable, List<Property> properties) {
        /** Copies and validates one section. */
        Section {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
            properties = List.copyOf(properties);
        }
    }

    /** One typed read-only property row. */
    record Property(
            String identity,
            String displayName,
            ProjectValueKind valueKind,
            String value,
            ValueOrigin origin,
            boolean required,
            Optional<String> description,
            Map<String, String> constraints) {
        /** Copies and validates one property projection. */
        Property {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(description, "description");
            constraints = Map.copyOf(constraints);
        }
    }

    /** Explains where the displayed property value came from. */
    enum ValueOrigin {
        /** Explicitly present in the authored definition. */
        AUTHORED,
        /** Supplied by safe descriptor metadata. */
        DEFAULT,
        /** Required or optional value with neither authored data nor a default. */
        UNSET
    }
}
