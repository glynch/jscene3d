/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetCatalogLoadResult;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.input.InputMapLoadResult;
import io.github.glynch.jscene3d.project.input.InputMapLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.extension.ApplicationRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/** Generic manifest-driven host used by editor play and exported-game launchers. */
public final class ProjectRuntimeHost implements ProjectHost {
    private final String engineVersion;
    private final ClassLoader classLoader;
    private final ProjectRuntimeEnvironment environment;

    /**
     * Creates a reusable host for one engine environment and extension class path.
     *
     * @param engineVersion running semantic engine version
     * @param classLoader extension and provider class loader
     * @param environment host-selected engine facilities
     */
    public ProjectRuntimeHost(String engineVersion, ClassLoader classLoader, ProjectRuntimeEnvironment environment) {
        this.engineVersion = Objects.requireNonNull(engineVersion, "engineVersion");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    public HostedProject load(Path projectRoot) {
        GameProject project = loadProject(projectRoot);
        Path startupScene =
                project.runtime().startupScene().orElse(project.runtime().entryScene());
        return load(project, startupScene);
    }

    /**
     * Loads the manifest-selected gameplay entry world rather than an optional startup world.
     *
     * @param projectRoot project directory containing {@code project.json}
     * @return composed inactive gameplay project
     */
    public HostedProject loadEntry(Path projectRoot) {
        GameProject project = loadProject(projectRoot);
        return load(project, project.runtime().entryScene());
    }

    /** Composes one validated authored world while retaining the loaded project configuration. */
    private HostedProject load(GameProject project, Path worldPath) {
        RegisteredTypeCatalog types = loadTypes(project);
        AssetCatalog assets = loadAssets(project);
        AssetMetadata startup = startupAsset(worldPath, assets);
        Optional<InputMapDefinition> inputMap = loadInputMap(project);
        List<ComponentRuntimeExtension> extensions = runtimeExtensions();
        ProjectContent content = loadContent(project, types, assets);
        List<WorldModuleBinding<?>> modules = List.copyOf(environment.createWorldModules(inputMap));
        WorldCompositionResult composition = WorldComposer.compose(
                content.definitions(),
                AssetRef.<WorldDefinition>to(startup.id()),
                types,
                extensions,
                modules,
                content.resources());
        if (!composition.isComposed()) {
            ProjectHostException failure =
                    new ProjectHostException("startup world composition failed", composition.diagnostics());
            closeModules(modules, failure);
            throw failure;
        }
        HostedProject hosted =
                new HostedProject(project, assets, composition.world().orElseThrow());
        prepareApplication(hosted, extensions);
        return hosted;
    }

    /** Loads the optional authored input map before constructing runtime modules. */
    private static Optional<InputMapDefinition> loadInputMap(GameProject project) {
        return project.runtime().inputMap().map(path -> {
            InputMapLoadResult result = new InputMapLoader().load(project, path);
            return result.definition()
                    .orElseThrow(
                            () -> new ProjectHostException("project input-map loading failed", result.diagnostics()));
        });
    }

    /** Loads project-scoped authored and generated content through the host-selected environment. */
    private ProjectContent loadContent(GameProject project, RegisteredTypeCatalog types, AssetCatalog assets) {
        try {
            return Objects.requireNonNull(
                    environment.loadContent(project, types, assets), "runtime environment project content");
        } catch (RuntimeException failure) {
            throw new ProjectHostException("project runtime content loading failed", failure);
        }
    }

    /** Loads and validates the project manifest. */
    private GameProject loadProject(Path projectRoot) {
        ProjectLoadResult result = new ProjectLoader(engineVersion).load(projectRoot);
        return result.project()
                .orElseThrow(() -> new ProjectHostException("project manifest loading failed", result.diagnostics()));
    }

    /** Combines declared extension metadata with safe host-selected built-ins. */
    private RegisteredTypeCatalog loadTypes(GameProject project) {
        ExtensionCatalogLoadResult result = new ExtensionCatalogLoader(engineVersion).load(project, classLoader);
        if (!result.isComplete()) {
            throw new ProjectHostException("extension catalog loading failed", result.diagnostics());
        }
        List<ExtensionDescriptor> descriptors = new ArrayList<>(result.catalog().extensions());
        descriptors.addAll(environment.descriptors());
        try {
            return RegisteredTypeCatalog.of(descriptors);
        } catch (IllegalArgumentException failure) {
            throw new ProjectHostException("runtime environment descriptors are invalid", failure);
        }
    }

    /** Scans the project root for stable definition identities. */
    private static AssetCatalog loadAssets(GameProject project) {
        AssetCatalogLoadResult result = AssetCatalog.scan(project.root());
        return result.catalog()
                .orElseThrow(() -> new ProjectHostException("asset catalog loading failed", result.diagnostics()));
    }

    /** Resolves one selected authored path to a world-definition identity. */
    private static AssetMetadata startupAsset(Path worldPath, AssetCatalog assets) {
        AssetMetadata startup = assets.assets().stream()
                .filter(asset -> asset.path().equals(worldPath))
                .findFirst()
                .orElseThrow(
                        () -> new ProjectHostException("startup world is absent from the asset catalog: " + worldPath));
        if (startup.kind() != AssetKind.WORLD_DEFINITION) {
            throw new ProjectHostException("startup asset is not a world definition: " + startup.path());
        }
        return startup;
    }

    /** Discovers application providers and combines them with host-owned built-ins. */
    private List<ComponentRuntimeExtension> runtimeExtensions() {
        List<ComponentRuntimeExtension> extensions = new ArrayList<>(environment.runtimeExtensions());
        try {
            ServiceLoader.load(ComponentRuntimeExtension.class, classLoader).forEach(extensions::add);
        } catch (ServiceConfigurationError failure) {
            throw new ProjectHostException("runtime extension discovery failed", failure);
        }
        return List.copyOf(extensions);
    }

    /** Invokes only the manifest-selected application preparation entry point. */
    private static void prepareApplication(HostedProject hosted, List<ComponentRuntimeExtension> extensions) {
        String applicationId = hosted.project().runtime().applicationExtension();
        ComponentRuntimeExtension selected = extensions.stream()
                .filter(extension -> extension.id().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> closeAfterFailure(
                        hosted,
                        new ProjectHostException(
                                "application runtime extension was not discovered: " + applicationId)));
        if (selected instanceof ApplicationRuntimeExtension application) {
            try {
                application.prepare(hosted);
            } catch (RuntimeException failure) {
                throw closeAfterFailure(
                        hosted,
                        new ProjectHostException("application runtime preparation failed: " + applicationId, failure));
            }
        }
    }

    /** Closes a composed project before returning one terminal preparation failure. */
    private static ProjectHostException closeAfterFailure(HostedProject hosted, ProjectHostException failure) {
        try {
            hosted.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
        return failure;
    }

    /** Closes caller-owned adapters after composition declines ownership. */
    private static void closeModules(List<WorldModuleBinding<?>> modules, ProjectHostException primaryFailure) {
        for (int index = modules.size() - 1; index >= 0; index--) {
            try {
                modules.get(index).module().close();
            } catch (RuntimeException failure) {
                primaryFailure.addSuppressed(failure);
            }
        }
    }
}
