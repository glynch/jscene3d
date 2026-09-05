/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.scene3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.game.GameRuntime;
import io.github.glynch.jscene3d.physics.PhysicsWorld;
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
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises an editor-describable character against declarative static collision. */
final class DeclarativeCharacterPhysicsTest {
    private static final String VERSION = "0.1.0-SNAPSHOT";
    private static final String PROJECT = """
            {
              "schemaVersion": 1,
              "identity": {"id": "character.test", "name": "Character Test", "version": "1.0.0"},
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
                    "id": "environment",
                    "type": "io.github.glynch.jscene3d/static-body-3d",
                    "typeVersion": 1,
                    "children": [
                      {
                        "id": "floor",
                        "type": "io.github.glynch.jscene3d/collision-shape-3d",
                        "typeVersion": 1,
                        "properties": {
                          "shape": {"$ref": "project:resources/floor.shape.json"},
                          "position": [0, -0.5, 0]
                        }
                      },
                      {
                        "id": "wall",
                        "type": "io.github.glynch.jscene3d/collision-shape-3d",
                        "typeVersion": 1,
                        "properties": {
                          "shape": {"$ref": "project:resources/wall.shape.json"},
                          "position": [2.5, 1, 0]
                        }
                      }
                    ]
                  },
                  {
                    "id": "player",
                    "type": "io.github.glynch.jscene3d/character-body-3d",
                    "typeVersion": 1,
                    "properties": {
                      "position": [0, 1, 0],
                      "jump-speed": 0,
                      "maximum-step-height": 0.25
                    },
                    "children": [
                      {
                        "id": "player-shape",
                        "type": "io.github.glynch.jscene3d/collision-shape-3d",
                        "typeVersion": 1,
                        "properties": {"shape": {"$ref": "project:resources/player.shape.json"}}
                      }
                    ]
                  }
                ]
              }
            }
            """;
    private static final String FLOOR_SHAPE = shape(10, 1, 10);
    private static final String WALL_SHAPE = shape(1, 2, 10);
    private static final String PLAYER_SHAPE = """
            {
              "schemaVersion": 1,
              "type": "io.github.glynch.jscene3d/capsule-shape-3d",
              "typeVersion": 1,
              "properties": {"radius": 0.5, "segment-length": 1}
            }
            """;

    @TempDir
    private Path projectDirectory;

    /** Composes, configures, moves, collides, synchronizes, and closes a character body. */
    @Test
    void composesCharacterBody() throws IOException {
        writeProject();
        PhysicsWorld world = new PhysicsWorld();
        JScene3dRuntimeExtension extension = new JScene3dRuntimeExtension(
                (scene, camera) -> {
                    // Headless character composition deliberately has no render target.
                },
                world);
        ProjectRuntime runtime = load(extension);
        CharacterBody3d player =
                (CharacterBody3d) runtime.findNode("player").orElseThrow().object();

        assertThat(world.collisionObjectCount()).isEqualTo(2);
        assertThat(world.colliderCount()).isEqualTo(3);
        assertThat(player.controller().settings().jumpSpeed()).isZero();
        assertThat(player.controller().settings().movementSettings().maximumStepHeight())
                .isEqualTo(0.25F);
        try (GameRuntime gameRuntime = new GameRuntime(runtime)) {
            gameRuntime.start();
            for (int update = 0; update < 120; update++) {
                player.controller().move(new Vector3f(4, 0, 0), 1.0F / 60.0F);
            }
            Vector3f position = player.controller().body().position(new Vector3f());

            assertThat(position.x).isCloseTo(1.499F, within(0.002F));
            assertThat(position.y).isCloseTo(1.0F, within(0.002F));
            assertThat(player.controller().isGrounded()).isTrue();
        }
        assertThat(world.collisionObjectCount()).isZero();
        assertThat(world.colliderCount()).isZero();
    }

    /** Creates the complete declarative project fixture. */
    private void writeProject() throws IOException {
        write("project.json", PROJECT);
        write("application/main.scene.json", SCENE);
        write("resources/floor.shape.json", FLOOR_SHAPE);
        write("resources/wall.shape.json", WALL_SHAPE);
        write("resources/player.shape.json", PLAYER_SHAPE);
    }

    /** Loads one successfully composed runtime. */
    private ProjectRuntime load(JScene3dRuntimeExtension extension) {
        GameProject project =
                new ProjectLoader(VERSION).load(projectDirectory).project().orElseThrow();
        ProjectRuntimeLoadResult result =
                new ProjectRuntimeLoader(VERSION).load(project, getClass().getClassLoader(), List.of(extension));
        assertThat(result.diagnostics()).isEmpty();
        return result.runtime().orElseThrow();
    }

    /** Writes one UTF-8 project file below the temporary project root. */
    private void write(String relativePath, String content) throws IOException {
        Path target = projectDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }

    /** Creates one box collision resource document. */
    private static String shape(int width, int height, int depth) {
        return """
                {
                  "schemaVersion": 1,
                  "type": "io.github.glynch.jscene3d/box-shape-3d",
                  "typeVersion": 1,
                  "properties": {"width": WIDTH, "height": HEIGHT, "depth": DEPTH}
                }
                """.replace("WIDTH", Integer.toString(width))
                .replace("HEIGHT", Integer.toString(height))
                .replace("DEPTH", Integer.toString(depth));
    }
}
