/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.imports;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises safe import-definition loading without executing import extensions. */
final class ImportLoaderTest {
    private static final String PROJECT_MANIFEST = """
            {
              "schemaVersion": 1,
              "identity": {
                "id": "example.import-test",
                "name": "Import Test",
                "version": "1.0.0"
              },
              "engine": {"requires": ">=0.1.0-SNAPSHOT <0.2.0"},
              "runtime": {
                "applicationExtension": "example.import-test",
                "entryScene": "main.scene.json"
              },
              "extensions": [
                {"id": "example.import-test", "requires": "1.0.0"}
              ],
              "assets": [
                {
                  "id": "source-data",
                  "type": "example.import-test/source-data",
                  "path": "assets/source.dat"
                }
              ],
              "imports": ["imports/source.import.json"]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    private GameProject project;

    /** Creates one valid project for each import-definition test. */
    @BeforeEach
    void createProject() throws IOException {
        write("assets/source.dat", "source");
        write("main.scene.json", "{}");
        write(ProjectLoader.MANIFEST_NAME, PROJECT_MANIFEST);
        project = new ProjectLoader("0.1.0-SNAPSHOT")
                .load(temporaryDirectory)
                .project()
                .orElseThrow();
    }

    /** Loads ordered selection, importer-wide settings, and per-item settings. */
    @Test
    void loadsCompleteImportDefinition() throws IOException {
        write("imports/source.import.json", """
                {
                  "$schema": "../schema/import-1.schema.json",
                  "schemaVersion": 1,
                  "id": "source-scene",
                  "source": "asset:source-data",
                  "importer": "example.import-test/source-importer",
                  "selection": ["scenes/main", "animations/walk"],
                  "settings": {
                    "generateCollision": true
                  },
                  "itemSettings": {
                    "animations/walk": {
                      "loop": true
                    }
                  }
                }
                """);

        ImportLoadResult result = new ImportLoader().load(project, Path.of("imports/source.import.json"));

        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.isValid()).isTrue();
        ImportDefinition definition = result.definition().orElseThrow();
        assertThat(definition.id()).isEqualTo("source-scene");
        assertThat(definition.asset()).isEqualTo(project.assets().getFirst());
        assertThat(definition.importer()).isEqualTo("example.import-test/source-importer");
        assertThat(definition.selection()).containsExactly("scenes/main", "animations/walk");
        assertThat(definition.settings()).containsEntry("generateCollision", new ProjectValue.BooleanValue(true));
        assertThat(definition.itemSettings().get("animations/walk"))
                .containsEntry("loop", new ProjectValue.BooleanValue(true));
    }

    /** Collects independent schema, identity, source, selection, and settings errors. */
    @Test
    void collectsSemanticImportErrors() throws IOException {
        write("imports/source.import.json", """
                {
                  "$schema": "wrong.json",
                  "schemaVersion": 2,
                  "id": "Bad Id",
                  "source": "file:missing",
                  "importer": "bad",
                  "selection": ["maps/MAP01", "maps/MAP01", "../escape"],
                  "settings": [],
                  "itemSettings": {
                    "missing/item": []
                  }
                }
                """);

        ImportLoadResult result = new ImportLoader().load(project, Path.of("imports/source.import.json"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.definition()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(ProjectDiagnostic::code)
                .containsExactly(
                        "import.schema.unsupported",
                        "import.schema.uri",
                        "import.field.identifier",
                        "import.source.namespace",
                        "import.field.type",
                        "import.selection.duplicate",
                        "import.selection.identity",
                        "import.settings.object",
                        "import.item-settings.object");
    }

    /** Rejects unknown fields and import-definition paths outside the project. */
    @Test
    void rejectsUnknownAndEscapingImportDefinitions() throws IOException {
        write("imports/source.import.json", """
                {
                  "schemaVersion": 1,
                  "id": "source-scene",
                  "source": "asset:source-data",
                  "importer": "example.import-test/source-importer",
                  "selection": ["scenes/main"],
                  "unknown": true
                }
                """);
        ImportLoader loader = new ImportLoader();

        ImportLoadResult unknown = loader.load(project, Path.of("imports/source.import.json"));
        ImportLoadResult escaping = loader.load(project, Path.of("../outside.import.json"));

        assertThat(unknown.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo("import.json");
        assertThat(escaping.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo("import.path.escape");
    }

    /** Publishes the Import Definition version-one schema for editors and validation tools. */
    @Test
    void bundlesVersionOneImportSchema() throws IOException {
        String resourcePath = "/META-INF/jscene3d/project/import-1.schema.json";

        try (var input = getClass().getResourceAsStream(resourcePath)) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("\"$id\": \"https://jscene3d.org/schemas/import-1.json\"")
                    .contains("\"selection\"")
                    .contains("\"itemSettings\"");
        }
    }

    /** Writes one UTF-8 project test file. */
    private void write(String relativePath, String content) throws IOException {
        Path target = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
