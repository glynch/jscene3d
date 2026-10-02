/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.loading;

import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
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
import io.github.glynch.jscene3d.project.manifest.ProjectDiagnosticCode;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        validateMainScene(project, authored, diagnostics);
        for (AssetMetadata metadata : authored.assets()) {
            assets.add(loadDefinitionAsset(metadata, definitions, types, diagnostics));
        }
        for (GameProject.AssetSource source : project.assets()) {
            assets.add(new ProjectAsset(source.id(), source.id(), ProjectAsset.Kind.SOURCE_ASSET, source.path()));
        }
        for (ImportDefinition definition : imports) {
            assets.add(new ProjectAsset(
                    definition.id(), definition.id(), ProjectAsset.Kind.IMPORT_DEFINITION, definition.source()));
        }
        return List.copyOf(assets);
    }

    /** Reports invalid Main Scene identity without preventing the Project from opening for authoring. */
    private static void validateMainScene(
            GameProject project, AssetCatalog authored, LinkedHashSet<ProjectDiagnostic> diagnostics) {
        project.runtime().mainScene().ifPresent(reference -> {
            Optional<AssetMetadata> selected = authored.find(reference.id());
            if (selected.isEmpty()) {
                diagnostics.add(mainSceneDiagnostic(project, ProjectDiagnosticCode.SCENE_REFERENCE_MISSING));
            } else if (selected.orElseThrow().kind() != AssetKind.SCENE_DEFINITION) {
                diagnostics.add(mainSceneDiagnostic(project, ProjectDiagnosticCode.SCENE_REFERENCE_KIND));
            }
        });
    }

    /** Creates one non-terminal Project-level Main Scene diagnostic. */
    private static ProjectDiagnostic mainSceneDiagnostic(GameProject project, ProjectDiagnosticCode code) {
        return new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR, code, project.descriptor().toUri(), "/runtime/mainScene", Map.of());
    }

    /** Loads one authored definition and records its complete validation diagnostics. */
    private static ProjectAsset loadDefinitionAsset(
            AssetMetadata metadata,
            DefinitionResolver definitions,
            RegisteredTypeCatalog types,
            LinkedHashSet<ProjectDiagnostic> diagnostics) {
        String fallback = metadata.path().getFileName().toString();
        if (metadata.kind() == AssetKind.ENTITY_DEFINITION) {
            DefinitionLoadResult<EntityDefinition> result = definitions.loadEntity(AssetRef.to(metadata.id()), types);
            diagnostics.addAll(result.diagnostics());
            Optional<EntityDefinition> definition = result.definition();
            String label = definition.map(EntityDefinition::name).orElse(fallback);
            return new ProjectAsset(
                    label, metadata.id().toString(), ProjectAsset.Kind.ENTITY_DEFINITION, metadata.path());
        }
        DefinitionLoadResult<SceneDefinition> result = definitions.loadScene(AssetRef.to(metadata.id()), types);
        diagnostics.addAll(result.diagnostics());
        Optional<SceneDefinition> definition = result.definition();
        String label = definition.map(SceneDefinition::name).orElse(fallback);
        return new ProjectAsset(label, metadata.id().toString(), ProjectAsset.Kind.SCENE_DEFINITION, metadata.path());
    }
}
