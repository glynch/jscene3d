/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.composition.CompositionPlan;
import io.github.glynch.jscene3d.project.composition.CompositionPlanResult;
import io.github.glynch.jscene3d.project.composition.CompositionPlanner;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.RuntimeResourceProvider;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
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
     * @param definitions authored and imported definition resolver
     * @param definition validated root Scene definition
     * @param types validated component descriptor catalog
     * @param extensions trusted executable runtime extensions
     * @param modules host-supplied world-module bindings
     * @param resources host-owned runtime resource provider
     * @return complete inactive world
     */
    public static World compose(
            URI source,
            DefinitionResolver definitions,
            SceneDefinition definition,
            RegisteredTypeCatalog types,
            Collection<ComponentRuntimeExtension> extensions,
            Collection<WorldModuleBinding<?>> modules,
            RuntimeResourceProvider resources) {
        CompositionPlanResult planning = CompositionPlanner.plan(source, definition, definitions, types);
        CompositionPlan plan =
                planning.plan().orElseThrow(() -> new RuntimeDiagnosticsException(planning.diagnostics()));
        FactoryBindings factories = WorldRuntimeExtensions.register(source, types, extensions);
        WorldModules worldModules = new WorldModules(modules);
        WorldCompositionServices services =
                new WorldCompositionServices(source, definitions, types, factories, worldModules, resources);
        InternalWorld world = new InternalWorld(definition, services);
        AllocatedWorld allocation = new EntityGraphAllocator(world, plan).allocate();
        return RuntimeComponentConstructor.construct(allocation, factories);
    }
}
