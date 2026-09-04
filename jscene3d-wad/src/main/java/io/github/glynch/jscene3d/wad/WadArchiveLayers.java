/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import io.github.glynch.jscene3d.wad.internal.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit ordered view of independently validated WAD archives.
 *
 * <p>Earlier archives have lower precedence and later archives have higher precedence. The class applies no Doom or
 * game-specific interpretation to names, kinds, namespaces, or lump contents.
 */
public final class WadArchiveLayers {
    private final List<WadArchive> layers;
    private final List<WadLumpReference> lumps;

    /** Stores one non-empty immutable layer set. */
    private WadArchiveLayers(List<WadArchive> layers) {
        this.layers = List.copyOf(layers);
        if (this.layers.isEmpty()) {
            throw new IllegalArgumentException("layers must not be empty");
        }
        this.lumps = flatten(this.layers);
    }

    /**
     * Creates an explicit low-to-high precedence archive view.
     *
     * @param layers independently validated archives in precedence order
     * @return immutable layered view
     */
    public static WadArchiveLayers of(List<WadArchive> layers) {
        return new WadArchiveLayers(Objects.requireNonNull(layers, "layers"));
    }

    /**
     * Returns archives from lowest to highest precedence.
     *
     * @return immutable ordered archives
     */
    public List<WadArchive> layers() {
        return layers;
    }

    /**
     * Returns all lumps ordered first by layer and then by directory position.
     *
     * @return immutable flattened references
     */
    public List<WadLumpReference> lumps() {
        return lumps;
    }

    /**
     * Returns every occurrence of one case-insensitive name in precedence order.
     *
     * @param name printable ASCII WAD name of at most eight characters
     * @return immutable matching references
     */
    public List<WadLumpReference> lumpsNamed(String name) {
        String normalizedName = Preconditions.requireLumpName(name, "name");
        return lumps.stream()
                .filter(reference -> reference.lump().name().equals(normalizedName))
                .toList();
    }

    /**
     * Resolves the highest-precedence occurrence of one case-insensitive name.
     *
     * @param name printable ASCII WAD name of at most eight characters
     * @return highest-precedence occurrence when present
     */
    public Optional<WadLumpReference> lastLumpNamed(String name) {
        String normalizedName = Preconditions.requireLumpName(name, "name");
        for (int index = lumps.size() - 1; index >= 0; index--) {
            WadLumpReference reference = lumps.get(index);
            if (reference.lump().name().equals(normalizedName)) {
                return Optional.of(reference);
            }
        }
        return Optional.empty();
    }

    /** Flattens archive directories while retaining each source and layer identity. */
    private static List<WadLumpReference> flatten(List<WadArchive> layers) {
        List<WadLumpReference> flattened = new ArrayList<>();
        for (int layer = 0; layer < layers.size(); layer++) {
            WadArchive archive = layers.get(layer);
            for (WadLump lump : archive.lumps()) {
                flattened.add(new WadLumpReference(layer, archive, lump));
            }
        }
        return List.copyOf(flattened);
    }
}
