/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireAbsoluteUri;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.internal.RawResource;
import io.github.glynch.jscene3d.project.resource.internal.ResourceValidator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
            return failure(
                    resolvedPath,
                    ResourceDiagnosticCode.PATH_ESCAPES_PROJECT,
                    "resource path is outside the project directory");
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return failure(
                    resolvedPath,
                    ResourceDiagnosticCode.FILE_MISSING,
                    "resource does not exist or is not a regular file: " + resolvedPath);
        }
        Path source;
        try {
            source = resolvedPath.toRealPath();
        } catch (IOException exception) {
            return failure(
                    resolvedPath,
                    ResourceDiagnosticCode.FILE_READ_FAILED,
                    "resource path cannot be resolved: " + exception.getMessage());
        }
        if (!source.startsWith(validProject.root())) {
            return failure(
                    source,
                    ResourceDiagnosticCode.PATH_ESCAPES_PROJECT,
                    "resource resolves outside the project directory");
        }
        return readResource(validProject, source);
    }

    /** Parses and semantically validates one resource. */
    private ResourceLoadResult readResource(GameProject project, Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            RawResource raw = jsonReader.read(input, RawResource.class);
            ResourceValidator.ValidationResult validation = ResourceValidator.validate(raw, project, source.toUri());
            return new ResourceLoadResult(validation.resource(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    source,
                    ResourceDiagnosticCode.JSON_INVALID,
                    "resource is not valid JScene3D Resource JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(
                    source,
                    ResourceDiagnosticCode.FILE_READ_FAILED,
                    "resource cannot be read: " + exception.getMessage());
        }
    }

    /**
     * Loads one generated resource from a logical source without exposing its physical storage.
     *
     * <p>The caller retains ownership of {@code input}; this method consumes but does not close it.
     *
     * @param project containing validated project
     * @param source absolute logical resource source
     * @param input serialized JScene3D Resource JSON
     * @return validated resource or structured loading errors
     */
    public ResourceLoadResult load(GameProject project, URI source, InputStream input) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        URI validSource = requireAbsoluteUri(source, "source").normalize();
        InputStream validInput = Objects.requireNonNull(input, "input");
        try {
            RawResource raw = jsonReader.read(validInput, RawResource.class);
            ResourceValidator.ValidationResult validation =
                    ResourceValidator.validate(raw, validProject, validSource.normalize());
            return new ResourceLoadResult(validation.resource(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    validSource,
                    ResourceDiagnosticCode.JSON_INVALID,
                    "resource is not valid JScene3D Resource JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(
                    validSource,
                    ResourceDiagnosticCode.FILE_READ_FAILED,
                    "resource cannot be read: " + exception.getMessage());
        }
    }

    /** Creates one terminal error result. */
    private static ResourceLoadResult failure(Path source, DiagnosticCode code, String technicalDetail) {
        return failure(source.toUri(), code, technicalDetail);
    }

    /** Creates one terminal error result for a logical source. */
    private static ResourceLoadResult failure(URI source, DiagnosticCode code, String technicalDetail) {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, "", Map.of("technicalDetail", technicalDetail));
        return new ResourceLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
