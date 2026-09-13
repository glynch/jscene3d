/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import static io.github.glynch.jscene3d.editor.project.loading.EditorLoadingDiagnostics.error;

import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.diagnostics.EditorDiagnosticCode;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.inspector.EditorInspectorProjector;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionLoadResult;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Validates authored assets and projects them into editor-facing browser entries. */
final class EditorProjectAssetLoader {
    private EditorProjectAssetLoader() {}

    /** Validates definitions and builds deterministic asset-browser entries. */
    static List<ProjectAsset> load(
            GameProject project,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            List<ImportDefinition> imports,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        List<ProjectAsset> assets = new ArrayList<>();
        for (AssetMetadata metadata : authored.assets()) {
            assets.add(loadDefinitionAsset(metadata, project.root(), definitions, types, diagnostics));
        }
        for (GameProject.AssetSource source : project.assets()) {
            assets.add(new ProjectAsset(
                    source.id(),
                    source.id(),
                    ProjectAsset.Kind.SOURCE_ASSET,
                    source.path(),
                    EditorInspectorProjector.sourceAsset(source, project.root())));
        }
        for (ImportDefinition definition : imports) {
            assets.add(new ProjectAsset(
                    definition.id(),
                    definition.id(),
                    ProjectAsset.Kind.IMPORT_DEFINITION,
                    definition.source(),
                    EditorInspectorProjector.importDefinition(definition, project.root())));
        }
        return List.copyOf(assets);
    }

    /** Resolves and validates the manifest's startup world by its stable asset identity. */
    static Optional<WorldDefinition> loadStartupWorld(
            GameProject project,
            AssetCatalog authored,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        Optional<AssetMetadata> metadata = authored.assets().stream()
                .filter(candidate -> candidate.path().equals(project.runtime().entryScene()))
                .filter(candidate -> candidate.kind() == AssetKind.WORLD_DEFINITION)
                .findFirst();
        if (metadata.isEmpty()) {
            diagnostics.add(error(
                    project.runtime().entryScene(),
                    EditorDiagnosticCode.STARTUP_WORLD_MISSING,
                    "runtime.entryScene does not identify an authored world definition"));
            return Optional.empty();
        }
        DefinitionLoadResult<WorldDefinition> result =
                definitions.loadWorld(AssetRef.to(metadata.orElseThrow().id()), types);
        diagnostics.addAll(result.diagnostics());
        return result.definition();
    }

    /** Loads one authored definition and records its complete validation diagnostics. */
    private static ProjectAsset loadDefinitionAsset(
            AssetMetadata metadata,
            Path projectRoot,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        String fallback = metadata.path().getFileName().toString();
        if (metadata.kind() == AssetKind.ENTITY_DEFINITION) {
            DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(metadata.id()), types);
            diagnostics.addAll(result.diagnostics());
            Optional<EntityDefinition> definition = result.definition();
            String label = definition.map(EntityDefinition::name).orElse(fallback);
            EditorSelection selection = definition
                    .map(value -> EditorInspectorProjector.entityDefinition(value, metadata.path(), projectRoot, types))
                    .orElseGet(() -> unavailableAssetSelection(metadata, label, projectRoot));
            return new ProjectAsset(
                    label, metadata.id().toString(), ProjectAsset.Kind.ENTITY_DEFINITION, metadata.path(), selection);
        }
        DefinitionLoadResult<WorldDefinition> result = definitions.loadWorld(AssetRef.to(metadata.id()), types);
        diagnostics.addAll(result.diagnostics());
        Optional<WorldDefinition> definition = result.definition();
        String label = definition.map(WorldDefinition::name).orElse(fallback);
        EditorSelection selection = definition
                .map(value -> EditorInspectorProjector.worldDefinition(value, metadata.path(), projectRoot))
                .orElseGet(() -> unavailableAssetSelection(metadata, label, projectRoot));
        return new ProjectAsset(
                label, metadata.id().toString(), ProjectAsset.Kind.WORLD_DEFINITION, metadata.path(), selection);
    }

    /** Preserves selection metadata when complete definition data is unavailable. */
    private static EditorSelection unavailableAssetSelection(AssetMetadata metadata, String label, Path projectRoot) {
        String source = projectRoot.relativize(metadata.path()).toString();
        EditorDetails view = new EditorDetails(
                label,
                metadata.kind() == AssetKind.ENTITY_DEFINITION ? "Entity definition" : "World definition",
                source,
                metadata.id().toString(),
                List.of(new EditorIcon(EditorIcons.READ_ONLY, "Unavailable definition · read-only")),
                List.of(new EditorDetails.Section(
                        "Definition",
                        Optional.of("Complete definition data is unavailable; see Diagnostics"),
                        false,
                        List.of())));
        String key = EditorSelectionKinds.ASSET.value() + ":" + metadata.path() + ':' + metadata.id();
        return new EditorSelection(EditorSelectionKinds.ASSET, key, view);
    }
}
