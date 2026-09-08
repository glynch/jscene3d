/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Reads build values embedded in the editor artifact. */
final class EditorBuildInfo {
    private static final String RESOURCE = "editor-build.properties";

    /** Prevents construction of this build metadata reader. */
    private EditorBuildInfo() {
        throw new AssertionError("EditorBuildInfo cannot be instantiated");
    }

    /** Returns the engine version against which opened projects are validated. */
    static String engineVersion() {
        try (InputStream input = EditorBuildInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Editor build metadata is missing: " + RESOURCE);
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            String version = properties.getProperty("engineVersion", "").strip();
            if (version.isEmpty() || version.contains("${")) {
                throw new IllegalStateException("Editor engine version is missing from build metadata");
            }
            return version;
        } catch (IOException exception) {
            throw new IllegalStateException("Editor build metadata cannot be read", exception);
        }
    }
}
