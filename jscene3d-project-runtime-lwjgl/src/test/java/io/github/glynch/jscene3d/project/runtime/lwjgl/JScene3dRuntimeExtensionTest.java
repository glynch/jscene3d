/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.game.GameRuntime;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.lights.AmbientLight;
import io.github.glynch.jscene3d.materials.LambertMaterial;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoadResult;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoader;
import io.github.glynch.jscene3d.project.runtime.lwjgl.internal.Scene3dRenderHost;
import io.github.glynch.jscene3d.scenes.Scene;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the built-in descriptor-to-engine composition without creating a native window. */
final class JScene3dRuntimeExtensionTest {
    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final String PROJECT = """
            {
              "schemaVersion": 1,
              "identity": {"id": "test.project", "name": "Test Project", "version": "1.0.0"},
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "io.github.glynch.jscene3d",
                "entryScene": "application/main.scene.json"
              },
              "extensions": [{"id": "io.github.glynch.jscene3d", "requires": "0.1.0-SNAPSHOT"}]
            }
            """;
    private static final String SCENE = """
            {
              "schemaVersion": 1,
              "id": "main",
              "root": {
                "id": "world",
                "type": "io.github.glynch.jscene3d/group-3d",
                "typeVersion": 1,
                "properties": {"background": "#102030"},
                "children": [
                  {
                    "id": "camera",
                    "type": "io.github.glynch.jscene3d/perspective-camera-3d",
                    "typeVersion": 1,
                    "properties": {
                      "position": [4, 3, 6],
                      "target": [0, 0, 0],
                      "field-of-view-degrees": 55
                    }
                  },
                  {
                    "id": "light",
                    "type": "io.github.glynch.jscene3d/ambient-light-3d",
                    "typeVersion": 1,
                    "properties": {"color": "#ffffff", "intensity": 0.9}
                  },
                  {
                    "id": "cube",
                    "type": "io.github.glynch.jscene3d/mesh-instance-3d",
                    "typeVersion": 1,
                    "properties": {
                      "geometry": {"$ref": "project:resources/cube.geometry.json"},
                      "material": {"$ref": "project:resources/cyan.material.json"},
                      "position": [1, 2, 3],
                      "rotation-degrees": [10, 20, 30]
                    }
                  },
                  {
                    "id": "hidden-cube",
                    "enabled": false,
                    "type": "io.github.glynch.jscene3d/mesh-instance-3d",
                    "typeVersion": 1,
                    "properties": {
                      "geometry": {"$ref": "project:resources/cube.geometry.json"},
                      "material": {"$ref": "project:resources/cyan.material.json"}
                    }
                  }
                ]
              }
            }
            """;
    private static final String GEOMETRY = """
            {
              "schemaVersion": 1,
              "type": "io.github.glynch.jscene3d/box-geometry-3d",
              "typeVersion": 1,
              "properties": {"width": 2, "height": 3, "depth": 4}
            }
            """;
    private static final String MATERIAL = """
            {
              "schemaVersion": 1,
              "type": "io.github.glynch.jscene3d/lambert-material-3d",
              "typeVersion": 1,
              "properties": {"color": "#35d0ba"}
            }
            """;

    @TempDir
    private Path projectDirectory;

    private GameProject loadedProject;
    private RecordingRenderHost host;
    private JScene3dRuntimeExtension extension;
    private ProjectRuntime runtime;

    /** Creates a complete project fixture and composes it through the public loader seam. */
    @BeforeEach
    void composeProject() throws IOException {
        write("project.json", PROJECT);
        write("application/main.scene.json", SCENE);
        write("resources/cube.geometry.json", GEOMETRY);
        write("resources/cyan.material.json", MATERIAL);
        loadedProject =
                new ProjectLoader(VERSION).load(projectDirectory).project().orElseThrow();
        host = new RecordingRenderHost();
        extension = new JScene3dRuntimeExtension(host);
        ProjectRuntimeLoadResult result =
                new ProjectRuntimeLoader(VERSION).load(loadedProject, getClass().getClassLoader(), List.of(extension));
        assertThat(result.diagnostics()).isEmpty();
        runtime = result.runtime().orElseThrow();
    }

    /** Composes authored node properties and submits the resulting scene and active camera. */
    @Test
    void composesAndRendersBuiltIn3dTypes() {
        try (GameRuntime gameRuntime = new GameRuntime(runtime)) {
            gameRuntime.start();
            gameRuntime.render();

            Scene scene = host.scene();
            assertThat(scene.background()).isEqualTo(Color.srgb(0x102030));
            assertThat(scene.children())
                    .extracting(Object::getClass)
                    .containsExactly(PerspectiveCamera.class, AmbientLight.class, Mesh.class, Mesh.class);
            PerspectiveCamera camera = host.camera();
            assertThat(camera.position()).isEqualTo(new Vector3f(4.0f, 3.0f, 6.0f));
            assertThat(camera.fieldOfView()).isCloseTo((float) Math.toRadians(55.0), offset(0.0001f));
        }
        assertThat(host.renderCount()).isOne();
    }

    /** Applies mesh properties, shares native resources, and closes those resources once. */
    @Test
    void appliesMeshStateAndOwnsSharedResources() {
        BufferGeometry geometry;
        LambertMaterial material;
        try (GameRuntime gameRuntime = new GameRuntime(runtime)) {
            gameRuntime.start();
            gameRuntime.render();
            Mesh visible = (Mesh) host.scene().children().get(2);
            Mesh hidden = (Mesh) host.scene().children().get(3);
            geometry = visible.geometry();
            material = (LambertMaterial) visible.material();

            assertThat(visible.position()).isEqualTo(new Vector3f(1.0f, 2.0f, 3.0f));
            assertThat(hidden.isVisible()).isFalse();
            assertThat(hidden.geometry()).isSameAs(geometry);
            assertThat(hidden.material()).isSameAs(material);
            assertThat(geometry.vertexCount()).isEqualTo(24);
            assertThat(material.color()).isEqualTo(Color.srgb(0x35d0ba));
        }
        assertThat(geometry.isClosed()).isTrue();
        assertThat(material.isClosed()).isTrue();
    }

    /** Diagnoses accidental reuse of extension state across separate project runtimes. */
    @Test
    void rejectsReuseAcrossProjectRuntimes() {
        ProjectRuntimeLoadResult result =
                new ProjectRuntimeLoader(VERSION).load(loadedProject, getClass().getClassLoader(), List.of(extension));
        runtime.close();

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("runtime.extension.registration");
    }

    /** Writes one UTF-8 project file below the temporary project root. */
    private void write(String relativePath, String content) throws IOException {
        Path target = projectDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /** Captures render submissions made by the root runtime object. */
    private static final class RecordingRenderHost implements Scene3dRenderHost {
        private @Nullable Scene renderedScene;
        private @Nullable PerspectiveCamera renderedCamera;
        private int renderCount;

        @Override
        public void render(Scene scene, PerspectiveCamera camera) {
            renderedScene = scene;
            renderedCamera = camera;
            renderCount++;
        }

        /** Returns the last submitted scene. */
        private Scene scene() {
            return Objects.requireNonNull(renderedScene, "renderedScene");
        }

        /** Returns the last submitted camera. */
        private PerspectiveCamera camera() {
            return Objects.requireNonNull(renderedCamera, "renderedCamera");
        }

        /** Returns the number of render submissions. */
        private int renderCount() {
            return renderCount;
        }
    }
}
