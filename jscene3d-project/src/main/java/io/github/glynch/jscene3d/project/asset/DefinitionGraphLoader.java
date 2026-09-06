/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import io.github.glynch.jscene3d.project.asset.internal.DefinitionDocumentReader;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves typed assets and validates their transitive entity-definition graph. */
final class DefinitionGraphLoader {
    private final AssetCatalog catalog;
    private final List<ProjectDiagnostic> diagnostics = new ArrayList<>();
    private final Map<AssetId, EntityDefinition> loadedEntities = new HashMap<>();
    private final Set<AssetId> validatedEntities = new HashSet<>();
    private final LinkedHashSet<AssetId> visitingEntities = new LinkedHashSet<>();

    /** Stores one catalog-local graph load. */
    private DefinitionGraphLoader(AssetCatalog catalog) {
        this.catalog = catalog;
    }

    /** Loads one entity definition and validates its complete inclusion graph. */
    static DefinitionLoadResult<EntityDefinition> loadEntity(
            AssetCatalog catalog, AssetRef<EntityDefinition> reference) {
        DefinitionGraphLoader loader = new DefinitionGraphLoader(catalog);
        Optional<AssetMetadata> metadata =
                loader.resolve(reference.id(), AssetKind.ENTITY_DEFINITION, catalog.root(), "");
        Optional<EntityDefinition> definition = metadata.flatMap(loader::readEntity);
        metadata.ifPresent(value -> definition.ifPresent(asset -> loader.validateEntityGraph(value, asset)));
        return loader.result(definition);
    }

    /** Loads one world and validates every transitively placed entity definition. */
    static DefinitionLoadResult<WorldDefinition> loadWorld(AssetCatalog catalog, AssetRef<WorldDefinition> reference) {
        DefinitionGraphLoader loader = new DefinitionGraphLoader(catalog);
        Optional<AssetMetadata> metadata =
                loader.resolve(reference.id(), AssetKind.WORLD_DEFINITION, catalog.root(), "");
        Optional<WorldDefinition> definition = metadata.flatMap(loader::readWorld);
        if (metadata.isPresent() && definition.isPresent()) {
            loader.validateEntries(
                    definition.orElseThrow().roots(), metadata.orElseThrow().path());
        }
        return loader.result(definition);
    }

    /** Reads one entity definition and appends its source-local diagnostics. */
    private Optional<EntityDefinition> readEntity(AssetMetadata metadata) {
        DefinitionDocumentReader.ReadResult<EntityDefinition> result =
                DefinitionDocumentReader.readEntity(catalog.root(), metadata);
        diagnostics.addAll(result.diagnostics());
        return result.value();
    }

    /** Reads one world definition and appends its source-local diagnostics. */
    private Optional<WorldDefinition> readWorld(AssetMetadata metadata) {
        DefinitionDocumentReader.ReadResult<WorldDefinition> result =
                DefinitionDocumentReader.readWorld(catalog.root(), metadata);
        diagnostics.addAll(result.diagnostics());
        return result.value();
    }

    /** Validates one entity's transitive placement graph with cycle detection and memoization. */
    private void validateEntityGraph(AssetMetadata metadata, EntityDefinition definition) {
        AssetId id = metadata.id();
        if (validatedEntities.contains(id)) {
            return;
        }
        if (!visitingEntities.add(id)) {
            addCycle(metadata.path(), id);
            return;
        }
        loadedEntities.put(id, definition);
        validateEntries(List.of(definition.root()), metadata.path());
        visitingEntities.remove(id);
        validatedEntities.add(id);
    }

    /** Traverses placements in deterministic authored hierarchy order. */
    private void validateEntries(List<? extends EntityEntry> entries, Path source) {
        for (EntityEntry entry : entries) {
            if (entry instanceof EntityPlacement placement) {
                validatePlacement(placement, source);
            } else if (entry instanceof LocalEntity local) {
                validateEntries(local.children(), source);
            }
        }
    }

    /** Resolves and recursively validates one placed reusable entity definition. */
    private void validatePlacement(EntityPlacement placement, Path source) {
        AssetId id = placement.definition().id();
        Optional<AssetMetadata> metadata = resolve(id, AssetKind.ENTITY_DEFINITION, source, "");
        if (metadata.isEmpty()) {
            return;
        }
        if (visitingEntities.contains(id)) {
            addCycle(source, id);
            return;
        }
        if (validatedEntities.contains(id)) {
            return;
        }
        EntityDefinition definition = loadedEntities.get(id);
        if (definition == null) {
            Optional<EntityDefinition> loaded = readEntity(metadata.orElseThrow());
            if (loaded.isEmpty()) {
                return;
            }
            definition = loaded.orElseThrow();
            loadedEntities.put(id, definition);
        }
        validateEntityGraph(metadata.orElseThrow(), definition);
    }

    /** Resolves one stable identity and validates its expected kind. */
    private Optional<AssetMetadata> resolve(AssetId id, AssetKind expectedKind, Path source, String location) {
        Optional<AssetMetadata> metadata = catalog.find(id);
        if (metadata.isEmpty()) {
            addError(source, AssetDiagnosticCode.REFERENCE_MISSING, "asset ID was not found: " + id, location);
            return Optional.empty();
        }
        if (metadata.orElseThrow().kind() != expectedKind) {
            addError(
                    source,
                    AssetDiagnosticCode.REFERENCE_KIND_INVALID,
                    "asset " + id + " has kind " + metadata.orElseThrow().kind() + " but " + expectedKind
                            + " is required",
                    location);
            return Optional.empty();
        }
        return metadata;
    }

    /** Reports one inclusion cycle with its stable identity chain. */
    private void addCycle(Path source, AssetId repeatedId) {
        List<String> chain = visitingEntities.stream().map(AssetId::toString).collect(Collectors.toList());
        chain.add(repeatedId.toString());
        addError(
                source,
                AssetDiagnosticCode.DEFINITION_CYCLE,
                "definition inclusion cycle: " + String.join(" -> ", chain),
                "");
    }

    /** Appends one structured graph diagnostic. */
    private void addError(Path source, AssetDiagnosticCode code, String detail, String location) {
        DiagnosticCollector collector = new DiagnosticCollector(source);
        collector.error(code, detail, location);
        diagnostics.addAll(collector.diagnostics());
    }

    /** Produces the public result, withholding the root definition after any transitive error. */
    private <T> DefinitionLoadResult<T> result(Optional<T> value) {
        boolean hasErrors =
                diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        return new DefinitionLoadResult<>(hasErrors ? Optional.empty() : value, diagnostics);
    }
}
