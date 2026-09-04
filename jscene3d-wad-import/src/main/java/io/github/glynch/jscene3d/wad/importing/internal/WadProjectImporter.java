/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.importing.internal;

import io.github.glynch.jscene3d.project.importing.ImportArtifactDescriptor;
import io.github.glynch.jscene3d.project.importing.SourceItem;
import io.github.glynch.jscene3d.project.importing.SourceItemRelation;
import io.github.glynch.jscene3d.project.importing.extension.ImportInspectionContext;
import io.github.glynch.jscene3d.project.importing.extension.ImportPreparationContext;
import io.github.glynch.jscene3d.project.importing.extension.ProjectImporter;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadDiagnostic;
import io.github.glynch.jscene3d.wad.WadLoadResult;
import io.github.glynch.jscene3d.wad.WadLoader;
import io.github.glynch.jscene3d.wad.WadLump;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Adapts validated WAD archives to the generic project import lifecycle. */
public final class WadProjectImporter implements ProjectImporter {
    private static final String ARCHIVE_IDENTITY = "archive";
    private static final String ARCHIVE_ITEM_KIND = "io.github.glynch.jscene3d.wad/archive";
    private static final String LUMP_ITEM_KIND = "io.github.glynch.jscene3d.wad/lump";
    private static final String INDEX_ARTIFACT_IDENTITY = "archive/index";
    private static final String INDEX_MEDIA_TYPE = "application/vnd.jscene3d.wad-index+json";
    private static final String LUMP_MEDIA_TYPE = "application/vnd.jscene3d.wad-lump";

    /** Creates a stateless WAD project importer. */
    public WadProjectImporter() {
        // Public construction is required by the exported extension registration seam.
    }

    @Override
    public void inspect(ImportInspectionContext context) {
        loadAndDescribe(context);
    }

    @Override
    public void prepare(ImportPreparationContext context) throws IOException {
        Optional<WadArchive> loaded = loadAndDescribe(context);
        if (loaded.isEmpty()) {
            return;
        }
        WadArchive archive = loaded.orElseThrow();
        Set<String> selectedLumps = selectedLumps(context, archive.lumps());
        context.artifact(
                ImportArtifactDescriptor.payload(INDEX_ARTIFACT_IDENTITY, INDEX_MEDIA_TYPE),
                output -> WadIndexWriter.write(output, context.asset().id(), archive, selectedLumps));
        for (WadLump lump : archive.lumps()) {
            context.checkCancelled();
            String identity = lumpIdentity(lump);
            if (selectedLumps.contains(identity)) {
                context.artifact(
                        ImportArtifactDescriptor.payload(identity, LUMP_MEDIA_TYPE),
                        output -> writeLump(archive, lump, output));
            }
        }
    }

    /** Loads one archive, forwards WAD diagnostics, and declares its source-item graph. */
    private static Optional<WadArchive> loadAndDescribe(ImportInspectionContext context) {
        context.checkCancelled();
        WadLoadResult result = load(context);
        result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
        if (!result.isValid()) {
            return Optional.empty();
        }
        WadArchive archive = result.archive().orElseThrow();
        describe(context, archive);
        return Optional.of(archive);
    }

    /** Loads through fingerprint validation when the project declares an expected digest. */
    private static WadLoadResult load(ImportInspectionContext context) {
        Optional<String> expectedSha256 = context.asset().sha256();
        return expectedSha256.isPresent()
                ? WadLoader.load(context.asset().path(), expectedSha256.orElseThrow())
                : WadLoader.load(context.asset().path());
    }

    /** Declares one archive root and every ordered lump as selectable source items. */
    private static void describe(ImportInspectionContext context, WadArchive archive) {
        List<SourceItemRelation> contents = archive.lumps().stream()
                .map(WadProjectImporter::lumpIdentity)
                .map(identity -> new SourceItemRelation("contains", identity))
                .toList();
        Map<String, ProjectValue> archiveProperties = new LinkedHashMap<>();
        archiveProperties.put("kind", text(archive.kind().name()));
        archiveProperties.put("size", number(archive.provenance().fileSize()));
        archiveProperties.put("sha256", text(archive.provenance().sha256()));
        archiveProperties.put("lump-count", number(archive.lumps().size()));
        context.sourceItem(new SourceItem(
                ARCHIVE_IDENTITY,
                ARCHIVE_ITEM_KIND,
                "WAD archive (" + archive.kind().name() + ')',
                true,
                archiveProperties,
                contents));
        for (WadLump lump : archive.lumps()) {
            context.checkCancelled();
            context.sourceItem(new SourceItem(
                    lumpIdentity(lump), LUMP_ITEM_KIND, displayName(lump), true, lumpProperties(lump), List.of()));
        }
    }

    /** Returns importer-local identities selected directly or through the archive root. */
    private static Set<String> selectedLumps(ImportPreparationContext context, List<WadLump> lumps) {
        Set<String> selection = Set.copyOf(context.definition().selection());
        Set<String> selected = new LinkedHashSet<>();
        if (selection.contains(ARCHIVE_IDENTITY)) {
            lumps.stream().map(WadProjectImporter::lumpIdentity).forEach(selected::add);
        }
        lumps.stream()
                .map(WadProjectImporter::lumpIdentity)
                .filter(selection::contains)
                .forEach(selected::add);
        return Set.copyOf(selected);
    }

    /** Forwards one feature-owned WAD diagnostic without reducing its structured identity. */
    private static void report(ImportInspectionContext context, WadDiagnostic diagnostic) {
        if (diagnostic.severity() == WadDiagnostic.Severity.ERROR) {
            context.error(diagnostic.code(), diagnostic.location(), diagnostic.details());
        } else {
            context.warning(diagnostic.code(), diagnostic.location(), diagnostic.details());
        }
    }

    /** Writes exactly one validated lump to engine-owned staging output. */
    private static void writeLump(WadArchive archive, WadLump lump, OutputStream output) throws IOException {
        try (InputStream input = archive.openStream(lump)) {
            input.transferTo(output);
        }
    }

    /** Builds a reorder-sensitive and duplicate-safe portable identity for one lump. */
    static String lumpIdentity(WadLump lump) {
        String encodedName =
                HexFormat.of().withUpperCase().formatHex(lump.name().getBytes(StandardCharsets.US_ASCII));
        String nonEmptyName = encodedName.isEmpty() ? "00" : encodedName;
        return "lumps/" + String.format(Locale.ROOT, "%08d", lump.index()) + '/' + nonEmptyName;
    }

    /** Builds the source label shown by inspection clients. */
    private static String displayName(WadLump lump) {
        String name = lump.name().isEmpty() ? "(unnamed)" : lump.name();
        return name + " (#" + lump.index() + ')';
    }

    /** Builds ordered source metadata for one lump. */
    private static Map<String, ProjectValue> lumpProperties(WadLump lump) {
        Map<String, ProjectValue> properties = new LinkedHashMap<>();
        properties.put("name", text(lump.name()));
        properties.put("index", number(lump.index()));
        properties.put("offset", number(lump.offset()));
        properties.put("size", number(lump.size()));
        return properties;
    }

    /** Wraps one integral source property without losing numeric precision. */
    private static ProjectValue number(long value) {
        return new ProjectValue.NumberValue(BigDecimal.valueOf(value));
    }

    /** Wraps one source text property. */
    private static ProjectValue text(String value) {
        return new ProjectValue.TextValue(value);
    }
}
