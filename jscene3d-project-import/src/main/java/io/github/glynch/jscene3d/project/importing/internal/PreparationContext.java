/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing.internal;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.importing.ArtifactContentWriter;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.ImportExecution;
import io.github.glynch.jscene3d.project.importing.ImportPhase;
import io.github.glynch.jscene3d.project.importing.ImportProgress;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import io.github.glynch.jscene3d.project.importing.extension.ImportPreparationContext;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Concrete adapter context writing artifacts into one owned staging workspace. */
public final class PreparationContext extends AbstractImportContext implements ImportPreparationContext {
    private final ImportDefinition definition;
    private final TemporaryWorkspace workspace;
    private final List<StagedArtifact> artifacts = new ArrayList<>();
    private final Set<String> artifactIdentities = new LinkedHashSet<>();

    /**
     * Creates one preparation context.
     *
     * @param project containing project
     * @param definition import definition being prepared
     * @param execution caller-owned execution policy
     * @param workspace owned staging workspace
     */
    public PreparationContext(
            GameProject project, ImportDefinition definition, ImportExecution execution, TemporaryWorkspace workspace) {
        super(project, definition.asset(), execution);
        this.definition = Objects.requireNonNull(definition, "definition");
        this.workspace = Objects.requireNonNull(workspace, "workspace");
    }

    @Override
    public ImportDefinition definition() {
        return definition;
    }

    @Override
    public void artifact(ImportArtifactDescriptor descriptor, ArtifactContentWriter content) throws IOException {
        checkCancelled();
        ImportArtifactDescriptor validDescriptor = Objects.requireNonNull(descriptor, "descriptor");
        ArtifactContentWriter validContent = Objects.requireNonNull(content, "content");
        if (!artifactIdentities.add(validDescriptor.identity())) {
            throw new IllegalArgumentException("artifact identity is duplicated: " + validDescriptor.identity());
        }
        progress(ImportProgress.phase(ImportPhase.WRITING, "Writing " + validDescriptor.identity()));
        String relativePath = "artifacts/" + String.format(Locale.ROOT, "%05d.bin", artifacts.size());
        Path path = workspace.root().resolve(relativePath);
        Files.createDirectories(path.getParent());
        try (OutputStream output = Files.newOutputStream(path)) {
            validContent.write(output);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(path);
            artifactIdentities.remove(validDescriptor.identity());
            throw exception;
        }
        checkCancelled();
        ImportedArtifactMetadata metadata =
                new ImportedArtifactMetadata(validDescriptor, ImportHashes.file(path), Files.size(path));
        artifacts.add(new StagedArtifact(metadata, path, relativePath));
    }

    /**
     * Returns completely written artifacts in adapter declaration order.
     *
     * @return immutable staged artifacts
     */
    public List<StagedArtifact> artifacts() {
        return List.copyOf(artifacts);
    }

    /** Returns the staging workspace owned by this preparation. */
    TemporaryWorkspace workspace() {
        return workspace;
    }

    /**
     * Reports one import-definition error rather than an adapter-source error.
     *
     * @param code stable diagnostic code
     * @param message actionable diagnostic message
     * @param location JSON Pointer within the import definition
     */
    public void definitionError(String code, String message, String location) {
        definitionDiagnostic(ProjectDiagnostic.Severity.ERROR, code, message, location);
    }

    /**
     * Reports one import-definition warning rather than an adapter-source warning.
     *
     * @param code stable diagnostic code
     * @param message actionable diagnostic message
     * @param location JSON Pointer within the import definition
     */
    public void definitionWarning(String code, String message, String location) {
        definitionDiagnostic(ProjectDiagnostic.Severity.WARNING, code, message, location);
    }

    /** Adds one validated diagnostic associated with the authored import definition. */
    private void definitionDiagnostic(
            ProjectDiagnostic.Severity severity, String code, String message, String location) {
        diagnostic(new ProjectDiagnostic(
                severity, code, message, definition.source().toUri(), location));
    }
}
