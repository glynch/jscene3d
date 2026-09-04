/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.extension.internal;

import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isProjectId;
import static io.github.glynch.jscene3d.project.internal.ProjectIdentifiers.isRegisteredTypeId;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.extension.DescriptorPresentation;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.ProjectValueKind;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeScope;
import io.github.glynch.jscene3d.project.internal.DiagnosticCollector;
import io.github.glynch.jscene3d.project.internal.SemanticVersion;
import io.github.glynch.jscene3d.project.internal.SemanticVersionRequirement;
import io.github.glynch.jscene3d.project.internal.ValidationContext;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.project.value.internal.ProjectValueDecoder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Converts nullable extension JSON into safe immutable descriptor metadata. */
public final class ExtensionDescriptorValidator {
    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_URI = "https://jscene3d.org/schemas/extension-1.json";

    private final SemanticVersion engineVersion;
    private final DiagnosticCollector diagnostics;
    private final ValidationContext fields;
    private final ProjectValueDecoder values;

    /** Stores one validation context. */
    private ExtensionDescriptorValidator(URI source, SemanticVersion engineVersion) {
        this.engineVersion = engineVersion;
        diagnostics = new DiagnosticCollector(source);
        fields = new ValidationContext(diagnostics, "extension");
        values = ProjectValueDecoder.plain();
    }

    /**
     * Validates one raw extension descriptor.
     *
     * @param raw nullable deserialization model
     * @param source absolute descriptor resource URI
     * @param engineVersion running engine version
     * @return validated descriptor and ordered diagnostics
     */
    public static ValidationResult validate(RawExtensionDescriptor raw, URI source, SemanticVersion engineVersion) {
        ExtensionDescriptorValidator validator = new ExtensionDescriptorValidator(source, engineVersion);
        Optional<ExtensionDescriptor> descriptor = validator.validate(raw);
        return new ValidationResult(descriptor, validator.diagnostics.diagnostics());
    }

