/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.coordination;

import java.util.Objects;

/**
 * One adapter-independent request to build an exact saved-project revision.
 *
 * @param revision monotonically increasing saved-project revision
 * @param kind requested semantic build intent
 */
public record ProjectBuildRequest(long revision, ProjectBuildKind kind) {
    /** Validates one build request. */
    public ProjectBuildRequest {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        Objects.requireNonNull(kind, "kind");
    }
}
