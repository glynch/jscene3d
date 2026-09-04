/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.resource.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isRegisteredTypeId;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.ProjectPathResolver;
import io.github.glynch.jscene3d.project.internal.ProjectReferenceDecoder;
import io.github.glynch.jscene3d.project.internal.ProjectSchemaReferences;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.resource.ResourceDefinition;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Converts nullable resource JSON into one validated immutable definition. */
public final class ResourceValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/resource-1.json";
    private static final String LOCAL_SCHEMA_REFERENCE = "schema/resource-1.schema.json";

    private final GameProject project;
    private final Path source;
    private final DiagnosticCollector diagnostics;
    private final ValidationContext fields;
    private final ProjectValueDecoder values;

    /** Stores one resource-validation context. */
    private ResourceValidator(GameProject project, Path source) {
        this.project = project;
        this.source = source;
        diagnostics = new DiagnosticCollector(source);
        ProjectPathResolver paths = new ProjectPathResolver(project.root(), diagnostics, "resource");
        fields = new ValidationContext(diagnostics, "resource");
        ProjectReferenceDecoder references = new ProjectReferenceDecoder(project, paths, diagnostics, "resource");
        values = ProjectValueDecoder.withReferences(references::decode);
    }

    /**
     * Validates one raw resource.
     *
     * @param raw nullable deserialization model
     * @param project containing validated project
     * @param source canonical resource source path
     * @return validated resource or ordered diagnostics
     */
    public static ValidationResult validate(RawResource raw, GameProject project, Path source) {
        ResourceValidator validator = new ResourceValidator(project, source);
        Optional<ResourceDefinition> resource = validator.validate(raw);
        return new ValidationResult(resource, validator.diagnostics.diagnostics());
    }

    /** Validates fields in deterministic document order. */
    private Optional<ResourceDefinition> validate(RawResource raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        String id = fields.requiredText(raw.type(), "/type");
        if (!id.isEmpty() && !isRegisteredTypeId(id)) {
            diagnostics.error(
                    "resource.type.identifier",
                    "type must contain an extension id and local type separated by one slash",
                    "/type");
        }
        int version = raw.typeVersion() == null ? 0 : raw.typeVersion();
        if (version < 1) {
            diagnostics.error("resource.type.version", "typeVersion must be positive", "/typeVersion");
        }
        Map<String, ProjectValue> properties = validateProperties(raw.properties());
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(new ResourceDefinition(source, new RegisteredType(id, version), properties));
    }

    /** Validates the authoritative version and optional schema reference. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    "resource.schema.unsupported",
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null
                && !ProjectSchemaReferences.matches(
                        project.root(), source, schema, SCHEMA_URI, LOCAL_SCHEMA_REFERENCE)) {
            diagnostics.warning(
                    "resource.schema.uri",
                    "$schema does not identify the bundled Resource version 1 schema",
                    "/$schema");
        }
    }

    /** Converts the optional properties object into portable values. */
    private Map<String, ProjectValue> validateProperties(@Nullable JsonNode raw) {
        if (raw == null) {
            return Map.of();
        }
        if (!raw.isObject()) {
            diagnostics.error("resource.value.object", "properties must be an object", "/properties");
            return Map.of();
        }
        return values.decodeObject(raw, "/properties").values();
    }

    /** Validated resource and ordered diagnostics returned to the public loader.
     *
     * @param resource validated resource when no errors were produced
     * @param diagnostics ordered validation errors and warnings
     */
    public record ValidationResult(Optional<ResourceDefinition> resource, List<ProjectDiagnostic> diagnostics) {}
}
