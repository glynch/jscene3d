/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.imports;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePortableLocator;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireRegisteredTypeId;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, structurally validated definition of one deterministic source import. */
public final class ImportDefinition {
    private final Path source;
    private final String id;
    private final GameProject.AssetSource asset;
    private final String importer;
    private final List<String> selection;
    private final Map<String, ProjectValue> settings;
    private final Map<String, Map<String, ProjectValue>> itemSettings;

    /**
     * Creates one import definition.
     *
     * @param source normalized absolute definition-document path
     * @param id stable project-local import identity
     * @param asset authoritative source asset
     * @param importer extension-qualified importer identity
     * @param selection ordered, unique source-item identities
     * @param settings importer-wide settings
     * @param itemSettings settings indexed by source-item identity
     */
    public ImportDefinition(
            Path source,
            String id,
            GameProject.AssetSource asset,
            String importer,
            List<String> selection,
            Map<String, ProjectValue> settings,
            Map<String, Map<String, ProjectValue>> itemSettings) {
        this.source = requireNormalizedAbsolute(source, "source");
        this.id = requireLocalId(id, "id");
        this.asset = Objects.requireNonNull(asset, "asset");
        this.importer = requireRegisteredTypeId(importer, "importer");
        this.selection = copySelection(selection);
        this.settings = immutableProjectValues(settings, "settings");
        this.itemSettings = copyItemSettings(itemSettings);
    }

    /**
     * Returns the import-definition document path.
     *
     * @return normalized absolute document path
     */
    public Path source() {
        return source;
    }

    /**
     * Returns the stable project-local import identity.
     *
     * @return import identity
     */
    public String id() {
        return id;
    }

    /**
     * Returns the authoritative source asset.
     *
     * @return project asset
     */
    public GameProject.AssetSource asset() {
        return asset;
    }

    /**
     * Returns the extension-qualified importer identity.
     *
     * @return importer identity
     */
    public String importer() {
        return importer;
    }

    /**
     * Returns selected source-item identities in authored order.
     *
     * @return immutable, duplicate-free selection
     */
    public List<String> selection() {
        return selection;
    }

    /**
     * Returns importer-wide settings in authored order.
     *
     * @return immutable settings
     */
    public Map<String, ProjectValue> settings() {
        return settings;
    }

    /**
     * Returns settings indexed by source-item identity in authored order.
     *
     * @return immutable per-item settings
     */
    public Map<String, Map<String, ProjectValue>> itemSettings() {
        return itemSettings;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ImportDefinition definition
                && source.equals(definition.source)
                && id.equals(definition.id)
                && asset.equals(definition.asset)
                && importer.equals(definition.importer)
                && selection.equals(definition.selection)
                && settings.equals(definition.settings)
                && itemSettings.equals(definition.itemSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id, asset, importer, selection, settings, itemSettings);
    }

    @Override
    public String toString() {
        return "ImportDefinition[source=" + source + ", id=" + id + ", asset=" + asset + ", importer=" + importer
                + ", selection=" + selection + ", settings=" + settings + ", itemSettings=" + itemSettings + ']';
    }

    /** Copies and validates ordered source-item identities. */
    private static List<String> copySelection(List<String> selection) {
        Objects.requireNonNull(selection, "selection");
        List<String> copied = new ArrayList<>(selection.size());
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String identity : selection) {
            String validIdentity = requirePortableLocator(identity, "selection entry");
            if (!unique.add(validIdentity)) {
                throw new IllegalArgumentException("selection contains a duplicate identity: " + validIdentity);
            }
            copied.add(validIdentity);
        }
        return List.copyOf(copied);
    }

    /** Copies per-item settings while preserving source order. */
    private static Map<String, Map<String, ProjectValue>> copyItemSettings(
            Map<String, Map<String, ProjectValue>> itemSettings) {
        Objects.requireNonNull(itemSettings, "itemSettings");
        Map<String, Map<String, ProjectValue>> copied = new LinkedHashMap<>();
        itemSettings.forEach((identity, values) -> copied.put(
                requirePortableLocator(identity, "itemSettings key"),
                immutableProjectValues(values, "itemSettings[" + identity + "]")));
        return Collections.unmodifiableMap(copied);
    }
}
