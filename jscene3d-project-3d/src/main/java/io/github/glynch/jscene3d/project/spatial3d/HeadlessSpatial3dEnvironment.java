/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeEnvironment;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLease;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.util.List;
import java.util.Optional;

/** Standard headless-capable three-dimensional environment for project play and tests. */
public final class HeadlessSpatial3dEnvironment implements ProjectRuntimeEnvironment {
    private static final RuntimeResourceProvider NO_RESOURCES = new RuntimeResourceProvider() {
        @Override
        public <T> RuntimeResourceLease<T> acquire(ResourceReference reference, Class<T> valueType) {
            throw new IllegalStateException("the headless 3D environment has no runtime resource adapter");
        }
    };

    /** Creates a stateless environment which opens fresh spatial adapters for each project. */
    public HeadlessSpatial3dEnvironment() {
        // Public construction is the stable host configuration entry point.
    }

    @Override
    public List<ExtensionDescriptor> descriptors() {
        return List.of(Spatial3dDescriptors.extensionDescriptor());
    }

    @Override
    public List<ComponentRuntimeExtension> runtimeExtensions() {
        return List.of(new Spatial3dRuntimeExtension());
    }

    @Override
    public List<WorldModuleBinding<?>> createWorldModules(Optional<InputMapDefinition> inputMap) {
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();
        return List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial));
    }

    @Override
    public ProjectContent loadContent(GameProject project, RegisteredTypeCatalog types, AssetCatalog authored) {
        return new ProjectContent(authored, NO_RESOURCES);
    }
}
