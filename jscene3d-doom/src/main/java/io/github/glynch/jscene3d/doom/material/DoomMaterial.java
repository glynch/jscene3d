package io.github.glynch.jscene3d.doom.material;

import io.github.glynch.jscene3d.wad.WadLump;
import java.util.List;
import java.util.Objects;

/**
 * One imported Doom material and the source lumps needed to reproduce it.
 *
 * @param name normalized source material name
 * @param kind source material namespace
 * @param image decoded RGBA image
 * @param sourceLumps ordered source lumps needed to reproduce the material
 */
public record DoomMaterial(String name, Kind kind, RgbaImage image, List<WadLump> sourceLumps) {
    /** Material role in classic map rendering. */
    public enum Kind {
        /** A composite sidedef texture. */
        WALL_TEXTURE,
        /** A 64-by-64 sector plane image. */
        FLAT
    }

    /** Creates an immutable imported material. */
    public DoomMaterial {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(image, "image");
        sourceLumps = List.copyOf(sourceLumps);
    }
}
