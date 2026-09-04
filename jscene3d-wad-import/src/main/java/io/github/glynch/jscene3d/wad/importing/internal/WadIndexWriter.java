/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.importing.internal;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.github.glynch.jscene3d.wad.WadArchive;
import io.github.glynch.jscene3d.wad.WadLump;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;

/** Writes the portable ordered archive index artifact. */
final class WadIndexWriter {
    private static final JsonFactory JSON_FACTORY =
            JsonFactory.builder().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();

    /** Prevents construction of this stateless serializer. */
    private WadIndexWriter() {
        throw new AssertionError("WadIndexWriter cannot be instantiated");
    }

    /** Writes one deterministic archive index without recording a machine-specific source path. */
    static void write(OutputStream output, String assetId, WadArchive archive, Set<String> selectedLumps)
            throws IOException {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(assetId, "assetId");
        Objects.requireNonNull(archive, "archive");
        Objects.requireNonNull(selectedLumps, "selectedLumps");
        try (JsonGenerator generator = JSON_FACTORY.createGenerator(output, JsonEncoding.UTF8)) {
            generator.setPrettyPrinter(prettyPrinter());
            generator.writeStartObject();
            generator.writeNumberField("schemaVersion", 1);
            generator.writeStringField("archiveKind", archive.kind().name());
            writeSource(generator, assetId, archive);
            generator.writeArrayFieldStart("lumps");
            for (WadLump lump : archive.lumps()) {
                writeLump(generator, lump, selectedLumps);
            }
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeRaw('\n');
        }
    }

    /** Creates the deterministic indentation policy for generated JSON artifacts. */
    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(indenter);
        prettyPrinter.indentArraysWith(indenter);
        return prettyPrinter;
    }

    /** Writes portable source provenance. */
    private static void writeSource(JsonGenerator generator, String assetId, WadArchive archive) throws IOException {
        generator.writeObjectFieldStart("source");
        generator.writeStringField("asset", assetId);
        generator.writeNumberField("size", archive.provenance().fileSize());
        generator.writeStringField("sha256", archive.provenance().sha256());
        generator.writeEndObject();
    }

    /** Writes one directory entry and its optional imported artifact identity. */
    private static void writeLump(JsonGenerator generator, WadLump lump, Set<String> selectedLumps) throws IOException {
        String identity = WadProjectImporter.lumpIdentity(lump);
        generator.writeStartObject();
        generator.writeStringField("identity", identity);
        generator.writeNumberField("index", lump.index());
        generator.writeStringField("name", lump.name());
        generator.writeNumberField("offset", lump.offset());
        generator.writeNumberField("size", lump.size());
        if (selectedLumps.contains(identity)) {
            generator.writeStringField("artifact", identity);
        } else {
            generator.writeNullField("artifact");
        }
        generator.writeEndObject();
    }
}
