/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.configuration;

import io.github.glynch.jscene3d.configuration.SettingKey;
import io.github.glynch.jscene3d.configuration.SettingRegistry;
import io.github.glynch.jscene3d.editor.EditorProjectSession;
import io.github.glynch.jscene3d.editor.configuration.EditorConfiguration;
import io.github.glynch.jscene3d.editor.configuration.EditorConfigurationChange;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorEventSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Window-scoped effective configuration exposed read-only to activated extensions. */
public final class EditorConfigurationContext implements EditorConfiguration, AutoCloseable {
    private static final SettingRegistry EMPTY_REGISTRY = SettingRegistry.of(List.of());

    private final EditorEventSource<EditorConfigurationChange> changes = new EditorEventSource<>();
    private EditorRegistration sessionRegistration = () -> {};
    private @Nullable EditorProjectSession session;

    /** Publishes one fully loaded project's setting registry and effective values. */
    public void showProject(EditorProjectSession replacement) {
        sessionRegistration.close();
        session = Objects.requireNonNull(replacement, "replacement");
        sessionRegistration = replacement.onDidChangeConfiguration().subscribe(changes::emit);
    }

    /** Clears all project-scoped configuration while retaining extension subscriptions. */
    public void clearProject() {
        sessionRegistration.close();
        sessionRegistration = () -> {};
        session = null;
    }

    @Override
    public SettingRegistry registry() {
        EditorProjectSession current = session;
        return current == null ? EMPTY_REGISTRY : current.configuration().registry();
    }

    @Override
    public <T> Optional<T> get(SettingKey<T> key) {
        EditorProjectSession current = session;
        if (current == null
                || current.configuration().registry().find(key.value()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(current.configuration().get(key));
    }

    @Override
    public EditorEvent<EditorConfigurationChange> onDidChange() {
        return changes;
    }

    @Override
    public void close() {
        clearProject();
    }
}
