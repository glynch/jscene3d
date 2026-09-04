/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the complete public loading seam with real project directories. */
final class ProjectLoaderTest {
    private static final String MINIMAL_MANIFEST = """
            {
              "$schema": "https://jscene3d.org/schemas/project-1.json",
              "schemaVersion": 1,
              "identity": {
                "id": "example.test-game",
                "name": "Test Game",
                "version": "1.2.3"
              },
              "engine": {
                "requires": ">=0.1.0-SNAPSHOT <0.2.0"
              },
              "runtime": {
                "applicationExtension": "example.test-game",
                "entryScene": "scenes/main.scene.json"
              },
              "extensions": [
                {
                  "id": "example.test-game",
                  "requires": ">=1.0.0 <2.0.0"
                }
              ]
            }
            """;

    @TempDir
    private Path temporaryDirectory;

    /** Loads every version-one field and resolves portable paths against the canonical root. */
    @Test
    void loadsCompleteManifest() throws IOException {
        createFile("images/icon.png");
        createFile("LICENSE.txt");
        createFile("THIRD_PARTY_NOTICES.md");
        createFile("CREDITS.md");
        createFile("scenes/main.scene.json");
        createFile("game/systems.json");
        createFile("config/input.json");
        createFile("assets/game.dat");
        createFile("imports/game.import.json");
        createFile("exports/desktop.json");
        writeManifest("""
                {
                  "$schema": "https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion": 1,
                  "identity": {
                    "id": "example.test-game",
                    "name": "Test Game",
                    "version": "1.2.3-beta.1+build.7",
                    "created": "2025-12-01",
                    "released": "2026-09-03",
                    "description": "A complete test project.",
                    "icon": "images/icon.png"
                  },
                  "authors": [
                    {
                      "name": "Example Author",
                      "roles": ["creator", "developer"],
                      "url": "https://example.com/author"
                    }
                  ],
                  "links": {
                    "homepage": "https://example.com/game",
                    "source": "https://example.com/source",
                    "issues": "https://example.com/issues"
                  },
                  "legal": {
                    "projectLicense": {
                      "expression": "Apache-2.0",
                      "file": "LICENSE.txt"
                    },
                    "thirdPartyNotices": "THIRD_PARTY_NOTICES.md",
                    "credits": "CREDITS.md"
                  },
                  "engine": {
                    "requires": ">=0.1.0-SNAPSHOT <0.2.0",
                    "authoredWith": "0.1.0-SNAPSHOT"
                  },
                  "runtime": {
                    "applicationExtension": "example.test-game",
                    "entryScene": "scenes/main.scene.json",
                    "projectSystems": "game/systems.json",
                    "inputMap": "config/input.json"
                  },
                  "extensions": [
                    {
                      "id": "example.test-game",
                      "requires": ">=1.0.0 <2.0.0"
                    },
                    {
                      "id": "org.jscene3d.physics",
                      "requires": "1.0.0"
                    }
                  ],
                  "assets": [
                    {
                      "id": "game-data",
                      "type": "example.test-game/test-data",
                      "path": "assets/game.dat",
                      "sha256": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    }
                  ],
                  "imports": ["imports/game.import.json"],
                  "exportPresets": ["exports/desktop.json"],
                  "catalog": {
                    "genres": ["action"],
                    "tags": ["test", "three-dimensional"],
                    "players": {"minimum": 1, "maximum": 4},
                    "contentWarnings": ["fantasy violence"]
                  }
                }
                """);

        ProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.isValid()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        GameProject project = result.project().orElseThrow();
        Path canonicalRoot = temporaryDirectory.toRealPath();
        assertIdentity(project, canonicalRoot);
        assertMetadata(project);
        assertRuntime(project, canonicalRoot);
        assertContent(project, canonicalRoot);
    }

