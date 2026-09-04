/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.runtime.internal.DoomRuntimeExtension;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportManager;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.resource.ResourceLoader;
import io.github.glynch.jscene3d.project.runtime.extension.NodeControllerFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactory;
import io.github.glynch.jscene3d.project.runtime.extension.ResourceFactoryContext;
import io.github.glynch.jscene3d.project.runtime.extension.SceneNodeFactory;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Proves imported native Doom-map resources become typed runtime maps. */
final class DoomRuntimeExtensionTest {
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.doom-runtime-test",
                "name": "Doom Runtime Test",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "io.github.glynch.jscene3d.doom",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {"id": "io.github.glynch.jscene3d.wad", "requires": "0.1.0-SNAPSHOT"},
                {"id": "io.github.glynch.jscene3d.doom", "requires": "0.1.0-SNAPSHOT"}
              ],
              "assets": [
                {
                  "id": "content",
                  "type": "io.github.glynch.jscene3d.wad/source",
                  "path": "assets/content.wad"
                }
              ],
              "imports": ["imports/maps.import.json"]
            }
            """;
    private static final String IMPORT_DEFINITION = """
            {
              "schemaVersion": 1,
              "id": "maps",
              "source": "asset:content",
              "importer": "io.github.glynch.jscene3d.doom/maps",
              "selection": ["maps/MAP01"]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject project;
    private RegisteredTypeCatalog catalog;
    private ImportDefinition definition;

    /** Creates one complete portable map import fixture. */
    @BeforeEach
    void createProject() throws IOException {
        Path projectDirectory = Files.createDirectory(temporaryDirectory.resolve("project"));
        Files.createDirectories(projectDirectory.resolve("assets"));
        Files.createDirectories(projectDirectory.resolve("imports"));
        Files.writeString(projectDirectory.resolve("main.scene.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(
                projectDirectory.resolve(ProjectLoader.MANIFEST_NAME), PROJECT_MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(
                projectDirectory.resolve("imports/maps.import.json"), IMPORT_DEFINITION, StandardCharsets.UTF_8);
        TestDoomWadFiles.write(projectDirectory.resolve("assets/content.wad"), TestDoomWadFiles.validMap("MAP01"));
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(projectDirectory)
                .project()
                .orElseThrow();
        catalog = new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                .load(project, getClass().getClassLoader())
                .catalog();
        definition = new ImportLoader()
                .load(project, Path.of("imports/maps.import.json"))
                .definition()
                .orElseThrow();
    }

    /** Reconstructs every map-table family through the registered resource factory. */
    @Test
    void createsTypedMapFromImportedResource() throws IOException {
        ResourceDefinition resource = importResource();
        CapturingRegistry registry = new CapturingRegistry();
        new DoomRuntimeExtension().register(registry);

        Object value = registry.factory().create(new TestResourceContext(project, resource));

        assertThat(registry.type()).isEqualTo(resource.type());
        assertThat(value).isInstanceOf(DoomMap.class);
        DoomMap map = (DoomMap) value;
        assertThat(map.name()).isEqualTo("MAP01");
        assertThat(map.things()).hasSize(1);
        assertThat(map.vertices()).hasSize(2);
        assertThat(map.linedefs()).hasSize(1);
        assertThat(map.sidedefs()).hasSize(1);
        assertThat(map.sectors()).hasSize(1);
        assertThat(map.segs()).hasSize(1);
        assertThat(map.subsectors()).hasSize(1);
        assertThat(map.nodes()).isEmpty();
        assertThat(map.rejectBytes()).containsExactly(0);
        assertThat(map.blockmap().cells()).containsExactly(List.of(0));
    }

    /** Imports and parses the native resource emitted for MAP01. */
    private ResourceDefinition importResource() throws IOException {
        ImportManager manager = ImportManager.create(
                project,
                catalog,
                temporaryDirectory.resolve("cache"),
                getClass().getClassLoader(),
                List.of());
        try (PreparedImport prepared = manager.prepare(definition)) {
            prepared.commit();
        }
        try (ImportedArtifact artifact =
                        manager.openArtifact(definition, "maps/MAP01").orElseThrow();
                InputStream input = artifact.openStream()) {
            return new ResourceLoader()
                    .load(project, URI.create("import:maps/maps/MAP01"), input)
                    .resource()
                    .orElseThrow();
        }
    }

    /** Captures the single resource registration made by the runtime extension. */
    private static final class CapturingRegistry implements ProjectRuntimeRegistry {
        private RegisteredType type;
        private ResourceFactory factory;

        @Override
        public void registerSceneNode(RegisteredType registeredType, SceneNodeFactory sceneNodeFactory) {
            throw new AssertionError("Doom runtime must not register scene nodes");
        }

        @Override
        public void registerNodeController(RegisteredType registeredType, NodeControllerFactory controllerFactory) {
            throw new AssertionError("Doom runtime must not register node controllers");
        }

        @Override
        public void registerResource(RegisteredType registeredType, ResourceFactory resourceFactory) {
            type = registeredType;
            factory = resourceFactory;
        }

        /** Returns the captured resource type. */
        RegisteredType type() {
            return type;
        }

        /** Returns the captured resource factory. */
        ResourceFactory factory() {
            return factory;
        }
    }

    /** Minimal creation context for a resource without nested dependencies. */
    private record TestResourceContext(GameProject project, ResourceDefinition definition)
            implements ResourceFactoryContext {
        @Override
        public Map<String, ProjectValue> properties() {
            return definition.properties();
        }

        @Override
        public <T> T resolveResource(ResourceReference reference, Class<T> valueType) {
            throw new AssertionError("Doom map resources have no nested runtime resources");
        }
    }
}
