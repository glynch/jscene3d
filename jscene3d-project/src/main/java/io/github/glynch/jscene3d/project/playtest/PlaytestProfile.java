/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.playtest;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Named development-only project launch configuration.
 *
 * @param name stable profile name
 * @param scene project-relative world-definition path
 * @param parameters application-defined portable launch parameters
 */
public record PlaytestProfile(String name, Path scene, Map<String, ProjectValue> parameters) {
    /** Copies and validates the authored profile. */
    public PlaytestProfile {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        scene = Objects.requireNonNull(scene, "scene").normalize();
        if (scene.isAbsolute() || scene.toString().isBlank() || scene.startsWith("..")) {
            throw new IllegalArgumentException("scene must be a project-relative path");
        }
        parameters = Map.copyOf(parameters);
    }
}