    /** Keeps an absent source asset editable while surfacing it to tools as a warning. */
    @Test
    void returnsProjectWithWarningForMissingAsset() throws IOException {
        createFile("scenes/main.scene.json");
        writeManifest(
                MINIMAL_MANIFEST.replace(
                        "\n}",
                        ",\n  \"assets\": [{\"id\": \"game-data\", \"type\": \"example.test-game/test-data\", \"path\": \"assets/game.dat\"}]\n}"));

        ProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.isValid()).isTrue();
        assertThat(result.project()).isPresent();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(ProjectDiagnostic.Severity.WARNING);
            assertThat(diagnostic.code()).isEqualTo("project.path.missing");
            assertThat(diagnostic.location()).isEqualTo("/assets/0/path");
        });
    }

    /** Allows an engine-generated project to declare no external source assets. */
    @Test
    void loadsManifestWithoutAssets() throws IOException {
        createFile("scenes/main.scene.json");
        writeManifest(MINIMAL_MANIFEST);

        ProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.isValid()).isTrue();
        assertThat(result.project().orElseThrow().assets()).isEmpty();
    }

    /** Rejects fields unknown to the selected manifest schema. */
    @Test
    void rejectsUnknownJsonField() throws IOException {
        writeManifest(MINIMAL_MANIFEST.replace("\"schemaVersion\": 1,", "\"schemaVersion\": 1,\n  \"mystery\": true,"));

        assertSingleError("project.manifest.json");
    }

    /** Rejects duplicate keys instead of silently accepting the last value. */
    @Test
    void rejectsDuplicateJsonField() throws IOException {
        writeManifest(
                MINIMAL_MANIFEST.replace("\"schemaVersion\": 1,", "\"schemaVersion\": 1,\n  \"schemaVersion\": 1,"));

        assertSingleError("project.manifest.json");
    }

    /** Reports syntax and cross-field errors together in stable validation order. */
    @Test
    void collectsSemanticErrors() throws IOException {
        writeManifest("""
                {
                  "$schema": "https://example.com/wrong-schema.json",
                  "schemaVersion": 2,
                  "identity": {
                    "id": "Not Portable",
                    "name": " ",
                    "version": "tomorrow",
                    "created": "03/09/2026"
                  },
                  "authors": [
                    {"name": "Author", "roles": ["developer", "developer"], "url": "relative"}
                  ],
                  "engine": {
                    "requires": ">=0.2.0",
                    "authoredWith": "latest"
                  },
                  "runtime": {
                    "applicationExtension": "missing.extension",
                    "entryScene": "../outside.scene.json"
                  },
                  "extensions": [
                    {"id": "Bad Extension", "requires": "latest"},
                    {"id": "example.duplicate", "requires": "1.0.0"},
                    {"id": "example.duplicate", "requires": "1.0.0"}
                  ],
                  "assets": [
                    {"id": "wad", "type": "doom-wad", "path": "assets/game.wad", "sha256": "bad"},
                    {"id": "wad", "type": "example.test-game/doom-wad", "path": "assets/other.wad"}
                  ],
                  "imports": ["imports/one.json", "imports/one.json"],
                  "catalog": {
                    "players": {"minimum": 2, "maximum": 1}
                  }
                }
                """);

        ProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.isValid()).isFalse();
        assertThat(result.project()).isEmpty();
        assertThat(result.diagnostics())
                .extracting(ProjectDiagnostic::code)
                .contains(
                        "project.schema.unsupported",
                        "project.schema.uri",
                        "project.identity.id",
                        "project.field.required",
                        "project.field.version",
                        "project.field.date",
                        "project.field.duplicate",
                        "project.field.uri",
                        "project.engine.incompatible",
                        "project.path.escape",
                        "project.extension.id",
                        "project.extension.requirement",
                        "project.extension.duplicate",
                        "project.field.type",
                        "project.asset.sha256",
                        "project.asset.duplicate",
                        "project.path.duplicate",
                        "project.catalog.players",
                        "project.runtime.extension.missing");
    }

    /** Rejects both lexical traversal and non-portable path separators. */
    @Test
    void rejectsUnsafePaths() throws IOException {
        writeManifest(MINIMAL_MANIFEST
                .replace("scenes/main.scene.json", "../outside.scene.json")
                .replace("\"version\": \"1.2.3\"", "\"version\": \"1.2.3\", \"icon\": \"images\\\\icon.png\""));

        ProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .extracting(ProjectDiagnostic::code)
                .contains("project.path.escape", "project.path.portable");
    }

    /** Returns a structured error when the project directory is absent. */
    @Test
    void reportsMissingProjectDirectory() {
        Path missing = temporaryDirectory.resolve("missing");

        ProjectLoadResult result = loader().load(missing);

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo("project.directory.missing");
    }

    /** Returns a structured error when project.json is absent. */
    @Test
    void reportsMissingManifest() {
        ProjectLoadResult result = loader().load(temporaryDirectory);

        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics())
                .singleElement()
                .extracting(ProjectDiagnostic::code)
                .isEqualTo("project.manifest.missing");
    }

    /** Treats an invalid running engine version as a programmer configuration error. */
    @Test
    void rejectsInvalidLoaderEngineVersion() {
        assertThatThrownBy(() -> new ProjectLoader("latest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("engineVersion must be a semantic version");
    }

    /** Publishes the JSON schema as a module resource for editors and other tools. */
    @Test
    void bundlesVersionOneSchema() throws IOException {
        String resource = "/META-INF/jscene3d/project/project-1.schema.json";

        try (var input = getClass().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("\"$id\": \"https://jscene3d.org/schemas/project-1.json\"")
                    .contains("\"applicationExtension\"")
                    .doesNotContain("gameProvider");
        }
    }

    /** Verifies complete identity values. */
    private static void assertIdentity(GameProject project, Path canonicalRoot) {
        assertThat(project.root()).isEqualTo(canonicalRoot);
        assertThat(project.metadata().identity()).isSameAs(project.identity());
        assertThat(project.identity().id()).isEqualTo("example.test-game");
        assertThat(project.identity().version()).isEqualTo("1.2.3-beta.1+build.7");
        assertThat(project.identity().created()).contains(LocalDate.of(2025, 12, 1));
        assertThat(project.identity().released()).contains(LocalDate.of(2026, 9, 3));
        assertThat(project.identity().description()).contains("A complete test project.");
        assertThat(project.identity().icon()).contains(canonicalRoot.resolve("images/icon.png"));
    }

    /** Verifies complete attribution and catalog values. */
    private static void assertMetadata(GameProject project) {
        assertThat(project.authors()).singleElement().satisfies(author -> {
            assertThat(author.name()).isEqualTo("Example Author");
            assertThat(author.roles()).containsExactly("creator", "developer");
            assertThat(author.url()).contains(URI.create("https://example.com/author"));
        });
        assertThat(project.links().homepage()).contains(URI.create("https://example.com/game"));
        assertThat(project.legal().projectLicense())
                .get()
                .extracting(GameProject.ProjectLicense::expression)
                .isEqualTo("Apache-2.0");
        assertThat(project.engine().authoredWith()).contains("0.1.0-SNAPSHOT");
        assertThat(project.catalog().genres()).containsExactly("action");
        assertThat(project.catalog().players()).contains(new GameProject.PlayerRange(1, 4));
    }

    /** Verifies complete runtime configuration. */
    private static void assertRuntime(GameProject project, Path canonicalRoot) {
        assertThat(project.runtime().applicationExtension()).isEqualTo("example.test-game");
        assertThat(project.runtime().entryScene()).isEqualTo(canonicalRoot.resolve("scenes/main.scene.json"));
        assertThat(project.runtime().projectSystems()).contains(canonicalRoot.resolve("game/systems.json"));
        assertThat(project.runtime().inputMap()).contains(canonicalRoot.resolve("config/input.json"));
        assertThat(project.extensions())
                .containsExactly(
                        new GameProject.ExtensionRequirement("example.test-game", ">=1.0.0 <2.0.0"),
                        new GameProject.ExtensionRequirement("org.jscene3d.physics", "1.0.0"));
    }

    /** Verifies complete project content references. */
    private static void assertContent(GameProject project, Path canonicalRoot) {
        assertThat(project.assets()).singleElement().satisfies(asset -> {
            assertThat(asset.path()).isEqualTo(canonicalRoot.resolve("assets/game.dat"));
            assertThat(asset.sha256()).contains("a".repeat(64));
        });
        assertThat(project.imports()).containsExactly(canonicalRoot.resolve("imports/game.import.json"));
        assertThat(project.exportPresets()).containsExactly(canonicalRoot.resolve("exports/desktop.json"));
        assertThat(project.files().assets()).isEqualTo(project.assets());
    }

    /** Loads with the engine version under development. */
    private static ProjectLoader loader() {
        return new ProjectLoader("0.1.0-SNAPSHOT");
    }

    /** Writes a manifest to the temporary project root. */
    private void writeManifest(String content) throws IOException {
        Files.writeString(temporaryDirectory.resolve(ProjectLoader.MANIFEST_NAME), content);
    }

    /** Creates one referenced project file, including parent directories. */
    private void createFile(String relativePath) throws IOException {
        Path file = temporaryDirectory.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "test");
    }

    /** Verifies one terminal loader diagnostic. */
    private void assertSingleError(String code) {
        ProjectLoadResult result = loader().load(temporaryDirectory);
        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(ProjectDiagnostic.Severity.ERROR);
            assertThat(diagnostic.code()).isEqualTo(code);
        });
    }
}
