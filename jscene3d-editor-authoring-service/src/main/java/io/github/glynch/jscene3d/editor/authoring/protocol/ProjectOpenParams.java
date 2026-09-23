/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/**
 * Generic project-root or descriptor selection.
 *
 * @param path project directory or selected descriptor path
 */
public record ProjectOpenParams(String path) {
    /** Validates the selected path text. */
    public ProjectOpenParams {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
    }
}
