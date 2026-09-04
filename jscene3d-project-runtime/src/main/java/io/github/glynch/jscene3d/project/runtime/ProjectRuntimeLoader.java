/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactLookup;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.internal.FactoryBindings;
import io.github.glynch.jscene3d.project.runtime.internal.Preconditions;
import io.github.glynch.jscene3d.project.runtime.internal.ProjectRuntimeComposer;
import io.github.glynch.jscene3d.project.runtime.internal.RuntimeCompositionException;
import io.github.glynch.jscene3d.project.runtime.internal.RuntimeDiagnosticsException;
import io.github.glynch.jscene3d.project.runtime.internal.RuntimeRegistry;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.scene.SceneLoadResult;
import io.github.glynch.jscene3d.project.scene.SceneLoader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/** Opens a validated project as an executable {@link ProjectRuntime}. */
public final class ProjectRuntimeLoader {
    private final ExtensionCatalogLoader catalogLoader;
    private final SceneLoader sceneLoader = new SceneLoader();

    /**
     * Creates a runtime loader for one running engine version.
     *
     * @param engineVersion semantic engine version used for extension compatibility
     */
    public ProjectRuntimeLoader(String engineVersion) {
        catalogLoader = new ExtensionCatalogLoader(engineVersion);
    }

    /**
     * Discovers safe descriptors and trusted implementations, then composes the entry scene.
     *
     * @param project validated project manifest
     * @param classLoader loader containing extension descriptors and service providers
     * @return composed runtime or structured diagnostics
     */
    public ProjectRuntimeLoadResult load(GameProject project, ClassLoader classLoader) {
        return load(project, classLoader, List.of());
    }

    /**
     * Discovers class-path extensions and appends trusted host-provided runtime extensions.
     *
     * <p>This overload allows an embedding host to contribute extensions that require live host
     * objects and therefore cannot be constructed by {@link ServiceLoader}.
     *
     * @param project validated project manifest
     * @param classLoader loader containing extension descriptors and service providers
     * @param hostExtensions trusted runtime extensions constructed by the embedding host
     * @return composed runtime or structured diagnostics
     */
    public ProjectRuntimeLoadResult load(
            GameProject project, ClassLoader classLoader, Collection<ProjectRuntimeExtension> hostExtensions) {
        return loadDiscovered(project, classLoader, hostExtensions, Optional.empty());
    }

    /**
     * Discovers extensions and composes a runtime able to resolve published imported resources.
     *
     * @param project validated project manifest
     * @param classLoader loader containing extension descriptors and service providers
     * @param hostExtensions trusted runtime extensions constructed by the embedding host
     * @param importedArtifacts host-owned lookup for published imported artifacts
     * @return composed runtime or structured diagnostics
     */
    public ProjectRuntimeLoadResult load(
            GameProject project,
            ClassLoader classLoader,
            Collection<ProjectRuntimeExtension> hostExtensions,
            ImportedArtifactLookup importedArtifacts) {
        return loadDiscovered(
                project, classLoader, hostExtensions, Optional.of(Objects.requireNonNull(importedArtifacts)));
    }

    /** Discovers metadata and implementations before composing through optional imported content. */
    private ProjectRuntimeLoadResult loadDiscovered(
            GameProject project,
            ClassLoader classLoader,
            Collection<ProjectRuntimeExtension> hostExtensions,
            Optional<ImportedArtifactLookup> importedArtifacts) {
        GameProject validProject = Objects.requireNonNull(project, "project");
        ClassLoader validClassLoader = Objects.requireNonNull(classLoader, "classLoader");
        List<ProjectRuntimeExtension> validHostExtensions = List.copyOf(hostExtensions);
        ExtensionCatalogLoadResult catalogResult = catalogLoader.load(validProject, validClassLoader);
        List<ProjectDiagnostic> diagnostics = new ArrayList<>(catalogResult.diagnostics());
        if (hasErrors(diagnostics)) {
            return ProjectRuntimeLoadResult.failure(diagnostics);
        }
        List<ProjectRuntimeExtension> extensions = discoverExtensions(validProject, validClassLoader, diagnostics);
        extensions.addAll(validHostExtensions);
        if (hasErrors(diagnostics)) {
            return ProjectRuntimeLoadResult.failure(diagnostics);
        }
        return loadResolved(validProject, catalogResult.catalog(), extensions, importedArtifacts, diagnostics);
    }

