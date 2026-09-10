/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnosticCode;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoadResult;
import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.ImportManager;
import io.github.glynch.jscene3d.project.importing.ImportedArtifact;
import io.github.glynch.jscene3d.project.importing.ImportedArtifactMetadata;
import io.github.glynch.jscene3d.project.importing.PreparedImport;
import io.github.glynch.jscene3d.project.importing.SourceInspection;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.imports.ImportLoader;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the complete service-discovered Doom project-import integration. */
final class DoomImportExtensionTest {
    private static final String IMPORTER_IDENTIFIER = "io.github.glynch.jscene3d.doom/maps";
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.doom-import-test",
                "name": "Doom Import Test",
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

    private Path projectDirectory;
    private Path wadPath;
    private GameProject project;
    private RegisteredTypeCatalog catalog;

    /** Creates one project containing two classic map markers. */
    @BeforeEach
    void createProject() throws IOException {
        projectDirectory = Files.createDirectory(temporaryDirectory.resolve("project"));
        Files.createDirectories(projectDirectory.resolve("assets"));
        Files.createDirectories(projectDirectory.resolve("imports"));
        Files.writeString(projectDirectory.resolve("main.scene.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(
                projectDirectory.resolve(ProjectLoader.MANIFEST_NAME), PROJECT_MANIFEST, StandardCharsets.UTF_8);
        Files.writeString(
                projectDirectory.resolve("imports/maps.import.json"), IMPORT_DEFINITION, StandardCharsets.UTF_8);
        wadPath = projectDirectory.resolve("assets/content.wad");
        List<TestDoomWadFiles.LumpContent> lumps = TestDoomWadFiles.doorMap("MAP01");
        lumps.addAll(TestDoomWadFiles.validMap("E1M1"));
        TestDoomWadFiles.write(wadPath, TestDoomWadFiles.withMinimalMaterials(lumps));
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(projectDirectory)
                .project()
                .orElseThrow();
        ExtensionCatalogLoadResult result = new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                .load(project, getClass().getClassLoader());
        if (!result.diagnostics().isEmpty()) {
            throw new IllegalStateException("test extension descriptors are invalid: " + result.diagnostics());
        }
        catalog = result.catalog();
    }

    /** Exposes selectable maps with stable identities and source metadata. */
    @Test
    void inspectsDiscoveredMaps() {
        SourceInspection inspection = manager().inspect("content", IMPORTER_IDENTIFIER);

        assertThat(inspection.isValid()).isTrue();
        assertThat(inspection.items()).extracting(SourceItem::identity).containsExactly("maps/MAP01", "maps/E1M1");
        assertThat(inspection.items().getFirst().isSelectable()).isTrue();
        assertThat(inspection.items().getFirst().properties())
                .containsEntry("map", new ProjectValue.TextValue("MAP01"));
    }

    /** Publishes one selected map as a closed graph of generic project artifacts. */
    @Test
    void publishesSelectedMapDefinitionAndResources() throws IOException {
        ImportManager manager = manager();
        ImportDefinition definition = definition();

        try (PreparedImport prepared = manager.prepare(definition)) {
            assertThat(prepared.preview().diagnostics()).isEmpty();
            assertThat(prepared.preview().isValid()).isTrue();
            assertThat(prepared.preview().artifacts())
                    .extracting(ImportedArtifactMetadata::identity)
                    .contains(
                            "maps/MAP01/definition",
                            "maps/MAP01/resources/collision/static",
                            "maps/MAP01/payloads/collision/static.mesh",
                            "maps/MAP01/resources/collision/doors/00001",
                            "maps/MAP01/resources/collision/door-obstruction/00001");
            prepared.commit();
        }

        String entityDefinition =
                new String(read(manager, definition, "maps/MAP01/definition"), StandardCharsets.UTF_8);
        assertThat(entityDefinition)
                .startsWith("{\n  \"$schema\" : \"https://jscene3d.org/schemas/entity-definition-1.json\",")
                .contains(
                        "\"name\" : \"MAP01\"",
                        "io.github.glynch.jscene3d.spatial3d/transform-3d",
                        "io.github.glynch.jscene3d.physics3d/static-body-3d",
                        "io.github.glynch.jscene3d.physics3d/collision-shape-3d",
                        "io.github.glynch.jscene3d.physics3d/collision-sensor-3d",
                        "io.github.glynch.jscene3d.spatial3d/mesh-renderer-3d",
                        "io.github.glynch.jscene3d.doom/door",
                        "\"profile\" : \"normal\"",
                        "\"obstruction-entered\"",
                        "\"obstruction-exited\"")
                .doesNotContain(projectDirectory.toString())
                .endsWith("}\n");
    }

    /** Publishes a type-19 floor independently from static geometry and connects its player-only walk-over trigger. */
    @Test
    void publishesMovingFloorAndTrigger() throws IOException {
        TestDoomWadFiles.write(
                wadPath, TestDoomWadFiles.withMinimalMaterials(TestDoomWadFiles.movingFloorMap("MAP01")));
        ImportManager manager = manager();
        ImportDefinition definition = definition();

        try (PreparedImport prepared = manager.prepare(definition)) {
            assertThat(prepared.preview().diagnostics()).isEmpty();
            assertThat(prepared.preview().isValid()).isTrue();
            assertThat(prepared.preview().artifacts())
                    .extracting(ImportedArtifactMetadata::identity)
                    .contains(
                            "maps/MAP01/resources/collision/floors/00001",
                            "maps/MAP01/resources/floors/00001/meshes/00000",
                            "maps/MAP01/resources/collision/floor-triggers/00000");
            prepared.commit();
        }

        String entityDefinition =
                new String(read(manager, definition, "maps/MAP01/definition"), StandardCharsets.UTF_8);
        assertThat(entityDefinition)
                .contains(
                        "\"name\" : \"Moving floor 1\"",
                        "io.github.glynch.jscene3d.doom/floor",
                        "\"profile\" : \"walk_once_lower_to_highest_surrounding\"",
                        "\"raised-height\" : 2.0",
                        "\"lowered-height\" : 0.0",
                        "\"name\" : \"Walk-over trigger 0\"",
                        "\"category-bits\" : 16.0",
                        "\"mask-bits\" : 2.0",
                        "\"endpointId\" : \"overlap-entered\"",
                        "\"endpointId\" : \"trigger-entered\"")
                .endsWith("}\n");
    }

    /** Preserves feature-owned Doom diagnostic codes during failed preparation. */
    @Test
    void preservesDoomDiagnostics() throws IOException {
        List<TestDoomWadFiles.LumpContent> lumps = List.of(
                new TestDoomWadFiles.LumpContent("MAP01", new byte[0]),
                new TestDoomWadFiles.LumpContent("TEXTMAP", new byte[] {1}));
        TestDoomWadFiles.write(wadPath, lumps);

        try (PreparedImport prepared = manager().prepare(definition())) {
            assertThat(prepared.preview().isValid()).isFalse();
            assertThat(prepared.preview().artifacts()).isEmpty();
            assertThat(prepared.preview().diagnostics()).singleElement().satisfies(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo(DoomDiagnosticCode.MAP_FORMAT_UDMF_UNSUPPORTED);
                assertThat(diagnostic.message())
                        .isEqualTo(DoomDiagnosticCode.MAP_FORMAT_UDMF_UNSUPPORTED.defaultMessage());
                assertThat(diagnostic.location()).isEqualTo("/maps/MAP01/TEXTMAP");
            });
        }
    }

    /** Creates a service-discovered manager using only public project interfaces. */
    private ImportManager manager() {
        return ImportManager.create(
                project,
                catalog,
                temporaryDirectory.resolve("cache"),
                getClass().getClassLoader(),
                List.of());
    }

    /** Loads the current import definition. */
    private ImportDefinition definition() {
        return new ImportLoader()
                .load(project, Path.of("imports/maps.import.json"))
                .definition()
                .orElseThrow();
    }

    /** Reads one published artifact completely. */
    private static byte[] read(ImportManager manager, ImportDefinition definition, String identity) throws IOException {
        try (ImportedArtifact artifact =
                        manager.openArtifact(definition, identity).orElseThrow();
                InputStream input = artifact.openStream()) {
            return input.readAllBytes();
        }
    }
}
