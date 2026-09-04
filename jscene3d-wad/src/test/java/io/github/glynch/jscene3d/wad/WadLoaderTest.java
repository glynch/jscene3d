/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises WAD loading, diagnostics, provenance, and bounded source access. */
final class WadLoaderTest {
    /** Preserves directory order, duplicates, fingerprints, and opaque content. */
    @Test
    void indexesAndReadsValidArchive(@TempDir Path temporaryDirectory) throws IOException {
        Path source = WadTestFiles.write(
                temporaryDirectory.resolve("valid.wad"),
                "IWAD",
                List.of(
                        new WadTestFiles.TestLump("MARKER", new byte[0]),
                        new WadTestFiles.TestLump("DATA", new byte[] {1, 2, 3, 4}),
                        new WadTestFiles.TestLump("DUP", new byte[] {5}),
                        new WadTestFiles.TestLump("DUP", new byte[] {6, 7})));

        WadLoadResult result = WadLoader.load(source);

        assertThat(result.isValid()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        WadArchive archive = result.archive().orElseThrow();
        assertThat(archive.kind()).isEqualTo(WadKind.IWAD);
        assertThat(archive.provenance().source()).isEqualTo(source.toRealPath());
        assertThat(archive.provenance().fileSize()).isEqualTo(Files.size(source));
        assertThat(archive.provenance().sha256()).hasSize(64).isLowerCase();
        assertThat(archive.lumps()).extracting(WadLump::name).containsExactly("MARKER", "DATA", "DUP", "DUP");
        assertThat(archive.lumpsNamed("dup")).extracting(WadLump::index).containsExactly(2, 3);
        WadLump overridden = archive.lastLumpNamed("DuP").orElseThrow();
        assertThat(archive.readAllBytes(overridden, 2)).containsExactly(6, 7);
        assertThat(archive.lastLumpNamed("ABSENT")).isEmpty();
    }

    /** Provides independent bounded streams with ordinary InputStream behavior. */
    @Test
    void streamsOnlyTheSelectedLump(@TempDir Path temporaryDirectory) throws IOException {
        Path source = WadTestFiles.write(
                temporaryDirectory.resolve("stream.wad"),
                "PWAD",
                List.of(new WadTestFiles.TestLump("CONTENT", new byte[] {10, 11, 12, 13})));
        WadArchive archive = WadLoader.load(source).archive().orElseThrow();
        WadLump lump = archive.lumps().getFirst();

        try (InputStream input = archive.openStream(lump)) {
            assertThat(input.available()).isEqualTo(4);
            assertThat(input.read()).isEqualTo(10);
            assertThat(input.skip(2)).isEqualTo(2L);
            byte[] tail = new byte[4];
            assertThat(input.read(tail, 1, 3)).isEqualTo(1);
            assertThat(tail).containsExactly(0, 13, 0, 0);
            assertThat(input.read()).isEqualTo(-1);
            assertThat(input.skip(-1)).isZero();
        }
    }

    /** Rejects missing files and sources shorter than the fixed header. */
    @Test
    void diagnosesMissingAndTruncatedSources(@TempDir Path temporaryDirectory) throws IOException {
        Path missing = temporaryDirectory.resolve("missing.wad");
        Path truncated = temporaryDirectory.resolve("truncated.wad");
        Files.writeString(truncated, "IWAD", StandardCharsets.US_ASCII);

        WadLoadResult missingResult = WadLoader.load(missing);
        WadLoadResult truncatedResult = WadLoader.load(truncated);

        assertThat(missingResult.archive()).isEmpty();
        assertThat(missingResult.diagnostics())
                .singleElement()
                .extracting(WadDiagnostic::code)
                .isEqualTo("wad.source.missing");
        assertThat(truncatedResult.archive()).isEmpty();
        assertThat(truncatedResult.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("wad.header.truncated");
            assertThat(diagnostic.location()).isEqualTo("/header");
        });
    }

