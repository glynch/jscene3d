/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.configuration;

import io.github.glynch.jscene3d.configuration.definition.SettingKey;
import io.github.glynch.jscene3d.configuration.registry.SettingRegistry;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import java.util.Optional;

/** Read-only effective project configuration available to activated editor extensions. */
public interface EditorConfiguration {
    /**
     * Returns setting declarations available for the current project.
     *
     * @return setting registry
     */
    SettingRegistry registry();

    /**
     * Returns one effective typed value when a project declares that setting.
     *
     * @param key requested typed setting key
     * @param <T> setting value type
     * @return effective value, or empty when it is not declared
     */
    <T> Optional<T> get(SettingKey<T> key);

    /**
     * Returns the typed event fired after an effective setting changes.
     *
     * @return configuration change event
     */
    EditorEvent<EditorConfigurationChange> onDidChange();
}
