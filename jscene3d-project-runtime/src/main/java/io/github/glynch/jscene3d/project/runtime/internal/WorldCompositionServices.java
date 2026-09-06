/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import java.net.URI;
import java.util.Objects;

/** Immutable collaborators shared by initial and runtime entity composition. */
record WorldCompositionServices(
        URI source,
        DefinitionResolver definitions,
        RegisteredTypeCatalog types,
        FactoryBindings factories,
        WorldModules modules,
        RuntimeResourceProvider resources) {
    /** Validates every required world composition collaborator. */
    WorldCompositionServices {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(definitions, "definitions");
        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(factories, "factories");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(resources, "resources");
    }
}