    /** Diagnoses unsupported signatures and invalid directory header fields. */
    @Test
    void diagnosesInvalidHeaderFields(@TempDir Path temporaryDirectory) throws IOException {
        Path signature = WadTestFiles.writeRaw(temporaryDirectory.resolve("signature.wad"), "NOPE", 0, 12, new byte[0]);
        Path count = WadTestFiles.writeRaw(temporaryDirectory.resolve("count.wad"), "IWAD", -1, 12, new byte[0]);
        Path offset = WadTestFiles.writeRaw(temporaryDirectory.resolve("offset.wad"), "PWAD", 0, -1, new byte[0]);
        Path directory = WadTestFiles.writeRaw(temporaryDirectory.resolve("directory.wad"), "IWAD", 1, 12, new byte[0]);

        WadLoadResult signatureResult = WadLoader.load(signature);
        WadLoadResult countResult = WadLoader.load(count);
        WadLoadResult offsetResult = WadLoader.load(offset);
        WadLoadResult directoryResult = WadLoader.load(directory);

        assertThat(signatureResult.diagnostics())
                .singleElement()
                .extracting(WadDiagnostic::code)
                .isEqualTo("wad.header.signature");
        assertThat(countResult.diagnostics())
                .singleElement()
                .extracting(WadDiagnostic::code)
                .isEqualTo("wad.directory.count");
        assertThat(offsetResult.diagnostics())
                .singleElement()
                .extracting(WadDiagnostic::code)
                .isEqualTo("wad.directory.offset");
        assertThat(directoryResult.diagnostics())
                .singleElement()
                .extracting(WadDiagnostic::code)
                .isEqualTo("wad.directory.bounds");
    }

    /** Diagnoses directory entries with invalid ranges or encoded names. */
    @Test
    void diagnosesInvalidLumps(@TempDir Path temporaryDirectory) throws IOException {
        ByteBuffer invalidBounds = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        invalidBounds.putInt(1_000).putInt(8);
        WadTestFiles.putName(invalidBounds, "BROKEN");
        Path bounds =
                WadTestFiles.writeRaw(temporaryDirectory.resolve("bounds.wad"), "PWAD", 1, 12, invalidBounds.array());
        ByteBuffer invalidName = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        invalidName.putInt(0).putInt(0).put(new byte[] {'A', 0, 'B', 0, 0, 0, 0, 0});
        Path name = WadTestFiles.writeRaw(temporaryDirectory.resolve("name.wad"), "IWAD", 1, 12, invalidName.array());

        WadLoadResult boundsResult = WadLoader.load(bounds);
        WadLoadResult nameResult = WadLoader.load(name);

        assertThat(boundsResult.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("wad.lump.bounds");
            assertThat(diagnostic.location()).isEqualTo("/directory/0");
        });
        assertThat(nameResult.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo("wad.lump.name");
            assertThat(diagnostic.location()).isEqualTo("/directory/0/name");
        });
    }

    /** Verifies expected fingerprints before exposing archive metadata. */
    @Test
    void checksExpectedFingerprint(@TempDir Path temporaryDirectory) throws IOException {
        Path source = WadTestFiles.write(temporaryDirectory.resolve("digest.wad"), "IWAD", List.of());
        String actual =
                WadLoader.load(source).archive().orElseThrow().provenance().sha256();

        WadLoadResult matching = WadLoader.load(source, actual.toUpperCase(Locale.ROOT));
        WadLoadResult mismatch = WadLoader.load(source, "0".repeat(64));

        assertThat(matching.isValid()).isTrue();
        assertThat(mismatch.archive()).isEmpty();
        assertThat(mismatch.diagnostics())
                .singleElement()
                .extracting(WadDiagnostic::code)
                .isEqualTo("wad.source.sha256");
        assertThatIllegalArgumentException().isThrownBy(() -> WadLoader.load(source, "invalid"));
    }

    /** Rejects foreign entries, oversized allocations, and changed source sizes. */
    @Test
    void enforcesReadOwnershipAndSourceSnapshot(@TempDir Path temporaryDirectory) throws IOException {
        Path source = WadTestFiles.write(
                temporaryDirectory.resolve("bounded.wad"),
                "IWAD",
                List.of(new WadTestFiles.TestLump("DATA", new byte[] {1, 2, 3})));
        WadArchive archive = WadLoader.load(source).archive().orElseThrow();
        WadLump lump = archive.lumps().getFirst();
        WadLump foreign = new WadLump(0, "OTHER", lump.offset(), lump.size());

        assertThatIllegalArgumentException().isThrownBy(() -> archive.openStream(foreign));
        assertThatIllegalArgumentException().isThrownBy(() -> archive.readAllBytes(lump, 2));

        Files.write(source, new byte[] {1});

        assertThatThrownBy(() -> archive.openStream(lump))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("changed");
    }
}
