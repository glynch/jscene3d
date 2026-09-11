/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.icon;

import io.github.glynch.jscene3d.editor.view.EditorIconId;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Resolves semantic icon identities through one validated, resource-backed icon theme. */
final class JavaFxIconRegistry {
    private static final String BUILT_IN_THEME = "editor-icons.properties";
    private static final String FALLBACK = "fallback";
    private static final String PATH_SUFFIX = ".path";
    private static final String TONE_SUFFIX = ".tone";

    private final Map<EditorIconId, JavaFxIconGlyph> glyphs;
    private final JavaFxIconGlyph fallback;

    private JavaFxIconRegistry(Map<EditorIconId, JavaFxIconGlyph> glyphs, JavaFxIconGlyph fallback) {
        this.glyphs = Map.copyOf(glyphs);
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    /** Loads and validates the icon theme distributed with the editor. */
    static JavaFxIconRegistry builtIn() {
        return load(JavaFxIconRegistry.class, BUILT_IN_THEME);
    }

    /** Resolves an icon identity, returning the theme's generic glyph when it is unknown. */
    JavaFxIconGlyph resolve(EditorIconId id) {
        return glyphs.getOrDefault(Objects.requireNonNull(id, "id"), fallback);
    }

    static JavaFxIconRegistry load(Class<?> resourceAnchor, String resourceName) {
        Class<?> anchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        String name = Objects.requireNonNull(resourceName, "resourceName");
        Properties properties = new Properties();
        try (InputStream input = anchor.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("icon theme resource was not found: " + name);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("unable to load icon theme resource: " + name, exception);
        }

        JavaFxIconGlyph fallback = glyph(properties, FALLBACK, name);
        Map<EditorIconId, JavaFxIconGlyph> glyphs = new HashMap<>();
        properties.stringPropertyNames().stream()
                .filter(key -> key.endsWith(PATH_SUFFIX))
                .map(key -> key.substring(0, key.length() - PATH_SUFFIX.length()))
                .filter(key -> !key.equals(FALLBACK))
                .forEach(key -> glyphs.put(new EditorIconId(key), glyph(properties, key, name)));
        return new JavaFxIconRegistry(glyphs, fallback);
    }

    private static JavaFxIconGlyph glyph(Properties properties, String key, String resourceName) {
        String path = required(properties, key + PATH_SUFFIX, resourceName);
        String toneName = properties.getProperty(key + TONE_SUFFIX, JavaFxIconGlyph.Tone.PRIMARY.name());
        try {
            return new JavaFxIconGlyph(path, JavaFxIconGlyph.Tone.valueOf(toneName));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "invalid icon tone in " + resourceName + ": " + key + TONE_SUFFIX + "=" + toneName, exception);
        }
    }

    private static String required(Properties properties, String key, String resourceName) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing icon theme property in " + resourceName + ": " + key);
        }
        return value;
    }
}
