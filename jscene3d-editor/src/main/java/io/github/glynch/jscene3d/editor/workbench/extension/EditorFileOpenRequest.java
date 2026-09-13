/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import java.net.URI;
import java.util.Objects;

/** Workbench-internal request to preview or permanently open a workspace file. */
public record EditorFileOpenRequest(URI resource, Disposition disposition) {
    /** How the requested editor participates in the current file session. */
    public enum Disposition {
        /** Reusable editor that preserves focus in the requesting view. */
        PREVIEW,

        /** Permanent editor selected with focus. */
        PINNED
    }

    /** Validates one file-open request. */
    public EditorFileOpenRequest {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(disposition, "disposition");
    }
}
