/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.renderer.process.RendererConfiguration.ProjectLaunch;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.desktop.StandardProjectEnvironment;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.input.InputMapDefinition;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.runtime.HostedProject;
import io.github.glynch.jscene3d.project.runtime.ProjectContent;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeEnvironment;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ProjectRendererSessionTest {
    private static final String PROJECT_ID = "io.github.glynch.renderer-test";
    private static final String WORLD_ID = "2f26576c-570d-4338-bc30-52bc41def3a5";
    private static final String PROJECT_MANIFEST = """
            {
              "$schema": "https://jscene3d.org/schemas/project-1.json",
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.renderer-test",
                "name": "Renderer Test",
                "version": "0.1.0"
              },
              "engine": {
                "requires": ">=0.1.0-SNAPSHOT <0.2.0",
                "authoredWith": "0.1.0-SNAPSHOT"
              },
              "runtime": {
                "applicationExtension": "io.github.glynch.renderer-test",
                "entryScene": "worlds/main.world.json",
                "startupScene": "worlds/intro.world.json"
              },
              "extensions": [
                {
                  "id": "io.github.glynch.renderer-test",
                  "requires": ">=0.1.0 <0.2.0"
                }
              ]
            }
            """;
    private static final String OPTIONAL_STARTUP_WORLD_DEFINITION = """
            {
              "$schema": "https://jscene3d.org/schemas/world-definition-1.json",
              "assetId": "ee595fcf-3e5b-453a-9f02-6160d85e98a9",
              "assetType": "world-definition",
              "formatVersion": 1,
              "name": "Optional Startup World",
              "connections": [],
              "roots": []
            }
            """;
    private static final String WORLD_DEFINITION = """
            {
              "$schema": "https://jscene3d.org/schemas/world-definition-1.json",
              "assetId": "2f26576c-570d-4338-bc30-52bc41def3a5",
              "assetType": "world-definition",
              "formatVersion": 1,
              "name": "Renderer Test World",
              "connections": [],
              "roots": []
            }
            """;
    private static final String EXTENSION_DESCRIPTOR = """
            {
              "$schema": "https://jscene3d.org/schemas/extension-1.json",
              "schemaVersion": 1,
              "id": "io.github.glynch.renderer-test",
              "version": "0.1.0",
              "engineRequires": ">=0.1.0-SNAPSHOT <0.2.0",
              "displayName": "Renderer Test",
              "types": [],
              "components": []
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsPreparedEntryWorldWhenManifestHasDifferentOptionalStartupScene() throws IOException {
        Path projectRoot = writeProject();
        Path publishedContent = temporaryDirectory.resolve("published");
        ProjectLaunch launch =
                new ProjectLaunch(projectRoot, publishedContent, "0.1.0-SNAPSHOT", PROJECT_ID, AssetId.from(WORLD_ID));

        try (URLClassLoader runtimeLoader = runtimeClassLoader()) {
            HostedProject project = ProjectRendererSession.loadProject(
                    launch, runtimeLoader, new TestProjectEnvironment(publishedContent));

            assertThat(project.project().identity().id()).isEqualTo(PROJECT_ID);
            assertThat(project.world().definition().id()).isEqualTo(AssetId.from(WORLD_ID));
            assertThat(project.world().isActive()).isFalse();

            project.world().activate();
            assertThat(project.world().isActive()).isTrue();

            project.close();
            project.close();
            assertThat(project.world().isClosed()).isTrue();
        }
    }

    @Test
    void propagatesPreparedIdentityMismatchInsteadOfRenderingFallbackContent() throws IOException {
        Path projectRoot = writeProject();
        Path publishedContent = temporaryDirectory.resolve("published");
        ProjectLaunch projectMismatch = new ProjectLaunch(
                projectRoot, publishedContent, "0.1.0-SNAPSHOT", "example.other-project", AssetId.from(WORLD_ID));
        ProjectLaunch worldMismatch = new ProjectLaunch(
                projectRoot,
                publishedContent,
                "0.1.0-SNAPSHOT",
                PROJECT_ID,
                AssetId.from("8a0187d4-87cd-4b86-9fd7-f8fedbfc3153"));
        TestProjectEnvironment environment = new TestProjectEnvironment(publishedContent);
        try (URLClassLoader runtimeLoader = runtimeClassLoader()) {
            assertThatThrownBy(() -> ProjectRendererSession.loadProject(projectMismatch, runtimeLoader, environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("project identity");
        }
        try (URLClassLoader runtimeLoader = runtimeClassLoader()) {
            assertThatThrownBy(() -> ProjectRendererSession.loadProject(worldMismatch, runtimeLoader, environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("startup world");
        }
    }

    private Path writeProject() throws IOException {
        Path projectRoot = temporaryDirectory.resolve("project");
        Files.createDirectories(projectRoot.resolve("worlds"));
        Files.writeString(projectRoot.resolve("renderer-test.j3d"), PROJECT_MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("worlds/main.world.json"), WORLD_DEFINITION, StandardCharsets.UTF_8);
        Files.writeString(
                projectRoot.resolve("worlds/intro.world.json"),
                OPTIONAL_STARTUP_WORLD_DEFINITION,
                StandardCharsets.UTF_8);
        return projectRoot;
    }

    private URLClassLoader runtimeClassLoader() throws IOException {
        Path classPath = temporaryDirectory.resolve("runtime-classpath");
        Path metadata = classPath.resolve("META-INF/jscene3d");
        Files.createDirectories(metadata);
        Files.writeString(metadata.resolve("extension.json"), EXTENSION_DESCRIPTOR, StandardCharsets.UTF_8);
        return new URLClassLoader(
                new URL[] {classPath.toUri().toURL()}, getClass().getClassLoader());
    }

    /** Runtime-artifact provider used to prove the renderer's isolated extension discovery path. */
    public static final class TestRuntimeExtension implements ComponentRuntimeExtension {
        @Override
        public String id() {
            return PROJECT_ID;
        }

        @Override
        public void register(ComponentFactoryRegistry registry) {
            // This minimal project has no extension-owned components.
        }
    }

    /** Standard project environment plus the fixture's isolated runtime extension. */
    private static final class TestProjectEnvironment implements ProjectRuntimeEnvironment {
        private final StandardProjectEnvironment delegate;

        private TestProjectEnvironment(Path publishedContent) {
            delegate = new StandardProjectEnvironment(publishedContent);
        }

        @Override
        public List<ExtensionDescriptor> descriptors() {
            return delegate.descriptors();
        }

        @Override
        public List<ComponentRuntimeExtension> runtimeExtensions() {
            List<ComponentRuntimeExtension> extensions = new ArrayList<>(delegate.runtimeExtensions());
            extensions.add(new TestRuntimeExtension());
            return List.copyOf(extensions);
        }

        @Override
        public List<WorldModuleBinding<?>> createWorldModules(Optional<InputMapDefinition> inputMap) {
            return delegate.createWorldModules(inputMap);
        }

        @Override
        public ProjectContent loadContent(GameProject project, RegisteredTypeCatalog types, AssetCatalog authored) {
            return delegate.loadContent(project, types, authored);
        }
    }
}
