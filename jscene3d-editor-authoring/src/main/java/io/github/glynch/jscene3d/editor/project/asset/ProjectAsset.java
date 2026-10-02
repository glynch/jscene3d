/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.asset;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import java.nio.file.Path;
import java.util.Objects;

/**
 * One immutable authored asset with retained stable identity and source.
 *
 * @param label author-facing asset label
 * @param identity stable asset identity
 * @param kind asset category
 * @param source authoritative source path
 */
public record ProjectAsset(String label, String identity, Kind kind, Path source) {
    /** Validates one asset projection. */
    public ProjectAsset {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
    }

    /** Preserves the project-authored label without adding Java-owned UI decoration. */
    @Override
    public String toString() {
        return label;
    }

    /** Asset categories displayed by the initial editor browser. */
    public enum Kind {
        /** Reusable entity definition. */
        ENTITY_DEFINITION("entity-definition", "Entity definition"),
        /** Scene definition. */
        SCENE_DEFINITION("scene-definition", "Scene definition"),
        /** Authoritative source or resource asset. */
        SOURCE_ASSET("source-asset", "Source asset"),
        /** Source-import definition. */
        IMPORT_DEFINITION("import-definition", "Import definition");

        private final AuthoringText label;

        /** Stores the author-facing category label. */
        Kind(String codeSuffix, String defaultLabel) {
            label = AuthoringText.message("editor.asset.kind." + codeSuffix, defaultLabel);
        }

        /**
         * Returns the author-facing kind label.
         *
         * @return author-facing kind label
         */
        public AuthoringText label() {
            return label;
        }
    }
}
