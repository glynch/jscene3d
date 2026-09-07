/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.game.input.InputWorldModule;
import io.github.glynch.jscene3d.game.input.ProjectInput;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.input.InputActionDefinition;
import io.github.glynch.jscene3d.project.input.InputBinding;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.input.InputValueType;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.physics3d.Physics3dWorldModule;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class StandardProjectEnvironmentTest {
    private static final String MANIFEST = """
            {
              "$schema": "https://jscene3d.org/schemas/project-1.json",
              "schemaVersion": 1,
              "identity": {
                "id": "example.desktop-test",
                "name": "Desktop Test",
                "version": "0.1.0"
              },
              "engine": {
                "requires": ">=0.1.0-SNAPSHOT <0.2.0"
              },
              "runtime": {
                "applicationExtension": "example.desktop-test",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {
                  "id": "example.desktop-test",
                  "requires": ">=0.1.0 <0.2.0"
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void suppliesFreshStandardWorldModulesWithoutAnAuthoredInputMap() {
        StandardProjectEnvironment environment = new StandardProjectEnvironment(temporaryDirectory);

        List<WorldModuleBinding<?>> modules = environment.createWorldModules(Optional.empty());

        assertThat(environment.descriptors()).hasSize(2);
        assertThat(environment.runtimeExtensions()).hasSize(2);
        assertThat(modules)
                .extracting(binding -> binding.type().getName())
                .containsExactly(
                        InputWorldModule.class.getName(),
                        Spatial3dWorldModule.class.getName(),
                        Physics3dWorldModule.class.getName());
        assertThat(modules.getFirst().module())
                .isInstanceOf(InputWorldModule.class)
                .extracting(module -> ((InputWorldModule) module).snapshot())
                .isEqualTo(ActionSnapshot.empty());
        close(modules);
    }

    @Test
    void compilesTheAuthoredInputMapIntoTheWorldInputModule() {
        StandardProjectEnvironment environment = new StandardProjectEnvironment(temporaryDirectory);
        InputMapDefinition inputMap = new InputMapDefinition(
                temporaryDirectory.resolve("input-map.json").toAbsolutePath().normalize(),
                Map.of(
                        "pulse",
                        new InputActionDefinition(
                                InputValueType.BUTTON, List.of(new InputBinding.KeyboardKey("SPACE")))));

        List<WorldModuleBinding<?>> modules = environment.createWorldModules(Optional.of(inputMap));

        assertThat(modules.getFirst().module()).isInstanceOf(ProjectInput.class);
        close(modules);
    }

    @Test
    void combinesAuthoredDefinitionsWithPublishedProjectContent() throws IOException {
        Files.writeString(temporaryDirectory.resolve("project.json"), MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("main.scene.json"), "test", StandardCharsets.UTF_8);
        GameProject project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
        AssetCatalog authored = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        StandardProjectEnvironment environment = new StandardProjectEnvironment(temporaryDirectory.resolve("cache"));
        RegisteredTypeCatalog types = RegisteredTypeCatalog.of(environment.descriptors());

        ProjectContent content = environment.loadContent(project, types, authored);

        assertThat(content.definitions()).isNotNull();
        assertThat(content.resources()).isNotNull();
    }

    /** Closes world modules in ownership-reverse order. */
    private static void close(List<WorldModuleBinding<?>> modules) {
        for (int index = modules.size() - 1; index >= 0; index--) {
            modules.get(index).module().close();
        }
    }
}
