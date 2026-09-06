/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportArtifactKind;
import io.github.glynch.jscene3d.project.importing.ImportManager;
import io.github.glynch.jscene3d.project.importing.ImportedDefinitionResolver;
import io.github.glynch.jscene3d.project.importing.ImportedRuntimeResources;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.runtime.WorldComposer;
import io.github.glynch.jscene3d.project.runtime.WorldModuleBinding;
import io.github.glynch.jscene3d.project.spatial3d.Material3dResource;
import io.github.glynch.jscene3d.project.spatial3d.Mesh3dResource;
import io.github.glynch.jscene3d.project.spatial3d.MeshRenderer3d;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dAdapters;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dDescriptors;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dResourceLoaders;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dRuntimeExtension;
import io.github.glynch.jscene3d.project.spatial3d.Spatial3dWorldModule;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Proves static glTF publication through the complete project-native runtime path. */
final class GltfProjectImportTest {
    private static final AssetId WORLD_ID = AssetId.from("cfe20599-8c10-4c5e-bccf-d2f281f664f4");
    private static final EntityId PLACEMENT_ID = EntityId.from("c02a20cb-59e2-4ac9-bb85-440967b8aecc");
    private static final String MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.gltf-project-test",
                "name": "glTF Project Test",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "io.github.glynch.jscene3d.gltf",
                "entryScene": "main.world.json"
              },
              "extensions": [
                {"id": "io.github.glynch.jscene3d.gltf", "requires": "0.1.0-SNAPSHOT"}
              ],
              "assets": [
                {
                  "id": "model",
                  "type": "io.github.glynch.jscene3d.gltf/source",
                  "path": "assets/model.glb"
                }
              ],
              "imports": ["imports/model.import.json"]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject project;
    private RegisteredTypeCatalog importTypes;
    private ImportDefinition definition;

    /** Creates one project containing a minimal embedded-buffer GLB. */
    @BeforeEach
    void createProject() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve("assets"));
        Files.createDirectories(temporaryDirectory.resolve("imports"));
        Path generated = GltfTestAssets.writeGlbTriangle(temporaryDirectory.resolve("assets"));
        Files.move(generated, temporaryDirectory.resolve("assets/model.glb"));
        Files.writeString(temporaryDirectory.resolve(ProjectLoader.MANIFEST_NAME), MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(temporaryDirectory.resolve("imports/model.import.json"), """
                {
                  "schemaVersion": 1,
                  "id": "model-import",
                  "source": "asset:model",
                  "importer": "io.github.glynch.jscene3d.gltf/scene",
                  "selection": ["scenes/0000"]
                }
                """, StandardCharsets.UTF_8);
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
        ExtensionCatalogLoadResult extensions = new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                .load(project, getClass().getClassLoader());
        assertThat(extensions.diagnostics()).isEmpty();
        importTypes = extensions.catalog();
        definition = new ImportLoader()
                .load(project, Path.of("imports/model.import.json"))
                .definition()
                .orElseThrow();
    }

    /** Imports, resolves, instantiates, and releases one project-native mesh entity. */
    @Test
    void composesPublishedStaticScene() throws IOException {
        ImportManager manager = manager();
        assertThat(manager.inspect("model", GltfProjectImportExtension.IMPORTER_IDENTIFIER)
                        .items())
                .extracting(SourceItem::identity)
                .containsExactly("scenes/0000");
        try (PreparedImport prepared = manager.prepare(definition)) {
            assertThat(prepared.preview().diagnostics()).isEmpty();
            assertThat(prepared.preview().artifacts())
                    .extracting(metadata -> metadata.descriptor().kind())
                    .containsExactly(
                            ImportArtifactKind.PAYLOAD,
                            ImportArtifactKind.RESOURCE,
                            ImportArtifactKind.RESOURCE,
                            ImportArtifactKind.ENTITY_DEFINITION);
            prepared.commit();
        }
        AssetId generatedId = manager.artifacts(definition).stream()
                .filter(metadata -> metadata.descriptor().kind() == ImportArtifactKind.ENTITY_DEFINITION)
                .findFirst()
                .orElseThrow()
                .descriptor()
                .assetId()
                .orElseThrow();
        DefinitionWriter.write(
                temporaryDirectory.resolve("main.world.json"),
                new WorldDefinition(
                        WORLD_ID,
                        "Imported model",
                        List.of(new EntityPlacement(PLACEMENT_ID, true, AssetRef.to(generatedId), Map.of()))));
        AssetCatalog authored = AssetCatalog.scan(temporaryDirectory).catalog().orElseThrow();
        DefinitionResolver definitions = ImportedDefinitionResolver.create(authored, List.of(definition), manager);
        RegisteredTypeCatalog runtimeTypes =
                RegisteredTypeCatalog.of(List.of(Spatial3dDescriptors.extensionDescriptor()));
        EntityDefinition generated = definitions
                .loadEntity(AssetRef.to(generatedId), runtimeTypes)
                .definition()
                .orElseThrow();
        LocalEntity sourceNode = (LocalEntity) generated.root().children().getFirst();
        ComponentDefinition rendererDefinition = sourceNode.components().stream()
                .filter(component -> component
                        .type()
                        .equals(Spatial3dDescriptors.meshRendererType().id()))
                .findFirst()
                .orElseThrow();
        Spatial3dWorldModule spatial = Spatial3dAdapters.standard();

        World world = WorldComposer.compose(
                        definitions,
                        AssetRef.to(WORLD_ID),
                        runtimeTypes,
                        List.of(new Spatial3dRuntimeExtension()),
                        List.of(WorldModuleBinding.of(Spatial3dWorldModule.class, spatial)),
                        ImportedRuntimeResources.create(
                                project, runtimeTypes, List.of(definition), manager, Spatial3dResourceLoaders.all()))
                .world()
                .orElseThrow();
        Entity runtimeNode = world.roots().getFirst().children().getFirst();
        MeshRenderer3d renderer = runtimeNode
                .component(rendererDefinition.id(), MeshRenderer3d.class)
                .orElseThrow();
        Mesh3dResource mesh = renderer.mesh();
        Material3dResource material = renderer.material();

        assertThat(mesh.isClosed()).isFalse();
        assertThat(material.isClosed()).isFalse();
        world.close();
        assertThat(renderer.isClosed()).isTrue();
        assertThat(mesh.isClosed()).isTrue();
        assertThat(material.isClosed()).isTrue();
    }

    /** Keeps unsupported dynamic and textured source features explicit in the initial profile. */
    @Test
    void rejectsUnsupportedProjectImportFeatures() throws IOException {
        Path animated = GltfTestAssets.writeAnimatedTriangle(temporaryDirectory);
        assertThatThrownBy(() -> GltfConverter.loadProject(animated, 0))
                .isInstanceOf(GltfLoadException.class)
                .hasMessageContaining("animations in project imports");

        Path textured = GltfTestAssets.writeTexturedTriangle(temporaryDirectory);
        assertThat(GltfConverter.inspectProject(textured).dependencies())
                .contains(temporaryDirectory.resolve("triangle.bin"), temporaryDirectory.resolve("pixel.png"));
        assertThatThrownBy(() -> GltfConverter.loadProject(textured, 1))
                .isInstanceOf(GltfLoadException.class)
                .hasMessageContaining("textures in project imports");
    }

    /** Creates one manager through service-discovered glTF registration. */
    private ImportManager manager() {
        return ImportManager.create(
                project,
                importTypes,
                temporaryDirectory.resolve("cache"),
                getClass().getClassLoader(),
                List.of());
    }
}
