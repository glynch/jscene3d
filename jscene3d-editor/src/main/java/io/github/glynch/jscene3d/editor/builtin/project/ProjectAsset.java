/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.project;

import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import java.nio.file.Path;
import java.util.Objects;

/**
 * One immutable Project-browser asset with retained stable identity and inspection data.
 *
 * @param label author-facing asset label
 * @param identity stable asset identity
 * @param kind asset category
 * @param source authoritative source path
 * @param selection selection published when the asset is chosen
 */
public record ProjectAsset(String label, String identity, Kind kind, Path source, EditorSelection selection) {
    /** Validates one asset projection. */
    public ProjectAsset {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(selection, "selection");
    }

    /** Shows only ordinary author-facing information in the asset browser. */
    @Override
    public String toString() {
        return label + "  ·  " + kind.label;
    }

    /** Asset categories displayed by the initial editor browser. */
    public enum Kind {
        /** Reusable entity definition. */
        ENTITY_DEFINITION("Entity definition"),
        /** World definition. */
        WORLD_DEFINITION("World definition"),
        /** Authoritative source or resource asset. */
        SOURCE_ASSET("Source asset"),
        /** Source-import definition. */
        IMPORT_DEFINITION("Import definition");

        private final String label;

        /** Stores the author-facing category label. */
        Kind(String label) {
            this.label = label;
        }

        /**
         * Returns the author-facing kind label.
         *
         * @return author-facing kind label
         */
        public String label() {
            return label;
        }
    }
}
