/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.normalizeAbsolute;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Immutable request to package one completed macOS application image as a disk image. */
public final class MacOsDiskImageRequest {
    private final Path applicationImage;
    private final Optional<Path> backgroundImage;
    private final Path outputDirectory;

    /** Builds one validated request from caller-supplied paths. */
    private MacOsDiskImageRequest(Builder builder) {
        applicationImage = normalizeAbsolute(
                Objects.requireNonNull(builder.applicationImage, "applicationImage"), "applicationImage");
        backgroundImage =
                Optional.ofNullable(builder.backgroundImage).map(path -> normalizeAbsolute(path, "backgroundImage"));
        outputDirectory = normalizeAbsolute(
                Objects.requireNonNull(builder.outputDirectory, "outputDirectory"), "outputDirectory");
    }

    /**
     * Starts a macOS disk-image request.
     *
     * @return empty request builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the completed macOS application image to package.
     *
     * @return normalized absolute {@code .app} path
     */
    public Path applicationImage() {
        return applicationImage;
    }

    /**
     * Returns the optional branded Finder background.
     *
     * <p>The exporter decodes common {@code ImageIO} formats and supplies the TIFF resource required by
     * {@code jpackage}.
     *
     * @return normalized absolute image path, or empty for the platform default
     */
    public Optional<Path> backgroundImage() {
        return backgroundImage;
    }

    /**
     * Returns the directory in which the disk image will be installed.
     *
     * @return normalized absolute destination directory
     */
    public Path outputDirectory() {
        return outputDirectory;
    }

    /** Mutable construction convenience for macOS disk-image configuration. */
    public static final class Builder {
        private @Nullable Path applicationImage;
        private @Nullable Path backgroundImage;
        private @Nullable Path outputDirectory;

        /** Prevents construction outside {@link MacOsDiskImageRequest#builder()}. */
        private Builder() {}

        /**
         * Sets the completed macOS application image to package.
         *
         * @param value application-image root
         * @return this builder
         */
        public Builder applicationImage(Path value) {
            applicationImage = value;
            return this;
        }

        /**
         * Sets the branded Finder background shown when the disk image opens.
         *
         * @param value source image decoded through {@code ImageIO}
         * @return this builder
         */
        public Builder backgroundImage(Path value) {
            backgroundImage = value;
            return this;
        }

        /**
         * Sets the directory in which the disk image will be installed.
         *
         * @param value destination directory
         * @return this builder
         */
        public Builder outputDirectory(Path value) {
            outputDirectory = value;
            return this;
        }

        /**
         * Creates the validated immutable request.
         *
         * @return macOS disk-image request
         */
        public MacOsDiskImageRequest build() {
            return new MacOsDiskImageRequest(this);
        }
    }
}
