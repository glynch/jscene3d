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
import java.util.Optional;
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

    /** Derives identity and delegates one complete disk-image package request. */
    @Test
    void exportsMacOsDiskImage() throws IOException {
        RecordingDiskImageTool tool = new RecordingDiskImageTool();
        MacOsDiskImageRequest request = request();

        MacOsDiskImage diskImage = new MacOsDiskImageExporter(tool, "Mac OS X").export(request);

        assertThat(diskImage.path())
                .isEqualTo(outputDirectory.resolve("Sample Game-1.2.3.dmg"))
                .isRegularFile()
                .hasContent("disk image");
        assertThat(tool.applicationImage()).isEqualTo(applicationImage.toRealPath());
        assertThat(tool.applicationName()).isEqualTo("Sample Game");
        assertThat(tool.applicationVersion()).isEqualTo("1.2.3");
        assertThat(tool.backgroundImage()).isEmpty();
    }

    /** Carries an optional branded background into the native packaging plan. */
    @Test
    void exportsBrandedMacOsDiskImage() throws IOException {
        Path background = temporaryDirectory.resolve("background.png");
        Files.writeString(background, "image", UTF_8);
        RecordingDiskImageTool tool = new RecordingDiskImageTool();
        MacOsDiskImageRequest request = MacOsDiskImageRequest.builder()
                .applicationImage(applicationImage)
                .backgroundImage(background)
                .outputDirectory(outputDirectory)
                .build();

        new MacOsDiskImageExporter(tool, "Mac OS X").export(request);

        assertThat(tool.backgroundImage()).contains(background.toRealPath());
    }

    /** Leaves an existing disk image untouched when the external packaging tool fails. */
    @Test
    void preservesExistingDiskImageWhenJpackageFails() throws IOException {
        Path existing = outputDirectory.resolve("Sample Game-1.2.3.dmg");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "existing", UTF_8);
        MacOsDiskImageRequest request = request();
        DiskImageTool failingTool = (plan, output, resources) -> {
            throw new IOException("jpackage failed with exit code 1: deliberate failure");
        };
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
        private Path applicationImage = Path.of("uninvoked");
        private String applicationName = "";
        private String applicationVersion = "";
        private Optional<Path> backgroundImage = Optional.empty();
        private boolean invoked;

        @Override
        public void create(MacOsDiskImagePlan plan, Path outputDirectory, Path resourceDirectory) throws IOException {
            invoked = true;
            applicationImage = plan.applicationImage();
            applicationName = plan.applicationName();
            applicationVersion = plan.applicationVersion();
            backgroundImage = plan.backgroundImage();
            Files.writeString(outputDirectory.resolve(plan.outputPath().getFileName()), "disk image", UTF_8);
        }

        /** Returns whether this fake received an invocation. */
        boolean wasInvoked() {
            return invoked;
        }

        /** Returns the captured application image. */
        Path applicationImage() {
            return applicationImage;
        }

        /** Returns the captured application name. */
        String applicationName() {
            return applicationName;
        }

        /** Returns the captured native application version. */
        String applicationVersion() {
            return applicationVersion;
        }

        /** Returns the captured optional background image. */
        Optional<Path> backgroundImage() {
            return backgroundImage;
        }
    }
}
