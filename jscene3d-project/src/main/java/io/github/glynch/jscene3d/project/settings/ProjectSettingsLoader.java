/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Strict loader for the portable {@value ProjectSettings#SETTINGS_NAME} project settings document. */
public final class ProjectSettingsLoader {
    private final ProjectSettingsReader reader;

    /** Creates a stateless project-settings loader. */
    public ProjectSettingsLoader() {
        reader = new ProjectSettingsReader();
    }

    /** Loads shared settings, returning defaults when the optional document is absent.
     *
     * @param projectRoot project workspace root
     * @return validated settings or terminal diagnostics
     */
    public ProjectSettingsLoadResult load(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        Path source = root.resolve(ProjectSettings.SETTINGS_NAME);
        if (!Files.exists(source)) {
            return success(ProjectSettings.defaults());
        }
        if (!Files.isRegularFile(source)) {
            return failure(
                    source,
                    ProjectSettingsDiagnosticCode.SETTINGS_READ_FAILED,
                    "project settings path is not a regular file");
        }
        try (InputStream input = Files.newInputStream(source)) {
            return success(reader.read(input));
        } catch (JsonProcessingException exception) {
            return failure(source, ProjectSettingsDiagnosticCode.SETTINGS_INVALID, exception.getOriginalMessage());
        } catch (IllegalArgumentException exception) {
            return failure(
                    source,
                    ProjectSettingsDiagnosticCode.SETTINGS_INVALID,
                    Objects.requireNonNullElse(exception.getMessage(), exception.toString()));
        } catch (IOException exception) {
            return failure(
                    source,
                    ProjectSettingsDiagnosticCode.SETTINGS_READ_FAILED,
                    Objects.requireNonNullElse(exception.getMessage(), exception.toString()));
        }
    }

    private static ProjectSettingsLoadResult success(ProjectSettings settings) {
        return new ProjectSettingsLoadResult(Optional.of(settings), List.of());
    }

    private static ProjectSettingsLoadResult failure(Path source, ProjectSettingsDiagnosticCode code, String detail) {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                code,
                source.toUri(),
                "",
                Map.of("technicalDetail", Objects.requireNonNullElse(detail, code.defaultMessage())));
        return new ProjectSettingsLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
