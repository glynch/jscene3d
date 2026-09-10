/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Verifies complete application directories through the public export interface. */
final class ApplicationDirectoryExporterTest {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";

    @TempDir
    private Path temporaryDirectory;

    private Path projectRoot;
    private Path contentRoot;
    private Path applicationArtifact;
    private Path engineArtifact;
    private Path output;

    /** Creates one project containing runtime content, imported source, and development-only files. */
    @BeforeEach
    void createFixture() throws IOException {
        projectRoot = Files.createDirectory(temporaryDirectory.resolve("project-source"));
        contentRoot = Files.createDirectory(temporaryDirectory.resolve("published-content"));
        Path artifacts = Files.createDirectory(temporaryDirectory.resolve("artifacts"));
        applicationArtifact = write(artifacts.resolve("sample-game.jar"), "application");
        engineArtifact = write(artifacts.resolve("engine.jar"), "engine");
        output = temporaryDirectory.resolve("output/sample-game");
        writeProjectFixture();
        write(contentRoot.resolve("imports/model/active-generation"), "generation-a");
        write(contentRoot.resolve("imports/model/generation-a/artifact-index.json"), "{}");
        write(contentRoot.resolve("imports/model/generation-a/artifacts/00000.bin"), "published");
        write(contentRoot.resolve("imports/model/.commit.lock"), "lock");
        write(contentRoot.resolve("staging/incomplete.bin"), "incomplete");
    }

    /** Exports only runtime project data, committed publications, JARs, and generated launchers. */
    @Test
    void assemblesRelocatableApplicationDirectory() throws IOException {
        ApplicationDirectoryRequest request = request(List.of(applicationArtifact, engineArtifact));

        ApplicationDirectory applicationDirectory = new ApplicationDirectoryExporter().export(request);

        assertThat(applicationDirectory.root())
                .isEqualTo(output.toAbsolutePath().normalize());
        assertThat(applicationDirectory.runtimeArtifacts())
                .extracting(path -> path.getFileName().toString())
                .containsExactly("engine.jar", "sample-game.jar");
        assertThat(relativeFiles(applicationDirectory.root()))
                .containsExactly(
                        "application-directory.properties",
                        "bin/sample-game",
                        "bin/sample-game.cmd",
                        "content/imports/model/active-generation",
                        "content/imports/model/generation-a/artifact-index.json",
                        "content/imports/model/generation-a/artifacts/00000.bin",
                        "lib/engine.jar",
                        "lib/sample-game.jar",
                        "project/LICENSE",
                        "project/branding/background.png",
                        "project/branding/powered-by.png",
                        "project/branding/title.png",
                        "project/config/input-map.json",
                        "project/entities/item.entity.json",
                        "project/imports/model.import.json",
                        "project/project.json",
                        "project/resources/menu-background.resource.json",
                        "project/resources/runtime.bin",
                        "project/worlds/start.world.json");
    }

    /** Generates launchers containing no source-checkout paths or game-specific Java entry point. */
    @Test
    void generatesGenericRelativeLaunchers() throws IOException {
        ApplicationDirectory applicationDirectory =
                new ApplicationDirectoryExporter().export(request(List.of(applicationArtifact)));

        assertThat(applicationDirectory.posixLauncher())
                .content(StandardCharsets.UTF_8)
                .contains("DesktopProjectLauncher", "\"$application_home/project\"")
                .contains("'-XstartOnFirstThread'", "'0.1.0-SNAPSHOT'")
                .doesNotContain(projectRoot.toString(), "SampleGameApplication");
        assertThat(applicationDirectory.windowsLauncher())
                .content(StandardCharsets.UTF_8)
                .contains("DesktopProjectLauncher", "%APPLICATION_HOME%\\project", "\"-XstartOnFirstThread\"")
                .doesNotContain(projectRoot.toString(), "SampleGameApplication");
    }

    /** Executes the generated POSIX launcher and verifies the exact Java argument vector. */
    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void posixLauncherPassesOnlyIntendedArguments() throws IOException, InterruptedException {
        ApplicationDirectory applicationDirectory =
                new ApplicationDirectoryExporter().export(request(List.of(applicationArtifact)));
        Path fakeJavaHome = temporaryDirectory.resolve("fake-java-home");
        Path fakeJava = write(fakeJavaHome.resolve("bin/java"), "#!/bin/sh\nprintf '%s\\n' \"$@\"\n");
        Files.setPosixFilePermissions(
                fakeJava,
                EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE));

