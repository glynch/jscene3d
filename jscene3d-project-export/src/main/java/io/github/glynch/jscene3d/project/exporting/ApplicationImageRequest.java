/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.normalizeAbsolute;
import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.requireNonBlank;

import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Immutable request to wrap one completed application directory in a native image for the current host. */
public final class ApplicationImageRequest {
    private final Path applicationDirectory;
    private final String applicationVersion;
    private final Path outputDirectory;

    /** Builds one validated request from caller-supplied values. */
    private ApplicationImageRequest(Builder builder) {
        applicationDirectory = normalizeAbsolute(
                Objects.requireNonNull(builder.applicationDirectory, "applicationDirectory"), "applicationDirectory");
        applicationVersion = requireNonBlank(
                Objects.requireNonNull(builder.applicationVersion, "applicationVersion"), "applicationVersion");
        outputDirectory = normalizeAbsolute(
                Objects.requireNonNull(builder.outputDirectory, "outputDirectory"), "outputDirectory");
    }

    /**
     * Starts a native application-image request.
     *
     * @return empty request builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the completed application directory to package.
     *
     * @return normalized absolute application-directory root
     */
    public Path applicationDirectory() {
        return applicationDirectory;
    }

    /**
     * Returns the platform-compatible native application version.
     *
     * @return application version passed to the native packager
     */
    public String applicationVersion() {
        return applicationVersion;
    }

    /**
     * Returns the directory in which the platform image will be installed.
     *
     * @return normalized absolute destination directory
     */
    public Path outputDirectory() {
        return outputDirectory;
    }

    /** Mutable construction convenience for native application-image configuration. */
    public static final class Builder {
        private @Nullable Path applicationDirectory;
        private @Nullable String applicationVersion;
        private @Nullable Path outputDirectory;

        /** Prevents construction outside {@link ApplicationImageRequest#builder()}. */
        private Builder() {}

        /**
         * Sets the completed application directory to package.
         *
         * @param value application-directory root
         * @return this builder
         */
        public Builder applicationDirectory(Path value) {
            applicationDirectory = value;
            return this;
        }

        /**
         * Sets the platform-compatible native application version.
         *
         * @param value application version
         * @return this builder
         */
        public Builder applicationVersion(String value) {
            applicationVersion = value;
            return this;
        }

        /**
         * Sets the directory in which the platform image will be installed.
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
         * @return native application-image request
         */
        public ApplicationImageRequest build() {
            return new ApplicationImageRequest(this);
        }
    }
}
