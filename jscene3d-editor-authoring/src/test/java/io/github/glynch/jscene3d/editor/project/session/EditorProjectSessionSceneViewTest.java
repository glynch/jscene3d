/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies Scene View projection through the authoritative authored working-copy session. */
final class EditorProjectSessionSceneViewTest {
    private static final AssetId SCENE = AssetId.from("11111111-1111-4111-8111-111111111111");
    private static final AssetId MISSING_SCENE = AssetId.from("22222222-2222-4222-8222-222222222222");
    private static final EntityId ENTITY = EntityId.from("33333333-3333-4333-8333-333333333333");
    private static final ComponentId TRANSFORM = ComponentId.from("44444444-4444-4444-8444-444444444444");

    @TempDir
    private Path projectRoot;

    /** Projects unsaved accepted state and rejects stale or unavailable identities without disk reload. */
    @Test
    void projectsCurrentWorkingCopyRevision() throws IOException {
        writeProject();
        byte[] persistedBefore = Files.readAllBytes(sceneSource());
        try (EditorProjectSession session = new EditorProjectLoader(
                        "0.1.0-SNAPSHOT", getClass().getClassLoader())
                .load(projectRoot)
                .session()
                .orElseThrow()) {
            EditorRetainedDefinition retained =
                    session.retainDefinition(SCENE).definition().orElseThrow();
            SceneViewProjectionResult initial = session.projectSceneView(SCENE, 0L);
            InspectorMutationTarget.ComponentProperty position = new InspectorMutationTarget.ComponentProperty(
                    retained.hierarchy().roots().getFirst().occurrence(),
                    ENTITY,
                    TRANSFORM,
                    StandardSpatial3dDescriptors.positionProperty());

            AuthoringMutationResult mutation =
                    session.mutate(SCENE, position, new AuthoringMutation.Set(vector("9", "8", "7")), 0L);
            SceneViewProjectionResult current = session.projectSceneView(SCENE, 1L);
            SceneViewProjectionResult stale = session.projectSceneView(SCENE, 0L);
            SceneViewProjectionResult unavailable = session.projectSceneView(MISSING_SCENE, 0L);

            assertThat(initial.outcome()).isEqualTo(SceneViewProjectionResult.Outcome.PROJECTED);
            assertThat(projectedPosition(initial))
                    .isEqualTo(new SceneViewSnapshot.Vector3(
                            new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3")));
            assertThat(mutation)
                    .returns(AuthoringMutationResult.Outcome.ACCEPTED, AuthoringMutationResult::outcome)
                    .returns(1L, AuthoringMutationResult::revision)
                    .returns(true, AuthoringMutationResult::dirty);
            assertThat(current.outcome()).isEqualTo(SceneViewProjectionResult.Outcome.PROJECTED);
            assertThat(current.currentRevision()).hasValue(1L);
            assertThat(projectedPosition(current))
                    .isEqualTo(new SceneViewSnapshot.Vector3(
                            new BigDecimal("9"), new BigDecimal("8"), new BigDecimal("7")));
            assertThat(stale.outcome()).isEqualTo(SceneViewProjectionResult.Outcome.STALE_REVISION);
            assertThat(stale.currentRevision()).hasValue(1L);
            assertThat(stale.snapshot()).isEmpty();
            assertThat(unavailable.outcome()).isEqualTo(SceneViewProjectionResult.Outcome.SCENE_UNAVAILABLE);
            assertThat(unavailable.currentRevision()).isEmpty();
            assertThat(unavailable.snapshot()).isEmpty();
            assertThat(Files.readAllBytes(sceneSource())).isEqualTo(persistedBefore);
        }
    }

    /** Returns the only projected Transform 3D position in the fixture. */
    private static SceneViewSnapshot.Vector3 projectedPosition(SceneViewProjectionResult result) {
        return result.snapshot()
                .orElseThrow()
                .occurrences()
                .getFirst()
                .transform()
                .orElseThrow()
                .position();
    }

    /** Writes the minimal safe authoring project used by the working-copy test. */
    private void writeProject() throws IOException {
        write("scene-view.j3d", """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.scene-view","name":"Scene View","version":"1.0.0"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.scene-view"},
                  "extensions":[{"id":"example.scene-view","requires":">=1.0.0 <2.0.0"}]
                }
                """);
        write("src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.scene-view",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Scene View",
                  "types":[],
                  "components":[]
                }
                """);
        ComponentDefinition transform = new ComponentDefinition(
                TRANSFORM,
                StandardSpatial3dDescriptors.transformType().id(),
                StandardSpatial3dDescriptors.transformType().version(),
                Map.of(StandardSpatial3dDescriptors.positionProperty(), vector("1", "2", "3")));
        DefinitionWriter.write(
                sceneSource(),
                new SceneDefinition(
                        SCENE,
                        "Working Scene",
                        List.of(new LocalEntity(ENTITY, "Visual", true, List.of(transform), List.of()))));
    }

    /** Writes one UTF-8 project metadata file. */
    private void write(String relativePath, String content) throws IOException {
        Path target = projectRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /** Returns the authored Scene source used to prove no root disk reload occurred. */
    private Path sceneSource() {
        return projectRoot.resolve("scenes/working.scene.json");
    }

    /** Creates one exact three-number array. */
    private static ProjectValue.ArrayValue vector(String x, String y, String z) {
        return new ProjectValue.ArrayValue(List.of(number(x), number(y), number(z)));
    }

    /** Creates one exact number. */
    private static ProjectValue.NumberValue number(String value) {
        return new ProjectValue.NumberValue(new BigDecimal(value));
    }
}
