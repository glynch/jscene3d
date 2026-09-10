/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.playtest;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.internal.ProjectJsonReader;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Loads named local playtest profiles without making them part of exported project content. */
public final class PlaytestProfileLoader {
    /** Project-relative location reserved for local playtest profiles. */
    public static final Path PROFILES_PATH = Path.of("playtest/profiles.json");

    /** Supported profile document version. */
    private static final int SCHEMA_VERSION = 1;

    /** Creates a stateless profile loader. */
    public PlaytestProfileLoader() {
        // Stateless service.
    }

    /**
     * Loads one named profile from the project's local playtest document.
     *
     * @param projectRoot project directory
     * @param profileName selected profile name
     * @return validated playtest profile
     */
    public PlaytestProfile load(Path projectRoot, String profileName) {
        Path source = Objects.requireNonNull(projectRoot, "projectRoot")
                .resolve(PROFILES_PATH)
                .normalize();
        String validName = requireText(profileName, "profile name");
        try (InputStream input = Files.newInputStream(source)) {
            JsonNode root = requiredObject(ProjectJsonReader.strict().readTree(input), "profile document");
            requireVersion(root);
            JsonNode profiles = requiredObject(root.get("profiles"), "profiles");
            JsonNode rawProfile = profiles.get(validName);
            if (rawProfile == null) {
                throw new PlaytestProfileException("playtest profile is not defined: " + validName);
            }
            return decode(validName, requiredObject(rawProfile, "profile " + validName));
        } catch (PlaytestProfileException failure) {
            throw failure;
        } catch (IOException failure) {
            throw new PlaytestProfileException("cannot load playtest profiles: " + source, failure);
        }
    }

    /** Decodes one selected profile after document-level validation. */
    private static PlaytestProfile decode(String name, JsonNode raw) {
        String scene = requireText(raw.path("scene").textValue(), "scene");
        JsonNode rawParameters = raw.get("parameters");
        Map<String, ProjectValue> parameters = new LinkedHashMap<>();
        if (rawParameters != null) {
            requiredObject(rawParameters, "parameters")
                    .properties()
                    .forEach(entry -> parameters.put(
                            entry.getKey(), ProjectValueDecoder.plain().decode(entry.getValue(), "/parameters")));
        }
        return new PlaytestProfile(name, Path.of(scene), parameters);
    }

    /** Verifies the only supported document version. */
    private static void requireVersion(JsonNode root) {
        JsonNode version = root.get("schemaVersion");
        if (version == null || !version.isIntegralNumber() || version.intValue() != SCHEMA_VERSION) {
            throw new PlaytestProfileException("playtest schemaVersion must be " + SCHEMA_VERSION);
        }
    }

    /** Returns one required JSON object. */
    private static JsonNode requiredObject(JsonNode value, String description) {
        if (value == null || !value.isObject()) {
            throw new PlaytestProfileException(description + " must be an object");
        }
        return value;
    }

    /** Returns one required nonblank string. */
    private static String requireText(String value, String description) {
        if (value == null || value.isBlank()) {
            throw new PlaytestProfileException(description + " must be a nonblank string");
        }
        return value;
    }
}
