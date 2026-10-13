/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectSummary;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.AssetMetadata;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Maps one retained project session to the semantic project protocol contract. */
final class ProjectProtocolMapper {
    ProjectSummary summary(EditorProjectSession session) {
        GameProject project = session.project();
        ProjectSummary.SceneSummary mainScene = project.runtime()
                .mainScene()
                .flatMap(reference -> session.definitions()
                        .loadScene(reference, session.types())
                        .definition())
                .map(scene -> new ProjectSummary.SceneSummary(scene.id().toString(), scene.name()))
                .orElse(null);
        return new ProjectSummary(
                project.identity().id(),
                project.identity().name(),
                project.identity().version(),
                project.root().toString(),
                project.descriptor().toString(),
                mainScene,
                new ProjectSummary.AssetCounts(
                        session.authoredAssets().assets().size(),
                        session.assets().size()),
                catalog(session, mainScene));
    }

    private static ProjectSummary.ProjectCatalog catalog(
            EditorProjectSession session, ProjectSummary.@Nullable SceneSummary mainScene) {
        Map<String, ProjectAsset> projections = session.assets().stream()
                .filter(asset -> asset.kind() == ProjectAsset.Kind.SCENE_DEFINITION
                        || asset.kind() == ProjectAsset.Kind.ENTITY_DEFINITION)
                .collect(Collectors.toUnmodifiableMap(ProjectAsset::identity, asset -> asset));
        List<ProjectSummary.CatalogEntry> scenes = session.authoredAssets().assets().stream()
                .filter(asset -> asset.kind() == AssetKind.SCENE_DEFINITION)
                .map(asset -> entry(
                        asset,
                        projections,
                        mainScene != null && mainScene.id().equals(asset.id().toString())))
                .toList();
        List<ProjectSummary.CatalogEntry> entities = session.authoredAssets().assets().stream()
                .filter(asset -> asset.kind() == AssetKind.ENTITY_DEFINITION)
                .map(asset -> entry(asset, projections, false))
                .toList();
        return new ProjectSummary.ProjectCatalog(scenes, entities);
    }

    private static ProjectSummary.CatalogEntry entry(
            AssetMetadata metadata, Map<String, ProjectAsset> projections, boolean mainScene) {
        ProjectAsset projection =
                Objects.requireNonNull(projections.get(metadata.id().toString()), "definition projection");
        return new ProjectSummary.CatalogEntry(
                metadata.id().toString(),
                projection.label(),
                metadata.path().toUri().toString(),
                "authored",
                true,
                mainScene);
    }
}
