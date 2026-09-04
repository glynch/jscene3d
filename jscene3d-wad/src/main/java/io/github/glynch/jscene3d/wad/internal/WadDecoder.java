/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.internal;

import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadDiagnostic;
import io.github.glynch.jscene3d.wad.WadDiagnosticCode;
import io.github.glynch.jscene3d.wad.WadKind;
import io.github.glynch.jscene3d.wad.WadLoadResult;
import io.github.glynch.jscene3d.wad.WadLump;
import io.github.glynch.jscene3d.wad.WadProvenance;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Strict bounded decoder for WAD headers and directories. */
public final class WadDecoder {
    private static final int HEADER_SIZE = 12;
    private static final int DIRECTORY_ENTRY_SIZE = 16;
    private static final int NAME_SIZE = 8;
    private static final int MAX_LUMP_COUNT = 1_000_000;

    /** Prevents instantiation of this decoding policy. */
    private WadDecoder() {
        throw new AssertionError("WadDecoder cannot be instantiated");
    }

    /**
     * Loads one source without a required expected fingerprint.
     *
     * @param source source file to load
     * @return archive or source-aware diagnostics
     */
    public static WadLoadResult load(Path source) {
        return loadInternal(source, null);
    }

    /**
     * Loads one source with a prevalidated expected fingerprint.
     *
     * @param source source file to load
     * @param expectedSha256 required lowercase SHA-256 fingerprint
     * @return archive or source-aware diagnostics
     */
    public static WadLoadResult load(Path source, String expectedSha256) {
        return loadInternal(source, Objects.requireNonNull(expectedSha256, "expectedSha256"));
    }

    /** Resolves, fingerprints, and decodes one source into data or diagnostics. */
    private static WadLoadResult loadInternal(Path source, @Nullable String expectedSha256) {
        Path normalizedSource =
                Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        List<WadDiagnostic> diagnostics = new ArrayList<>();
        if (!Files.isRegularFile(normalizedSource)) {
            return error(diagnostics, normalizedSource, WadDiagnosticCode.SOURCE_MISSING, "");
        }

        try {
            Path resolvedSource = normalizedSource.toRealPath();
            long fileSize = Files.size(resolvedSource);
            String sha256 = WadHashes.sha256(resolvedSource);
            if (expectedSha256 != null && !sha256.equals(expectedSha256)) {
                return error(
                        diagnostics,
                        resolvedSource,
                        WadDiagnosticCode.SOURCE_FINGERPRINT_MISMATCH,
                        "",
                        Map.of("actualSha256", sha256, "expectedSha256", expectedSha256));
            }
            WadProvenance provenance = new WadProvenance(resolvedSource, fileSize, sha256);
            return readDirectory(provenance, diagnostics);
        } catch (IOException exception) {
            return error(
                    diagnostics,
                    normalizedSource,
                    WadDiagnosticCode.SOURCE_READ_FAILED,
                    "",
                    Map.of("failure", failureMessage(exception)));
        }
    }

    /** Reads and validates the complete directory using one source snapshot description. */
    private static WadLoadResult readDirectory(WadProvenance provenance, List<WadDiagnostic> diagnostics)
            throws IOException {
        try (FileChannel channel = FileChannel.open(provenance.source(), StandardOpenOption.READ)) {
            if (channel.size() != provenance.fileSize()) {
                return error(diagnostics, provenance.source(), WadDiagnosticCode.SOURCE_CHANGED, "");
            }
            if (provenance.fileSize() < HEADER_SIZE) {
                return error(
                        diagnostics,
                        provenance.source(),
                        WadDiagnosticCode.HEADER_TRUNCATED,
                        "/header",
                        Map.of("minimumBytes", Integer.toString(HEADER_SIZE)));
            }

            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            readFully(channel, header, 0L);
            header.flip();
            Optional<WadKind> kind = parseKind(header, provenance.source(), diagnostics);
            if (kind.isEmpty()) {
                return new WadLoadResult(Optional.empty(), diagnostics);
            }
            int lumpCount = header.getInt();
            int directoryOffset = header.getInt();
            Optional<WadLoadResult> headerError =
                    validateDirectoryHeader(provenance, diagnostics, lumpCount, directoryOffset);
            if (headerError.isPresent()) {
                return headerError.orElseThrow();
            }
            return readEntries(provenance, diagnostics, channel, kind.orElseThrow(), lumpCount, directoryOffset);
        }
    }

    /** Validates count, offset, and complete directory bounds. */
    private static Optional<WadLoadResult> validateDirectoryHeader(
            WadProvenance provenance, List<WadDiagnostic> diagnostics, int lumpCount, int directoryOffset) {
        if (lumpCount < 0 || lumpCount > MAX_LUMP_COUNT) {
            return Optional.of(error(
                    diagnostics,
                    provenance.source(),
                    WadDiagnosticCode.DIRECTORY_COUNT_INVALID,
                    "/header/lumpCount",
                    Map.of("lumpCount", Integer.toString(lumpCount))));
        }
        if (directoryOffset < 0) {
            return Optional.of(error(
                    diagnostics,
                    provenance.source(),
                    WadDiagnosticCode.DIRECTORY_OFFSET_INVALID,
                    "/header/directoryOffset"));
        }
        long directorySize = (long) lumpCount * DIRECTORY_ENTRY_SIZE;
        if (directoryOffset > provenance.fileSize() || directorySize > provenance.fileSize() - directoryOffset) {
            return Optional.of(
                    error(diagnostics, provenance.source(), WadDiagnosticCode.DIRECTORY_OUT_OF_BOUNDS, "/directory"));
        }
        return Optional.empty();
    }

