/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import java.util.Objects;

/** Java-owned inputs for one isolated project-world renderer process.
 *
 * @param projectGeneration active authoring project generation
 * @param projectId stable project identity
 * @param projectName project display name
 * @param projectRoot normalized absolute project root
 * @param publishedContentRoot normalized absolute published-content root
 * @param engineVersion running JScene3D engine version
 * @param worldAssetId stable startup-world identity
 * @param worldName startup-world display name
 * @param runtimeArtifacts normalized absolute runtime artifacts configured for isolated execution
 */
public record ViewportLaunchSpecification(
        long projectGeneration,
        String projectId,
        String projectName,
        String projectRoot,
        String publishedContentRoot,
        String engineVersion,
        String worldAssetId,
        String worldName,
        List<String> runtimeArtifacts) {
    /** Validates the closed renderer-launch contract. */
    public ViewportLaunchSpecification {
        if (projectGeneration <= 0) {
            throw new IllegalArgumentException("projectGeneration must be positive");
        }
        requireNonBlank(projectId, "projectId");
        requireNonBlank(projectName, "projectName");
        requireNonBlank(projectRoot, "projectRoot");
        requireNonBlank(publishedContentRoot, "publishedContentRoot");
        requireNonBlank(engineVersion, "engineVersion");
        requireNonBlank(worldAssetId, "worldAssetId");
        requireNonBlank(worldName, "worldName");
        runtimeArtifacts = List.copyOf(runtimeArtifacts);
        if (runtimeArtifacts.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("runtimeArtifacts must contain only non-blank paths");
        }
    }

    private static void requireNonBlank(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
