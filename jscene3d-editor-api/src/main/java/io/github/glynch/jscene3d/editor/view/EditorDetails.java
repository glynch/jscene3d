/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, toolkit-independent details presented for an editor selection. */
public record EditorDetails(
        String title,
        String kind,
        String source,
        String identity,
        List<EditorIcon> decorations,
        List<Section> sections) {
    /** Copies and validates one complete details projection. */
    public EditorDetails {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(identity, "identity");
        decorations = List.copyOf(decorations);
        sections = List.copyOf(sections);
    }

    /** One collapsible group of related properties. */
    public record Section(
            String title, Optional<String> description, boolean metadataAvailable, List<Property> properties) {
        /** Copies and validates one section. */
        public Section {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(description, "description");
            properties = List.copyOf(properties);
        }
    }

    /** One typed read-only property row. */
    public record Property(
            String identity,
            String displayName,
            String valueKind,
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

    /** Explains where a displayed property value originated. */
    public enum ValueOrigin {
        AUTHORED,
        DEFAULT,
        UNSET
    }
}
