/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises public WAD value validation independently of binary decoding. */
final class WadModelTest {
    private static final String SHA_256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    /** Normalizes case and preserves valid scalar metadata. */
    @Test
    void createsValidatedValues(@TempDir Path temporaryDirectory) {
        Path source = temporaryDirectory.toAbsolutePath().normalize().resolve("source.wad");
        WadProvenance provenance = new WadProvenance(source, 32L, SHA_256.toUpperCase(Locale.ROOT));
        WadLump lump = new WadLump(0, "mixed", 12L, 4);
        WadDiagnostic diagnostic =
                new WadDiagnostic(WadDiagnostic.Severity.WARNING, "wad.test", source, "/directory/0", "Test warning");
        WadLoadResult result = new WadLoadResult(Optional.empty(), List.of(diagnostic));

        assertThat(provenance.sha256()).isEqualTo(SHA_256);
        assertThat(lump.name()).isEqualTo("MIXED");
        assertThat(diagnostic.severity()).isEqualTo(WadDiagnostic.Severity.WARNING);
        assertThat(result.isValid()).isFalse();
    }

    /** Rejects malformed paths, fingerprints, names, indices, and ranges. */
    @Test
    void rejectsInvalidValues(@TempDir Path temporaryDirectory) {
        Path relative = Path.of("source.wad");
        Path source = temporaryDirectory.toAbsolutePath().normalize().resolve("source.wad");

        assertThatIllegalArgumentException().isThrownBy(() -> new WadProvenance(relative, 0L, SHA_256));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadProvenance(source, -1L, SHA_256));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadProvenance(source, 0L, "not-a-digest"));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadLump(-1, "NAME", 0L, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadLump(0, "TOO-LONG!", 0L, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadLump(0, "BAD\n", 0L, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadLump(0, "NAME", -1L, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadLump(0, "NAME", 0L, -1));
    }

    /** Requires sequential directory indexes and source-contained ranges. */
    @Test
    void validatesArchiveDirectory(@TempDir Path temporaryDirectory) {
        Path source = temporaryDirectory.toAbsolutePath().normalize().resolve("source.wad");
        WadProvenance provenance = new WadProvenance(source, 20L, SHA_256);
        List<WadLump> wrongIndex = List.of(new WadLump(1, "NAME", 12L, 1));
        List<WadLump> outOfBounds = List.of(new WadLump(0, "NAME", 19L, 2));

        assertThatIllegalArgumentException().isThrownBy(() -> new WadArchive(provenance, WadKind.IWAD, wrongIndex));
        assertThatIllegalArgumentException().isThrownBy(() -> new WadArchive(provenance, WadKind.IWAD, outOfBounds));
    }
}
