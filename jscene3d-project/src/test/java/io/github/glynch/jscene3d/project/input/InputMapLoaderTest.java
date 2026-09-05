/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InputMapLoaderTest {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";

    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsBindingsInAuthoredOrder() throws IOException {
        GameProject project = createProject();
        Path inputMap = write("""
                {
                  "$schema": "../schema/input-map-1.schema.json",
                  "schemaVersion": 1,
                  "actions": {
                    "move-forward": [
                      {"device": "keyboard", "key": "W"},
                      {"device": "keyboard", "key": "UP"}
                    ],
                    "fire": [{"device": "mouse", "button": "LEFT"}]
                  }
                }
                """);

        InputMapLoadResult result = new InputMapLoader().load(project, inputMap);

        assertThat(result.isValid()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        InputMapDefinition definition = result.definition().orElseThrow();
        assertThat(definition.source()).isEqualTo(inputMap.toRealPath());
        assertThat(definition.actions()).containsOnlyKeys("move-forward", "fire");
        assertThat(definition.actions().get("move-forward"))
                .containsExactly(
                        new InputBinding(InputBinding.Device.KEYBOARD, "W"),
                        new InputBinding(InputBinding.Device.KEYBOARD, "UP"));
        assertThat(definition.actions().get("fire"))
                .containsExactly(new InputBinding(InputBinding.Device.MOUSE_BUTTON, "LEFT"));
    }

    @Test
    void reportsStructuralAndBindingErrors() throws IOException {
        GameProject project = createProject();
        Path inputMap = write("""
                {
                  "schemaVersion": 2,
                  "actions": {
                    "Bad Action": [],
                    "fire": [
                      null,
                      {"device": "controller", "key": "A"},
                      {"device": "keyboard", "key": "W", "button": "LEFT"},
                      {"device": "keyboard", "key": "W"},
                      {"device": "keyboard", "key": "W"}
                    ]
                  }
                }
                """);

        InputMapLoadResult result = new InputMapLoader().load(project, inputMap);

        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(ProjectDiagnostic::code)
                .contains(
                        InputMapDiagnosticCode.SCHEMA_UNSUPPORTED,
                        InputMapDiagnosticCode.ACTION_ID_INVALID,
                        InputMapDiagnosticCode.BINDINGS_EMPTY,
                        InputMapDiagnosticCode.BINDING_REQUIRED,
                        InputMapDiagnosticCode.DEVICE_UNSUPPORTED,
                        InputMapDiagnosticCode.CONTROL_CONFLICT,
                        InputMapDiagnosticCode.BINDING_DUPLICATE);
    }

    @Test
    void rejectsMissingAndEscapingPaths() throws IOException {
        GameProject project = createProject();

        InputMapLoadResult missing = new InputMapLoader().load(project, Path.of("application/missing.json"));
        InputMapLoadResult escaping = new InputMapLoader().load(project, temporaryDirectory.resolve("../outside.json"));

        assertThat(missing.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo(InputMapDiagnosticCode.FILE_MISSING);
        assertThat(escaping.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo(InputMapDiagnosticCode.PATH_ESCAPES_PROJECT);
    }

    @Test
    void exposesValueSemantics() throws IOException {
        Path source = temporaryDirectory.toRealPath().resolve("input-map.json");
        InputMapDefinition first = new InputMapDefinition(
                source, Map.of("fire", List.of(new InputBinding(InputBinding.Device.MOUSE_BUTTON, "LEFT"))));
        InputMapDefinition second = new InputMapDefinition(source, first.actions());

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.toString()).contains("input-map.json", "fire", "LEFT");
    }

    /** Creates the minimum validated project needed by the input-map loader. */
    private GameProject createProject() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve("application"));
        Files.writeString(temporaryDirectory.resolve("application/main.scene.json"), "{}");
        Files.createDirectories(temporaryDirectory.resolve("schema"));
        Files.writeString(temporaryDirectory.resolve("schema/input-map-1.schema.json"), "{}");
        Files.writeString(temporaryDirectory.resolve("project.json"), """
                {
                  "schemaVersion": 1,
                  "identity": {"id": "example.input", "name": "Input", "version": "1.0.0"},
                  "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime": {
                    "applicationExtension": "example.input",
                    "entryScene": "application/main.scene.json",
                    "inputMap": "application/input-map.json"
                  },
                  "extensions": [
                    {"id": "example.input", "requires": ">=0.1.0-SNAPSHOT <0.2.0"}
                  ]
                }
                """);
        return new ProjectLoader(ENGINE_VERSION)
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
    }

    /** Writes one authored input-map fixture. */
    private Path write(String content) throws IOException {
        Path path = temporaryDirectory.resolve("application/input-map.json");
        Files.writeString(path, content);
        return path.toRealPath();
    }
}
