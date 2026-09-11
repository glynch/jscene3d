/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project;

import java.net.URI;
import java.util.Objects;

/**
 * Stable toolkit-independent identity of the project currently opened by the editor.
 *
 * @param id non-blank permanent project identity
 * @param name non-blank human-readable project name
 * @param root absolute project-root URI
 */
public record EditorProject(String id, String name, URI root) {
    /** Validates one opened-project identity. */
    public EditorProject {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (!Objects.requireNonNull(root, "root").isAbsolute()) {
            throw new IllegalArgumentException("root must be absolute");
        }
    }
}
