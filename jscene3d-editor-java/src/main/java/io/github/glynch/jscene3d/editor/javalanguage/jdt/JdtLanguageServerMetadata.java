/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/** Build-filtered identity and provenance of the bundled JDT LS distribution. */
record JdtLanguageServerMetadata(String version, String archive, String sha256, String source) {
    private static final String RESOURCE = "jdtls.properties";

    static JdtLanguageServerMetadata load() {
        Properties properties = new Properties();
        try (InputStream stream = Objects.requireNonNull(
                        JdtLanguageServerMetadata.class.getResourceAsStream(RESOURCE),
                        "Missing bundled JDT LS metadata");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not read bundled JDT LS metadata", failure);
        }
        return new JdtLanguageServerMetadata(
                required(properties, "version"),
                required(properties, "archive"),
                required(properties, "sha256"),
                required(properties, "source"));
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key, "").strip();
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing JDT LS metadata property: " + key);
        }
        return value;
    }
}
