/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies native application images through the public exporter interface. */
final class ApplicationImageExporterTest {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";
    private static final String LAUNCHER_CLASS =
            "io/github/glynch/jscene3d/project/desktop/DesktopProjectLauncher.class";

    @TempDir
    private Path temporaryDirectory;

    private Path applicationDirectory;
    private Path outputDirectory;

    /** Creates one valid, self-describing application directory. */
    @BeforeEach
    void createFixture() throws IOException {
        Path projectRoot = Files.createDirectory(temporaryDirectory.resolve("project-source"));
        Path contentRoot = Files.createDirectory(temporaryDirectory.resolve("published-content"));
        Path artifacts = Files.createDirectory(temporaryDirectory.resolve("artifacts"));
        Path gameJar = jar(artifacts.resolve("sample-game.jar"), "sample/Game.class");
        Path desktopJar = jar(artifacts.resolve("jscene3d-project-desktop.jar"), LAUNCHER_CLASS);
        Files.writeString(projectRoot.resolve("project.json"), manifest(), UTF_8);
        Files.createDirectories(projectRoot.resolve("worlds"));
        Files.writeString(projectRoot.resolve("worlds/start.world.json"), worldDefinition(), UTF_8);
        applicationDirectory = temporaryDirectory.resolve("application-directory");
        outputDirectory = temporaryDirectory.resolve("native-output");
        ApplicationDirectoryRequest request = ApplicationDirectoryRequest.builder()
                .engineVersion(ENGINE_VERSION)
                .launcherName("sample-game")
                .projectRoot(projectRoot)
                .publishedContentRoot(contentRoot)
                .runtimeArtifacts(List.of(gameJar, desktopJar))
                .outputDirectory(applicationDirectory)
                .jvmArguments(List.of("-XstartOnFirstThread"))
                .build();
        new ApplicationDirectoryExporter().export(request);
    }

    /** Derives package metadata and delegates one complete structured invocation to jpackage. */
    @Test
    void exportsMacOsApplicationImage() throws IOException {
        RecordingJpackageTool tool = new RecordingJpackageTool();
        ApplicationImageRequest request = request();

        ApplicationImage image = new ApplicationImageExporter(tool, "Mac OS X").export(request);

        assertThat(image.root()).isEqualTo(outputDirectory.resolve("Sample Game.app"));
        assertThat(image.launcher()).isEqualTo(image.root().resolve("Contents/MacOS/Sample Game"));
        assertThat(image.launcher()).isRegularFile();
        assertThat(tool.option("--type")).isEqualTo("app-image");
        assertThat(tool.option("--name")).isEqualTo("Sample Game");
        assertThat(tool.option("--app-version")).isEqualTo("1.0.0");
        assertThat(tool.option("--description")).isEqualTo("A native packaging fixture");
        assertThat(tool.option("--main-jar")).isEqualTo("jscene3d-project-desktop.jar");
        assertThat(tool.option("--main-class"))
                .isEqualTo("io.github.glynch.jscene3d.project.desktop.DesktopProjectLauncher");
        assertThat(tool.option("--mac-package-identifier")).isEqualTo("io.github.glynch.sample-game");
        assertThat(tool.javaOptions())
                .containsExactly(
                        "-XstartOnFirstThread",
                        "-Djscene3d.launch.engine.version=" + ENGINE_VERSION,
                        "-Djscene3d.launch.project.directory=$APPDIR/project",
                        "-Djscene3d.launch.content.directory=$APPDIR/content");
        assertThat(tool.inputPaths())
                .containsExactly(
                        "jscene3d-project-desktop.jar",
                        "project/project.json",
                        "project/worlds/start.world.json",
                        "sample-game.jar");
    }

