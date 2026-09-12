/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.project.exporting.internal.ApplicationImageMetadata;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;

/** Verifies native disk-image command construction and branded resource staging. */
final class SystemDiskImageToolTest {
    @TempDir
    private Path temporaryDirectory;

    private Path applicationImage;
    private Path backgroundImage;
    private Path outputDirectory;
    private Path resourceDirectory;

    /** Creates one complete application image and one supported background image. */
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

        backgroundImage = temporaryDirectory.resolve("background.png");
        ImageIO.write(new BufferedImage(517, 270, BufferedImage.TYPE_INT_ARGB), "PNG", backgroundImage.toFile());
        outputDirectory = Files.createDirectories(temporaryDirectory.resolve("output"));
        resourceDirectory = Files.createDirectories(temporaryDirectory.resolve("resources"));
    }

    /** Passes the application image to jpackage and converts the branded background to TIFF. */
    @Test
    void createsBrandedDiskImage() throws IOException {
        RecordingJpackageTool jpackage = new RecordingJpackageTool();
        MacOsDiskImagePlan plan = brandedPlan();

        new SystemDiskImageTool(jpackage).create(plan, outputDirectory, resourceDirectory);

        assertThat(jpackage.arguments())
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
                        outputDirectory.toString(),
                        "--resource-dir",
                        resourceDirectory.toString());
        Path stagedBackground = resourceDirectory.resolve("Sample Game-background.tiff");
        assertThat(stagedBackground).isRegularFile();
        BufferedImage convertedBackground = ImageIO.read(stagedBackground.toFile());
        assertThat(convertedBackground.getWidth()).isEqualTo(517);
        assertThat(convertedBackground.getHeight()).isEqualTo(270);
        assertThat(convertedBackground.getColorModel().hasAlpha()).isFalse();
        assertThat(pixelSize(stagedBackground, "HorizontalPixelSize")).isCloseTo(25.4 / 72.0, within(0.001));
        assertThat(pixelSize(stagedBackground, "VerticalPixelSize")).isCloseTo(25.4 / 72.0, within(0.001));
    }

    /** Omits the resource directory when the project does not supply branded imagery. */
    @Test
    void createsUnbrandedDiskImage() throws IOException {
        RecordingJpackageTool jpackage = new RecordingJpackageTool();
        MacOsDiskImageRequest request = MacOsDiskImageRequest.builder()
                .applicationImage(applicationImage)
                .outputDirectory(outputDirectory)
                .build();
        MacOsDiskImagePlan plan = MacOsDiskImagePlan.prepare(request, "Mac OS X");

        new SystemDiskImageTool(jpackage).create(plan, outputDirectory, resourceDirectory);

        assertThat(jpackage.arguments()).doesNotContain("--resource-dir");
        assertThat(resourceDirectory).isEmptyDirectory();
    }

    /** Rejects undecodable imagery before invoking the external packaging tool. */
    @Test
    void rejectsUnsupportedBackgroundImage() throws IOException {
        Files.writeString(backgroundImage, "not an image", UTF_8);
        RecordingJpackageTool jpackage = new RecordingJpackageTool();
        MacOsDiskImagePlan plan = brandedPlan();
        SystemDiskImageTool diskImageTool = new SystemDiskImageTool(jpackage);

        assertThatThrownBy(() -> diskImageTool.create(plan, outputDirectory, resourceDirectory))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not a supported image");
        assertThat(jpackage.arguments()).isEmpty();
    }

    /** Prepares a branded disk-image plan from the shared fixture. */
    private MacOsDiskImagePlan brandedPlan() throws IOException {
        MacOsDiskImageRequest request = MacOsDiskImageRequest.builder()
                .applicationImage(applicationImage)
                .backgroundImage(backgroundImage)
                .outputDirectory(outputDirectory)
                .build();
        return MacOsDiskImagePlan.prepare(request, "Mac OS X");
    }

    /** Reads one standard pixel-size metadata value from the staged TIFF. */
    private static double pixelSize(Path image, String name) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(image.toFile())) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                reader.setInput(input);
                Node metadata = reader.getImageMetadata(0).getAsTree("javax_imageio_1.0");
                IIOMetadataNode value = findMetadataNode(metadata, name)
                        .orElseThrow(() -> new IllegalArgumentException("missing image metadata node: " + name));
                return Double.parseDouble(value.getAttribute("value"));
            } finally {
                reader.dispose();
            }
        }
    }

    /** Finds a required metadata node by name. */
    private static Optional<IIOMetadataNode> findMetadataNode(Node node, String name) {
        if (node.getNodeName().equals(name)) {
            return Optional.of((IIOMetadataNode) node);
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Optional<IIOMetadataNode> match = findMetadataNode(child, name);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    /** Captures the exact argument vector supplied to the external tool. */
    private static final class RecordingJpackageTool implements JpackageTool {
        private List<String> arguments = List.of();

        @Override
        public JpackageToolResult execute(List<String> suppliedArguments) {
            arguments = List.copyOf(suppliedArguments);
            return new JpackageToolResult(0, "");
        }

        /** Returns the most recently captured arguments. */
        List<String> arguments() {
            return arguments;
        }
    }
}
