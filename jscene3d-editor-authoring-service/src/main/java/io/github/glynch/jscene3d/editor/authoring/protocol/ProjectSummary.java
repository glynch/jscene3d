/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;

/**
 * Deliberately small wire projection of an opened authoring project.
 *
 * @param id stable project identity
 * @param name author-facing project name
 * @param version authored project version
 * @param root canonical project root
 * @param descriptor actual descriptor path loaded by Java
 * @param startupWorld startup-world identity and name
 * @param assetCounts coarse authored and projected asset counts
 */
public record ProjectSummary(
        String id,
        String name,
        String version,
        String root,
        String descriptor,
        WorldSummary startupWorld,
        AssetCounts assetCounts) {
    /** Validates one successful project summary. */
    public ProjectSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(startupWorld, "startupWorld");
        Objects.requireNonNull(assetCounts, "assetCounts");
    }

    /**
     * Startup-world identity exposed by the first authoring milestone.
     *
     * @param id stable startup-world asset identity
     * @param name author-facing startup-world name
     */
    public record WorldSummary(String id, String name) {
        /** Validates the startup-world summary. */
        public WorldSummary {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }
    }

    /**
     * Coarse project asset counts available without exposing the session model.
     *
     * @param authored discovered authored definition count
     * @param projected asset-browser projection count
     */
    public record AssetCounts(int authored, int projected) {
        /** Validates non-negative counts. */
        public AssetCounts {
            if (authored < 0 || projected < 0) {
                throw new IllegalArgumentException("Asset counts must be non-negative");
            }
        }
    }
}
