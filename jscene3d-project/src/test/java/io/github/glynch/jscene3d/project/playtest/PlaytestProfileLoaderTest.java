/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.playtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the public local-profile loading boundary. */
final class PlaytestProfileLoaderTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsNamedProfileAndPortableParameters() throws IOException {
        writeProfiles("""
                {
                  "schemaVersion": 1,
                  "profiles": {
                    "moving-floor-34": {
                      "scene": "worlds/map01.world.json",
                      "parameters": {
                        "example.invulnerable": true,
                        "example.spawn": [43, 0.875, 21.5]
                      }
                    }
                  }
                }
                """);

        PlaytestProfile profile = new PlaytestProfileLoader().load(temporaryDirectory, "moving-floor-34");

        assertThat(profile.name()).isEqualTo("moving-floor-34");
        assertThat(profile.scene()).isEqualTo(Path.of("worlds/map01.world.json"));
        assertThat(profile.parameters().get("example.invulnerable")).isEqualTo(new ProjectValue.BooleanValue(true));
        assertThat(profile.parameters().get("example.spawn")).isInstanceOf(ProjectValue.ArrayValue.class);
    }

    @Test
    void rejectsUnknownProfile() throws IOException {
        writeProfiles("""
                {"schemaVersion": 1, "profiles": {}}
                """);
        PlaytestProfileLoader loader = new PlaytestProfileLoader();

        assertThatThrownBy(() -> loader.load(temporaryDirectory, "missing"))
                .isInstanceOf(PlaytestProfileException.class)
                .hasMessage("playtest profile is not defined: missing");
    }

    /** Writes the conventional local playtest document. */
    private void writeProfiles(String document) throws IOException {
        Path source = temporaryDirectory.resolve(PlaytestProfileLoader.PROFILES_PATH);
        Files.createDirectories(source.getParent());
        Files.writeString(source, document);
    }
}
