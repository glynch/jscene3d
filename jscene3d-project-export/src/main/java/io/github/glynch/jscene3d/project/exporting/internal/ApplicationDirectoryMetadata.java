/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.requireLauncherName;
import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.requireNonBlank;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Deterministic machine-readable launch metadata stored in every application directory.
 *
 * @param engineVersion exact engine compatibility version
 * @param launcherName portable application-directory launcher name
 * @param jvmArguments immutable JVM arguments required by the application
 */
public record ApplicationDirectoryMetadata(String engineVersion, String launcherName, List<String> jvmArguments) {
    /** Application-directory-relative metadata path. */
    public static final Path PATH = Path.of("application-directory.properties");

    private static final String FORMAT_VERSION = "1";
    private static final String FORMAT_VERSION_PROPERTY = "format-version";
    private static final String ENGINE_VERSION_PROPERTY = "engine-version";
    private static final String LAUNCHER_NAME_PROPERTY = "launcher-name";
    private static final String JVM_ARGUMENT_COUNT_PROPERTY = "jvm-argument-count";
    private static final String JVM_ARGUMENT_PROPERTY_PREFIX = "jvm-argument.";

    /** Validates and copies application-directory metadata. */
    public ApplicationDirectoryMetadata {
        engineVersion = requireNonBlank(engineVersion, "engineVersion");
        launcherName = requireLauncherName(launcherName);
        jvmArguments = List.copyOf(jvmArguments);
    }

    /**
     * Writes launch metadata below an application-directory staging root.
     *
     * @param root application-directory staging root
     * @throws IOException when metadata cannot be written
     */
    public void write(Path root) throws IOException {
        Path destination = Objects.requireNonNull(root, "root").resolve(PATH);
        StringBuilder text = new StringBuilder()
                .append(FORMAT_VERSION_PROPERTY)
                .append('=')
                .append(FORMAT_VERSION)
                .append('\n')
                .append(ENGINE_VERSION_PROPERTY)
                .append('=')
                .append(escape(engineVersion))
                .append('\n')
                .append(LAUNCHER_NAME_PROPERTY)
                .append('=')
                .append(escape(launcherName))
                .append('\n')
                .append(JVM_ARGUMENT_COUNT_PROPERTY)
                .append('=')
                .append(jvmArguments.size())
                .append('\n');
        for (int index = 0; index < jvmArguments.size(); index++) {
            text.append(JVM_ARGUMENT_PROPERTY_PREFIX)
                    .append(index)
                    .append('=')
                    .append(escape(jvmArguments.get(index)))
                    .append('\n');
        }
        Files.writeString(destination, text, StandardCharsets.UTF_8);
    }

    /**
     * Reads and validates launch metadata from an assembled application directory.
     *
     * @param root application-directory root
     * @return validated launch metadata
     * @throws IOException when metadata cannot be read
     */
    public static ApplicationDirectoryMetadata read(Path root) throws IOException {
        Path source = Objects.requireNonNull(root, "root").resolve(PATH);
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        requireFormatVersion(properties, source);
        int argumentCount = requireArgumentCount(properties, source);
        List<String> arguments = new ArrayList<>(argumentCount);
        for (int index = 0; index < argumentCount; index++) {
            arguments.add(requirePresentProperty(properties, JVM_ARGUMENT_PROPERTY_PREFIX + index, source));
        }
        return new ApplicationDirectoryMetadata(
                requireProperty(properties, ENGINE_VERSION_PROPERTY, source),
                requireProperty(properties, LAUNCHER_NAME_PROPERTY, source),
                arguments);
    }

    /** Requires the one supported metadata format. */
    private static void requireFormatVersion(Properties properties, Path source) {
        String version = requireProperty(properties, FORMAT_VERSION_PROPERTY, source);
        if (!version.equals(FORMAT_VERSION)) {
            throw new IllegalArgumentException(
                    "unsupported application-directory metadata format in " + source + ": " + version);
        }
    }

    /** Returns the declared non-negative JVM argument count. */
    private static int requireArgumentCount(Properties properties, Path source) {
        String value = requireProperty(properties, JVM_ARGUMENT_COUNT_PROPERTY, source);
        try {
            int count = Integer.parseInt(value);
            if (count < 0) {
                throw new IllegalArgumentException(
                        "invalid JVM argument count in application-directory metadata " + source + ": " + value);
            }
            return count;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid JVM argument count in application-directory metadata " + source + ": " + value, exception);
        }
    }

    /** Returns one required non-blank property. */
    private static String requireProperty(Properties properties, String name, Path source) {
        String value = requirePresentProperty(properties, name, source);
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "required application-directory metadata property is absent or blank in " + source + ": " + name);
        }
        return value;
    }

    /** Returns one required property whose value may deliberately be empty. */
    private static String requirePresentProperty(Properties properties, String name, Path source) {
        String value = properties.getProperty(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "required application-directory metadata property is absent in " + source + ": " + name);
        }
        return value;
    }

    /** Escapes one value for deterministic UTF-8 Java-properties output. */
    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\' || character == '=' || character == ':' || character == '#' || character == '!') {
                escaped.append('\\');
            }
            escaped.append(character);
        }
        return escaped.toString();
    }
}
