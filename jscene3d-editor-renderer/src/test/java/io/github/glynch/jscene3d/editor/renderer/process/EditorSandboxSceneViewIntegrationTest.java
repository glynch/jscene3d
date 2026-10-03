/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.renderer.process.RendererConfiguration.SceneViewLaunch;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewProjectionResult;
import io.github.glynch.jscene3d.project.asset.AssetId;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Exercises the safe Scene View product boundary with the permanent editor sandbox. */
final class EditorSandboxSceneViewIntegrationTest {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";
    private static final String PROJECT_ID = "io.github.glynch.jscene3d.editor-sandbox";
    private static final AssetId MAIN_SCENE = AssetId.from("05931084-bd10-4f1c-b4d6-4182be5c1a2c");
    private static final Path PROJECT_ROOT = Path.of("..", "jscene3d-project-examples", "src", "main", "editor-sandbox")
            .toAbsolutePath()
            .normalize();

    /** Projects and realizes Main through the same safe project/resource path used by the renderer process. */
    @Test
    void realizesMainSceneThroughProductResourceBoundary() {
        SceneViewLaunch launch = new SceneViewLaunch(
                PROJECT_ROOT, PROJECT_ROOT.resolve("target/import-cache"), ENGINE_VERSION, PROJECT_ID, MAIN_SCENE);

        try (EditorProjectSession project = SceneViewProcessSession.loadProject(launch)) {
            assertThat(project.retainDefinition(MAIN_SCENE).definition()).isPresent();
            SceneViewProjectionResult projection = project.projectSceneView(MAIN_SCENE, 0L);
            assertThat(projection.outcome()).isEqualTo(SceneViewProjectionResult.Outcome.PROJECTED);

            try (SceneViewRendererSession renderer =
                    new SceneViewRendererSession(ProductSceneViewResourceResolver.create(
                            project.project(), project.types(), launch.publishedContentRoot()))) {
                SceneViewRealizationResult result =
                        renderer.replaceSnapshot(projection.snapshot().orElseThrow());

                assertThat(result.status()).isEqualTo(SceneViewRealizationResult.Status.APPLIED);
                assertThat(result.diagnostics()).isEmpty();
            }
        }
    }
}
