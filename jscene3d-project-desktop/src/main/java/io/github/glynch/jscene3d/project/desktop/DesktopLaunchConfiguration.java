/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** Resolved values needed to start one packaged desktop project. */
final class DesktopLaunchConfiguration {
    private static final int ARGUMENT_COUNT = 3;

    private final String engineVersion;
    private final Path projectDirectory;
    private final Path contentDirectory;
    private final Optional<String> playtestProfile;

    /** Stores validated command-line or launch-property values. */
    private DesktopLaunchConfiguration(
            String engineVersion, Path projectDirectory, Path contentDirectory, Optional<String> playtestProfile) {
        this.engineVersion = requireNonBlank(engineVersion, "engineVersion");
        this.projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory");
        this.contentDirectory = Objects.requireNonNull(contentDirectory, "contentDirectory");
        this.playtestProfile = Objects.requireNonNull(playtestProfile, "playtestProfile");
    }

    /**
     * Resolves an explicit three-argument launch or a zero-argument packaged launch.
     *
     * @param arguments command-line launch values
     * @param properties system properties used by a packaged launcher
     * @return resolved launch configuration
     * @throws IllegalArgumentException when the launch form or a required value is invalid
     */
    static DesktopLaunchConfiguration resolve(String[] arguments, Properties properties) {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(properties, "properties");
        return switch (arguments.length) {
            case 0 -> fromProperties(properties);
            case ARGUMENT_COUNT -> fromArguments(arguments, properties);
            default ->
                throw new IllegalArgumentException(
                        "expected either no arguments with packaged launch properties or engine-version, "
                                + "project-directory, and published-content-directory arguments");
        };
    }

    /** Returns the exact engine version selected by the packaging layer. */
    String engineVersion() {
        return engineVersion;
    }

    /** Returns the authored project directory selected by the packaging layer. */
    Path projectDirectory() {
        return projectDirectory;
    }

    /** Returns the published-content directory selected by the packaging layer. */
    Path contentDirectory() {
        return contentDirectory;
    }

    /** Returns the optional local-development profile selected outside the public game interface. */
    Optional<String> playtestProfile() {
        return playtestProfile;
    }

    /** Resolves the ordered command-line form. */
    private static DesktopLaunchConfiguration fromArguments(String[] arguments, Properties properties) {
        return new DesktopLaunchConfiguration(
                arguments[0],
                Path.of(arguments[1]),
                Path.of(arguments[2]),
                optionalProperty(properties, DesktopProjectLauncher.PLAYTEST_PROFILE_PROPERTY));
    }

    /** Resolves the native-packager property form. */
    private static DesktopLaunchConfiguration fromProperties(Properties properties) {
        return new DesktopLaunchConfiguration(
                requireProperty(properties, DesktopProjectLauncher.ENGINE_VERSION_PROPERTY),
                Path.of(requireProperty(properties, DesktopProjectLauncher.PROJECT_DIRECTORY_PROPERTY)),
                Path.of(requireProperty(properties, DesktopProjectLauncher.CONTENT_DIRECTORY_PROPERTY)),
                optionalProperty(properties, DesktopProjectLauncher.PLAYTEST_PROFILE_PROPERTY));
    }

    /** Returns one optional trimmed launch property. */
    private static Optional<String> optionalProperty(Properties properties, String name) {
        return Optional.ofNullable(properties.getProperty(name))
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }

    /** Returns one required non-blank launch property. */
    private static String requireProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("required packaged launch property is absent or blank: " + name);
        }
        return value;
    }

    /** Returns one required non-blank text value. */
    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
