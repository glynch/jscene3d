/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.manifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.internal.SemanticVersion;
import io.github.glynch.jscene3d.project.manifest.internal.ManifestValidator;
import io.github.glynch.jscene3d.project.manifest.internal.ProjectDescriptorLocator;
import io.github.glynch.jscene3d.project.manifest.internal.RawManifest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Headless loader for a versioned JScene3D game-project descriptor. */
public final class ProjectLoader {
    /** Required suffix for a current project-named descriptor. */
    public static final String DESCRIPTOR_SUFFIX = ".j3d";

    /** Temporarily supported legacy fixed descriptor filename. */
    public static final String LEGACY_DESCRIPTOR_NAME = "jscene3d.json";

    /** Compatibility alias for callers which still create legacy project fixtures. */
    public static final String MANIFEST_NAME = LEGACY_DESCRIPTOR_NAME;

    private final SemanticVersion engineVersion;
    private final String engineVersionText;
    private final ProjectJsonReader jsonReader;

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
        jsonReader = ProjectJsonReader.strict();
    }

    /**
     * Loads and validates a project without loading extensions or executing asset-import code.
     *
     * @param projectPath existing project directory or selected descriptor path
     * @return validated project or structured loading errors
     */
    public ProjectLoadResult load(Path projectPath) {
        ProjectDescriptorLocator.Result discovery = ProjectDescriptorLocator.locate(projectPath);
        if (discovery.location().isEmpty()) {
            return new ProjectLoadResult(Optional.empty(), discovery.diagnostics());
        }
        ProjectDescriptorLocator.Location location = discovery.location().orElseThrow();
        return readManifest(location.root(), location.descriptor(), discovery.diagnostics());
    }

    /** Parses and semantically validates the manifest. */
    private ProjectLoadResult readManifest(Path root, Path manifest, List<ProjectDiagnostic> discoveryDiagnostics) {
        try (InputStream input = Files.newInputStream(manifest)) {
            RawManifest raw = jsonReader.read(input, RawManifest.class);
            ManifestValidator.ValidationResult validation =
                    ManifestValidator.validate(raw, root, manifest, engineVersion, engineVersionText);
            List<ProjectDiagnostic> diagnostics = new ArrayList<>(discoveryDiagnostics);
            diagnostics.addAll(validation.diagnostics());
            return new ProjectLoadResult(validation.project(), diagnostics);
        } catch (JsonProcessingException exception) {
            return failure(
                    manifest,
                    ProjectDiagnosticCode.MANIFEST_JSON_INVALID,
                    "project manifest is not valid Project Manifest JSON: " + exception.getOriginalMessage(),
                    discoveryDiagnostics);
        } catch (IOException exception) {
            return failure(
                    manifest,
                    ProjectDiagnosticCode.MANIFEST_READ_FAILED,
                    "project manifest cannot be read: " + exception.getMessage(),
                    discoveryDiagnostics);
        }
    }

    /** Creates one terminal error result. */
    private static ProjectLoadResult failure(
            Path source, DiagnosticCode code, String technicalDetail, List<ProjectDiagnostic> precedingDiagnostics) {
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source.toUri(), "", Map.of("technicalDetail", technicalDetail));
        List<ProjectDiagnostic> diagnostics = new ArrayList<>(precedingDiagnostics);
        diagnostics.add(diagnostic);
        return new ProjectLoadResult(Optional.empty(), diagnostics);
    }
}
