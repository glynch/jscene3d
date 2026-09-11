/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.nio.file.Path;
import java.util.Objects;

/** One immutable asset-browser projection with retained stable identity and inspection data. */
record EditorAssetItem(String label, String identity, Kind kind, Path source, EditorSelection selection) {
    /** Validates one asset projection. */
    EditorAssetItem {
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
    enum Kind {
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

        /** Returns the author-facing kind label. */
        String label() {
            return label;
        }
    }
}
