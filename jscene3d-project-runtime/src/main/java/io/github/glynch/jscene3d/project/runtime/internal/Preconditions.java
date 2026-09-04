/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import java.util.Objects;

/** Shared project-runtime argument policies. */
public final class Preconditions {
    private Preconditions() {}

    /**
     * Requires text containing at least one non-whitespace character.
     *
     * @param value candidate text
     * @param name argument name used in failure messages
     * @return validated text
     */
    public static String requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }
}
