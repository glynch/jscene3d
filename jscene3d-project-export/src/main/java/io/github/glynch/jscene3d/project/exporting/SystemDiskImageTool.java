/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Creates branded macOS disk images through the non-interactive {@code jpackage} command. */
final class SystemDiskImageTool implements DiskImageTool {
    private static final String BACKGROUND_SUFFIX = "-background.tiff";
    private static final double FINDER_DPI = 72.0;
    private static final double MILLIMETRES_PER_INCH = 25.4;
    private static final String STANDARD_METADATA = "javax_imageio_1.0";

    private final JpackageTool tool;

    /** Uses the {@code jpackage} executable belonging to the current Java runtime. */
    SystemDiskImageTool() {
        this(new SystemJpackageTool());
    }

    /** Stores a replaceable command adapter for deterministic tests. */
    SystemDiskImageTool(JpackageTool tool) {
        this.tool = tool;
    }

    @Override
    public void create(MacOsDiskImagePlan plan, Path outputDirectory, Path resourceDirectory) throws IOException {
        List<String> arguments = arguments(plan, outputDirectory, resourceDirectory);
        JpackageToolResult result = tool.execute(arguments);
        if (result.exitCode() != 0) {
            throw new IOException("jpackage failed with exit code " + result.exitCode() + ":\n" + result.output());
        }
    }

    /** Builds the native packaging arguments and prepares optional background resources. */
    private static List<String> arguments(MacOsDiskImagePlan plan, Path outputDirectory, Path resourceDirectory)
            throws IOException {
        List<String> arguments = new ArrayList<>();
        option(arguments, "--type", "dmg");
        option(arguments, "--name", plan.applicationName());
        option(arguments, "--app-version", plan.applicationVersion());
        option(arguments, "--app-image", plan.applicationImage().toString());
        option(arguments, "--dest", outputDirectory.toString());
        if (plan.backgroundImage().isPresent()) {
            stageBackground(
                    plan.backgroundImage().orElseThrow(),
                    resourceDirectory.resolve(plan.applicationName() + BACKGROUND_SUFFIX));
            option(arguments, "--resource-dir", resourceDirectory.toString());
        }
        return List.copyOf(arguments);
    }

    /** Decodes a project-owned image and writes the TIFF resource required by {@code jpackage}. */
    private static void stageBackground(Path source, Path destination) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IOException("disk-image background is not a supported image: " + source);
        }
        BufferedImage finderImage = flattenAlpha(image);
        Path temporary = Files.createTempFile(destination.getParent(), ".background-", ".tiff");
        try {
            writeFinderTiff(finderImage, temporary);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Removes alpha because Finder does not reliably render transparent TIFF disk-image backgrounds. */
    private static BufferedImage flattenAlpha(BufferedImage source) {
        BufferedImage destination =
                new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = destination.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return destination;
    }

    /** Writes an opaque TIFF with the physical resolution expected by Finder's disk-image presentation. */
    private static void writeFinderTiff(BufferedImage image, Path destination) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new IOException("current Java runtime cannot encode TIFF disk-image backgrounds");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(destination.toFile())) {
            if (output == null) {
                throw new IOException("cannot open disk-image background for TIFF output: " + destination);
            }
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            IIOMetadata metadata = finderMetadata(writer, image, parameters);
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, metadata), parameters);
        } finally {
            writer.dispose();
        }
    }

    /** Creates standard 72-DPI metadata which the TIFF writer maps to native resolution tags. */
    private static IIOMetadata finderMetadata(ImageWriter writer, BufferedImage image, ImageWriteParam parameters)
            throws IOException {
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        IIOMetadata metadata = writer.getDefaultImageMetadata(type, parameters);
        IIOMetadataNode root = new IIOMetadataNode(STANDARD_METADATA);
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        double pixelSize = MILLIMETRES_PER_INCH / FINDER_DPI;
        dimension.appendChild(metadataValue("HorizontalPixelSize", pixelSize));
        dimension.appendChild(metadataValue("VerticalPixelSize", pixelSize));
        root.appendChild(dimension);
        metadata.mergeTree(STANDARD_METADATA, root);
        return metadata;
    }

    /** Creates one numeric standard image-metadata node. */
    private static IIOMetadataNode metadataValue(String name, double value) {
        IIOMetadataNode node = new IIOMetadataNode(name);
        node.setAttribute("value", Double.toString(value));
        return node;
    }

    /** Appends one native packaging option and its indivisible value. */
    private static void option(List<String> arguments, String name, String value) {
        arguments.add(name);
        arguments.add(value);
    }
}