        ProcessBuilder builder = new ProcessBuilder(
                        applicationDirectory.posixLauncher().toString())
                .directory(temporaryDirectory.toFile())
                .redirectErrorStream(true);
        builder.environment().put("JAVA_HOME", fakeJavaHome.toString());
        Process process = builder.start();
        String outputText;
        try (InputStream processOutput = process.getInputStream()) {
            outputText = new String(processOutput.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exitCode = process.waitFor();

        assertThat(exitCode).isZero();
        assertThat(outputText.lines())
                .containsExactly(
                        "-XstartOnFirstThread",
                        "-classpath",
                        applicationDirectory.root().resolve("lib/*").toString(),
                        "io.github.glynch.jscene3d.project.desktop.DesktopProjectLauncher",
                        ENGINE_VERSION,
                        applicationDirectory.projectDirectory().toString(),
                        applicationDirectory.publishedContentDirectory().toString());
    }

    /** Replaces a previous directory only after a complete new staging directory exists. */
    @Test
    void replacesPreviousApplicationDirectory() throws IOException {
        ApplicationDirectoryExporter exporter = new ApplicationDirectoryExporter();
        ApplicationDirectoryRequest request = request(List.of(applicationArtifact));
        exporter.export(request);
        write(output.resolve("obsolete.txt"), "obsolete");

        exporter.export(request);

        assertThat(output.resolve("obsolete.txt")).doesNotExist();
        assertThat(output.resolve("project/project.json")).isRegularFile();
    }

    /** Leaves an existing output untouched when validation rejects an incomplete runtime class path. */
    @Test
    void preservesPreviousDirectoryWhenValidationFails() throws IOException {
        Path marker = write(output.resolve("existing.txt"), "existing");
        Path missingArtifact = temporaryDirectory.resolve("missing.jar");
        ApplicationDirectoryRequest invalidRequest = request(List.of(missingArtifact));
        ApplicationDirectoryExporter exporter = new ApplicationDirectoryExporter();

        assertThatThrownBy(() -> exporter.export(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtime artifact is not a regular file");
        assertThat(marker).content(StandardCharsets.UTF_8).isEqualTo("existing");
    }

    /** Rejects distinct runtime artifacts which would overwrite the same packaged filename. */
    @Test
    void rejectsArtifactFilenameCollisions() throws IOException {
        Path otherDirectory = Files.createDirectory(temporaryDirectory.resolve("other-artifacts"));
        Path duplicate = write(otherDirectory.resolve("engine.jar"), "other engine");
        ApplicationDirectoryRequest invalidRequest = request(List.of(engineArtifact, duplicate));
        ApplicationDirectoryExporter exporter = new ApplicationDirectoryExporter();

        assertThatThrownBy(() -> exporter.export(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same filename");
    }

    /** Builds the standard request used by each export behavior test. */
    private ApplicationDirectoryRequest request(List<Path> artifacts) {
        return ApplicationDirectoryRequest.builder()
                .engineVersion(ENGINE_VERSION)
                .launcherName("sample-game")
                .projectRoot(projectRoot)
                .publishedContentRoot(contentRoot)
                .runtimeArtifacts(artifacts)
                .outputDirectory(output)
                .jvmArguments(List.of("-XstartOnFirstThread"))
                .build();
    }

    /** Writes the manifest and every category of file used to test export selection. */
    private void writeProjectFixture() throws IOException {
        write(projectRoot.resolve("project.json"), manifest());
        write(projectRoot.resolve("worlds/start.world.json"), definition("world-definition"));
        write(projectRoot.resolve("entities/item.entity.json"), definition("entity-definition"));
        write(projectRoot.resolve("config/input-map.json"), "{}");
        write(projectRoot.resolve("branding/background.png"), "background");
        write(projectRoot.resolve("branding/title.png"), "title");
        write(projectRoot.resolve("branding/powered-by.png"), "badge");
        write(projectRoot.resolve("resources/menu-background.resource.json"), encodedImageResource());
        write(projectRoot.resolve("resources/runtime.bin"), "runtime resource");
        write(projectRoot.resolve("assets/model.gltf"), "raw import source");
        write(projectRoot.resolve("imports/model.import.json"), importDefinition());
        write(projectRoot.resolve("export/desktop.json"), "{}");
        write(projectRoot.resolve("playtest/profiles.json"), "{}");
        write(projectRoot.resolve("LICENSE"), "Sample license");
        write(projectRoot.resolve("pom.xml"), "<project />");
        write(projectRoot.resolve("src/main/java/SampleGameApplication.java"), "class SampleGameApplication {}");
    }

    /** Returns one valid project manifest with both imported and runtime-native asset sources. */
    private static String manifest() {
        return """
                {
                  "$schema": "https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion": 1,
                  "identity": {
                    "id": "io.github.glynch.sample-game",
                    "name": "Sample Game",
                    "version": "0.1.0"
                  },
                  "legal": {
                    "projectLicense": {
                      "expression": "Apache-2.0",
                      "file": "LICENSE"
                    }
                  },
                  "engine": {
                    "requires": ">=0.1.0-SNAPSHOT <0.2.0"
                  },
                  "runtime": {
                    "applicationExtension": "io.github.glynch.sample-game",
                    "entryScene": "worlds/start.world.json",
                    "inputMap": "config/input-map.json"
                  },
                  "launch": {
                    "splash": {
                      "background": "branding/background.png",
                      "title": "branding/title.png",
                      "poweredByBadges": ["branding/powered-by.png"],
                      "minimumDurationSeconds": 2.0
                    }
                  },
                  "extensions": [
                    {
                      "id": "io.github.glynch.sample-game",
                      "requires": ">=0.1.0 <0.2.0"
                    }
                  ],
                  "assets": [
                    {
                      "id": "menu-background",
                      "type": "io.github.glynch.sample/overlay-image",
                      "path": "resources/menu-background.resource.json"
                    },
                    {
                      "id": "model",
                      "type": "io.github.glynch.sample/model-source",
                      "path": "assets/model.gltf"
                    },
                    {
                      "id": "runtime-resource",
                      "type": "io.github.glynch.sample/runtime-resource",
                      "path": "resources/runtime.bin"
                    }
                  ],
                  "imports": ["imports/model.import.json"],
                  "exportPresets": ["export/desktop.json"]
                }
                """;
    }

    /** Returns one resource definition with an authored project payload dependency. */
    private static String encodedImageResource() {
        return """
                {
                  "$schema": "https://jscene3d.org/schemas/resource-1.json",
                  "schemaVersion": 1,
                  "type": "io.github.glynch.sample/overlay-image",
                  "typeVersion": 1,
                  "properties": {
                    "payload": {"$ref": "project:branding/background.png"},
                    "encoding": "png"
                  }
                }
                """;
    }

    /** Returns one discoverable definition header. */
    private static String definition(String type) {
        String id = type.equals("world-definition")
                ? "1af6e97a-0401-4772-b476-29e2dcc8e77c"
                : "88376f49-183d-4d1c-96bf-eaaddd50ed91";
        return """
                {
                  "assetId": "$ID",
                  "assetType": "$TYPE",
                  "formatVersion": 1
                }
                """.replace("$ID", id).replace("$TYPE", type);
    }

    /** Returns an import recipe which identifies the raw asset excluded from runtime output. */
    private static String importDefinition() {
        return """
                {
                  "$schema": "https://jscene3d.org/schemas/import-1.json",
                  "schemaVersion": 1,
                  "id": "model-import",
                  "source": "asset:model",
                  "importer": "io.github.glynch.sample/model",
                  "selection": ["scenes/0000"]
                }
                """;
    }

    /** Writes one UTF-8 fixture file after creating its parent directory. */
    private static Path write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /** Returns regular-file paths relative to one application directory in portable sorted order. */
    private static List<String> relativeFiles(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace(root.getFileSystem().getSeparator(), "/"))
                    .sorted()
                    .toList();
        }
    }
}
