/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/**
 * Conditional selection of a replacement project.
 *
 * @param expectedProjectGeneration active project generation the caller intends to replace
 * @param path replacement project directory or selected descriptor path
 */
public record ProjectReplaceParams(long expectedProjectGeneration, String path) {
    /** Validates the expected generation and selected path text. */
    public ProjectReplaceParams {
        if (expectedProjectGeneration <= 0) {
            throw new IllegalArgumentException("expectedProjectGeneration must be positive");
        }
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
    }
}
