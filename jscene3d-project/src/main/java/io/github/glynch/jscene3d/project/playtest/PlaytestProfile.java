/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.playtest;

import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.scene.SceneDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.Objects;

/** Named development-only project launch configuration.
 *
 * @param name stable profile name
 * @param scene stable selected-scene reference
 * @param parameters application-defined portable launch parameters
 */
public record PlaytestProfile(String name, AssetRef<SceneDefinition> scene, Map<String, ProjectValue> parameters) {
    /** Copies and validates the authored profile. */
    public PlaytestProfile {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(scene, "scene");
        parameters = Map.copyOf(parameters);
    }
}
