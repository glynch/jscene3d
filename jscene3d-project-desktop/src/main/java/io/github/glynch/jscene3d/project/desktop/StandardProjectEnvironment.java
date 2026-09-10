/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import io.github.glynch.jscene3d.game.StandardGameDescriptors;
import io.github.glynch.jscene3d.game.application.ApplicationControl;
import io.github.glynch.jscene3d.game.application.GameApplicationRuntimeExtension;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.game.presentation.GamePresentationResourceLoaders;
import io.github.glynch.jscene3d.game.presentation.GamePresentationRuntimeExtension;
import io.github.glynch.jscene3d.game.presentation.PresentationWorldModule;
import io.github.glynch.jscene3d.game.project3d.Game3dRuntimeExtension;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.PublishedProjectContent;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.physics3d.Physics3dAdapters;
import io.github.glynch.jscene3d.project.physics3d.Physics3dResourceLoaders;
import io.github.glynch.jscene3d.project.physics3d.Physics3dRuntimeExtension;
import io.github.glynch.jscene3d.project.physics3d.Physics3dWorldModule;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeEnvironment;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLoader;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dResourceLoaders;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Standard game environment combining semantic input, 3D presentation, and 3D physics.
 *
 * <p>Each load creates fresh world-owned adapters. Authored definitions are combined with
 * previously published import artifacts from the supplied read-only cache; this environment never
 * executes importers or writes project content.
 */
public final class StandardProjectEnvironment implements ProjectRuntimeEnvironment {
    private final Path publishedImports;
    private final Supplier<ApplicationControl> applicationControls;

    /** Selects the read-only published-import cache used by project composition.
     *
     * @param publishedImports cache containing complete build- or editor-published generations
     */
    public StandardProjectEnvironment(Path publishedImports) {
        this(publishedImports, new DesktopApplicationState()::createControl);
    }

    /** Retains the desktop session's world-control adapter factory. */
    StandardProjectEnvironment(Path publishedImports, Supplier<ApplicationControl> applicationControls) {
        this.publishedImports = Objects.requireNonNull(publishedImports, "publishedImports")
                .toAbsolutePath()
                .normalize();
        this.applicationControls = Objects.requireNonNull(applicationControls, "applicationControls");
    }

    @Override
    public List<ExtensionDescriptor> descriptors() {
        return StandardGameDescriptors.all();
    }

    @Override
    public List<ComponentRuntimeExtension> runtimeExtensions() {
        return List.of(
                new GameApplicationRuntimeExtension(),
                new Spatial3dRuntimeExtension(),
                new Physics3dRuntimeExtension(),
                new Game3dRuntimeExtension(),
                new GamePresentationRuntimeExtension());
    }

    @Override
    public List<WorldModuleBinding<?>> createWorldModules(Optional<InputMapDefinition> inputMap) {
        ProjectInput input = inputMap.map(ProjectInput::new).orElseGet(ProjectInput::empty);
        return List.of(
                WorldModuleBinding.of(InputWorldModule.class, input),
                WorldModuleBinding.of(ApplicationControl.class, applicationControls.get()),
                WorldModuleBinding.of(Spatial3dWorldModule.class, Spatial3dAdapters.standard()),
                WorldModuleBinding.of(Physics3dWorldModule.class, Physics3dAdapters.standard()),
                WorldModuleBinding.of(PresentationWorldModule.class, new StandardPresentationWorldModule()));
    }

    @Override
    public ProjectContent loadContent(GameProject project, RegisteredTypeCatalog types, AssetCatalog authored) {
        List<RuntimeResourceLoader<?>> loaders = new ArrayList<>();
        loaders.addAll(Spatial3dResourceLoaders.all());
        loaders.addAll(Physics3dResourceLoaders.all());
        loaders.addAll(GamePresentationResourceLoaders.all());
        return PublishedProjectContent.load(project, types, authored, publishedImports, loaders);
    }
}
