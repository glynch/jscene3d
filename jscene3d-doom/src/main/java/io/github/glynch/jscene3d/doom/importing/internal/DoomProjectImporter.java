/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.importing.internal;

import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnostic;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.map.DoomMapDecodeResult;
import io.github.glynch.jscene3d.doom.map.DoomMapDecoder;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Adapts classic Doom map structures to the generic project import lifecycle. */
final class DoomProjectImporter implements ProjectImporter {
    private static final String MAPS_IDENTITY = "maps";
    private static final String MAPS_ITEM_KIND = "io.github.glynch.jscene3d.doom/map-collection";
    private static final String MAP_ITEM_KIND = "io.github.glynch.jscene3d.doom/map";
    private static final RegisteredType MAP_RESOURCE_TYPE =
            new RegisteredType(DoomImportExtension.MAP_RESOURCE_TYPE_IDENTIFIER, DoomImportExtension.TYPE_VERSION);
    private final DoomMapDecoder decoder = new DoomMapDecoder();

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
        for (String mapName : selectedMapNames(context, decoder.discover(archive))) {
            context.checkCancelled();
            DoomMapDecodeResult result = decoder.decode(archive, mapName);
            result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
            if (result.isValid()) {
                DoomMap map = result.map().orElseThrow();
                context.artifact(
                        ImportArtifactDescriptor.resource(mapIdentity(mapName), MAP_RESOURCE_TYPE, List.of()),
                        output -> DoomMapResourceWriter.write(
                                output, context.asset().id(), archive, map));
            }
        }
    }

    /** Loads one archive, forwards WAD diagnostics, and declares its Doom map graph. */
    private Optional<WadArchive> loadAndDescribe(ImportInspectionContext context) {
        context.checkCancelled();
        WadLoadResult result = load(context);
        result.diagnostics().forEach(diagnostic -> report(context, diagnostic));
        if (!result.isValid()) {
            return Optional.empty();
        }
        WadArchive archive = result.archive().orElseThrow();
        describe(context, archive, decoder.discover(archive));
        return Optional.of(archive);
    }

    /** Loads through fingerprint validation when the project declares an expected digest. */
    private static WadLoadResult load(ImportInspectionContext context) {
        Optional<String> expectedSha256 = context.asset().sha256();
        return expectedSha256.isPresent()
                ? WadLoader.load(context.asset().path(), expectedSha256.orElseThrow())
                : WadLoader.load(context.asset().path());
    }

    /** Declares one selectable collection and every conventional map marker. */
    private static void describe(ImportInspectionContext context, WadArchive archive, List<String> mapNames) {
        List<SourceItemRelation> contents = mapNames.stream()
                .map(DoomProjectImporter::mapIdentity)
                .map(identity -> new SourceItemRelation("contains", identity))
                .toList();
        context.sourceItem(new SourceItem(
                MAPS_IDENTITY,
                MAPS_ITEM_KIND,
                "Doom maps",
                true,
                Map.of("map-count", number(mapNames.size())),
                contents));
        for (String mapName : mapNames) {
            context.checkCancelled();
            Map<String, ProjectValue> properties = new LinkedHashMap<>();
            properties.put("name", text(mapName));
            properties.put("marker-index", number(lastMarkerIndex(archive, mapName)));
            context.sourceItem(
                    new SourceItem(mapIdentity(mapName), MAP_ITEM_KIND, mapName, true, properties, List.of()));
        }
    }

    /** Resolves direct selections and collection expansion to ordered map names. */
    private static List<String> selectedMapNames(ImportPreparationContext context, List<String> mapNames) {
        Set<String> selection = Set.copyOf(context.definition().selection());
        Set<String> selected = new LinkedHashSet<>();
        if (selection.contains(MAPS_IDENTITY)) {
            selected.addAll(mapNames);
        }
        mapNames.stream()
                .filter(mapName -> selection.contains(mapIdentity(mapName)))
                .forEach(selected::add);
        return List.copyOf(selected);
    }

    /** Returns the final marker directory position for inspection metadata. */
    private static int lastMarkerIndex(WadArchive archive, String mapName) {
        int result = -1;
        for (WadLump lump : archive.lumps()) {
            if (lump.name().equals(mapName)) {
                result = lump.index();
            }
        }
        return result;
    }

    /** Forwards one feature-owned WAD diagnostic intact. */
    private static void report(ImportInspectionContext context, WadDiagnostic diagnostic) {
        if (diagnostic.severity() == WadDiagnostic.Severity.ERROR) {
            context.error(diagnostic.code(), diagnostic.location(), diagnostic.details());
        } else {
            context.warning(diagnostic.code(), diagnostic.location(), diagnostic.details());
        }
    }

    /** Forwards one feature-owned Doom diagnostic intact. */
    private static void report(ImportInspectionContext context, DoomDiagnostic diagnostic) {
        if (diagnostic.severity() == DoomDiagnostic.Severity.ERROR) {
            context.error(diagnostic.code(), diagnostic.location(), diagnostic.details());
        } else {
            context.warning(diagnostic.code(), diagnostic.location(), diagnostic.details());
        }
    }

    /** Returns the stable source-item and artifact identity for one map. */
    private static String mapIdentity(String mapName) {
        return "maps/" + mapName;
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