    /** Leaves an existing image untouched when the external packaging tool fails. */
    @Test
    void preservesExistingImageWhenJpackageFails() throws IOException {
        Path marker = outputDirectory.resolve("Sample Game.app/existing.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "existing", UTF_8);
        ApplicationImageRequest request = request();
        JpackageTool failingTool = ignored -> new JpackageToolResult(1, "deliberate failure");
        ApplicationImageExporter exporter = new ApplicationImageExporter(failingTool, "Mac OS X");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("jpackage failed with exit code 1", "deliberate failure");
        assertThat(marker).content(UTF_8).isEqualTo("existing");
    }

    /** Rejects unsupported hosts before invoking an external tool. */
    @Test
    void rejectsUnsupportedHost() {
        ApplicationImageRequest request = request();
        RecordingJpackageTool tool = new RecordingJpackageTool();
        ApplicationImageExporter exporter = new ApplicationImageExporter(tool, "Linux");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("macOS only");
        assertThat(tool.wasInvoked()).isFalse();
    }

    /** Rejects application directories which omit the generic desktop launcher. */
    @Test
    void rejectsMissingDesktopLauncher() throws IOException {
        Files.delete(applicationDirectory.resolve("lib/jscene3d-project-desktop.jar"));
        ApplicationImageRequest request = request();
        ApplicationImageExporter exporter = new ApplicationImageExporter(new RecordingJpackageTool(), "Mac OS X");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not contain the generic desktop launcher");
    }

    /** Builds the standard native-image request. */
    private ApplicationImageRequest request() {
        return ApplicationImageRequest.builder()
                .applicationDirectory(applicationDirectory)
                .applicationVersion("1.0.0")
                .outputDirectory(outputDirectory)
                .build();
    }

    /** Writes one minimally valid JAR containing the named entry. */
    private static Path jar(Path path, String entryName) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new JarEntry(entryName));
            output.closeEntry();
        }
        return path;
    }

    /** Returns one valid packaged project manifest. */
    private static String manifest() {
        return """
                {
                  "$schema": "https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion": 1,
                  "identity": {
                    "id": "io.github.glynch.sample-game",
                    "name": "Sample Game",
                    "version": "0.1.0",
                    "description": "A native packaging fixture"
                  },
                  "engine": {
                    "requires": ">=0.1.0-SNAPSHOT <0.2.0"
                  },
                  "runtime": {
                    "applicationExtension": "io.github.glynch.sample-game",
                    "entryScene": "worlds/start.world.json"
                  },
                  "extensions": [
                    {
                      "id": "io.github.glynch.sample-game",
                      "requires": ">=0.1.0 <0.2.0"
                    }
                  ]
                }
                """;
    }

    /** Returns one discoverable world-definition header. */
    private static String worldDefinition() {
        return """
                {
                  "assetId": "1af6e97a-0401-4772-b476-29e2dcc8e77c",
                  "assetType": "world-definition",
                  "formatVersion": 1
                }
                """;
    }

    /** Fake tool which records inputs and creates the minimal expected macOS app-image layout. */
    private static final class RecordingJpackageTool implements JpackageTool {
        private List<String> arguments = List.of();
        private List<String> inputPaths = List.of();

        @Override
        public JpackageToolResult execute(List<String> suppliedArguments) throws IOException {
            arguments = List.copyOf(suppliedArguments);
            Path input = Path.of(option("--input"));
            inputPaths = relativeFiles(input);
            Path image = Path.of(option("--dest")).resolve(option("--name") + ".app");
            Path launcher = image.resolve("Contents/MacOS").resolve(option("--name"));
            Path runtimeModules = image.resolve("Contents/runtime/Contents/Home/lib/modules");
            Files.createDirectories(launcher.getParent());
            Files.createDirectories(runtimeModules.getParent());
            Files.writeString(launcher, "launcher", UTF_8);
            Files.writeString(runtimeModules, "runtime", UTF_8);
            return new JpackageToolResult(0, "");
        }

        /** Returns whether this fake received an invocation. */
        boolean wasInvoked() {
            return !arguments.isEmpty();
        }

        /** Returns the value following the first named option. */
        String option(String name) {
            int index = arguments.indexOf(name);
            if (index < 0 || index + 1 >= arguments.size()) {
                throw new IllegalStateException("missing recorded option: " + name);
            }
            return arguments.get(index + 1);
        }

        /** Returns all recorded Java options in declaration order. */
        List<String> javaOptions() {
            return values("--java-options");
        }

        /** Returns staged input file paths captured during execution. */
        List<String> inputPaths() {
            return inputPaths;
        }

        /** Returns every value following the named repeated option. */
        private List<String> values(String name) {
            Stream.Builder<String> values = Stream.builder();
            for (int index = 0; index < arguments.size() - 1; index++) {
                if (arguments.get(index).equals(name)) {
                    values.add(arguments.get(index + 1));
                }
            }
            return values.build().toList();
        }

        /** Lists staged regular files using portable sorted relative paths. */
        private static List<String> relativeFiles(Path root) throws IOException {
            try (Stream<Path> paths = Files.walk(root)) {
                return paths.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace(root.getFileSystem().getSeparator(), "/"))
                        .sorted()
                        .toList();
            }
        }
    }
}
