/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.immutableCommandArguments;
import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.immutablePaths;
import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.normalizeAbsolute;
import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.requireLauncherName;
import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.requireNonBlank;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Immutable description of one relocatable desktop application-directory export.
 *
 * <p>The request identifies already-resolved runtime JARs rather than a Maven, Gradle, or editor dependency model.
 * The exporter owns the output layout, project-file selection, published-content filtering, and launcher generation.
 */
public final class ApplicationDirectoryRequest {
    private final String engineVersion;
    private final String launcherName;
    private final Path projectRoot;
    private final Path publishedContentRoot;
    private final List<Path> runtimeArtifacts;
    private final Path outputDirectory;
    private final List<String> jvmArguments;

    /** Builds one validated request from caller-supplied values. */
    private ApplicationDirectoryRequest(Builder builder) {
        engineVersion =
                requireNonBlank(Objects.requireNonNull(builder.engineVersion, "engineVersion"), "engineVersion");
        launcherName = requireLauncherName(Objects.requireNonNull(builder.launcherName, "launcherName"));
        projectRoot = normalizeAbsolute(Objects.requireNonNull(builder.projectRoot, "projectRoot"), "projectRoot");
        publishedContentRoot = normalizeAbsolute(
                Objects.requireNonNull(builder.publishedContentRoot, "publishedContentRoot"), "publishedContentRoot");
        runtimeArtifacts = immutablePaths(builder.runtimeArtifacts, "runtimeArtifacts");
        if (runtimeArtifacts.isEmpty()) {
            throw new IllegalArgumentException("runtimeArtifacts must not be empty");
        }
        outputDirectory = normalizeAbsolute(
                Objects.requireNonNull(builder.outputDirectory, "outputDirectory"), "outputDirectory");
        jvmArguments = immutableCommandArguments(builder.jvmArguments, "jvmArguments");
    }

    /**
     * Starts an application-directory request.
     *
     * @return empty request builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the exact engine version supplied to the packaged launcher.
     *
     * @return semantic engine version
     */
    public String engineVersion() {
        return engineVersion;
    }

    /**
     * Returns the portable base name used for generated launcher files.
     *
     * @return launcher name
     */
    public String launcherName() {
        return launcherName;
    }

    /**
     * Returns the authored project directory.
     *
     * @return normalized absolute project root
     */
    public Path projectRoot() {
        return projectRoot;
    }

    /**
     * Returns the completed import-publication directory.
     *
     * @return normalized absolute published-content root
     */
    public Path publishedContentRoot() {
        return publishedContentRoot;
    }

    /**
     * Returns the complete application runtime class path, including the game artifact.
     *
     * @return immutable normalized absolute artifact paths
     */
    public List<Path> runtimeArtifacts() {
        return runtimeArtifacts;
    }

    /**
     * Returns the application directory to replace after successful staging.
     *
     * @return normalized absolute output directory
     */
    public Path outputDirectory() {
        return outputDirectory;
    }

    /**
     * Returns JVM arguments placed before the packaged launcher's class path.
     *
     * @return immutable arguments in invocation order
     */
    public List<String> jvmArguments() {
        return jvmArguments;
    }

    /** Mutable construction convenience for the evolving export configuration. */
    public static final class Builder {
        private @Nullable String engineVersion;
        private @Nullable String launcherName;
        private @Nullable Path projectRoot;
        private @Nullable Path publishedContentRoot;
        private List<Path> runtimeArtifacts = List.of();
        private @Nullable Path outputDirectory;
        private List<String> jvmArguments = List.of();

        /** Prevents construction outside {@link ApplicationDirectoryRequest#builder()}. */
        private Builder() {}

        /**
         * Sets the exact engine version supplied to the packaged launcher.
         *
         * @param value semantic engine version
         * @return this builder
         */
        public Builder engineVersion(String value) {
            engineVersion = value;
            return this;
        }

        /**
         * Sets the portable base name for generated launcher files.
         *
         * @param value launcher name
         * @return this builder
         */
        public Builder launcherName(String value) {
            launcherName = value;
            return this;
        }

        /**
         * Sets the authored project directory.
         *
         * @param value project root
         * @return this builder
         */
        public Builder projectRoot(Path value) {
            projectRoot = value;
            return this;
        }

        /**
         * Sets the completed import-publication directory.
         *
         * @param value published-content root
         * @return this builder
         */
        public Builder publishedContentRoot(Path value) {
            publishedContentRoot = value;
            return this;
        }

        /**
         * Sets the complete runtime class path, including the game artifact.
         *
         * @param values runtime JAR paths
         * @return this builder
         */
        public Builder runtimeArtifacts(List<Path> values) {
            runtimeArtifacts = List.copyOf(values);
            return this;
        }

        /**
         * Sets the application directory replaced after successful staging.
         *
         * @param value output directory
         * @return this builder
         */
        public Builder outputDirectory(Path value) {
            outputDirectory = value;
            return this;
        }

        /**
         * Sets JVM arguments placed before the launcher's class path.
         *
         * @param values JVM arguments in invocation order
         * @return this builder
         */
        public Builder jvmArguments(List<String> values) {
            jvmArguments = List.copyOf(values);
            return this;
        }

        /**
         * Creates the validated immutable request.
         *
         * @return application-directory request
         */
        public ApplicationDirectoryRequest build() {
            return new ApplicationDirectoryRequest(this);
        }
    }
}
