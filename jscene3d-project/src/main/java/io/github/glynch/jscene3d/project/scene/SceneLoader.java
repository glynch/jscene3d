/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.scene.internal.RawScene;
import io.github.glynch.jscene3d.project.scene.internal.SceneValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Headless loader for versioned JScene3D scene definitions. */
public final class SceneLoader {
    private final ProjectJsonReader jsonReader;

    /** Creates a headless scene loader. */
    public SceneLoader() {
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Loads the entry scene declared by a validated project.
     *
     * @param project containing validated project
     * @return validated scene or structured loading errors
     */
    public SceneLoadResult loadEntryScene(GameProject project) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        return load(validProject, validProject.runtime().entryScene());
    }

    /**
     * Loads and validates one scene without loading extensions or starting runtime systems.
     *
     * @param project containing validated project
     * @param sceneFile absolute scene path, or a path relative to the project root
     * @return validated scene or structured loading errors
     */
    public SceneLoadResult load(GameProject project, Path sceneFile) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        Path suppliedPath = Objects.requireNonNull(sceneFile, "sceneFile");
        Path resolvedPath = suppliedPath.isAbsolute()
                ? suppliedPath.normalize()
                : validProject.root().resolve(suppliedPath).normalize();
        if (!resolvedPath.startsWith(validProject.root())) {
            return failure(resolvedPath, "scene.path.escape", "scene path is outside the project directory");
        }
        if (!Files.isRegularFile(resolvedPath)) {
            return failure(
                    resolvedPath,
                    "scene.file.missing",
                    "scene does not exist or is not a regular file: " + resolvedPath);
        }
        Path source;
        try {
            source = resolvedPath.toRealPath();
        } catch (IOException exception) {
            return failure(resolvedPath, "scene.file.read", "scene path cannot be resolved: " + exception.getMessage());
        }
        if (!source.startsWith(validProject.root())) {
            return failure(source, "scene.path.escape", "scene resolves outside the project directory");
        }
        return readScene(validProject, source);
    }

    /** Parses and semantically validates one scene. */
    private SceneLoadResult readScene(GameProject project, Path source) {
        try (InputStream input = Files.newInputStream(source)) {
            RawScene raw = jsonReader.read(input, RawScene.class);
            SceneValidator.ValidationResult validation = SceneValidator.validate(raw, project, source);
            return new SceneLoadResult(validation.scene(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    source, "scene.json", "scene is not valid JScene3D Scene JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(source, "scene.file.read", "scene cannot be read: " + exception.getMessage());
        }
    }

    /** Creates one terminal error result. */
    private static SceneLoadResult failure(Path source, String code, String message) {
        ProjectDiagnostic diagnostic =
                new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, source.toUri(), "");
        return new SceneLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
