/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        Map<String, String> details = new HashMap<>(Map.of("expectedSize", "32"));
        WadDiagnostic diagnostic = new WadDiagnostic(
                WadDiagnostic.Severity.WARNING, WadDiagnosticCode.SOURCE_CHANGED, source, "/directory/0", details);
        WadLoadResult result = new WadLoadResult(Optional.empty(), List.of(diagnostic));
        DiagnosticCode sharedCode = diagnostic.code();
        details.put("actualSize", "16");

        assertThat(provenance.sha256()).isEqualTo(SHA_256);
        assertThat(lump.name()).isEqualTo("MIXED");
        assertThat(diagnostic.severity()).isEqualTo(WadDiagnostic.Severity.WARNING);
        assertThat(sharedCode.code()).isEqualTo("wad.source.changed");
        assertThat(diagnostic.message()).isEqualTo("The WAD source changed while it was being loaded");
        assertThat(diagnostic.details()).containsOnlyKeys("expectedSize");
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

    /** Keeps WAD diagnostic keys unique and English fallbacks non-blank. */
    @Test
    void validatesDiagnosticCatalog() {
        assertThat(WadDiagnosticCode.values())
                .extracting(WadDiagnosticCode::code)
                .doesNotHaveDuplicates()
                .allSatisfy(code -> assertThat(code).isNotBlank());
        assertThat(WadDiagnosticCode.values())
                .extracting(WadDiagnosticCode::defaultMessage)
                .allSatisfy(message -> assertThat(message).isNotBlank());
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
