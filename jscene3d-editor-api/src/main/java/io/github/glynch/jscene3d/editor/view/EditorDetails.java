/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, toolkit-independent details presented for an editor selection.
 *
 * @param title user-facing title of the selected item
 * @param kind user-facing kind of the selected item
 * @param source source containing the selected item
 * @param identity stable identity of the selected item
 * @param decorations semantic icons decorating the details header
 * @param sections property sections in presentation order
 */
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

    /**
     * One collapsible group of related properties.
     *
     * @param title user-facing section title
     * @param description optional explanation of the section
     * @param metadataAvailable whether metadata exists for the section
     * @param properties property rows in presentation order
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
     * One typed property row, optionally backed by an editor command.
     *
     * @param identity stable identity of the property
     * @param displayName user-facing property name
     * @param valueKind semantic type of the displayed value
     * @param value formatted value displayed to the user
     * @param origin origin of the displayed value
     * @param required whether the property is required by its schema
     * @param description optional explanation of the property
     * @param constraints named constraints applying to the property
     * @param editor optional command accepting a complete replacement value
     */
    public record Property(
            String identity,
            String displayName,
            String valueKind,
            String value,
            ValueOrigin origin,
            boolean required,
            Optional<String> description,
            Map<String, String> constraints,
            Optional<EditorPropertyEditor> editor) {
        /** Copies and validates one property projection. */
        public Property {
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(valueKind, "valueKind");
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(description, "description");
            constraints = Map.copyOf(constraints);
            Objects.requireNonNull(editor, "editor");
        }
    }

    /** Explains where a displayed property value originated. */
    public enum ValueOrigin {
        /** The project explicitly supplies the value. */
        AUTHORED,
        /** The schema or runtime supplies the value by default. */
        DEFAULT,
        /** No value is currently supplied. */
        UNSET
    }
}
