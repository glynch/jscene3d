/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import io.github.glynch.jscene3d.configuration.definition.SettingKey;
import java.util.Objects;

/** Describes one persisted project-setting change.
 *
 * @param key changed setting declaration
 * @param projectOverride whether a project override is now present
 * @param revision resulting authoring revision
 */
public record AuthoringConfigurationChange(SettingKey<?> key, boolean projectOverride, long revision) {
    /** Validates the setting identity and revision. */
    public AuthoringConfigurationChange {
        Objects.requireNonNull(key, "key");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
