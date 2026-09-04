/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.util.Objects;

/** JSON Pointer construction rules shared by project validators. */
public final class JsonPointers {
    /** Prevents instantiation of this JSON Pointer container. */
    private JsonPointers() {
        throw new AssertionError("JsonPointers cannot be instantiated");
    }

    /**
     * Escapes one JSON Pointer reference token.
     *
     * @param value unescaped object key
     * @return escaped reference token
     */
    public static String escapeSegment(String value) {
        return Objects.requireNonNull(value, "value").replace("~", "~0").replace("/", "~1");
    }
}
