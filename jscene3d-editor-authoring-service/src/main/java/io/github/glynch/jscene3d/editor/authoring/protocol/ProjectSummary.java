/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
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
 * @param catalog semantic definition catalog derived by Java from the authoritative asset catalog
 */
public record ProjectSummary(
        String id,
        String name,
        String version,
        String root,
        String descriptor,
        @Nullable SceneSummary mainScene,
        AssetCounts assetCounts,
        ProjectCatalog catalog) {
    /** Validates one successful project summary. */
    public ProjectSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(assetCounts, "assetCounts");
        Objects.requireNonNull(catalog, "catalog");
        List<CatalogEntry> selected =
                catalog.scenes().stream().filter(CatalogEntry::mainScene).toList();
        if (mainScene == null && !selected.isEmpty()) {
            throw new IllegalArgumentException("catalog cannot identify a Main Scene when none is configured");
        }
        if (mainScene != null
                && (selected.size() != 1 || !selected.getFirst().id().equals(mainScene.id()))) {
            throw new IllegalArgumentException("catalog Main Scene must match stable configured identity");
        }
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

    /**
     * Semantic definition groups exposed by the Project view.
     *
     * @param scenes Scene definitions
     * @param entityDefinitions reusable Entity definitions
     */
    public record ProjectCatalog(List<CatalogEntry> scenes, List<CatalogEntry> entityDefinitions) {
        /** Stores immutable semantic groups and enforces group-specific invariants. */
        public ProjectCatalog {
            scenes = List.copyOf(Objects.requireNonNull(scenes, "scenes"));
            entityDefinitions = List.copyOf(Objects.requireNonNull(entityDefinitions, "entityDefinitions"));
            if (scenes.stream().filter(CatalogEntry::mainScene).count() > 1) {
                throw new IllegalArgumentException("catalog cannot contain multiple Main Scenes");
            }
            if (entityDefinitions.stream().anyMatch(CatalogEntry::mainScene)) {
                throw new IllegalArgumentException("entity definitions cannot be the Main Scene");
            }
        }
    }

    /**
     * One Java-classified semantic definition entry.
     *
     * @param id stable asset identity
     * @param name author-facing definition name
     * @param source authoritative source URI
     * @param origin definition origin, currently {@code authored} or {@code generated}
     * @param editable whether the definition may be edited through the authoring service
     * @param mainScene whether this Scene is the Project's configured Main Scene
     */
    public record CatalogEntry(
            String id, String name, String source, String origin, boolean editable, boolean mainScene) {
        /** Validates the closed semantic catalog vocabulary. */
        public CatalogEntry {
            id = requireNonBlank(id, "id");
            name = requireNonBlank(name, "name");
            source = requireNonBlank(source, "source");
            origin = requireNonBlank(origin, "origin");
            if (!origin.equals("authored") && !origin.equals("generated")) {
                throw new IllegalArgumentException("origin must be authored or generated");
            }
            if (origin.equals("generated") && editable) {
                throw new IllegalArgumentException("generated catalog entries cannot be editable");
            }
        }
    }

    /** Requires non-blank transport text. */
    private static String requireNonBlank(String value, String name) {
        String valid = Objects.requireNonNull(value, name);
        if (valid.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return valid;
    }
}
