/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.configuration;

import io.github.glynch.jscene3d.configuration.SettingKey;
import java.util.Objects;

/**
 * Immutable notification that one effective setting may have changed.
 *
 * @param key affected setting key
 * @param projectOverride whether the shared project document now supplies the effective candidate
 */
public record EditorConfigurationChange(SettingKey<?> key, boolean projectOverride) {
    /** Validates one change notification. */
    public EditorConfigurationChange {
        Objects.requireNonNull(key, "key");
    }

    /** Returns whether this notification affects the requested typed key. */
    public boolean affects(SettingKey<?> candidate) {
        return key.value().equals(Objects.requireNonNull(candidate, "candidate").value());
    }
}
