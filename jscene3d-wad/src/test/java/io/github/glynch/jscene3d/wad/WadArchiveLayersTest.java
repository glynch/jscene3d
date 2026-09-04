/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises explicit WAD layering without interpreting lump names or contents. */
final class WadArchiveLayersTest {
    /** Preserves layer order and resolves later duplicate names with higher precedence. */
    @Test
    void resolvesExplicitLayers(@TempDir Path temporaryDirectory) throws IOException {
        WadArchive base = load(
                temporaryDirectory.resolve("base.wad"),
                "IWAD",
                List.of(
                        new WadTestFiles.TestLump("SHARED", new byte[] {1}),
                        new WadTestFiles.TestLump("BASE", new byte[] {2})));
        WadArchive patch = load(
                temporaryDirectory.resolve("patch.wad"),
                "PWAD",
                List.of(
                        new WadTestFiles.TestLump("SHARED", new byte[] {3}),
                        new WadTestFiles.TestLump("PATCH", new byte[] {4})));

        WadArchiveLayers layers = WadArchiveLayers.of(List.of(base, patch));

        assertThat(layers.layers()).containsExactly(base, patch);
        assertThat(layers.lumps())
                .extracting(reference -> reference.lump().name())
                .containsExactly("SHARED", "BASE", "SHARED", "PATCH");
        assertThat(layers.lumpsNamed("shared"))
                .extracting(WadLumpReference::layer)
                .containsExactly(0, 1);
        WadLumpReference resolved = layers.lastLumpNamed("SHARED").orElseThrow();
        assertThat(resolved.layer()).isEqualTo(1);
        assertThat(resolved.archive()).isSameAs(patch);
        assertThat(resolved.readAllBytes(1)).containsExactly(3);
        assertThat(layers.lastLumpNamed("ABSENT")).isEmpty();
    }

    /** Defensively copies layers and validates referenced archive ownership. */
    @Test
    void validatesLayerValues(@TempDir Path temporaryDirectory) throws IOException {
        WadArchive archive = load(
                temporaryDirectory.resolve("single.wad"),
                "PWAD",
                List.of(new WadTestFiles.TestLump("ONLY", new byte[] {1})));
        List<WadArchive> mutable = new ArrayList<>();
        mutable.add(archive);
        WadArchiveLayers layers = WadArchiveLayers.of(mutable);
        mutable.clear();
        WadLump foreign = new WadLump(0, "OTHER", archive.lumps().getFirst().offset(), 1);

        assertThat(layers.layers()).containsExactly(archive);
        assertThat(layers.lumps())
                .singleElement()
                .extracting(WadLumpReference::layer)
                .isEqualTo(0);
        assertThatIllegalArgumentException().isThrownBy(() -> WadArchiveLayers.of(List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadLumpReference(0, archive, foreign));
    }

    /** Loads one fixture archive required by a layering scenario. */
    private static WadArchive load(Path source, String signature, List<WadTestFiles.TestLump> lumps)
            throws IOException {
        WadTestFiles.write(source, signature, lumps);
        return WadLoader.load(source).archive().orElseThrow();
    }
}
