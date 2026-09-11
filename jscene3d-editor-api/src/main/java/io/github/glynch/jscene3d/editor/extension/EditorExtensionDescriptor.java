/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

import java.util.Objects;
import java.util.Optional;

/**
 * Safe descriptive metadata for an installed editor extension.
 *
 * @param id stable reverse-domain extension identity
 * @param displayName non-blank user-facing name
 * @param description non-blank summary of the extension's purpose
 * @param publisher non-blank publisher name
 * @param version optional installed version
 * @param builtIn whether the extension is distributed with the editor
 */
public record EditorExtensionDescriptor(
        String id,
        String displayName,
        String description,
        String publisher,
        Optional<String> version,
        boolean builtIn) {
    /** Copies and validates installed extension metadata. */
    public EditorExtensionDescriptor {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("extension identity must not be blank");
        }
        if (Objects.requireNonNull(displayName, "displayName").isBlank()) {
            throw new IllegalArgumentException("extension display name must not be blank");
        }
        if (Objects.requireNonNull(description, "description").isBlank()) {
            throw new IllegalArgumentException("extension description must not be blank");
        }
        if (Objects.requireNonNull(publisher, "publisher").isBlank()) {
            throw new IllegalArgumentException("extension publisher must not be blank");
        }
        version = Objects.requireNonNull(version, "version").map(String::strip).filter(value -> !value.isEmpty());
    }
}
