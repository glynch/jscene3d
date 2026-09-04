/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.examples;

import io.github.glynch.jscene3d.io.TemporaryWorkspace;
import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadArchiveLayers;
import io.github.glynch.jscene3d.wad.WadKind;
import io.github.glynch.jscene3d.wad.WadLoadResult;
import io.github.glynch.jscene3d.wad.WadLoader;
import io.github.glynch.jscene3d.wad.WadLumpReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/** Inspects a supplied WAD or demonstrates explicit layering with deterministic generated fixtures. */
public final class WadInspectionExample {
    private static final Logger LOGGER = Logger.getLogger(WadInspectionExample.class.getName());

    /** Prevents instantiation of this application entry point. */
    private WadInspectionExample() {
        throw new AssertionError("WadInspectionExample cannot be instantiated");
    }

    /**
     * Inspects one optional source path, or runs the self-contained archive-layering example when omitted.
     *
     * @param arguments zero arguments for generated fixtures or one source WAD path
     * @throws IOException when fixture creation or bounded lump reading fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length > 1) {
            throw new IllegalArgumentException("expected zero arguments or one WAD path");
        }
        if (arguments.length == 1) {
            inspect(Path.of(arguments[0]).toAbsolutePath().normalize());
            return;
        }
        demonstrateLayers();
    }

    /** Loads and reports one caller-supplied archive without interpreting its lump names. */
    private static void inspect(Path source) {
        WadArchive archive = requireArchive(WadLoader.load(source));
        LOGGER.info(
                () -> "Loaded " + archive.kind() + " with " + archive.lumps().size() + " lumps; SHA-256 "
                        + archive.provenance().sha256());
    }

    /** Creates two small archives and resolves a duplicate name from the later layer. */
    private static void demonstrateLayers() throws IOException {
        try (TemporaryWorkspace workspace = TemporaryWorkspace.create("jscene3d-wad-example-")) {
            Path baseSource = ExampleWadFiles.writeSingleLump(
                    workspace.root().resolve("base.wad"), WadKind.IWAD, "MESSAGE", "base");
            Path patchSource = ExampleWadFiles.writeSingleLump(
                    workspace.root().resolve("patch.wad"), WadKind.PWAD, "MESSAGE", "patched");
            WadArchive base = requireArchive(WadLoader.load(baseSource));
            WadArchive patch = requireArchive(WadLoader.load(patchSource));
            WadArchiveLayers layers = WadArchiveLayers.of(List.of(base, patch));
            WadLumpReference resolved = layers.lastLumpNamed("MESSAGE").orElseThrow();
            String content = new String(resolved.readAllBytes(64), StandardCharsets.US_ASCII);
            LOGGER.info(() -> "Resolved layer " + resolved.layer() + " content: " + content);
        }
    }

    /** Requires a successful load while retaining structured diagnostics in failures. */
    private static WadArchive requireArchive(WadLoadResult result) {
        return result.archive()
                .orElseThrow(() -> new IllegalArgumentException("WAD could not be loaded: " + result.diagnostics()));
    }
}
