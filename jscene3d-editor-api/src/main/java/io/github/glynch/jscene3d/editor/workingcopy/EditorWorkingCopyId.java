/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workingcopy;

import java.net.URI;
import java.util.Objects;

/**
 * Identifies one editor working copy independently of its presentation.
 *
 * @param resource canonical resource URI
 * @param typeId stable working-copy type within that resource
 */
public record EditorWorkingCopyId(URI resource, String typeId) {
    /** Validates the complete resource identity. */
    public EditorWorkingCopyId {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(typeId, "typeId");
        if (typeId.isBlank()) {
            throw new IllegalArgumentException("typeId must not be blank");
        }
    }
}
