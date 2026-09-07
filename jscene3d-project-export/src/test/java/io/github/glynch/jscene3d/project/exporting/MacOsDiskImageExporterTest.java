/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.exporting.internal.ApplicationImageMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies macOS disk images through the public exporter interface. */
final class MacOsDiskImageExporterTest {
    @TempDir
    private Path temporaryDirectory;

    private Path applicationImage;
    private Path outputDirectory;

    /** Creates one valid, self-describing application image. */
    @BeforeEach
    void createFixture() throws IOException {
        applicationImage = temporaryDirectory.resolve("Sample Game.app");
        Path applicationContent = Files.createDirectories(applicationImage.resolve("Contents/app"));
        Path launcher = applicationImage.resolve("Contents/MacOS/Sample Game");
        Path runtime = applicationImage.resolve("Contents/runtime/Contents/Home/lib/modules");
        Files.createDirectories(launcher.getParent());
        Files.createDirectories(runtime.getParent());
        Files.writeString(launcher, "launcher", UTF_8);
        Files.writeString(runtime, "runtime", UTF_8);
        new ApplicationImageMetadata("Sample Game", "1.2.3").write(applicationContent);
        outputDirectory = temporaryDirectory.resolve("distribution");
    }

    /** Derives identity from the application image and delegates one structured invocation to jpackage. */
    @Test
    void exportsMacOsDiskImage() throws IOException {
        RecordingJpackageTool tool = new RecordingJpackageTool();
        MacOsDiskImageRequest request = request();

        MacOsDiskImage diskImage = new MacOsDiskImageExporter(tool, "Mac OS X").export(request);

        assertThat(diskImage.path())
                .isEqualTo(outputDirectory.resolve("Sample Game-1.2.3.dmg"))
                .isRegularFile()
                .hasContent("disk image");
        assertThat(tool.arguments())
                .containsExactly(
                        "--type",
                        "dmg",
                        "--name",
                        "Sample Game",
                        "--app-version",
                        "1.2.3",
                        "--app-image",
                        applicationImage.toRealPath().toString(),
                        "--dest",
                        tool.option("--dest"));
    }

    /** Leaves an existing disk image untouched when the external packaging tool fails. */
    @Test
    void preservesExistingDiskImageWhenJpackageFails() throws IOException {
        Path existing = outputDirectory.resolve("Sample Game-1.2.3.dmg");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "existing", UTF_8);
        MacOsDiskImageRequest request = request();
        JpackageTool failingTool = ignored -> new JpackageToolResult(1, "deliberate failure");
        MacOsDiskImageExporter exporter = new MacOsDiskImageExporter(failingTool, "Mac OS X");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("jpackage failed with exit code 1", "deliberate failure");
        assertThat(existing).content(UTF_8).isEqualTo("existing");
    }

    /** Rejects unsupported hosts before invoking an external tool. */
    @Test
    void rejectsUnsupportedHost() {
        MacOsDiskImageRequest request = request();
        RecordingJpackageTool tool = new RecordingJpackageTool();
        MacOsDiskImageExporter exporter = new MacOsDiskImageExporter(tool, "Linux");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("macOS only");
        assertThat(tool.wasInvoked()).isFalse();
    }

    /** Rejects application images whose embedded identity metadata is absent. */
    @Test
    void rejectsMissingApplicationImageMetadata() throws IOException {
        Files.delete(applicationImage.resolve("Contents/app").resolve(ApplicationImageMetadata.PATH));
        MacOsDiskImageRequest request = request();
        MacOsDiskImageExporter exporter = new MacOsDiskImageExporter(new RecordingJpackageTool(), "Mac OS X");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(ApplicationImageMetadata.PATH.toString());
    }

    /** Builds the standard disk-image request. */
    private MacOsDiskImageRequest request() {
        return MacOsDiskImageRequest.builder()
                .applicationImage(applicationImage)
                .outputDirectory(outputDirectory)
                .build();
    }

    /** Fake tool which records its arguments and creates the expected DMG in its private destination. */
    private static final class RecordingJpackageTool implements JpackageTool {
        private List<String> arguments = List.of();

        @Override
        public JpackageToolResult execute(List<String> suppliedArguments) throws IOException {
            arguments = List.copyOf(suppliedArguments);
            Path diskImage =
                    Path.of(option("--dest")).resolve(option("--name") + '-' + option("--app-version") + ".dmg");
            Files.writeString(diskImage, "disk image", UTF_8);
            return new JpackageToolResult(0, "");
        }

        /** Returns whether this fake received an invocation. */
        boolean wasInvoked() {
            return !arguments.isEmpty();
        }

        /** Returns all recorded arguments. */
        List<String> arguments() {
            return arguments;
        }

        /** Returns the value following the named option. */
        String option(String name) {
            int index = arguments.indexOf(name);
            if (index < 0 || index + 1 >= arguments.size()) {
                throw new IllegalStateException("missing recorded option: " + name);
            }
            return arguments.get(index + 1);
        }
    }
}
