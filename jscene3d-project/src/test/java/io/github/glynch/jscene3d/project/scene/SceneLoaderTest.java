/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.scene;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises safe scene loading without runtime extension or graphics initialization. */
final class SceneLoaderTest {
    private static final String PROJECT_MANIFEST = """
            {
              "$schema": "schema/project-1.schema.json",
              "schemaVersion": 1,
              "identity": {
                "id": "example.scene-test",
                "name": "Scene Test",
                "version": "1.0.0"
              },
              "engine": {
                "requires": ">=0.1.0-SNAPSHOT <0.2.0"
              },
              "runtime": {
                "applicationExtension": "example.scene-test",
                "entryScene": "scenes/main.scene.json"
              },
              "extensions": [
                {"id": "example.scene-test", "requires": "1.0.0"}
              ],
              "assets": [
                {
                  "id": "source-data",
                  "type": "example.scene-test/source-data",
                  "path": "assets/source.dat"
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject project;

    /** Creates a valid project boundary for every scene test. */
    @BeforeEach
    void createProject() throws IOException {
        createFile("assets/source.dat", "source");
        createFile(ProjectLoader.MANIFEST_NAME, PROJECT_MANIFEST);
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
    }

    /** Loads a typed tree, nested scene instance, controller, values, references, and connection. */
    @Test
    void loadsCompleteSceneDefinition() throws IOException {
        createFile("resources/cube.mesh.json", "{}");
        createFile("scenes/hud.scene.json", "{}");
        writeEntryScene("""
                {
                  "$schema": "../schema/scene-1.schema.json",
                  "schemaVersion": 1,
                  "id": "main",
                  "root": {
                    "id": "root",
                    "name": "Application",
                    "type": "example.scene-test/group-3d",
                    "typeVersion": 1,
                    "properties": {
                      "visible": true,
                      "label": "Root",
                      "count": 3,
                      "nothing": null,
                      "weights": [1, 2.5],
                      "settings": {"speed": 4},
                      "mesh": {"$ref": "project:resources/cube.mesh.json"},
                      "source": {"$ref": "asset:source-data"},
                      "geometry": {"$ref": "import:map01/geometry/main"}
                    },
                    "children": [
                      {
                        "id": "timer",
                        "enabled": false,
                        "type": "example.scene-test/timer",
                        "typeVersion": 1,
                        "controller": {
                          "type": "example.scene-test/pulse-controller",
                          "typeVersion": 2,
                          "properties": {"interval": 0.5}
                        }
                      },
                      {
                        "id": "cube",
                        "type": "example.scene-test/mesh-instance-3d",
                        "typeVersion": 1
                      },
                      {
                        "id": "hud",
                        "instance": "scenes/hud.scene.json",
                        "overrides": {"title": "Scene Test"}
                      }
                    ]
                  },
                  "connections": [
                    {
                      "from": {"node": "timer", "signal": "timeout"},
                      "to": {"node": "cube", "action": "toggle"}
                    }
                  ]
                }
                """);

        SceneLoadResult result = new SceneLoader().loadEntryScene(project);

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.isValid()).isTrue();
        SceneDefinition scene = result.scene().orElseThrow();
        assertThat(scene.id()).isEqualTo("main");
        assertThat(scene.source()).isEqualTo(entryScene().toRealPath());
        assertRootValues(scene.root());
        assertChildren(scene.root().children());
        assertThat(scene.connections())
                .containsExactly(new SceneConnection(
                        new SceneConnection.SignalEndpoint("timer", "timeout"),
                        new SceneConnection.ActionEndpoint("cube", "toggle")));
    }

    /** Rejects unknown fields using the same strict JSON policy as project manifests. */
    @Test
    void rejectsUnknownSceneField() throws IOException {
        writeEntryScene("""
                {
                  "schemaVersion": 1,
                  "id": "main",
                  "mystery": true,
                  "root": {
                    "id": "root",
                    "type": "example.scene-test/group-3d",
                    "typeVersion": 1
                  }
                }
                """);

        assertSingleError("scene.json");
    }

    /** Reports tree, source, reference, and connection errors together. */
    @Test
    void collectsSemanticSceneErrors() throws IOException {
        writeEntryScene("""
                {
                  "$schema": "wrong.json",
                  "schemaVersion": 2,
                  "id": "Bad Scene",
                  "root": {
                    "id": "duplicate",
                    "type": "bad-type",
                    "typeVersion": 0,
                    "properties": {
                      "badReference": {"$ref": "asset:missing"},
                      "badObject": {"$ref": 42, "other": true}
                    },
                    "children": [
                      {
                        "id": "duplicate",
                        "instance": "../outside.scene.json"
                      }
                    ]
                  },
                  "connections": [
                    {
                      "from": {"node": "missing", "signal": "Bad Signal"},
                      "to": {"node": "duplicate", "action": "toggle"}
                    }
                  ]
                }
                """);

        SceneLoadResult result = new SceneLoader().loadEntryScene(project);

        assertThat(result.isValid()).isFalse();
        assertThat(result.scene()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains(
                        "scene.schema.unsupported",
                        "scene.schema.uri",
                        "scene.field.identifier",
                        "scene.type.identifier",
                        "scene.type.version",
                        "scene.reference.asset.missing",
                        "scene.reference.object",
                        "scene.node.duplicate",
                        "scene.path.escape",
                        "scene.connection.node");
    }

    /** Rejects a node that ambiguously declares both a registered type and an instance. */
    @Test
    void rejectsAmbiguousNodeSource() throws IOException {
        writeEntryScene("""
                {
                  "schemaVersion": 1,
                  "id": "main",
                  "root": {
                    "id": "root",
                    "type": "example.scene-test/group-3d",
                    "typeVersion": 1,
                    "instance": "scenes/other.scene.json"
                  }
                }
                """);

        SceneLoadResult result = new SceneLoader().loadEntryScene(project);

        assertThat(result.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("scene.node.source");
    }

    /** Returns a terminal diagnostic when the entry scene does not exist. */
    @Test
    void reportsMissingEntryScene() {
        SceneLoadResult result = new SceneLoader().loadEntryScene(project);

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo("scene.file.missing");
    }

    /** Publishes the Scene version-one schema for editors and validation tools. */
    @Test
    void bundlesVersionOneSceneSchema() throws IOException {
        String resource = "/META-INF/jscene3d/project/scene-1.schema.json";

        try (var input = getClass().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("\"$id\": \"https://jscene3d.org/schemas/scene-1.json\"")
                    .contains("\"connections\"")
                    .contains("\"controller\"");
        }
    }

    /** Verifies the complete root property value family. */
    private static void assertRootValues(SceneNodeDefinition root) {
        assertThat(root.id()).isEqualTo("root");
        assertThat(root.name()).contains("Application");
        SceneNodeDefinition.TypedNode typed = (SceneNodeDefinition.TypedNode) root.source();
        assertThat(typed.type()).isEqualTo(new RegisteredType("example.scene-test/group-3d", 1));
        assertThat(typed.properties())
                .containsEntry("visible", new ProjectValue.BooleanValue(true))
                .containsEntry("label", new ProjectValue.TextValue("Root"))
                .containsEntry("count", new ProjectValue.NumberValue(new BigDecimal("3")))
                .containsEntry("nothing", ProjectValue.NullValue.INSTANCE);
        assertThat(typed.properties().get("mesh")).isInstanceOfSatisfying(ProjectValue.ReferenceValue.class, mesh -> {
            assertThat(mesh.reference().kind()).isEqualTo(ResourceReference.Kind.PROJECT);
            assertThat(mesh.reference().locator()).isEqualTo("resources/cube.mesh.json");
            assertThat(mesh.reference().projectPath()).isPresent();
        });
        assertThat(typed.properties().get("source"))
                .isEqualTo(new ProjectValue.ReferenceValue(ResourceReference.asset("source-data")));
        assertThat(typed.properties().get("geometry"))
                .isEqualTo(new ProjectValue.ReferenceValue(ResourceReference.imported("map01/geometry/main")));
    }

    /** Verifies typed children, a controller, and a nested scene instance. */
    private static void assertChildren(List<SceneNodeDefinition> children) {
        assertThat(children).extracting(SceneNodeDefinition::id).containsExactly("timer", "cube", "hud");
        SceneNodeDefinition timer = children.get(0);
        assertThat(timer.enabled()).isFalse();
        assertThat(timer.controller()).get().satisfies(controller -> {
            assertThat(controller.type()).isEqualTo(new RegisteredType("example.scene-test/pulse-controller", 2));
            assertThat(controller.properties())
                    .containsEntry("interval", new ProjectValue.NumberValue(new BigDecimal("0.5")));
        });
        SceneNodeDefinition.SceneInstance hud =
                (SceneNodeDefinition.SceneInstance) children.get(2).source();
        assertThat(hud.scene().getFileName()).hasToString("hud.scene.json");
        assertThat(hud.overrides()).containsEntry("title", new ProjectValue.TextValue("Scene Test"));
    }

    /** Writes the project entry scene. */
    private void writeEntryScene(String content) throws IOException {
        createFile("scenes/main.scene.json", content);
    }

    /** Returns the declared entry-scene path. */
    private Path entryScene() {
        return temporaryDirectory.resolve("scenes/main.scene.json");
    }

    /** Creates one test file and any parent directories. */
    private void createFile(String relativePath, String content) throws IOException {
        Path file = temporaryDirectory.resolve(relativePath);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, content);
    }

    /** Verifies one terminal scene loader diagnostic. */
    private void assertSingleError(String code) {
        SceneLoadResult result = new SceneLoader().loadEntryScene(project);
        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(diagnostic -> diagnostic.code().code())
                .isEqualTo(code);
    }
}
