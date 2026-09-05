/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.input.internal.InputMapValidator;
import io.github.glynch.jscene3d.project.input.internal.RawInputMap;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Headless loader for versioned project input-map definitions. */
public final class InputMapLoader {
    private final ProjectJsonReader jsonReader;

    /** Creates a strict input-map loader. */
    public InputMapLoader() {
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Loads and validates one input map without accessing a native input device.
     *
     * @param project containing validated project
     * @param inputMapFile absolute definition path, or a path relative to the project root
     * @return validated definition or structured diagnostics
     */
    public InputMapLoadResult load(GameProject project, Path inputMapFile) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        Path suppliedPath = Objects.requireNonNull(inputMapFile, "inputMapFile");
        Path resolvedPath = suppliedPath.isAbsolute()
                ? suppliedPath.normalize()
                : validProject.root().resolve(suppliedPath).normalize();
        if (!resolvedPath.startsWith(validProject.root())) {
            return failure(
                    resolvedPath,
                    InputMapDiagnosticCode.PATH_ESCAPES_PROJECT,
                    "input-map path is outside the project directory");
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return failure(
                    resolvedPath,
                    InputMapDiagnosticCode.FILE_MISSING,
                    "input-map definition does not exist or is not a regular file: " + resolvedPath);
        }
        Path source;
        try {
            source = resolvedPath.toRealPath();
        } catch (IOException exception) {
            return failure(
                    resolvedPath,
                    InputMapDiagnosticCode.FILE_READ_FAILED,
                    "input-map path cannot be resolved: " + exception.getMessage());
        }
        if (!source.startsWith(validProject.root())) {
            return failure(
                    source,
                    InputMapDiagnosticCode.PATH_ESCAPES_PROJECT,
                    "input-map definition resolves outside the project directory");
        }
        return readDefinition(validProject, source);
    }

    /** Parses and semantically validates one input-map definition. */
    private InputMapLoadResult readDefinition(GameProject project, Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            RawInputMap raw = jsonReader.read(input, RawInputMap.class);
            InputMapValidator.ValidationResult validation = InputMapValidator.validate(raw, project, source);
            return new InputMapLoadResult(validation.definition(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    source,
                    InputMapDiagnosticCode.JSON_INVALID,
                    "input-map definition is not valid JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(
                    source,
                    InputMapDiagnosticCode.FILE_READ_FAILED,
                    "input-map definition cannot be read: " + exception.getMessage());
        }
    }

    /** Creates one terminal error result. */
    private static InputMapLoadResult failure(Path source, DiagnosticCode code, String technicalDetail) {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source.toUri(), "", Map.of("technicalDetail", technicalDetail));
        return new InputMapLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
