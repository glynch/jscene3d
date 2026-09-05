/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.game.GameRuntime;
import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
import io.github.glynch.jscene3d.physics.StaticBody;
import io.github.glynch.jscene3d.physics.queries.RaycastHit;
import io.github.glynch.jscene3d.physics.shapes.SphereShape;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoadResult;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises editor-describable static collision through the public project-runtime seam. */
final class DeclarativeStaticPhysicsTest {
    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final String PROJECT = """
            {
              "schemaVersion": 1,
              "identity": {"id": "physics.test", "name": "Physics Test", "version": "1.0.0"},
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
                "children": [
                  {
                    "id": "camera",
                    "type": "io.github.glynch.jscene3d/perspective-camera-3d",
                    "typeVersion": 1
                  },
                  {
                    "id": "wall",
                    "type": "io.github.glynch.jscene3d/static-body-3d",
                    "typeVersion": 1,
                    "properties": {"position": [2, 0, 0]},
                    "children": [
                      {
                        "id": "wall-shape",
                        "type": "io.github.glynch.jscene3d/collision-shape-3d",
                        "typeVersion": 1,
                        "properties": {
                          "shape": {"$ref": "project:resources/wall.shape.json"},
                          "position": [0.5, 0, 0],
                          "category-bits": 4,
                          "mask-bits": 8
                        }
                      },
                      {
                        "id": "sphere-shape",
                        "type": "io.github.glynch.jscene3d/collision-shape-3d",
                        "typeVersion": 1,
                        "properties": {
                          "shape": {"$ref": "project:resources/sphere.shape.json"},
                          "position": [0, 10, 0]
                        }
                      },
                      {
                        "id": "capsule-shape",
                        "type": "io.github.glynch.jscene3d/collision-shape-3d",
                        "typeVersion": 1,
                        "properties": {
                          "shape": {"$ref": "project:resources/capsule.shape.json"},
                          "position": [0, -10, 0]
                        }
                      }
                    ]
                  }
                ]
              }
            }
            """;
    private static final String SPHERE_SHAPE = """
            {
              "schemaVersion": 1,
              "type": "io.github.glynch.jscene3d/sphere-shape-3d",
              "typeVersion": 1,
              "properties": {"radius": 0.75}
            }
            """;
    private static final String CAPSULE_SHAPE = """
            {
              "schemaVersion": 1,
              "type": "io.github.glynch.jscene3d/capsule-shape-3d",
              "typeVersion": 1,
              "properties": {"radius": 0.5, "segment-length": 2}
            }
            """;
    private static final String BOX_SHAPE = """
            {
              "schemaVersion": 1,
              "type": "io.github.glynch.jscene3d/box-shape-3d",
              "typeVersion": 1,
              "properties": {"width": 2, "height": 2, "depth": 2}
            }
            """;

    @TempDir
    private Path projectDirectory;

    /** Registers authored body and collider transforms, filters queries, and releases both. */
    @Test
    void composesAndClosesStaticCollision() throws IOException {
        writeProject(SCENE);
        PhysicsWorld world = new PhysicsWorld();
        ProjectRuntime runtime = load(new JScene3dRuntimeExtension(
                (scene, camera) -> {
                    // Headless physics composition deliberately has no render target.
                },
                world));

        assertThat(world.collisionObjectCount()).isOne();
        assertThat(world.colliderCount()).isEqualTo(3);
        try (GameRuntime gameRuntime = new GameRuntime(runtime)) {
            gameRuntime.start();
            RaycastHit hit = world.raycast(new Vector3f(5, 0, 0), new Vector3f(-1, 0, 0), 10)
                    .orElseThrow();
            Collider collider = hit.collider();

            assertThat(hit.distance()).isCloseTo(1.5f, within(0.0001f));
            assertThat(collider.collisionFilter().categoryBits()).isEqualTo(4);
            assertThat(collider.collisionFilter().maskBits()).isEqualTo(8);
            assertThat(world.raycast(new Vector3f(5, 10, 0), new Vector3f(-1, 0, 0), 10)
                            .orElseThrow()
                            .distance())
                    .isCloseTo(2.25f, within(0.0001f));
            assertThat(world.raycast(new Vector3f(5, -10, 0), new Vector3f(-1, 0, 0), 10)
                            .orElseThrow()
                            .distance())
                    .isCloseTo(2.5f, within(0.0001f));
        }
        assertThat(world.collisionObjectCount()).isZero();
        assertThat(world.colliderCount()).isZero();
    }

    /** Leaves caller-owned collision objects registered when the project runtime closes. */
    @Test
    void preservesCollisionObjectsOwnedByTheCaller() throws IOException {
        writeProject(SCENE);
        PhysicsWorld world = new PhysicsWorld();
        StaticBody retained = world.addStaticBody(new Vector3f(-5, 0, 0), new Quaternionf());
        retained.addCollider(new SphereShape(1));
        ProjectRuntime runtime = load(new JScene3dRuntimeExtension(
                (scene, camera) -> {
                    // Headless physics composition deliberately has no render target.
                },
                world));

        assertThat(world.collisionObjectCount()).isEqualTo(2);
        runtime.close();

        assertThat(world.collisionObjectCount()).isOne();
        assertThat(world.colliderCount()).isOne();
        assertThat(retained.isRegistered()).isTrue();
    }

    /** Reports an authored collision shape that is not a direct child of a static body. */
    @Test
    void rejectsCollisionShapeWithoutStaticBodyParent() throws IOException {
        writeProject(SCENE.replace("io.github.glynch.jscene3d/static-body-3d", "io.github.glynch.jscene3d/group-3d"));
        ProjectRuntimeLoadResult result = loadResult(JScene3dRuntimeExtension.headless());

        assertThat(result.runtime()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("runtime.factory.create");
        assertThat(result.diagnostics().getFirst().details().get("technicalDetail"))
                .contains("collision-shape-3d requires a direct static-body-3d parent");
    }

    /** Creates the project files used by one test. */
    private void writeProject(String sceneContent) throws IOException {
        write("project.json", PROJECT);
        write("application/main.scene.json", sceneContent);
        write("resources/wall.shape.json", BOX_SHAPE);
        write("resources/sphere.shape.json", SPHERE_SHAPE);
        write("resources/capsule.shape.json", CAPSULE_SHAPE);
    }

    /** Loads one successfully composed runtime. */
    private ProjectRuntime load(JScene3dRuntimeExtension extension) {
        ProjectRuntimeLoadResult result = loadResult(extension);
        assertThat(result.diagnostics()).isEmpty();
        return result.runtime().orElseThrow();
    }

    /** Loads one project through the public manifest and runtime interfaces. */
    private ProjectRuntimeLoadResult loadResult(JScene3dRuntimeExtension extension) {
        GameProject loadedProject =
                new ProjectLoader(VERSION).load(projectDirectory).project().orElseThrow();
        return new ProjectRuntimeLoader(VERSION).load(loadedProject, getClass().getClassLoader(), List.of(extension));
    }

    /** Writes one UTF-8 project file below the temporary project root. */
    private void write(String relativePath, String content) throws IOException {
        Path target = projectDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
