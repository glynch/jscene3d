/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import static io.github.glynch.jscene3d.project.exporting.internal.Preconditions.requireNonBlank;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/** Deterministic metadata carried by every engine-produced native application image.
 *
 * @param applicationName platform application name
 * @param applicationVersion platform-native application version
 */
public record ApplicationImageMetadata(String applicationName, String applicationVersion) {
    /** Application-image content-relative metadata path. */
    public static final Path PATH = Path.of("application-image.properties");

    private static final String FORMAT_VERSION = "1";
    private static final String FORMAT_VERSION_PROPERTY = "format-version";
    private static final String APPLICATION_NAME_PROPERTY = "application-name";
    private static final String APPLICATION_VERSION_PROPERTY = "application-version";

    /** Validates application-image metadata. */
    public ApplicationImageMetadata {
        applicationName = requireNonBlank(applicationName, "applicationName");
        applicationVersion = requireNonBlank(applicationVersion, "applicationVersion");
    }

    /**
     * Writes metadata into one jpackage input directory.
     *
     * @param applicationContent jpackage input directory
     * @throws IOException when metadata cannot be written
     */
    public void write(Path applicationContent) throws IOException {
        Path destination =
                Objects.requireNonNull(applicationContent, "applicationContent").resolve(PATH);
        String text = FORMAT_VERSION_PROPERTY
                + '='
                + FORMAT_VERSION
                + '\n'
                + APPLICATION_NAME_PROPERTY
                + '='
                + escape(applicationName)
                + '\n'
                + APPLICATION_VERSION_PROPERTY
                + '='
                + escape(applicationVersion)
                + '\n';
        Files.writeString(destination, text, StandardCharsets.UTF_8);
    }

    /**
     * Reads metadata from one installed application-content directory.
     *
     * @param applicationContent installed application-content directory
     * @return validated application-image metadata
     * @throws IOException when metadata cannot be read
     */
    public static ApplicationImageMetadata read(Path applicationContent) throws IOException {
        Path source =
                Objects.requireNonNull(applicationContent, "applicationContent").resolve(PATH);
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String formatVersion = requireProperty(properties, FORMAT_VERSION_PROPERTY, source);
        if (!formatVersion.equals(FORMAT_VERSION)) {
            throw new IllegalArgumentException(
                    "unsupported application-image metadata format in " + source + ": " + formatVersion);
        }
        return new ApplicationImageMetadata(
                requireProperty(properties, APPLICATION_NAME_PROPERTY, source),
                requireProperty(properties, APPLICATION_VERSION_PROPERTY, source));
    }

    /** Returns one required non-blank metadata property. */
    private static String requireProperty(Properties properties, String name, Path source) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "required application-image metadata property is absent or blank in " + source + ": " + name);
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
