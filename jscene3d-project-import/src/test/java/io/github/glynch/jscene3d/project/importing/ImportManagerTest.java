/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.project.extension.ExtensionCatalogLoader;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises deterministic importing through the public orchestration and adapter seams. */
final class ImportManagerTest {
    private static final String IMPORTER_ID = "io.github.glynch.import-test/source-importer";
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "io.github.glynch.import-test",
                "name": "Import Test",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "io.github.glynch.import-test",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {"id": "io.github.glynch.import-test", "requires": "1.0.0"}
              ],
              "assets": [
                {
                  "id": "source-data",
                  "type": "io.github.glynch.import-test/source-data",
                  "path": "assets/source.txt"
                }
              ],
              "imports": ["imports/source.import.json"]
            }
            """;
    private static final String IMPORT_DEFINITION = """
            {
              "schemaVersion": 1,
              "id": "source-text",
              "source": "asset:source-data",
              "importer": "io.github.glynch.import-test/source-importer",
              "selection": ["entries/main"],
              "settings": {"uppercase": false}
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private Path projectDirectory;
    private Path cacheDirectory;
    private GameProject project;
    private RegisteredTypeCatalog catalog;
    private ImportDefinition definition;

    /** Creates one source project, cache root, catalog, and import definition. */
    @BeforeEach
    void createProject() throws IOException {
        projectDirectory = Files.createDirectory(temporaryDirectory.resolve("project"));
        cacheDirectory = Files.createDirectory(temporaryDirectory.resolve("cache"));
        write("assets/source.txt", "source-v1");
        write("assets/dependency.txt", "dependency-v1");
        write("main.scene.json", "{}");
        write("imports/source.import.json", IMPORT_DEFINITION);
        write(ProjectLoader.MANIFEST_NAME, PROJECT_MANIFEST);
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(projectDirectory)
                .project()
                .orElseThrow();
        catalog = new ExtensionCatalogLoader("0.1.0-SNAPSHOT")
                .load(project, getClass().getClassLoader())
                .catalog();
        definition = new ImportLoader()
                .load(project, Path.of("imports/source.import.json"))
                .definition()
                .orElseThrow();
    }

    /** Inspects, prepares, publishes, and reads one deterministic artifact. */
    @Test
    void publishesPreparedImport() throws IOException {
        List<ImportPhase> phases = new ArrayList<>();
        ImportExecution execution =
                ImportExecution.of(ImportCancellation.none(), progress -> phases.add(progress.phase()));
        ImportManager manager = manager();

        SourceInspection inspection = manager.inspect("source-data", IMPORTER_ID, execution);
        assertThat(inspection.diagnostics()).isEmpty();
        assertThat(inspection.items())
                .extracting(SourceItem::identity)
                .containsExactly("entries/main", "entries/child", "entries/unused");
        assertThat(inspection.dependencies()).containsKey(project.root().resolve("assets/dependency.txt"));

        try (PreparedImport prepared = manager.prepare(definition, execution)) {
            assertThat(prepared.preview().isValid()).isTrue();
            assertThat(prepared.preview().artifacts())
                    .extracting(ImportedArtifactMetadata::identity)
                    .containsExactly("output/main");
            prepared.commit();
            assertThat(prepared.isCommitted()).isTrue();
        }

        assertThat(manager.status(definition).state()).isEqualTo(ImportState.CURRENT);
        assertThat(read(manager, definition, "output/main")).isEqualTo("source-v1:dependency-v1");
        assertThat(phases).contains(ImportPhase.INSPECTING, ImportPhase.PREPARING, ImportPhase.COMMITTING);
    }

    /** Detects dependency changes and preserves the last publication after a failed reimport. */
    @Test
    void preservesPublishedGenerationWhenReimportFails() throws IOException {
        ImportManager manager = manager();
        publish(manager);
        write("assets/dependency.txt", "dependency-v2");

        assertThat(manager.status(definition).state()).isEqualTo(ImportState.STALE);

        write("assets/source.txt", "FAIL-v2");
        try (PreparedImport failed = manager.prepare(definition)) {
            assertThat(failed.preview().isValid()).isFalse();
            assertThat(failed.preview().diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactly("import.prepare.failed");
        }

        assertThat(manager.status(definition).state()).isEqualTo(ImportState.STALE);
        assertThat(read(manager, definition, "output/main")).isEqualTo("source-v1:dependency-v1");
    }

    /** Cancels before adapter execution without publishing a generation. */
    @Test
    void cancelsPreparationCooperatively() {
        ImportManager manager = manager();
        ImportExecution cancelled = ImportExecution.of(() -> true, ImportProgressReporter.none());

        assertThatExceptionOfType(ImportCancelledException.class)
                .isThrownBy(() -> manager.prepare(definition, cancelled));
        assertThat(manager.status(definition).state()).isEqualTo(ImportState.MISSING);
    }

    /** Rejects stale selections and diagnoses unused or missing per-item settings. */
    @Test
    void validatesSelectionAgainstPreparedSourceGraph() {
        ImportManager manager = manager();
        ImportDefinition configured = definition(
                List.of("entries/main"),
                Map.of(
                        "entries/child", Map.of("enabled", new ProjectValue.BooleanValue(true)),
                        "entries/unused", Map.of("enabled", new ProjectValue.BooleanValue(true)),
                        "entries/missing", Map.of("enabled", new ProjectValue.BooleanValue(true))));

        try (PreparedImport prepared = manager.prepare(configured)) {
            assertThat(prepared.preview().isValid()).isFalse();
            assertThat(prepared.preview().diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactlyInAnyOrder("import.item-settings.unused", "import.item-settings.missing");
            assertThatIllegalStateException().isThrownBy(prepared::commit);
        }

        ImportDefinition notSelectable = definition(List.of("entries/child"), Map.of());
        try (PreparedImport prepared = manager.prepare(notSelectable)) {
            assertThat(prepared.preview().diagnostics())
                    .extracting(diagnostic -> diagnostic.code().code())
                    .containsExactly("import.selection.not-selectable");
        }
    }

    /** Refuses publication when authoritative inputs change after preparation. */
    @Test
    void revalidatesInputsBeforeCommit() throws IOException {
        ImportManager manager = manager();
        PreparedImport prepared = manager.prepare(definition);
        write("assets/source.txt", "source-v2");

        assertThatExceptionOfType(ImportPublicationException.class).isThrownBy(prepared::commit);
        assertThat(prepared.isCommitted()).isFalse();
        prepared.close();
        assertThat(prepared.isClosed()).isTrue();
        prepared.close();
        assertThatIllegalStateException().isThrownBy(prepared::preview);
        assertThat(manager.openArtifact(definition, "output/main")).isEmpty();
    }

    /** Reports unavailable source and importer state without altering the cache. */
    @Test
    void reportsBlockedImports() throws IOException {
        ImportManager manager = manager();
        Files.delete(project.root().resolve("assets/source.txt"));

        ImportStatus missingSource = manager.status(definition);
        SourceInspection inspection = manager.inspect("source-data", IMPORTER_ID);

        assertThat(missingSource.state()).isEqualTo(ImportState.BLOCKED);
        assertThat(missingSource.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .containsExactly("import.source.missing");
        assertThat(inspection.isValid()).isFalse();
        assertThat(inspection.diagnostics())
                .extracting(diagnostic -> diagnostic.code().code())
                .contains("import.source.read");
        ImportDefinition unknownImporter = new ImportDefinition(
                definition.source(),
                definition.id(),
                definition.asset(),
                "io.github.glynch.import-test/unknown",
                definition.selection(),
                definition.settings(),
                definition.itemSettings());
        assertThat(manager.status(unknownImporter).state()).isEqualTo(ImportState.BLOCKED);
        assertThatIllegalArgumentException().isThrownBy(() -> manager.prepare(unknownImporter));
        assertThatIllegalArgumentException().isThrownBy(() -> manager.inspect("unknown-asset", IMPORTER_ID));
    }

    /** Preserves an importer-owned diagnostic identity, fallback, and structured details. */
    @Test
    void reportsImporterOwnedWarning() throws IOException {
        write("assets/source.txt", "WARN-v1");

        SourceInspection inspection = manager().inspect("source-data", IMPORTER_ID);

        assertThat(inspection.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code().code()).isEqualTo("test-import.source.notice");
            assertThat(diagnostic.message()).isEqualTo("The synthetic source requested a notice");
            assertThat(diagnostic.details()).containsEntry("sourceText", "WARN-v1");
        });
    }

    /** Creates the import manager through its complete public construction seam. */
    private ImportManager manager() {
        List<ProjectImportExtension> extensions = List.of(new TestImportExtension());
        return ImportManager.create(project, catalog, cacheDirectory, extensions);
    }

    /** Creates a definition variant for selection validation. */
    private ImportDefinition definition(List<String> selection, Map<String, Map<String, ProjectValue>> itemSettings) {
        return new ImportDefinition(
                definition.source(),
                definition.id(),
                definition.asset(),
                definition.importer(),
                selection,
                definition.settings(),
                itemSettings);
    }

    /** Publishes the current synthetic source. */
    private void publish(ImportManager manager) {
        try (PreparedImport prepared = manager.prepare(definition)) {
            prepared.commit();
        }
    }

    /** Reads one imported artifact without exposing its physical cache path. */
    private static String read(ImportManager manager, ImportDefinition definition, String identity) throws IOException {
        try (ImportedArtifact artifact =
                        manager.openArtifact(definition, identity).orElseThrow();
                InputStream input = artifact.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Writes one UTF-8 project file. */
    private void write(String relativePath, String content) throws IOException {
        Path target = projectDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
