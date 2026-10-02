/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Deliberately small wire projection of an opened authoring project.
 *
 * @param id stable project identity
 * @param name author-facing project name
 * @param version authored project version
 * @param root canonical project root
 * @param descriptor actual descriptor path loaded by Java
 * @param mainScene optional Main Scene identity and name
 * @param assetCounts coarse authored and projected asset counts
 */
public record ProjectSummary(
        String id,
        String name,
        String version,
        String root,
        String descriptor,
        @Nullable SceneSummary mainScene,
        AssetCounts assetCounts) {
    /** Validates one successful project summary. */
    public ProjectSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(assetCounts, "assetCounts");
    }

    /**
     * Optional Main Scene identity configured for Run Project.
     *
     * @param id stable Scene asset identity
     * @param name author-facing Scene name
     */
    public record SceneSummary(String id, String name) {
        /** Validates the Main Scene summary. */
        public SceneSummary {
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
