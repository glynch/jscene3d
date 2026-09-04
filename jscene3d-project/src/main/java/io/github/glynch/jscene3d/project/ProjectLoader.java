/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glynch.jscene3d.project.internal.ManifestValidator;
import io.github.glynch.jscene3d.project.internal.RawManifest;
import io.github.glynch.jscene3d.project.internal.SemanticVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Headless loader for a versioned {@value #MANIFEST_NAME} game-project manifest. */
public final class ProjectLoader {
    /** Conventional manifest filename within a project directory. */
    public static final String MANIFEST_NAME = "project.json";

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private final SemanticVersion engineVersion;
    private final String engineVersionText;

    /**
     * Creates a loader that validates compatibility with one JScene3D engine version.
     *
     * @param engineVersion semantic version of the running engine
     * @throws IllegalArgumentException if {@code engineVersion} is not a semantic version
     */
    public ProjectLoader(String engineVersion) {
        engineVersionText = Objects.requireNonNull(engineVersion, "engineVersion");
        this.engineVersion = SemanticVersion.parse(engineVersionText)
                .orElseThrow(() -> new IllegalArgumentException("engineVersion must be a semantic version"));
    }

    /**
     * Loads and validates a project without loading extensions or executing asset-import code.
     *
     * @param projectDirectory existing project directory containing {@value #MANIFEST_NAME}
     * @return validated project or structured loading errors
     */
    public ProjectLoadResult load(Path projectDirectory) {
        Path suppliedRoot = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath()
                .normalize();
        Path suppliedManifest = suppliedRoot.resolve(MANIFEST_NAME);
        if (!Files.isDirectory(suppliedRoot)) {
            return failure(
                    suppliedManifest,
                    "project.directory.missing",
                    "project directory does not exist or is not a directory: " + suppliedRoot);
        }

        Path root;
        try {
            root = suppliedRoot.toRealPath();
        } catch (IOException exception) {
            return failure(
                    suppliedManifest,
                    "project.directory.read",
                    "project directory cannot be resolved: " + exception.getMessage());
        }
        Path manifest = root.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            return failure(
                    manifest,
                    "project.manifest.missing",
                    "project manifest does not exist or is not a regular file: " + manifest);
        }
        if (!isInsideRoot(root, manifest)) {
            return failure(
                    manifest, "project.manifest.escape", "project manifest resolves outside the project directory");
        }
        return readManifest(root, manifest);
    }

    /** Parses and semantically validates the manifest. */
    private ProjectLoadResult readManifest(Path root, Path manifest) {
        try (InputStream input = Files.newInputStream(manifest)) {
            RawManifest raw = OBJECT_MAPPER.readValue(input, RawManifest.class);
            ManifestValidator.ValidationResult validation =
                    ManifestValidator.validate(raw, root, manifest, engineVersion, engineVersionText);
            return new ProjectLoadResult(validation.project(), validation.diagnostics());
        } catch (JsonProcessingException exception) {
            return failure(
                    manifest,
                    "project.manifest.json",
                    "project manifest is not valid Project Manifest JSON: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            return failure(
                    manifest, "project.manifest.read", "project manifest cannot be read: " + exception.getMessage());
        }
    }

    /** Returns whether an existing path's real target remains inside the project root. */
    private static boolean isInsideRoot(Path root, Path path) {
        try {
            return path.toRealPath().startsWith(root);
        } catch (IOException ignored) {
            return false;
        }
    }

    /** Creates a strict JSON reader with duplicate and trailing-token detection. */
    private static ObjectMapper createObjectMapper() {
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        return new ObjectMapper(factory)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }

    /** Creates one terminal error result. */
    private static ProjectLoadResult failure(Path source, String code, String message) {
        ProjectDiagnostic diagnostic =
                new ProjectDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, source, "");
        return new ProjectLoadResult(Optional.empty(), List.of(diagnostic));
    }
}