    /** Reads every validated directory entry while preserving order and duplicate names. */
    private static WadLoadResult readEntries(
            WadProvenance provenance,
            List<WadDiagnostic> diagnostics,
            FileChannel channel,
            WadKind kind,
            int lumpCount,
            int directoryOffset)
            throws IOException {
        int directorySize = Math.multiplyExact(lumpCount, DIRECTORY_ENTRY_SIZE);
        ByteBuffer directory = ByteBuffer.allocate(directorySize).order(ByteOrder.LITTLE_ENDIAN);
        readFully(channel, directory, directoryOffset);
        directory.flip();
        List<WadLump> lumps = new ArrayList<>(lumpCount);
        for (int index = 0; index < lumpCount; index++) {
            Optional<WadLoadResult> entryError = readEntry(provenance, diagnostics, directory, index, lumps);
            if (entryError.isPresent()) {
                return entryError.orElseThrow();
            }
        }
        return new WadLoadResult(Optional.of(new WadArchive(provenance, kind, lumps)), diagnostics);
    }

    /** Reads one directory entry or returns its terminal validation result. */
    private static Optional<WadLoadResult> readEntry(
            WadProvenance provenance,
            List<WadDiagnostic> diagnostics,
            ByteBuffer directory,
            int index,
            List<WadLump> lumps) {
        int offset = directory.getInt();
        int size = directory.getInt();
        byte[] rawName = new byte[NAME_SIZE];
        directory.get(rawName);
        String location = "/directory/" + index;
        if (offset < 0 || size < 0 || offset > provenance.fileSize() || size > provenance.fileSize() - offset) {
            return Optional.of(error(
                    diagnostics,
                    provenance.source(),
                    WadDiagnosticCode.LUMP_OUT_OF_BOUNDS,
                    location,
                    Map.of("lumpIndex", Integer.toString(index))));
        }
        Optional<String> name = WadNames.decode(rawName);
        if (name.isEmpty()) {
            return Optional.of(
                    error(diagnostics, provenance.source(), WadDiagnosticCode.LUMP_NAME_INVALID, location + "/name"));
        }
        lumps.add(new WadLump(index, name.orElseThrow(), offset, size));
        return Optional.empty();
    }

    /** Decodes the exact header signature without interpreting archive contents. */
    private static Optional<WadKind> parseKind(ByteBuffer header, Path source, List<WadDiagnostic> diagnostics) {
        byte[] signatureBytes = new byte[4];
        header.get(signatureBytes);
        String signature = new String(signatureBytes, StandardCharsets.US_ASCII);
        return switch (signature) {
            case "IWAD" -> Optional.of(WadKind.IWAD);
            case "PWAD" -> Optional.of(WadKind.PWAD);
            default -> {
                diagnostics.add(new WadDiagnostic(
                        WadDiagnostic.Severity.ERROR,
                        WadDiagnosticCode.HEADER_SIGNATURE_INVALID,
                        source,
                        "/header/signature",
                        Map.of("actualSignature", printable(signatureBytes))));
                yield Optional.empty();
            }
        };
    }

    /** Renders four signature bytes without inserting control characters into diagnostics. */
    private static String printable(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length);
        for (byte value : bytes) {
            int character = Byte.toUnsignedInt(value);
            text.append(character >= 0x20 && character <= 0x7e ? (char) character : '?');
        }
        return text.toString();
    }

    /** Reads a fixed-size region or rejects a source that changed during decoding. */
    private static void readFully(FileChannel channel, ByteBuffer target, long offset) throws IOException {
        long position = offset;
        while (target.hasRemaining()) {
            int count = channel.read(target, position);
            if (count < 0) {
                throw new EOFException("unexpected end of WAD");
            }
            position += count;
        }
    }

    /** Produces one terminal load result with a single appended error. */
    private static WadLoadResult error(
            List<WadDiagnostic> diagnostics, Path source, WadDiagnosticCode code, String location) {
        return error(diagnostics, source, code, location, Map.of());
    }

    /** Produces one terminal load result containing structured failure details. */
    private static WadLoadResult error(
            List<WadDiagnostic> diagnostics,
            Path source,
            WadDiagnosticCode code,
            String location,
            Map<String, String> details) {
        diagnostics.add(new WadDiagnostic(WadDiagnostic.Severity.ERROR, code, source, location, details));
        return new WadLoadResult(Optional.empty(), diagnostics);
    }

    /** Returns a stable non-null I/O failure detail. */
    private static String failureMessage(IOException exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName() : message;
    }
}
