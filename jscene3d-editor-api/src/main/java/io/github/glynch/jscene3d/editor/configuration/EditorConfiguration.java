/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.configuration;

import io.github.glynch.jscene3d.configuration.SettingKey;
import io.github.glynch.jscene3d.configuration.SettingRegistry;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import java.util.Optional;

/** Read-only effective project configuration available to activated editor extensions. */
public interface EditorConfiguration {
    /** Returns setting declarations available for the current project. */
    SettingRegistry registry();

    /** Returns one effective typed value when a project declares that setting. */
    <T> Optional<T> get(SettingKey<T> key);

    /** Returns the typed event fired after an effective setting changes. */
    EditorEvent<EditorConfigurationChange> onDidChange();
}