    /**
     * Composes a runtime from already resolved metadata and trusted implementations.
     *
     * <p>This overload supports embedded launchers and deterministic tests that perform artifact
     * resolution outside the service-loading boundary.
     *
     * @param project validated project manifest
     * @param catalog resolved safe type metadata
     * @param extensions trusted executable contributions
     * @return composed runtime or structured diagnostics
     */
    public ProjectRuntimeLoadResult load(
            GameProject project, RegisteredTypeCatalog catalog, Collection<ProjectRuntimeExtension> extensions) {
        return loadResolved(
                Objects.requireNonNull(project, "project"),
                Objects.requireNonNull(catalog, "catalog"),
                List.copyOf(extensions),
                Optional.empty(),
                new ArrayList<>());
    }

    /**
     * Composes from resolved metadata with a host lookup for published imported resources.
     *
     * @param project validated project manifest
     * @param catalog resolved safe type metadata
     * @param extensions trusted executable contributions
     * @param importedArtifacts host-owned lookup for published imported artifacts
     * @return composed runtime or structured diagnostics
     */
    public ProjectRuntimeLoadResult load(
            GameProject project,
            RegisteredTypeCatalog catalog,
            Collection<ProjectRuntimeExtension> extensions,
            ImportedArtifactLookup importedArtifacts) {
        return loadResolved(
                Objects.requireNonNull(project, "project"),
                Objects.requireNonNull(catalog, "catalog"),
                List.copyOf(extensions),
                Optional.of(Objects.requireNonNull(importedArtifacts, "importedArtifacts")),
                new ArrayList<>());
    }

    /** Loads and validates the entry scene before invoking trusted factories. */
    private ProjectRuntimeLoadResult loadResolved(
            GameProject project,
            RegisteredTypeCatalog catalog,
            List<ProjectRuntimeExtension> extensions,
            Optional<ImportedArtifactLookup> importedArtifacts,
            List<ProjectDiagnostic> diagnostics) {
        SceneLoadResult sceneResult = sceneLoader.loadEntryScene(project);
        diagnostics.addAll(sceneResult.diagnostics());
        if (hasErrors(diagnostics)) {
            return ProjectRuntimeLoadResult.failure(diagnostics);
        }
        SceneDefinition scene = sceneResult.scene().orElseThrow();
        diagnostics.addAll(catalog.validate(scene));
        validateSupportedConfiguration(project, diagnostics);
        if (hasErrors(diagnostics)) {
            return ProjectRuntimeLoadResult.failure(diagnostics);
        }
        FactoryBindings bindings = registerExtensions(project, catalog, extensions, diagnostics);
        if (hasErrors(diagnostics)) {
            return ProjectRuntimeLoadResult.failure(diagnostics);
        }
        try {
            ProjectRuntime runtime = new ProjectRuntimeComposer(
                            project, scene, catalog, bindings, importedArtifacts, diagnostics)
                    .compose();
            return ProjectRuntimeLoadResult.success(runtime, diagnostics);
        } catch (RuntimeDiagnosticsException exception) {
            diagnostics.addAll(exception.diagnostics());
        } catch (RuntimeCompositionException exception) {
            diagnostics.add(error(
                    scene.source().toUri(),
                    exception.code(),
                    Objects.toString(exception.getMessage(), "runtime composition failed"),
                    exception.location()));
        } catch (RuntimeException exception) {
            diagnostics.add(error(
                    scene.source().toUri(),
                    RuntimeDiagnosticCode.COMPOSITION_FAILED,
                    failureMessage("runtime composition failed", exception),
                    ""));
        }
        return ProjectRuntimeLoadResult.failure(diagnostics);
    }

    /** Discovers runtime providers without depending on implementation class names in project data. */
    private static List<ProjectRuntimeExtension> discoverExtensions(
            GameProject project, ClassLoader classLoader, List<ProjectDiagnostic> diagnostics) {
        List<ProjectRuntimeExtension> extensions = new ArrayList<>();
        try {
            ServiceLoader.load(ProjectRuntimeExtension.class, classLoader).forEach(extensions::add);
        } catch (ServiceConfigurationError error) {
            diagnostics.add(error(
                    manifest(project),
                    RuntimeDiagnosticCode.EXTENSION_DISCOVERY_FAILED,
                    failureMessage("runtime extension discovery failed", error),
                    "/runtime/applicationExtension"));
        }
        return extensions;
    }

