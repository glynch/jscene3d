/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.menu;

import io.github.glynch.jscene3d.editor.command.CommandLocationId;
import java.util.Objects;

/**
 * Declares one top-level menu without exposing the editor's UI toolkit.
 *
 * @param location stable command location identifying the menu
 * @param title non-blank user-facing menu title
 * @param order ascending top-level presentation order
 */
public record EditorMenuContribution(CommandLocationId location, String title, int order) {
    /** Validates one menu declaration. */
    public EditorMenuContribution {
        Objects.requireNonNull(location, "location");
        if (Objects.requireNonNull(title, "title").isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }
}