    /** Runs validation in stable descriptor order. */
    private Optional<ExtensionDescriptor> validate(RawExtensionDescriptor raw) {
        validateSchema(raw.schema(), raw.schemaVersion());
        String id = fields.requiredText(raw.id(), "/id");
        if (!id.isEmpty() && !isProjectId(id)) {
            diagnostics.error("extension.id", "id must be a lowercase reverse-domain identifier", "/id");
        }
        String version = fields.requiredText(raw.version(), "/version");
        if (!version.isEmpty() && SemanticVersion.parse(version).isEmpty()) {
            diagnostics.error("extension.version", "version must be a semantic version", "/version");
        }
        String engineRequires = fields.requiredText(raw.engineRequires(), "/engineRequires");
        validateEngineRequirement(engineRequires);
        String displayName = fields.requiredText(raw.displayName(), "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), "/description");
        String safeId = isProjectId(id) ? id : "invalid.extension";
        List<RegisteredTypeDescriptor> types = validateTypes(raw.types(), safeId);
        if (diagnostics.hasErrors()) {
            return Optional.empty();
        }
        DescriptorPresentation presentation = presentation(displayName, description);
        return Optional.of(new ExtensionDescriptor(id, version, engineRequires, presentation, types));
    }

    /** Validates the authoritative schema version and optional canonical URI. */
    private void validateSchema(@Nullable String schema, int schemaVersion) {
        if (schemaVersion != SCHEMA_VERSION) {
            diagnostics.error(
                    "extension.schema.unsupported",
                    "schemaVersion must be " + SCHEMA_VERSION + ": " + schemaVersion,
                    "/schemaVersion");
        }
        if (schema != null && !SCHEMA_URI.equals(schema)) {
            diagnostics.warning(
                    "extension.schema.uri",
                    "$schema does not identify the bundled Extension Descriptor version 1 schema",
                    "/$schema");
        }
    }

    /** Validates the extension's engine requirement and current compatibility. */
    private void validateEngineRequirement(String requirementText) {
        Optional<SemanticVersionRequirement> requirement = SemanticVersionRequirement.parse(requirementText);
        if (!requirementText.isEmpty() && requirement.isEmpty()) {
            diagnostics.error(
                    "extension.engine.requirement",
                    "engineRequires must contain semantic-version comparisons",
                    "/engineRequires");
        } else if (requirement.isPresent() && !requirement.orElseThrow().includes(engineVersion)) {
            diagnostics.error(
                    "extension.engine.incompatible",
                    "extension requires " + requirementText + " but the current engine is incompatible",
                    "/engineRequires");
        }
    }

    /** Validates registered types and removes invalid placeholders from the public result. */
    private List<RegisteredTypeDescriptor> validateTypes(
            @Nullable List<RawExtensionDescriptor.@Nullable Type> rawTypes, String extensionId) {
        if (rawTypes == null) {
            return List.of();
        }
        List<RegisteredTypeDescriptor> types = new ArrayList<>();
        Set<RegisteredType> unique = new HashSet<>();
        for (int index = 0; index < rawTypes.size(); index++) {
            String location = "/types/" + index;
            Optional<RegisteredTypeDescriptor> descriptor = validateType(rawTypes.get(index), extensionId, location);
            if (descriptor.isPresent() && !unique.add(descriptor.orElseThrow().type())) {
                diagnostics.error(
                        "extension.type.duplicate",
                        "registered type is duplicated: "
                                + descriptor.orElseThrow().type(),
                        location);
            } else {
                descriptor.ifPresent(types::add);
            }
        }
        return List.copyOf(types);
    }

    /** Validates one registered type. */
    private Optional<RegisteredTypeDescriptor> validateType(
            RawExtensionDescriptor.@Nullable Type raw, String extensionId, String location) {
        if (raw == null) {
            diagnostics.error("extension.field.required", "registered type must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredText(raw.id(), location + "/id");
        boolean validId = isRegisteredTypeId(id) && id.startsWith(extensionId + '/');
        if (!id.isEmpty() && !validId) {
            diagnostics.error(
                    "extension.type.id",
                    "type id must be qualified by its owning extension: " + extensionId,
                    location + "/id");
        }
        int version = positiveVersion(raw.typeVersion(), location + "/typeVersion");
        RegisteredTypeScope scope = scope(raw.scope(), location + "/scope");
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        List<PropertyDescriptor> properties = validateProperties(raw.properties(), location + "/properties");
        List<EndpointDescriptor> signals = validateEndpoints(raw.signals(), location + "/signals");
        List<EndpointDescriptor> actions = validateEndpoints(raw.actions(), location + "/actions");
        List<String> capabilities = registeredTypeList(raw.requiredCapabilities(), location + "/requiredCapabilities");
        if (!validId || version < 1 || displayName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RegisteredTypeDescriptor(
                new RegisteredType(id, version),
                scope,
                presentation(displayName, description),
                properties,
                signals,
                actions,
                capabilities));
    }

    /** Validates property descriptors. */
    private List<PropertyDescriptor> validateProperties(
            @Nullable List<RawExtensionDescriptor.@Nullable Property> rawProperties, String location) {
        if (rawProperties == null) {
            return List.of();
        }
        List<PropertyDescriptor> properties = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < rawProperties.size(); index++) {
            Optional<PropertyDescriptor> property = validateProperty(rawProperties.get(index), location + "/" + index);
            if (property.isPresent() && !unique.add(property.orElseThrow().id())) {
                diagnostics.error(
                        "extension.property.duplicate",
                        "property is duplicated: " + property.orElseThrow().id(),
                        location + "/" + index + "/id");
            } else {
                property.ifPresent(properties::add);
            }
        }
        return List.copyOf(properties);
    }

    /** Validates one property descriptor. */
    private Optional<PropertyDescriptor> validateProperty(
            RawExtensionDescriptor.@Nullable Property raw, String location) {
        if (raw == null) {
            diagnostics.error("extension.field.required", "property must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredLocalId(raw.id(), location + "/id");
        ProjectValueKind valueKind = valueKind(raw.valueKind(), location + "/valueKind");
        boolean required = raw.required() != null && raw.required();
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        Map<String, ProjectValue> editor = objectValues(raw.editor(), location + "/editor");
        Set<ResourceReference.Kind> acceptedReferences =
                referenceKinds(raw.acceptedReferences(), valueKind, location + "/acceptedReferences");
        Optional<ProjectValue> defaultValue = optionalValue(raw.defaultValue(), location + "/defaultValue");
        if (defaultValue.isPresent() && !accepts(valueKind, acceptedReferences, defaultValue.orElseThrow())) {
            diagnostics.error(
                    "extension.property.default",
                    "defaultValue does not satisfy valueKind and acceptedReferences",
                    location + "/defaultValue");
            defaultValue = Optional.empty();
        }
        if (required && defaultValue.isPresent()) {
            diagnostics.error(
                    "extension.property.required-default",
                    "a required property cannot also declare a defaultValue",
                    location);
        }
        if (id.isEmpty() || displayName.isEmpty()) {
            return Optional.empty();
        }
        DescriptorPresentation metadata = presentation(displayName, description);
        if (required) {
            return Optional.of(PropertyDescriptor.required(id, valueKind, metadata, editor, acceptedReferences));
        }
        if (defaultValue.isPresent()) {
            return Optional.of(PropertyDescriptor.optionalWithDefault(
                    id, valueKind, defaultValue.orElseThrow(), metadata, editor, acceptedReferences));
        }
        return Optional.of(PropertyDescriptor.optional(id, valueKind, metadata, editor, acceptedReferences));
    }

    /** Validates signal or action descriptors. */
    private List<EndpointDescriptor> validateEndpoints(
            @Nullable List<RawExtensionDescriptor.@Nullable Endpoint> rawEndpoints, String location) {
        if (rawEndpoints == null) {
            return List.of();
        }
        List<EndpointDescriptor> endpoints = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < rawEndpoints.size(); index++) {
            Optional<EndpointDescriptor> endpoint = validateEndpoint(rawEndpoints.get(index), location + "/" + index);
            if (endpoint.isPresent() && !unique.add(endpoint.orElseThrow().id())) {
                diagnostics.error(
                        "extension.endpoint.duplicate",
                        "endpoint is duplicated: " + endpoint.orElseThrow().id(),
                        location + "/" + index + "/id");
            } else {
                endpoint.ifPresent(endpoints::add);
            }
        }
        return List.copyOf(endpoints);
    }

    /** Validates one signal or action descriptor. */
    private Optional<EndpointDescriptor> validateEndpoint(
            RawExtensionDescriptor.@Nullable Endpoint raw, String location) {
        if (raw == null) {
            diagnostics.error("extension.field.required", "endpoint must be an object", location);
            return Optional.empty();
        }
        String id = fields.requiredLocalId(raw.id(), location + "/id");
        String displayName = fields.requiredText(raw.displayName(), location + "/displayName");
        Optional<String> description = fields.optionalText(raw.description(), location + "/description");
        Optional<RegisteredType> payload = registeredType(raw.payload(), location + "/payload");
        if (id.isEmpty() || displayName.isEmpty()) {
            return Optional.empty();
        }
        DescriptorPresentation metadata = presentation(displayName, description);
        return Optional.of(
                payload.isPresent()
                        ? EndpointDescriptor.withPayload(id, payload.orElseThrow(), metadata)
                        : EndpointDescriptor.withoutPayload(id, metadata));
    }

    /** Validates one optional registered payload type. */
    private Optional<RegisteredType> registeredType(
            RawExtensionDescriptor.@Nullable RegisteredType raw, String location) {
        if (raw == null) {
            return Optional.empty();
        }
        String id = fields.requiredText(raw.type(), location + "/type");
        if (!id.isEmpty() && !isRegisteredTypeId(id)) {
            diagnostics.error(
                    "extension.endpoint.payload", "payload type must be extension-qualified", location + "/type");
        }
        int version = positiveVersion(raw.typeVersion(), location + "/typeVersion");
        return isRegisteredTypeId(id) && version > 0 ? Optional.of(new RegisteredType(id, version)) : Optional.empty();
    }

    /** Validates one positive type version. */
    private int positiveVersion(@Nullable Integer version, String location) {
        int value = version == null ? 0 : version;
        if (value < 1) {
            diagnostics.error("extension.type.version", "typeVersion must be positive", location);
        }
        return value;
    }

    /** Parses one registered type scope. */
    private RegisteredTypeScope scope(@Nullable String value, String location) {
        String text = fields.requiredText(value, location);
        try {
            return RegisteredTypeScope.valueOf(text.replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    "extension.type.scope",
                    "scope must be scene-node, node-controller, project-system, resource, or importer",
                    location);
            return RegisteredTypeScope.RESOURCE;
        }
    }

    /** Parses one portable project-value kind. */
    private ProjectValueKind valueKind(@Nullable String value, String location) {
        String text = fields.requiredText(value, location);
        try {
            return ProjectValueKind.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            diagnostics.error(
                    "extension.property.kind",
                    "valueKind must be null, boolean, number, text, array, object, or reference",
                    location);
            return ProjectValueKind.NULL;
        }
    }

    /** Parses optional reference namespace constraints. */
    private Set<ResourceReference.Kind> referenceKinds(
            @Nullable List<@Nullable String> values, ProjectValueKind valueKind, String location) {
        if (values == null) {
            return Set.of();
        }
        Set<ResourceReference.Kind> result = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String value = fields.requiredText(values.get(index), location + "/" + index);
            try {
                ResourceReference.Kind kind = ResourceReference.Kind.valueOf(value.toUpperCase(Locale.ROOT));
                if (!result.add(kind)) {
                    diagnostics.error(
                            "extension.property.reference-duplicate",
                            "accepted reference namespace is duplicated: " + value,
                            location + "/" + index);
                }
            } catch (IllegalArgumentException ignored) {
                diagnostics.error(
                        "extension.property.reference-kind",
                        "accepted reference namespace must be project, asset, or import",
                        location + "/" + index);
            }
        }
        if (valueKind != ProjectValueKind.REFERENCE && !result.isEmpty()) {
            diagnostics.error(
                    "extension.property.reference-kind", "acceptedReferences require valueKind reference", location);
            return Set.of();
        }
        return Set.copyOf(result);
    }

    /** Validates an ordered list of extension-qualified type identifiers. */
    private List<String> registeredTypeList(@Nullable List<@Nullable String> values, String location) {
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String value = fields.requiredText(values.get(index), location + "/" + index);
            if (!value.isEmpty() && !isRegisteredTypeId(value)) {
                diagnostics.error(
                        "extension.capability.id",
                        "capability must be an extension-qualified identifier",
                        location + "/" + index);
            } else if (!value.isEmpty() && !unique.add(value)) {
                diagnostics.error(
                        "extension.capability.duplicate",
                        "required capability is duplicated: " + value,
                        location + "/" + index);
            } else if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    /** Returns whether a default value satisfies property constraints. */
    private static boolean accepts(
            ProjectValueKind kind, Set<ResourceReference.Kind> referenceKinds, ProjectValue value) {
        if (ProjectValueKind.of(value) != kind) {
            return false;
        }
        return !(value instanceof ProjectValue.ReferenceValue reference)
                || referenceKinds.isEmpty()
                || referenceKinds.contains(reference.reference().kind());
    }

    /** Converts an optional raw JSON value without interpreting resource references. */
    private Optional<ProjectValue> optionalValue(@Nullable JsonNode raw, String location) {
        return raw == null ? Optional.empty() : Optional.of(values.decode(raw, location));
    }

    /** Converts an optional JSON object to generic editor metadata. */
    private Map<String, ProjectValue> objectValues(@Nullable JsonNode raw, String location) {
        if (raw == null) {
            return Map.of();
        }
        if (!raw.isObject()) {
            diagnostics.error("extension.editor.object", "editor metadata must be an object", location);
            return Map.of();
        }
        return values.decodeObject(raw, location).values();
    }

    /** Builds presentation metadata from already validated values. */
    private static DescriptorPresentation presentation(String displayName, Optional<String> description) {
        return description.isPresent()
                ? DescriptorPresentation.described(displayName, description.orElseThrow())
                : DescriptorPresentation.named(displayName);
    }

    /** Validated extension descriptor and ordered diagnostics.
     *
     * @param descriptor validated descriptor when no errors occurred
     * @param diagnostics immutable ordered diagnostics
     */
    public record ValidationResult(Optional<ExtensionDescriptor> descriptor, List<ProjectDiagnostic> diagnostics) {}
}
