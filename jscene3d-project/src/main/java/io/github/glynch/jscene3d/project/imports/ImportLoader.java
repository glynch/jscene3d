/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.imports.internal.ImportValidator;
import io.github.glynch.jscene3d.project.imports.internal.RawImport;
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

/** Headless loader for versioned deterministic source-import definitions. */
public final class ImportLoader {
    private final ProjectJsonReader jsonReader;

    /** Creates a strict headless import-definition loader. */
    public ImportLoader() {
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Loads and validates one definition without executing its importer.
     *
     * @param project containing validated project
     * @param importFile absolute definition path, or a path relative to the project root
     * @return validated definition or structured loading errors
     */
    public ImportLoadResult load(GameProject project, Path importFile) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        Path suppliedPath = Objects.requireNonNull(importFile, "importFile");
        Path resolvedPath = suppliedPath.isAbsolute()
                ? suppliedPath.normalize()
                : validProject.root().resolve(suppliedPath).normalize();
        if (!resolvedPath.startsWith(validProject.root())) {
            return failure(
                    resolvedPath,
                    ImportDefinitionDiagnosticCode.PATH_ESCAPES_PROJECT,
                    "import path is outside the project directory");
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return failure(
                    resolvedPath,
                    ImportDefinitionDiagnosticCode.FILE_MISSING,
                    "import definition does not exist or is not a regular file: " + resolvedPath);
        }
        Path source;
        try {
            source = resolvedPath.toRealPath();
        } catch (IOException exception) {
            return failure(
                    resolvedPath,
                    ImportDefinitionDiagnosticCode.FILE_READ_FAILED,
                    "import path cannot be resolved: " + exception.getMessage());
        }
        if (!source.startsWith(validProject.root())) {
            return failure(
                    source,
                    ImportDefinitionDiagnosticCode.PATH_ESCAPES_PROJECT,
                    "import definition resolves outside the project directory");
        }
        return readDefinition(validProject, source);
    }

    /** Parses and semantically validates one import definition. */
    private ImportLoadResult readDefinition(GameProject project, Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            RawImport raw = jsonReader.read(input, RawImport.class);
            ImportValidator.ValidationResult validation = ImportValidator.validate(raw, project, source);
            return new ImportLoadResult(validation.definition(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    source,
                    ImportDefinitionDiagnosticCode.JSON_INVALID,
                    "import definition is not valid JScene3D Import JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(
                    source,
                    ImportDefinitionDiagnosticCode.FILE_READ_FAILED,
                    "import definition cannot be read: " + exception.getMessage());
        }
    }

    /** Creates one terminal error result. */
    private static ImportLoadResult failure(Path source, DiagnosticCode code, String technicalDetail) {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source.toUri(), "", Map.of("technicalDetail", technicalDetail));
        return new ImportLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
