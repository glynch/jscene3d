/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.internal.RawResource;
import io.github.glynch.jscene3d.project.resource.internal.ResourceValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Headless loader for versioned reusable project-resource definitions. */
public final class ResourceLoader {
    private final ProjectJsonReader jsonReader;

    /** Creates a strict headless resource loader. */
    public ResourceLoader() {
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Loads and validates one resource without executing its runtime factory.
     *
     * @param project containing validated project
     * @param resourceFile absolute resource path, or a path relative to the project root
     * @return validated resource or structured loading errors
     */
    public ResourceLoadResult load(GameProject project, Path resourceFile) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        Path suppliedPath = Objects.requireNonNull(resourceFile, "resourceFile");
        Path resolvedPath = suppliedPath.isAbsolute()
                ? suppliedPath.normalize()
                : validProject.root().resolve(suppliedPath).normalize();
        if (!resolvedPath.startsWith(validProject.root())) {
            return failure(resolvedPath, "resource.path.escape", "resource path is outside the project directory");
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return failure(
                    resolvedPath,
                    "resource.file.missing",
                    "resource does not exist or is not a regular file: " + resolvedPath);
        }
        Path source;
        try {
            source = resolvedPath.toRealPath();
        } catch (IOException exception) {
            return failure(
                    resolvedPath, "resource.file.read", "resource path cannot be resolved: " + exception.getMessage());
        }
        if (!source.startsWith(validProject.root())) {
            return failure(source, "resource.path.escape", "resource resolves outside the project directory");
        }
        return readResource(validProject, source);
    }

    /** Parses and semantically validates one resource. */
    private ResourceLoadResult readResource(GameProject project, Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            RawResource raw = jsonReader.read(input, RawResource.class);
            ResourceValidator.ValidationResult validation = ResourceValidator.validate(raw, project, source);
            return new ResourceLoadResult(validation.resource(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    source,
                    "resource.json",
                    "resource is not valid JScene3D Resource JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(source, "resource.file.read", "resource cannot be read: " + exception.getMessage());
        }
    }

    /** Creates one terminal error result. */
    private static ResourceLoadResult failure(Path source, String code, String message) {
        ProjectDiagnostic diagnostic =
                new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, source.toUri(), "");
        return new ResourceLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
