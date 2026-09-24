/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import java.util.Objects;

/** Snapshot emitted after authoritative authoring session state changes.
 *
 * @param revision authoritative revision after the change
 * @param dirty whether authored content differs from its saved baseline
 * @param kind semantic change category
 */
public record AuthoringSessionChange(long revision, boolean dirty, Kind kind) {
    /** Validates the revision and change kind. */
    public AuthoringSessionChange {
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
        Objects.requireNonNull(kind, "kind");
    }

    /** Authoritative session change categories. */
    public enum Kind {
        /** Authored in-memory content changed. */
        CONTENT,
        /** A project setting was persisted. */
        CONFIGURATION,
        /** Authored content was saved. */
        SAVED,
        /** Authored content was reverted to its saved baseline. */
        REVERTED
    }
}
