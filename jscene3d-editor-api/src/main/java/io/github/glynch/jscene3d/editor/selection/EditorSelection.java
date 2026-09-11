/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.selection;

import io.github.glynch.jscene3d.editor.view.EditorDetails;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable semantic selection shared between editor extensions.
 *
 * @param kind semantic kind of the selection
 * @param identity stable identity of the selected item within its kind
 * @param details optional details projection for inspection
 */
public record EditorSelection(EditorSelectionKindId kind, String identity, Optional<EditorDetails> details) {
    /** Copies and validates one selection. */
    public EditorSelection {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(identity, "identity");
        if (identity.isBlank()) {
            throw new IllegalArgumentException("selection identity must not be blank");
        }
        Objects.requireNonNull(details, "details");
    }

    /**
     * Creates an inspectable selection.
     *
     * @param kind semantic kind of the selection
     * @param identity stable identity of the selected item within its kind
     * @param details details projection for inspection
     */
    public EditorSelection(EditorSelectionKindId kind, String identity, EditorDetails details) {
        this(kind, identity, Optional.of(Objects.requireNonNull(details, "details")));
    }
}
