/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.importing;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.github.glynch.jscene3d.wad.WadDiagnosticCode;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the complete public WAD project-import integration. */
final class WadImportExtensionTest {
    private static final String FIRST_LUMP = "lumps/00000000/4F4E45";
    private static final String SECOND_LUMP = "lumps/00000001/445550";
    private static final String THIRD_LUMP = "lumps/00000002/445550";
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.wad-import-test",
                "name": "WAD Import Test",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "io.github.glynch.jscene3d.wad",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {"id": "io.github.glynch.jscene3d.wad", "requires": "0.1.0-SNAPSHOT"}
              ],
              "assets": [
                {
                  "id": "content",
                  "type": "io.github.glynch.jscene3d.wad/source",
                  "path": "assets/content.wad"
                }
              ],
              "imports": ["imports/content.import.json"]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private Path projectDirectory;
    private Path wadPath;
    private GameProject project;
    private RegisteredTypeCatalog catalog;

    /** Creates one project containing duplicate WAD lump names. */
    @BeforeEach
    void createProject() throws IOException {
        projectDirectory = Files.createDirectory(temporaryDirectory.resolve("project"));
        Files.createDirectories(projectDirectory.resolve("assets"));
        Files.createDirectories(projectDirectory.resolve("imports"));
        Files.writeString(projectDirectory.resolve("main.scene.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(
                projectDirectory.resolve(ProjectLoader.MANIFEST_NAME), PROJECT_MANIFEST, StandardCharsets.UTF_8);
        wadPath = projectDirectory.resolve("assets/content.wad");
        TestWadFiles.write(
                wadPath,
                List.of(
                        new TestWadFiles.LumpContent("ONE", new byte[] {1, 2}),
                        new TestWadFiles.LumpContent("DUP", new byte[] {3}),
                        new TestWadFiles.LumpContent("DUP", new byte[] {4, 5, 6})));
        writeImport(List.of(SECOND_LUMP));
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(projectDirectory)
                .project()
                .orElseThrow();
        ExtensionCatalogLoadResult result = new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                .load(project, getClass().getClassLoader());
        if (!result.diagnostics().isEmpty()) {
            throw new IllegalStateException("test extension descriptor is invalid: " + result.diagnostics());
        }
        catalog = result.catalog();
    }

    /** Preserves directory order, duplicate names, stable identities, and source metadata. */
    @Test
    void inspectsArchiveAndOrderedLumps() {
        SourceInspection inspection = manager().inspect("content", WadImportExtension.IMPORTER_IDENTIFIER);

        assertThat(inspection.isValid()).isTrue();
        assertThat(inspection.items())
                .extracting(SourceItem::identity)
                .containsExactly("archive", FIRST_LUMP, SECOND_LUMP, THIRD_LUMP);
        SourceItem archive = inspection.items().getFirst();
        assertThat(archive.kind()).isEqualTo("io.github.glynch.jscene3d.wad/archive");
        assertThat(archive.relations())
                .extracting(relation -> relation.targetIdentity())
                .containsExactly(FIRST_LUMP, SECOND_LUMP, THIRD_LUMP);
        SourceItem duplicate = inspection.items().get(2);
        assertThat(duplicate.displayName()).isEqualTo("DUP (#1)");
        assertThat(duplicate.properties())
                .containsEntry("name", new ProjectValue.TextValue("DUP"))
                .containsEntry("index", new ProjectValue.NumberValue(BigDecimal.ONE));
    }

    /** Imports one directly selected lump plus a complete portable archive index. */
    @Test
    void importsSelectedLumpAndCompleteIndex() throws IOException {
        ImportManager manager = manager();
        ImportDefinition definition = definition();

        try (PreparedImport prepared = manager.prepare(definition)) {
            assertThat(prepared.preview().isValid()).isTrue();
            assertThat(prepared.preview().artifacts())
                    .extracting(ImportedArtifactMetadata::identity)
                    .containsExactly("archive/index", SECOND_LUMP);
            prepared.commit();
        }

        assertThat(read(manager, definition, SECOND_LUMP)).containsExactly(3);
        String index = new String(read(manager, definition, "archive/index"), StandardCharsets.UTF_8);
        assertThat(index)
                .startsWith("{\n  \"schemaVersion\" : 1,")
                .contains("\"archiveKind\" : \"IWAD\"", "\"asset\" : \"content\"")
                .endsWith("}\n");
        assertThat(index)
                .containsSubsequence(
                        "\"identity\" : \"" + FIRST_LUMP + "\"",
                        "\"artifact\" : null",
                        "\"identity\" : \"" + SECOND_LUMP + "\"",
                        "\"artifact\" : \"" + SECOND_LUMP + "\"",
                        "\"identity\" : \"" + THIRD_LUMP + "\"",
                        "\"artifact\" : null");
    }

    /** Expands archive selection through its contains relations into every lump artifact. */
    @Test
    void importsAllLumpsWhenArchiveIsSelected() throws IOException {
        writeImport(List.of("archive"));
        ImportDefinition definition = definition();

        try (PreparedImport prepared = manager().prepare(definition)) {
            assertThat(prepared.preview().isValid()).isTrue();
            assertThat(prepared.preview().artifacts())
                    .extracting(ImportedArtifactMetadata::identity)
                    .containsExactly("archive/index", FIRST_LUMP, SECOND_LUMP, THIRD_LUMP);
        }
    }

    /** Preserves feature-owned WAD diagnostic codes through generic inspection. */
    @Test
    void preservesWadDiagnostics() throws IOException {
        Files.write(wadPath, new byte[] {'I', 'W'});

        SourceInspection inspection = manager().inspect("content", WadImportExtension.IMPORTER_IDENTIFIER);

        assertThat(inspection.isValid()).isFalse();
        assertThat(inspection.items()).isEmpty();
        assertThat(inspection.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(WadDiagnosticCode.HEADER_TRUNCATED);
            assertThat(diagnostic.message()).isEqualTo("The WAD header is incomplete");
        });
    }

    /** Creates a service-discovered manager using only public project APIs. */
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
                .load(project, Path.of("imports/content.import.json"))
                .definition()
                .orElseThrow();
    }

    /** Writes one import definition with an ordered authored selection. */
    private void writeImport(List<String> selection) throws IOException {
        String selected = selection.stream().map(value -> '"' + value + '"').collect(Collectors.joining(", "));
        String document = String.format(Locale.ROOT, """
                {
                  "schemaVersion": 1,
                  "id": "content",
                  "source": "asset:content",
                  "importer": "io.github.glynch.jscene3d.wad/archive",
                  "selection": [%s]
                }
                """, selected);
        Files.writeString(projectDirectory.resolve("imports/content.import.json"), document, StandardCharsets.UTF_8);
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
