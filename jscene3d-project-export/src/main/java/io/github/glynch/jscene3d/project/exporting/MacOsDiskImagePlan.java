/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.exporting.internal.ApplicationImageMetadata;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Complete validated macOS disk-image plan prepared without changing output. */
final class MacOsDiskImagePlan {
    private static final String APPLICATION_SUFFIX = ".app";

    private final MacOsDiskImageRequest request;
    private final Path applicationImage;
    private final Optional<Path> backgroundImage;
    private final ApplicationImageMetadata metadata;

    /** Stores a fully validated plan. */
    private MacOsDiskImagePlan(
            MacOsDiskImageRequest request,
            Path applicationImage,
            Optional<Path> backgroundImage,
            ApplicationImageMetadata metadata) {
        this.request = request;
        this.applicationImage = applicationImage;
        this.backgroundImage = backgroundImage;
        this.metadata = metadata;
    }

    /**
     * Validates one application image and derives all disk-image packaging values.
     *
     * @param request complete disk-image request
     * @param operatingSystemName host operating-system name
     * @return complete disk-image plan
     * @throws IOException when application-image files cannot be inspected
     */
    static MacOsDiskImagePlan prepare(MacOsDiskImageRequest request, String operatingSystemName) throws IOException {
        MacOsDiskImageRequest validRequest = Objects.requireNonNull(request, "request");
        MacOsPackageValues.requireHost(operatingSystemName);
        Path image = requireApplicationImage(validRequest.applicationImage());
        Optional<Path> background = validRequest.backgroundImage().map(MacOsDiskImagePlan::requireBackgroundImage);
        ApplicationImageMetadata metadata = ApplicationImageMetadata.read(image.resolve("Contents/app"));
        String applicationName = MacOsPackageValues.requireApplicationName(metadata.applicationName());
        MacOsPackageValues.requireApplicationVersion(metadata.applicationVersion());
        requireBundleName(image, applicationName);
        requireRegularFile(image.resolve("Contents/MacOS").resolve(applicationName), "native launcher");
        requireRegularFile(image.resolve("Contents/runtime/Contents/Home/lib/modules"), "bundled Java runtime");
        validateOutput(validRequest.outputDirectory(), image, applicationName, metadata.applicationVersion());
        return new MacOsDiskImagePlan(validRequest, image, background, metadata);
    }

    /** Returns the completed macOS application image. */
    Path applicationImage() {
        return applicationImage;
    }

    /** Returns the optional validated branded Finder background. */
    Optional<Path> backgroundImage() {
        return backgroundImage;
    }

    /** Returns the application name recorded in the application image. */
    String applicationName() {
        return metadata.applicationName();
    }

    /** Returns the platform-native version recorded in the application image. */
    String applicationVersion() {
        return metadata.applicationVersion();
    }

    /** Returns the final disk-image path. */
    Path outputPath() {
        return request.outputDirectory().resolve(applicationName() + '-' + applicationVersion() + ".dmg");
    }

    /** Resolves and checks the required application-image root. */
    private static Path requireApplicationImage(Path requestedImage) throws IOException {
        if (!Files.isDirectory(requestedImage)) {
            throw new IllegalArgumentException("applicationImage is not a directory: " + requestedImage);
        }
        Path image = requestedImage.toRealPath();
        if (!image.getFileName().toString().endsWith(APPLICATION_SUFFIX)) {
            throw new IllegalArgumentException("applicationImage does not have a .app suffix: " + image);
        }
        return image;
    }

    /** Resolves one regular background image without accepting symbolic links. */
    private static Path requireBackgroundImage(Path requestedImage) {
        if (!Files.isRegularFile(requestedImage) || Files.isSymbolicLink(requestedImage)) {
            throw new IllegalArgumentException("disk-image background is not a regular file: " + requestedImage);
        }
        try {
            return requestedImage.toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "disk-image background cannot be resolved: " + requestedImage, exception);
        }
    }

    /** Requires the bundle filename to agree with its embedded application name. */
    private static void requireBundleName(Path image, String applicationName) {
        String expectedName = applicationName + APPLICATION_SUFFIX;
        if (!image.getFileName().toString().equals(expectedName)) {
            throw new IllegalArgumentException("application-image name does not match its metadata: expected "
                    + expectedName + ", found " + image.getFileName());
        }
    }

    /** Requires one regular application-image file. */
    private static void requireRegularFile(Path file, String description) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(description + " is absent from application image: " + file);
        }
    }

    /** Rejects output which could overwrite or recursively contain the application-image input. */
    private static void validateOutput(
            Path outputDirectory, Path applicationImage, String applicationName, String applicationVersion) {
        Path outputPath = outputDirectory
                .resolve(applicationName + '-' + applicationVersion + ".dmg")
                .toAbsolutePath()
                .normalize();
        if (outputPath.startsWith(applicationImage) || applicationImage.startsWith(outputPath)) {
            throw new IllegalArgumentException("disk-image output overlaps the application image: " + outputPath);
        }
    }
}
