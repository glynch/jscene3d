/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import java.util.Objects;

/**
 * One stored and user-facing value offered by an enumerated setting.
 *
 * @param value non-blank stored value
 * @param label non-blank user-facing label
 */
public record SettingChoice(String value, String label) {
    /** Validates one choice. */
    public SettingChoice {
        if (Objects.requireNonNull(value, "value").isBlank()) {
            throw new IllegalArgumentException("setting choice value must not be blank");
        }
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("setting choice label must not be blank");
        }
    }
}
