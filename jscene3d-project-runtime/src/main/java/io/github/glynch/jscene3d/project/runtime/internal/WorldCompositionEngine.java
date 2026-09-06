/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceLookup;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.net.URI;
import java.util.Collection;

/** Internal composition pipeline hidden behind the public one-call world composer. */
public final class WorldCompositionEngine {
    /** Prevents construction of this stateless implementation entry point. */
    private WorldCompositionEngine() {
        throw new AssertionError("WorldCompositionEngine cannot be instantiated");
    }

    /**
     * Allocates the complete graph, constructs components transactionally, and returns an inactive world.
     *
     * @param source world source used for runtime diagnostics
     * @param assets authored asset catalog
     * @param definition validated root world definition
     * @param types validated component descriptor catalog
     * @param extensions trusted executable runtime extensions
     * @param resources shared runtime resource lookup
     * @return complete inactive world
     */
    public static World compose(
            URI source,
            AssetCatalog assets,
            WorldDefinition definition,
            RegisteredTypeCatalog types,
            Collection<ComponentRuntimeExtension> extensions,
            RuntimeResourceLookup resources) {
        FactoryBindings factories = WorldRuntimeExtensions.register(source, types, extensions);
        AllocatedWorld allocation = new EntityGraphAllocator(assets, types, definition, resources).allocate();
        return RuntimeComponentConstructor.construct(allocation, types, factories, resources);
    }
}
