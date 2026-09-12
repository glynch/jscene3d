/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import io.github.glynch.jscene3d.configuration.SettingDefinition;
import io.github.glynch.jscene3d.configuration.SettingKey;
import io.github.glynch.jscene3d.configuration.SettingRegistry;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves, validates, and atomically persists effective project configuration.
 *
 * <p>Invalid and unknown stored values remain in the document. Known invalid values fall back to their declaration's
 * default and produce diagnostics rather than preventing the project from opening.
 */
public final class ProjectConfiguration {
    private final Path projectRoot;
    private final SettingRegistry registry;
    private final ProjectSettingsSaver saver;
    private ProjectSettings settings;

    /** Creates one configuration from an already parsed settings document and declaration registry. */
    public ProjectConfiguration(Path projectRoot, SettingRegistry registry, ProjectSettings settings) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        this.registry = Objects.requireNonNull(registry, "registry");
        this.settings = Objects.requireNonNull(settings, "settings");
        saver = new ProjectSettingsSaver();
    }

    /** Returns all core and extension setting declarations. */
    public SettingRegistry registry() {
        return registry;
    }

    /** Returns the effective typed value, falling back to the declared default when necessary. */
    public <T> T get(SettingKey<T> key) {
        SettingDefinition<T> definition = registry.require(key);
        return settings.value(key.value())
                .map(value -> validOrDefault(definition, value))
                .orElse(definition.defaultValue());
    }

    /** Returns whether the shared project document explicitly contains one setting. */
    public boolean isOverridden(SettingKey<?> key) {
        return settings.settings()
                .containsKey(Objects.requireNonNull(key, "key").value());
    }

    /** Validates and atomically persists one project-scope override. */
    public <T> void update(SettingKey<T> key, T value) throws IOException {
        SettingDefinition<T> definition = registry.require(key);
        T validated = definition.validate(value);
        ProjectSettings updated =
                settings.with(key.value(), definition.valueType().store(validated));
        saver.save(projectRoot, updated);
        settings = updated;
    }

    /** Removes and atomically persists one project-scope override. */
    public void reset(SettingKey<?> key) throws IOException {
        registry.require(key);
        ProjectSettings updated = settings.without(key.value());
        saver.save(projectRoot, updated);
        settings = updated;
    }

    /** Resolves one effective path setting against the current project root. */
    public Path resolve(SettingKey<Path> key) {
        Path value = get(key);
        return value.isAbsolute()
                ? value.normalize()
                : projectRoot.resolve(value).normalize();
    }

    /** Returns diagnostics produced while resolving stored values. */
    public List<ProjectDiagnostic> diagnostics() {
        return validateStoredValues();
    }

    /** Returns the current generic settings document, including unknown extension values. */
    public ProjectSettings document() {
        return settings;
    }

    private List<ProjectDiagnostic> validateStoredValues() {
        ArrayList<ProjectDiagnostic> result = new ArrayList<>();
        settings.settings()
                .forEach((key, value) -> registry.find(key)
                        .ifPresentOrElse(
                                definition -> validateStoredValue(definition, value, result),
                                () -> result.add(warning(
                                        ProjectSettingsDiagnosticCode.SETTING_UNKNOWN,
                                        key,
                                        "No installed core module or extension declares this setting"))));
        return List.copyOf(result);
    }

    private void validateStoredValue(SettingDefinition<?> definition, Object value, List<ProjectDiagnostic> result) {
        try {
            definition.validate(value);
        } catch (IllegalArgumentException exception) {
            result.add(warning(
                    ProjectSettingsDiagnosticCode.SETTING_VALUE_INVALID,
                    definition.key().value(),
                    Objects.requireNonNullElse(exception.getMessage(), "The stored value is invalid")));
        }
    }

    private ProjectDiagnostic warning(ProjectSettingsDiagnosticCode code, String key, String detail) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.WARNING,
                code,
                projectRoot.resolve(ProjectSettings.SETTINGS_NAME).toUri(),
                "/settings/" + key,
                Map.of("technicalDetail", detail, "setting", key));
    }

    private static <T> T validOrDefault(SettingDefinition<T> definition, Object value) {
        try {
            return definition.validate(value);
        } catch (IllegalArgumentException ignored) {
            return definition.defaultValue();
        }
    }
}
