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

/**
 * Immutable editor-owned data rendered by the read-only Inspector.
 *
 * @param title selection title
 * @param kind human-readable selection kind
 * @param source source location
 * @param identity stable selection identity
 * @param generated whether the selected value is generated
 * @param sections ordered Inspector sections
 */
public record EditorInspectorView(
        String title, String kind, String source, String identity, boolean generated, List<Section> sections) {
    /** Copies and validates one complete Inspector projection. */
    public EditorInspectorView {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        sections = List.copyOf(sections);
    }

    /**
     * One collapsible Inspector section, usually representing a component descriptor.
     *
     * @param title section title
     * @param description optional description
     * @param metadataAvailable whether descriptor metadata was available
     * @param properties ordered property projections
     */
    public record Section(
            String title, Optional<String> description, boolean metadataAvailable, List<Property> properties) {
        /** Copies and validates one section. */
        public Section {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
            properties = List.copyOf(properties);
        }
    }

    /**
     * One typed read-only property row.
     *
     * @param identity stable property identity
     * @param displayName human-readable property name
     * @param valueKind descriptor value kind
     * @param value formatted read-only value
     * @param origin source of the displayed value
     * @param required whether the property is required
     * @param description optional descriptor description
     * @param constraints immutable generic constraints
     */
    public record Property(
            String identity,
            String displayName,
            ProjectValueKind valueKind,
            String value,
            ValueOrigin origin,
            boolean required,
            Optional<String> description,
            Map<String, String> constraints) {
        /** Copies and validates one property projection. */
        public Property {
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
    public enum ValueOrigin {
        /** Explicitly present in the authored definition. */
        AUTHORED,
        /** Supplied by safe descriptor metadata. */
        DEFAULT,
        /** Required or optional value with neither authored data nor a default. */
        UNSET
    }
}
