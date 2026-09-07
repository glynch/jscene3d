/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.util.List;

/** Host-selected engine facilities used to compose each project world. */
public interface ProjectRuntimeEnvironment {
    /**
     * Returns safe built-in descriptors available independently of project extension artifacts.
     *
     * @return immutable descriptor collection
     */
    List<ExtensionDescriptor> descriptors();

    /**
     * Returns executable built-in component factories corresponding to {@link #descriptors()}.
     *
     * @return immutable runtime-extension collection
     */
    List<ComponentRuntimeExtension> runtimeExtensions();

    /**
     * Creates fresh world-scoped adapter bindings for one load operation.
     *
     * <p>A successfully composed world takes ownership. The host closes adapters after failed composition.
     *
     * @return bindings in ownership and reverse-cleanup order
     */
    List<WorldModuleBinding<?>> createWorldModules();

    /**
     * Loads the definition resolver and immutable-resource provider for one validated project.
     *
     * <p>The returned resolver may combine the authored catalog with generated import publications. The returned
     * resource provider may use the project manifest and registered resource descriptors. Neither facility owns the
     * supplied project, type catalog, or authored catalog.
     *
     * @param project validated project manifest
     * @param types resolved safe descriptor catalog including this environment's built-ins
     * @param authored authored definition catalog
     * @return project-scoped composition content
     */
    ProjectContent loadContent(GameProject project, RegisteredTypeCatalog types, AssetCatalog authored);
}
