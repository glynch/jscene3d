/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.importing.internal.CacheStore;
import io.github.glynch.jscene3d.project.importing.internal.ImportCoordinator;
import io.github.glynch.jscene3d.project.importing.internal.ImportRegistry;
import io.github.glynch.jscene3d.project.importing.internal.ImporterBindings;
import io.github.glynch.jscene3d.project.importing.internal.Preconditions;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/** Project-scoped deterministic source inspection and import orchestration. */
public final class ImportManager implements ImportedArtifactLookup {
    private final ImportCoordinator coordinator;

    /** Stores one fully constructed coordinator. */
    private ImportManager(ImportCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Creates a manager from already resolved safe metadata and trusted host extensions.
     *
     * <p>The cache root may be outside the project and is created when absent. Operations are
     * synchronous; this type creates no threads. The manager supports concurrent independent
     * operations, while each returned {@link PreparedImport} is single-use and not thread-safe.
     *
     * @param project containing validated project
     * @param catalog resolved safe extension metadata
     * @param cacheRoot host-selected cache root
     * @param extensions trusted executable import contributions
     * @return project-scoped import manager
     */
    public static ImportManager create(
            GameProject project,
            RegisteredTypeCatalog catalog,
            Path cacheRoot,
            Collection<ProjectImportExtension> extensions) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        RegisteredTypeCatalog validCatalog = Objects.requireNonNull(catalog, "catalog");
        CacheStore cache = new CacheStore(Objects.requireNonNull(cacheRoot, "cacheRoot"));
        ImporterBindings bindings = register(validCatalog, List.copyOf(extensions));
        return new ImportManager(new ImportCoordinator(validProject, cache, bindings));
    }

    /**
     * Discovers class-path providers and appends trusted host-created extensions.
     *
     * @param project containing validated project
     * @param catalog resolved safe extension metadata
     * @param cacheRoot host-selected cache root
     * @param classLoader loader containing executable import providers
     * @param hostExtensions additional trusted host-created contributions
     * @return project-scoped import manager
     */
    public static ImportManager create(
            GameProject project,
            RegisteredTypeCatalog catalog,
            Path cacheRoot,
            ClassLoader classLoader,
            Collection<ProjectImportExtension> hostExtensions) {
        List<ProjectImportExtension> extensions = new ArrayList<>();
        ServiceLoader.load(ProjectImportExtension.class, Objects.requireNonNull(classLoader, "classLoader"))
                .forEach(extensions::add);
        extensions.addAll(List.copyOf(hostExtensions));
        return create(project, catalog, cacheRoot, extensions);
    }

    /**
     * Inspects one source using the default non-cancelling execution policy.
     *
     * @param assetId project-local source asset identity
     * @param importerId registered importer identity
     * @return immutable source inspection
     */
    public SourceInspection inspect(String assetId, String importerId) {
        return inspect(assetId, importerId, ImportExecution.defaults());
    }

    /**
     * Inspects one source without writing cache or project content.
     *
     * @param assetId project-local source asset identity
     * @param importerId registered importer identity
     * @param execution caller-owned cancellation and progress policy
     * @return immutable source inspection
     * @throws ImportCancelledException when cancellation is requested
     */
    public SourceInspection inspect(String assetId, String importerId, ImportExecution execution) {
        return coordinator.inspect(assetId, importerId, Objects.requireNonNull(execution, "execution"));
    }

    /**
     * Evaluates current source and dependency fingerprints without modifying the cache.
     *
     * @param definition structurally validated import definition belonging to this project
     * @return immutable import state and diagnostics
     */
    public ImportStatus status(ImportDefinition definition) {
        return coordinator.status(definition);
    }

    /**
     * Prepares one import using the default non-cancelling execution policy.
     *
     * @param definition structurally validated import definition belonging to this project
     * @return owned prepared transaction
     */
    public PreparedImport prepare(ImportDefinition definition) {
        return prepare(definition, ImportExecution.defaults());
    }

    /**
     * Prepares one complete candidate generation without publishing it.
     *
     * @param definition structurally validated import definition belonging to this project
     * @param execution caller-owned cancellation and progress policy retained through commit
     * @return owned prepared transaction, which must be closed
     * @throws ImportCancelledException when cancellation is requested
     */
    public PreparedImport prepare(ImportDefinition definition, ImportExecution execution) {
        return coordinator.prepare(definition, Objects.requireNonNull(execution, "execution"));
    }

    /**
     * Opens an artifact from the last successfully published generation, including when stale.
     *
     * @param definition structurally validated import definition belonging to this project
     * @param identity importer-local artifact identity
     * @return owned read handle when the active generation contains the identity
     */
    @Override
    public Optional<ImportedArtifact> openArtifact(ImportDefinition definition, String identity) {
        return coordinator.openArtifact(definition, identity);
    }

    /** Registers declared provider contributions in deterministic descriptor order. */
    private static ImporterBindings register(RegisteredTypeCatalog catalog, List<ProjectImportExtension> extensions) {
        Map<String, ProjectImportExtension> providers = indexProviders(extensions);
        ImporterBindings bindings = ImporterBindings.create();
        for (ExtensionDescriptor descriptor : catalog.extensions()) {
            ProjectImportExtension extension = providers.get(descriptor.id());
            if (extension != null) {
                ImportRegistry registry = new ImportRegistry(extension.id(), catalog, bindings);
                try {
                    extension.register(registry);
                } finally {
                    registry.closeRegistration();
                }
            }
        }
        return bindings;
    }

    /** Indexes unique executable providers by safe extension identity. */
    private static Map<String, ProjectImportExtension> indexProviders(List<ProjectImportExtension> extensions) {
        Map<String, ProjectImportExtension> providers = new LinkedHashMap<>();
        for (ProjectImportExtension extension : extensions) {
            ProjectImportExtension validExtension = Objects.requireNonNull(extension, "extensions entry");
            String id = Preconditions.requireNonBlank(validExtension.id(), "import extension id");
            if (providers.putIfAbsent(id, validExtension) != null) {
                throw new IllegalArgumentException("multiple import providers declare extension " + id);
            }
        }
        return providers;
    }
}
