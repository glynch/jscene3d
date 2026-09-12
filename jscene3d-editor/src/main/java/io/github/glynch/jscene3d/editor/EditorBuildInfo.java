/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/** Immutable build and runtime identity displayed by the editor and used for project compatibility checks. */
record EditorBuildInfo(
        String version,
        String commit,
        String buildDate,
        String javaVersion,
        String javafxVersion,
        String operatingSystem) {
    private static final String DEVELOPMENT_BUILD = "Development build";
    private static final String RESOURCE = "editor-build.properties";
    private static final EditorBuildInfo CURRENT = readCurrent();

    EditorBuildInfo {
        version = requireValue(version, "version");
        commit = requireValue(commit, "commit");
        buildDate = requireValue(buildDate, "buildDate");
        javaVersion = requireValue(javaVersion, "javaVersion");
        javafxVersion = requireValue(javafxVersion, "javafxVersion");
        operatingSystem = requireValue(operatingSystem, "operatingSystem");
    }

    /** Returns build and runtime identity for this editor process. */
    static EditorBuildInfo current() {
        return CURRENT;
    }

    /** Returns the engine version against which opened projects are validated. */
    static String engineVersion() {
        return CURRENT.version();
    }

    /** Formats the complete copyable About diagnostic text. */
    String aboutText() {
        return String.join(
                System.lineSeparator(),
                "Version: " + version,
                "Commit:",
                commit,
                "Build date: " + buildDate,
                "Java: " + javaVersion,
                "JavaFX: " + javafxVersion,
                "OS: " + operatingSystem);
    }

    private static EditorBuildInfo readCurrent() {
        Properties properties = readProperties();
        String commit = optionalValue(properties, "buildCommit", DEVELOPMENT_BUILD);
        if (!commit.equals(DEVELOPMENT_BUILD)
                && Boolean.parseBoolean(optionalValue(properties, "buildDirty", "false"))) {
            commit += " (dirty)";
        }
        return new EditorBuildInfo(
                requiredValue(properties, "engineVersion"),
                commit,
                optionalValue(properties, "buildTime", DEVELOPMENT_BUILD),
                System.getProperty("java.runtime.version", System.getProperty("java.version")),
                optionalValue(properties, "javafxVersion", DEVELOPMENT_BUILD),
                String.join(
                        " ",
                        System.getProperty("os.name"),
                        System.getProperty("os.arch"),
                        System.getProperty("os.version")));
    }

    private static Properties readProperties() {
        try (InputStream input = EditorBuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Editor build metadata is missing: " + RESOURCE);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Editor build metadata cannot be read", exception);
        }
    }

    private static String requiredValue(Properties properties, String key) {
        String value = optionalValue(properties, key, "");
        if (value.isEmpty()) {
            throw new IllegalStateException("Editor " + key + " is missing from build metadata");
        }
        return value;
    }

    private static String optionalValue(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key, "").strip();
        return value.isEmpty() || value.contains("${") ? fallback : value;
    }

    private static String requireValue(String value, String name) {
        String checked = Objects.requireNonNull(value, name).strip();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
