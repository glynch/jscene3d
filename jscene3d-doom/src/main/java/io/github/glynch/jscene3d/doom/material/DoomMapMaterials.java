package io.github.glynch.jscene3d.doom.material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable wall textures and flats referenced by one decoded map.
 *
 * @param mapName source map name
 * @param wallTextures referenced wall textures by normalized name
 * @param flats referenced flats by normalized name
 */
public record DoomMapMaterials(
        String mapName, Map<String, DoomMaterial> wallTextures, Map<String, DoomMaterial> flats) {
    /** Creates an immutable material set while preserving deterministic iteration order. */
    public DoomMapMaterials {
        Objects.requireNonNull(mapName, "mapName");
        if (mapName.isBlank()) {
            throw new IllegalArgumentException("mapName must not be blank");
        }
        wallTextures = Collections.unmodifiableMap(new LinkedHashMap<>(wallTextures));
        flats = Collections.unmodifiableMap(new LinkedHashMap<>(flats));
    }
}