    /** Registers contributions in safe descriptor order after resolving provider identities. */
    private static FactoryBindings registerExtensions(
            GameProject project,
            RegisteredTypeCatalog catalog,
            List<ProjectRuntimeExtension> extensions,
            List<ProjectDiagnostic> diagnostics) {
        Map<String, ProjectRuntimeExtension> providers = indexProviders(project, extensions, diagnostics);
        if (!providers.containsKey(project.runtime().applicationExtension())) {
            diagnostics.add(error(
                    manifest(project),
                    RuntimeDiagnosticCode.APPLICATION_EXTENSION_MISSING,
                    "application runtime extension was not discovered: "
                            + project.runtime().applicationExtension(),
                    "/runtime/applicationExtension"));
        }
        FactoryBindings bindings = new FactoryBindings();
        for (ExtensionDescriptor descriptor : catalog.extensions()) {
            ProjectRuntimeExtension extension = providers.get(descriptor.id());
            if (extension != null) {
                registerExtension(project, catalog, extension, bindings, diagnostics);
            }
        }
        return bindings;
    }

    /** Indexes unique declared runtime providers by their safe extension identity. */
    private static Map<String, ProjectRuntimeExtension> indexProviders(
            GameProject project, List<ProjectRuntimeExtension> extensions, List<ProjectDiagnostic> diagnostics) {
        Map<String, ProjectRuntimeExtension> providers = new LinkedHashMap<>();
        for (ProjectRuntimeExtension extension : extensions) {
            try {
                ProjectRuntimeExtension validExtension = Objects.requireNonNull(extension, "extensions entry");
                String id = Preconditions.requireNonBlank(validExtension.id(), "runtime extension id");
                if (providers.putIfAbsent(id, validExtension) != null) {
                    diagnostics.add(error(
                            manifest(project),
                            RuntimeDiagnosticCode.EXTENSION_DUPLICATE,
                            "multiple runtime providers declare extension " + id,
                            "/extensions"));
                }
            } catch (RuntimeException exception) {
                diagnostics.add(error(
                        manifest(project),
                        RuntimeDiagnosticCode.EXTENSION_INVALID,
                        failureMessage("invalid runtime extension provider", exception),
                        "/extensions"));
            }
        }
        return providers;
    }

    /** Invokes one extension inside a registration scope that closes on return. */
    private static void registerExtension(
            GameProject project,
            RegisteredTypeCatalog catalog,
            ProjectRuntimeExtension extension,
            FactoryBindings bindings,
            List<ProjectDiagnostic> diagnostics) {
        RuntimeRegistry registry = new RuntimeRegistry(extension.id(), catalog, bindings);
        try {
            extension.register(registry);
        } catch (RuntimeException exception) {
            diagnostics.add(error(
                    manifest(project),
                    RuntimeDiagnosticCode.EXTENSION_REGISTRATION_FAILED,
                    failureMessage("runtime extension registration failed for " + extension.id(), exception),
                    "/extensions"));
        } finally {
            registry.closeRegistration();
        }
    }

    /** Reports declarative runtime files not yet implemented by this first kernel. */
    private static void validateSupportedConfiguration(GameProject project, List<ProjectDiagnostic> diagnostics) {
        project.runtime()
                .projectSystems()
                .ifPresent(path -> diagnostics.add(error(
                        path.toUri(),
                        RuntimeDiagnosticCode.PROJECT_SYSTEMS_UNSUPPORTED,
                        "project-system definitions are not implemented by this runtime slice",
                        "")));
    }

    /** Returns whether any terminal diagnostic exists. */
    private static boolean hasErrors(List<ProjectDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
    }

    /** Creates one terminal diagnostic. */
    private static ProjectDiagnostic error(URI source, DiagnosticCode code, String technicalDetail, String location) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, location, Map.of("technicalDetail", technicalDetail));
    }

    /** Returns the project manifest URI used for runtime-provider diagnostics. */
    private static URI manifest(GameProject project) {
        return project.root().resolve(ProjectLoader.MANIFEST_NAME).toUri();
    }

    /** Adds implementation detail to a stable diagnostic prefix. */
    private static String failureMessage(String prefix, Throwable failure) {
        String detail = failure.getMessage();
        return detail == null ? prefix + ": " + failure.getClass().getSimpleName() : prefix + ": " + detail;
    }
}
