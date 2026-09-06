/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.internal.RuntimeCompositionException;
import io.github.glynch.jscene3d.project.runtime.internal.RuntimeDiagnosticsException;
import io.github.glynch.jscene3d.project.runtime.internal.WorldCompositionEngine;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates and transactionally composes authored worlds through registered component factories.
 *
 * <p>The single operation hides definition loading, placement expansion, instance scoping, complete graph allocation,
 * factory registration, effective-property resolution, authored reference binding, endpoint wiring, and rollback.
 * Success returns a complete inactive world; failure returns ordered structured diagnostics and never publishes the
 * partial world.
 */
public final class WorldComposer {
    /** Prevents construction of this stateless composition entry point. */
    private WorldComposer() {
        throw new AssertionError("WorldComposer cannot be instantiated");
    }

    /**
     * Loads, validates, and transactionally composes one inactive world.
     *
     * <p>All entity shells are allocated before the first component factory is invoked. All component factories finish
     * before authored references are bound. Descriptor-declared endpoints are implemented and authored connections are
     * resolved only after reference binding; every binding succeeds before the inactive world is published. Runtime
     * extensions register only executable factories for component descriptors they own. The supplied resource lookup
     * remains owned by the caller. World-module bindings use exact stable interfaces. A successfully composed world
     * takes ownership of their adapters and closes them after its component values; failed composition leaves every
     * adapter owned by the caller.
     *
     * @param assets stable authored asset catalog
     * @param reference startup world-definition reference
     * @param types safe resolved component descriptor catalog
     * @param extensions trusted executable runtime extensions
     * @param modules host-supplied world-module bindings in ownership and reverse-cleanup order
     * @param resources shared runtime resource lookup
     * @return inactive world or ordered structured diagnostics
     */
    public static WorldCompositionResult compose(
            AssetCatalog assets,
            AssetRef<WorldDefinition> reference,
            RegisteredTypeCatalog types,
            Collection<ComponentRuntimeExtension> extensions,
            Collection<WorldModuleBinding<?>> modules,
            RuntimeResourceLookup resources) {
        AssetCatalog validAssets = Objects.requireNonNull(assets, "assets");
        AssetRef<WorldDefinition> validReference = Objects.requireNonNull(reference, "reference");
        RegisteredTypeCatalog validTypes = Objects.requireNonNull(types, "types");
        List<ComponentRuntimeExtension> validExtensions = List.copyOf(extensions);
        List<WorldModuleBinding<?>> validModules = List.copyOf(modules);
        RuntimeResourceLookup validResources = Objects.requireNonNull(resources, "resources");
        DefinitionLoadResult<WorldDefinition> loaded = validAssets.loadWorld(validReference, validTypes);
        List<ProjectDiagnostic> diagnostics = new ArrayList<>(loaded.diagnostics());
        if (!loaded.isValid()) {
            return WorldCompositionResult.failure(diagnostics);
        }
        URI source = validAssets
                .find(validReference.id())
                .map(AssetMetadata::path)
                .map(Path::toUri)
                .orElseGet(() -> validAssets.root().toUri());
        try {
            World world = WorldCompositionEngine.compose(
                    source,
                    validAssets,
                    loaded.definition().orElseThrow(),
                    validTypes,
                    validExtensions,
                    validModules,
                    validResources);
            return WorldCompositionResult.success(world, diagnostics);
        } catch (RuntimeDiagnosticsException exception) {
            diagnostics.addAll(exception.diagnostics());
        } catch (RuntimeCompositionException exception) {
            diagnostics.add(error(
                    source,
                    exception.code(),
                    Objects.toString(exception.getMessage(), "world composition failed"),
                    exception.location()));
        } catch (RuntimeException exception) {
            diagnostics.add(error(
                    source,
                    RuntimeDiagnosticCode.COMPOSITION_FAILED,
                    failureMessage("world composition failed", exception),
                    ""));
        }
        return WorldCompositionResult.failure(diagnostics);
    }

    /** Creates one terminal structured runtime diagnostic. */
    private static ProjectDiagnostic error(URI source, DiagnosticCode code, String detail, String location) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, source, location, Map.of("technicalDetail", detail));
    }

    /** Appends the available implementation detail to a stable diagnostic prefix. */
    private static String failureMessage(String prefix, Throwable failure) {
        String detail = failure.getMessage();
        return detail == null ? prefix + ": " + failure.getClass().getSimpleName() : prefix + ": " + detail;
    }
}
