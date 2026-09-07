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
import java.util.stream.Stream;
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

    /** Derives identity and stages complete non-interactive disk-image content. */
    @Test
    void exportsMacOsDiskImage() throws IOException {
        RecordingDiskImageTool tool = new RecordingDiskImageTool();
        MacOsDiskImageRequest request = request();

        MacOsDiskImage diskImage = new MacOsDiskImageExporter(tool, "Mac OS X").export(request);

        assertThat(diskImage.path())
                .isEqualTo(outputDirectory.resolve("Sample Game-1.2.3.dmg"))
                .isRegularFile()
                .hasContent("disk image");
        assertThat(tool.volumeName()).isEqualTo("Sample Game");
        assertThat(tool.destination().getFileName()).hasToString("Sample Game-1.2.3.dmg");
        assertThat(tool.stagedPaths())
                .contains(
                        "Applications",
                        "Sample Game.app/Contents/MacOS/Sample Game",
                        "Sample Game.app/Contents/app/application-image.properties",
                        "Sample Game.app/Contents/runtime/Contents/Home/lib/modules");
        assertThat(tool.applicationsLinkTarget()).isEqualTo(Path.of("/Applications"));
    }

    /** Leaves an existing disk image untouched when the external packaging tool fails. */
    @Test
    void preservesExistingDiskImageWhenHdiutilFails() throws IOException {
        Path existing = outputDirectory.resolve("Sample Game-1.2.3.dmg");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "existing", UTF_8);
        MacOsDiskImageRequest request = request();
        DiskImageTool failingTool = (source, volumeName, destination) -> {
            throw new IOException("hdiutil failed with exit code 1: deliberate failure");
        };
        MacOsDiskImageExporter exporter = new MacOsDiskImageExporter(failingTool, "Mac OS X");

        assertThatThrownBy(() -> exporter.export(request))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("hdiutil failed with exit code 1", "deliberate failure");
        assertThat(existing).content(UTF_8).isEqualTo("existing");
    }

    /** Rejects unsupported hosts before invoking an external tool. */
    @Test
    void rejectsUnsupportedHost() {
        MacOsDiskImageRequest request = request();
        RecordingDiskImageTool tool = new RecordingDiskImageTool();
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
        MacOsDiskImageExporter exporter = new MacOsDiskImageExporter(new RecordingDiskImageTool(), "Mac OS X");

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

    /** Fake tool which captures prepared content and creates the expected DMG. */
    private static final class RecordingDiskImageTool implements DiskImageTool {
        private String volumeName = "";
        private Path destination = Path.of("uninvoked");
        private List<String> stagedPaths = List.of();
        private Path applicationsLinkTarget = Path.of("uninvoked");
        private boolean invoked;

        @Override
        public void create(Path sourceDirectory, String suppliedVolumeName, Path suppliedDestination)
                throws IOException {
            invoked = true;
            volumeName = suppliedVolumeName;
            destination = suppliedDestination;
            try (Stream<Path> paths = Files.walk(sourceDirectory)) {
                stagedPaths = paths.filter(path -> !path.equals(sourceDirectory))
                        .map(sourceDirectory::relativize)
                        .map(Path::toString)
                        .map(path ->
                                path.replace(sourceDirectory.getFileSystem().getSeparator(), "/"))
                        .sorted()
                        .toList();
            }
            applicationsLinkTarget = Files.readSymbolicLink(sourceDirectory.resolve("Applications"));
            Files.writeString(destination, "disk image", UTF_8);
        }

        /** Returns whether this fake received an invocation. */
        boolean wasInvoked() {
            return invoked;
        }

        /** Returns the captured mounted volume name. */
        String volumeName() {
            return volumeName;
        }

        /** Returns the captured private output path. */
        Path destination() {
            return destination;
        }

        /** Returns the staged disk-image paths captured before staging cleanup. */
        List<String> stagedPaths() {
            return stagedPaths;
        }

        /** Returns the destination of the staged Applications link. */
        Path applicationsLinkTarget() {
            return applicationsLinkTarget;
        }
    }
}
