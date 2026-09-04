/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.imports.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.imports.ImportDefinition;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.JsonPointers;
import io.github.glynch.jscene3d.project.internal.ProjectIdentifiers;
import io.github.glynch.jscene3d.project.internal.ProjectSchemaReferences;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Converts nullable Import JSON into one validated immutable definition. */
public final class ImportValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/import-1.json";
    private static final String LOCAL_SCHEMA_REFERENCE = "schema/import-1.schema.json";

    private final GameProject project;
    private final Path source;
    private final DiagnosticCollector diagnostics;
    private final ValidationContext fields;
    private final ProjectValueDecoder values;

    /** Stores one import-definition validation context. */
    private ImportValidator(GameProject project, Path source) {
        this.project = project;
        this.source = source;
        diagnostics = new DiagnosticCollector(source);
        fields = new ValidationContext(diagnostics, "import");
        values = ProjectValueDecoder.plain();
    }

    /**
     * Validates one raw import definition.
     *
     * @param raw nullable deserialization model
     * @param project containing validated project
     * @param source canonical definition source path
     * @return validated definition or ordered diagnostics
     */
    public static ValidationResult validate(RawImport raw, GameProject project, Path source) {
        ImportValidator validator = new ImportValidator(project, source);
        Optional<ImportDefinition> definition = validator.validate(raw);
        return new ValidationResult(definition, validator.diagnostics.diagnostics());
    }

    /** Validates fields in deterministic document order. */
    private Optional<ImportDefinition> validate(RawImport raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        String id = fields.requiredLocalId(raw.id(), "/id");
        Optional<GameProject.AssetSource> asset = validateAsset(raw.source());
        String importer = fields.requiredRegisteredTypeId(raw.importer(), "/importer");
        List<String> selection = validateSelection(raw.selection());
        Map<String, ProjectValue> settings = validateSettings(raw.settings());
        Map<String, Map<String, ProjectValue>> itemSettings = validateItemSettings(raw.itemSettings());
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(
                new ImportDefinition(source, id, asset.orElseThrow(), importer, selection, settings, itemSettings));
    }

    /** Validates the authoritative version and optional schema reference. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    "import.schema.unsupported",
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null
                && !ProjectSchemaReferences.matches(
                        project.root(), source, schema, SCHEMA_URI, LOCAL_SCHEMA_REFERENCE)) {
            diagnostics.warning(
                    "import.schema.uri",
                    "$schema does not identify the bundled Import Definition version 1 schema",
                    "/$schema");
        }
    }

    /** Resolves the required asset reference against the project manifest. */
    private Optional<GameProject.AssetSource> validateAsset(@Nullable String rawSource) {
        String reference = fields.requiredText(rawSource, "/source");
        if (reference.isEmpty()) {
            return Optional.empty();
        }
        if (!reference.startsWith("asset:") || reference.length() == "asset:".length()) {
            diagnostics.error(
                    "import.source.namespace", "source must be an asset reference such as asset:model", "/source");
            return Optional.empty();
        }
        String assetId = reference.substring("asset:".length());
        Optional<GameProject.AssetSource> asset = project.assets().stream()
                .filter(candidate -> candidate.id().equals(assetId))
                .findFirst();
        if (asset.isEmpty()) {
            diagnostics.error("import.source.missing", "source asset does not exist: " + assetId, "/source");
        }
        return asset;
    }

    /** Validates the required ordered, duplicate-free source-item selection. */
    private List<String> validateSelection(@Nullable JsonNode rawSelection) {
        if (rawSelection == null || !rawSelection.isArray()) {
            diagnostics.error("import.selection.array", "selection must be an array", "/selection");
            return List.of();
        }
        List<String> selection = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (int index = 0; index < rawSelection.size(); index++) {
            JsonNode rawIdentity = rawSelection.get(index);
            String location = "/selection/" + index;
            if (!rawIdentity.isTextual() || rawIdentity.textValue().isBlank()) {
                diagnostics.error("import.selection.text", "selection identities must be non-blank strings", location);
                continue;
            }
            String identity = rawIdentity.textValue();
            if (!seen.add(identity)) {
                diagnostics.error(
                        "import.selection.duplicate", "selection identity is duplicated: " + identity, location);
                continue;
            }
            if (!ProjectIdentifiers.isPortableLocator(identity)) {
                diagnostics.error(
                        "import.selection.identity",
                        "selection identity must be a portable relative locator",
                        location);
                continue;
            }
            selection.add(identity);
        }
        return List.copyOf(selection);
    }

    /** Validates the optional importer-wide settings object. */
    private Map<String, ProjectValue> validateSettings(@Nullable JsonNode rawSettings) {
        if (rawSettings == null) {
            return Map.of();
        }
        if (!rawSettings.isObject()) {
            diagnostics.error("import.settings.object", "settings must be an object", "/settings");
            return Map.of();
        }
        return values.decodeObject(rawSettings, "/settings").values();
    }

    /** Validates the optional source-item settings index. */
    private Map<String, Map<String, ProjectValue>> validateItemSettings(@Nullable JsonNode rawItemSettings) {
        if (rawItemSettings == null) {
            return Map.of();
        }
        if (!rawItemSettings.isObject()) {
            diagnostics.error("import.item-settings.index", "itemSettings must be an object", "/itemSettings");
            return Map.of();
        }
        Map<String, Map<String, ProjectValue>> itemSettings = new LinkedHashMap<>();
        rawItemSettings
                .properties()
                .forEach(entry -> validateItemSetting(entry.getKey(), entry.getValue(), itemSettings));
        return itemSettings;
    }

    /** Validates one per-source-item settings object. */
    private void validateItemSetting(
            String identity, JsonNode rawSettings, Map<String, Map<String, ProjectValue>> itemSettings) {
        String location = "/itemSettings/" + JsonPointers.escapeSegment(identity);
        if (!ProjectIdentifiers.isPortableLocator(identity)) {
            diagnostics.error(
                    "import.item-settings.identity", "itemSettings key must be a portable relative locator", location);
        }
        if (!rawSettings.isObject()) {
            diagnostics.error("import.item-settings.object", "item settings must be an object", location);
            return;
        }
        if (ProjectIdentifiers.isPortableLocator(identity)) {
            itemSettings.put(
                    identity, values.decodeObject(rawSettings, location).values());
        }
    }

    /** Validated import definition and ordered diagnostics returned to the public loader.
     *
     * @param definition validated definition when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<ImportDefinition> definition, List<ProjectDiagnostic> diagnostics) {}
}
