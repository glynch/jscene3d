/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import java.util.Arrays;
import java.util.Optional;

/** Kinds of authored assets understood by the initial entity-component project format. */
public enum AssetKind {
    /** Reusable single-root entity hierarchy. */
    ENTITY_DEFINITION("entity-definition", ".entity.json"),
    /** Authored world containing local entities and reusable-definition placements. */
    WORLD_DEFINITION("world-definition", ".world.json");

    private final String serializedName;
    private final String fileSuffix;

    /** Stores one stable serialized kind and its discoverable filename suffix. */
    AssetKind(String serializedName, String fileSuffix) {
        this.serializedName = serializedName;
        this.fileSuffix = fileSuffix;
    }

    /**
     * Returns the stable JSON value.
     *
     * @return serialized asset kind
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Returns the filename suffix scanned by the catalog.
     *
     * @return discoverable filename suffix
     */
    public String fileSuffix() {
        return fileSuffix;
    }

    /**
     * Finds a kind by its stable JSON value.
     *
     * @param value serialized value
     * @return matching kind, when supported
     */
    public static Optional<AssetKind> fromSerializedName(String value) {
        return Arrays.stream(values())
                .filter(kind -> kind.serializedName.equals(value))
                .findFirst();
    }

    /** Returns the stable JSON value. */
    @Override
    public String toString() {
        return serializedName;
    }
}
