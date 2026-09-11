/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.util.Objects;

/** Stable editor selection and the immutable Inspector data associated with it. */
record EditorSelection(Kind kind, String identity, EditorInspectorView inspector) {
    /** Validates one selection. */
    EditorSelection {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(inspector, "inspector");
    }

    /** Selection categories shared by Hierarchy and Project surfaces. */
    enum Kind {
        /** Opened world root. */
        WORLD,
        /** Entity authored directly in the containing definition. */
        LOCAL_ENTITY,
        /** Authored placement of a reusable entity definition. */
        PLACEMENT,
        /** Entity projected from inside a placed reusable definition. */
        GENERATED_ENTITY,
        /** Project-browser asset or import. */
        ASSET
    }
}
