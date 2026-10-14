/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewReadResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.SceneViewSnapshotDto;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Maps safe Scene View projections and launch metadata to the renderer protocol contract. */
final class SceneViewProtocolMapper {
    private final String engineVersion;

    SceneViewProtocolMapper(String engineVersion) {
        this.engineVersion = Objects.requireNonNull(engineVersion, "engineVersion");
    }

    SceneViewReadResult.LaunchSpecification launch(EditorProjectSession session, AssetId scene) {
        GameProject project = session.project();
        Path publishedContentRoot = EditorProjectLoader.resolvePublishedContentRoot(project.root());
        String sceneName = session.assets().stream()
                .filter(asset -> scene.toString().equals(asset.identity()))
                .map(ProjectAsset::label)
                .findFirst()
                .orElse(scene.toString());
        return new SceneViewReadResult.LaunchSpecification(
                project.identity().id(),
                project.identity().name(),
                project.root().toString(),
                publishedContentRoot.toString(),
                engineVersion,
                sceneName);
    }

    SceneViewSnapshotDto snapshot(SceneViewSnapshot snapshot) {
        return new SceneViewSnapshotDto(
                snapshot.scene().toString(),
                snapshot.revision(),
                snapshot.occurrences().stream().map(this::occurrence).toList());
    }

    private SceneViewSnapshotDto.VisualOccurrence occurrence(SceneViewSnapshot.VisualOccurrence occurrence) {
        return new SceneViewSnapshotDto.VisualOccurrence(
                occurrenceId(occurrence.occurrence()),
                occurrence.parent().map(this::occurrenceId).orElse(null),
                occurrence.authoredAsset().toString(),
                occurrence.authoredSource().toString(),
                occurrence.authoredEntity().toString(),
                occurrence.name().orElse(null),
                occurrence.enabled(),
                occurrence.transform().map(this::transform).orElse(null),
                occurrence.meshes().stream().map(this::mesh).toList(),
                occurrence.directionalLight().map(this::directionalLight).orElse(null),
                occurrence.cameras().stream().map(this::camera).toList());
    }

    private SceneViewSnapshotDto.Occurrence occurrenceId(CompositionOccurrenceId occurrence) {
        return new SceneViewSnapshotDto.Occurrence(
                occurrence.rootDefinition().toString(),
                occurrence.entityPath().stream().map(Object::toString).toList());
    }

    private SceneViewSnapshotDto.ComponentIdentity componentIdentity(SceneViewSnapshot.ComponentIdentity identity) {
        return new SceneViewSnapshotDto.ComponentIdentity(
                occurrenceId(identity.occurrence()),
                new SceneViewSnapshotDto.Scope(
                        identity.scope().definition().toString(),
                        occurrenceId(identity.scope().anchor())),
                identity.authoredEntity().toString(),
                identity.component().toString());
    }

    private static SceneViewSnapshotDto.Vector3 vector(SceneViewSnapshot.Vector3 vector) {
        return new SceneViewSnapshotDto.Vector3(
                vector.x().toPlainString(),
                vector.y().toPlainString(),
                vector.z().toPlainString());
    }

    private SceneViewSnapshotDto.Transform3d transform(SceneViewSnapshot.Transform3d transform) {
        return new SceneViewSnapshotDto.Transform3d(
                componentIdentity(transform.identity()),
                vector(transform.position()),
                vector(transform.orientationDegrees()),
                vector(transform.scale()));
    }

    private static SceneViewSnapshotDto.ResourceReference resource(ResourceReference reference) {
        return new SceneViewSnapshotDto.ResourceReference(
                reference.kind().name().toLowerCase(Locale.ROOT),
                reference.locator(),
                reference.projectPath().map(Path::toString).orElse(null));
    }

    private SceneViewSnapshotDto.MeshRenderer3d mesh(SceneViewSnapshot.MeshRenderer3d mesh) {
        return new SceneViewSnapshotDto.MeshRenderer3d(
                componentIdentity(mesh.identity()), resource(mesh.mesh()), resource(mesh.material()), mesh.visible());
    }

    private SceneViewSnapshotDto.DirectionalLight3d directionalLight(SceneViewSnapshot.DirectionalLight3d light) {
        return new SceneViewSnapshotDto.DirectionalLight3d(
                componentIdentity(light.identity()),
                vector(light.color()),
                light.intensity().toPlainString(),
                vector(light.target()));
    }

    private SceneViewSnapshotDto.Camera3d camera(SceneViewSnapshot.Camera3d camera) {
        return switch (camera) {
            case SceneViewSnapshot.PerspectiveCamera3d perspective ->
                new SceneViewSnapshotDto.PerspectiveCamera3d(
                        "perspective",
                        componentIdentity(perspective.identity()),
                        perspective.fieldOfViewDegrees().toPlainString(),
                        perspective.near().toPlainString(),
                        perspective.far().toPlainString(),
                        perspective.primary());
            case SceneViewSnapshot.OrthographicCamera3d orthographic -> {
                SceneViewSnapshot.OrthographicProjection projection = orthographic.projection();
                yield new SceneViewSnapshotDto.OrthographicCamera3d(
                        "orthographic",
                        componentIdentity(orthographic.identity()),
                        projection.left().toPlainString(),
                        projection.right().toPlainString(),
                        projection.top().toPlainString(),
                        projection.bottom().toPlainString(),
                        projection.near().toPlainString(),
                        projection.far().toPlainString(),
                        projection.zoom().toPlainString(),
                        orthographic.primary());
            }
        };
    }
}
