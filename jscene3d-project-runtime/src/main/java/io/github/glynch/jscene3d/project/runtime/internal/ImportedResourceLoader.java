/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.ImportArtifactKind;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoadResult;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.resource.ResourceLoadResult;
import io.github.glynch.jscene3d.project.resource.ResourceLoader;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Resolves imported resource documents through logical published-artifact identities. */
final class ImportedResourceLoader {
    private final GameProject project;
    private final Optional<ImportedArtifactLookup> artifacts;
    private final List<ProjectDiagnostic> runtimeDiagnostics;
    private final ImportLoader importLoader = new ImportLoader();
    private final ResourceLoader resourceLoader = new ResourceLoader();
    private final Map<String, ImportDefinition> definitions = new LinkedHashMap<>();
    private boolean definitionsLoaded;

    /** Stores the project, optional host lookup, and shared diagnostic destination. */
    ImportedResourceLoader(
            GameProject project,
            Optional<ImportedArtifactLookup> artifacts,
            List<ProjectDiagnostic> runtimeDiagnostics) {
        this.project = Objects.requireNonNull(project, "project");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.runtimeDiagnostics = Objects.requireNonNull(runtimeDiagnostics, "runtimeDiagnostics");
    }

    /** Loads and validates one imported resource artifact. */
    ResourceDefinition load(ResourceReference reference) {
        URI source = URI.create(reference.toString());
        ImportedArtifactLookup lookup = artifacts.orElseThrow(() -> failure(
                source,
                RuntimeDiagnosticCode.IMPORT_LOOKUP_MISSING,
                "the host did not supply imported-artifact lookup",
                ""));
        ImportLocator locator = ImportLocator.parse(reference.locator());
        ImportDefinition definition = requireDefinition(locator.importId(), source);
        ImportedArtifact artifact = openArtifact(lookup, definition, locator.output(), source);
        return readArtifact(artifact, locator.output(), source);
    }

    /** Opens one published artifact and maps host/cache failures to runtime diagnostics. */
    private static ImportedArtifact openArtifact(
            ImportedArtifactLookup lookup, ImportDefinition definition, String output, URI source) {
        try {
            return lookup.openArtifact(definition, output)
                    .orElseThrow(() -> failure(
                            source,
                            RuntimeDiagnosticCode.IMPORT_ARTIFACT_MISSING,
                            "published import " + definition.id() + " has no artifact " + output,
                            ""));
        } catch (RuntimeDiagnosticsException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw failure(
                    source,
                    RuntimeDiagnosticCode.IMPORT_ARTIFACT_OPEN_FAILED,
                    failureMessage("imported artifact lookup failed", exception),
                    "");
        }
    }

    /** Reads one owned artifact handle and verifies its published contract. */
    private ResourceDefinition readArtifact(ImportedArtifact artifact, String expectedIdentity, URI source) {
        try (artifact) {
            ImportArtifactDescriptor descriptor = artifact.metadata().descriptor();
            if (!expectedIdentity.equals(descriptor.identity())) {
                throw failure(
                        source,
                        RuntimeDiagnosticCode.IMPORT_ARTIFACT_IDENTITY_MISMATCH,
                        "requested artifact " + expectedIdentity + " but lookup returned " + descriptor.identity(),
                        "");
            }
            requireResourceArtifact(descriptor, source);
            try (InputStream input = artifact.openStream()) {
                ResourceLoadResult result = resourceLoader.load(project, source, input);
                if (!result.isValid()) {
                    throw new RuntimeDiagnosticsException(result.diagnostics());
                }
                runtimeDiagnostics.addAll(result.diagnostics());
                ResourceDefinition resource = result.resource().orElseThrow();
                if (!descriptor.resourceType().orElseThrow().equals(resource.type())) {
                    throw failure(
                            source,
                            RuntimeDiagnosticCode.IMPORT_ARTIFACT_TYPE_MISMATCH,
                            "published type " + descriptor.resourceType().orElseThrow()
                                    + " differs from resource document type " + resource.type(),
                            "/type");
                }
                return resource;
            }
        } catch (RuntimeDiagnosticsException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw failure(
                    source,
                    RuntimeDiagnosticCode.IMPORT_ARTIFACT_OPEN_FAILED,
                    failureMessage("imported resource could not be read", exception),
                    "");
        }
    }

    /** Requires a typed resource artifact before parsing its content. */
    private static void requireResourceArtifact(ImportArtifactDescriptor descriptor, URI source) {
        if (descriptor.kind() != ImportArtifactKind.RESOURCE) {
            throw failure(
                    source,
                    RuntimeDiagnosticCode.IMPORT_ARTIFACT_KIND_INVALID,
                    "artifact " + descriptor.identity() + " is " + descriptor.kind() + " rather than RESOURCE",
                    "");
        }
    }

    /** Returns one uniquely indexed import definition. */
    private ImportDefinition requireDefinition(String importId, URI source) {
        loadDefinitions();
        ImportDefinition definition = definitions.get(importId);
        if (definition == null) {
            throw failure(
                    source,
                    RuntimeDiagnosticCode.IMPORT_DEFINITION_MISSING,
                    "project declares no import definition named " + importId,
                    "");
        }
        return definition;
    }

    /** Loads and indexes all manifest-declared import definitions once. */
    private void loadDefinitions() {
        if (definitionsLoaded) {
            return;
        }
        definitionsLoaded = true;
        List<ProjectDiagnostic> errors = new ArrayList<>();
        for (var path : project.imports()) {
            ImportLoadResult result = importLoader.load(project, path);
            if (!result.isValid()) {
                errors.addAll(result.diagnostics());
                continue;
            }
            runtimeDiagnostics.addAll(result.diagnostics());
            ImportDefinition definition = result.definition().orElseThrow();
            ImportDefinition existing = definitions.putIfAbsent(definition.id(), definition);
            if (existing != null) {
                errors.add(diagnostic(
                        definition.source().toUri(),
                        RuntimeDiagnosticCode.IMPORT_DEFINITION_DUPLICATE,
                        "import identity " + definition.id() + " is also declared by " + existing.source(),
                        "/id"));
            }
        }
        if (!errors.isEmpty()) {
            throw new RuntimeDiagnosticsException(errors);
        }
    }

    /** Creates one structured runtime failure. */
    private static RuntimeDiagnosticsException failure(
            URI source, DiagnosticCode code, String technicalDetail, String location) {
        return new RuntimeDiagnosticsException(List.of(diagnostic(source, code, technicalDetail, location)));
    }

    /** Creates one structured runtime diagnostic. */
    private static ProjectDiagnostic diagnostic(
            URI source, DiagnosticCode code, String technicalDetail, String location) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, location, Map.of("technicalDetail", technicalDetail));
    }

    /** Adds implementation detail to a stable failure prefix. */
    private static String failureMessage(String prefix, Exception failure) {
        String detail = failure.getMessage();
        return detail == null ? prefix + ": " + failure.getClass().getSimpleName() : prefix + ": " + detail;
    }

    /** Parsed import-definition and output portions of an imported reference locator. */
    private record ImportLocator(String importId, String output) {
        /** Splits a locator already validated by the project-value decoder. */
        private static ImportLocator parse(String locator) {
            int separator = locator.indexOf('/');
            return new ImportLocator(locator.substring(0, separator), locator.substring(separator + 1));
        }
    }
}
